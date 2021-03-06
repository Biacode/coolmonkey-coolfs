package com.sfl.coolmonkey.coolfs.facade.storage.impl;

import com.sfl.coolmonkey.commons.api.model.CommonErrorType;
import com.sfl.coolmonkey.commons.api.model.response.ResultResponseModel;
import com.sfl.coolmonkey.coolfs.api.model.storage.FileLoadModel;
import com.sfl.coolmonkey.coolfs.api.model.storage.FileOriginModel;
import com.sfl.coolmonkey.coolfs.api.model.storage.FileUploadModel;
import com.sfl.coolmonkey.coolfs.api.model.storage.StoredFileInfoModel;
import com.sfl.coolmonkey.coolfs.api.model.storage.request.*;
import com.sfl.coolmonkey.coolfs.api.model.storage.response.*;
import com.sfl.coolmonkey.coolfs.facade.storage.StorageFacade;
import com.sfl.coolmonkey.coolfs.facade.storage.component.StorageFacadeConversionComponent;
import com.sfl.coolmonkey.coolfs.facade.storage.component.StorageFacadeValidationComponent;
import com.sfl.coolmonkey.coolfs.facade.test.AbstractFacadeImplTest;
import com.sfl.coolmonkey.coolfs.service.storage.StorageService;
import com.sfl.coolmonkey.coolfs.service.storage.dto.FileMetaDataDto;
import com.sfl.coolmonkey.coolfs.service.storage.dto.FileStoreDto;
import com.sfl.coolmonkey.coolfs.service.storage.model.FileOrigin;
import com.sfl.coolmonkey.coolfs.service.storage.model.FileStoreData;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.*;

/**
 * User: Arthur Asatryan
 * Company: SFL LLC
 * Date: 2/18/16
 * Time: 4:34 PM
 */
public class StorageFacadeImplTest extends AbstractFacadeImplTest {

    //region Test subject and mocks
    @TestSubject
    private final StorageFacade storageFacade = new StorageFacadeImpl();

    @Mock
    private StorageService storageService;

    @Mock
    private StorageFacadeConversionComponent storageFacadeConversionComponent;

    @Mock
    private StorageFacadeValidationComponent storageFacadeValidationComponent;
    //endregion

    //region Constructors
    public StorageFacadeImplTest() {
    }
    //endregion

    //region Test methods

    //region upload
    @Test
    public void testUploadWithInvalidArguments() {
        // Test data
        final String validUuid = UUID.randomUUID().toString();
        final FileUploadModel validFileUploadModel = new FileUploadModel(
                IOUtils.toInputStream("hi"),
                "fileName",
                "text/txt",
                FileOriginModel.IMPORT_CSV
        );
        // Reset
        resetAll();
        // Expectations
        // Replay
        replayAll();
        // Run test scenario
        try {
            storageFacade.upload(null);
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        try {
            storageFacade.upload(new UploadFileRequest(validUuid, null));
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        FileUploadModel invalidFileUploadModel = new FileUploadModel(null, "fileName", "text/txt", FileOriginModel.IMPORT_CSV);
        try {
            storageFacade.upload(new UploadFileRequest(validUuid, invalidFileUploadModel));
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        invalidFileUploadModel = new FileUploadModel(IOUtils.toInputStream("hi"), null, "text/txt", FileOriginModel.IMPORT_CSV);
        try {
            storageFacade.upload(new UploadFileRequest(validUuid, invalidFileUploadModel));
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        invalidFileUploadModel = new FileUploadModel(IOUtils.toInputStream("hi"), "fileName", "text/txt", null);
        try {
            storageFacade.upload(new UploadFileRequest(validUuid, invalidFileUploadModel));
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        // Verify
        verifyAll();
    }

    @Test
    public void testUploadWhenFileLengthIsGreaterThanFileMaxLength() {
        // Test data
        final String companyUuid = UUID.randomUUID().toString();
        final FileUploadModel uploadModel = getHelper().createFileUploadModel();
        final String uuid = uploadModel.getUuid();
        final UploadFileRequest request = new UploadFileRequest(companyUuid, uploadModel);
        request.setMaxFileLength((long) 2 * 1024);
        final FileStoreData fileStoreData = getHelper().createFileStoreData();
        final FileStoreDto dto = new FileStoreDto(
                uploadModel.getInputStream(),
                uploadModel.getFileName(),
                uploadModel.getContentType(),
                new FileMetaDataDto()
        );
        final Map<CommonErrorType, Object> errors = new HashMap<>();
        errors.put(CommonErrorType.IMPORT_FILE_MAX_SIZE_EXCEEDED, null);
        // Reset
        resetAll();
        // Expectations
        expect(storageFacadeConversionComponent.buildFileStoreDtoFromUploadFileModel(uploadModel)).andReturn(dto);
        expect(storageService.create(dto)).andReturn(uuid);
        expect(storageService.getByMetaUuid(uuid)).andReturn(fileStoreData);
        expect(storageFacadeValidationComponent.validateFileMaxLength(fileStoreData, request.getMaxFileLength())).andReturn(errors);
        storageService.deleteByMetaUuid(uuid);
        // Replay
        replayAll();
        // Run test scenario
        final ResultResponseModel<UploadFileResponse> result = storageFacade.upload(request);
        // Verify
        verifyAll();
        assertNotNull(result);
        assertTrue(result.hasErrors());
        getHelper().assertValidationErrors(result, Collections.singleton(CommonErrorType.IMPORT_FILE_MAX_SIZE_EXCEEDED));
    }

    @Test
    public void testUpload() {
        // Test data
        final String companyUuid = UUID.randomUUID().toString();
        final FileUploadModel uploadModel = getHelper().createFileUploadModel();
        final String uuid = uploadModel.getUuid();
        final UploadFileRequest request = new UploadFileRequest(companyUuid, uploadModel);
        final FileStoreData fileStoreData = getHelper().createFileStoreData();
        final FileStoreDto dto = new FileStoreDto(
                uploadModel.getInputStream(),
                uploadModel.getFileName(),
                uploadModel.getContentType(),
                new FileMetaDataDto()
        );
        final StoredFileInfoModel infoModel = getHelper().createStoredFileInfoModel();
        // Reset
        resetAll();
        // Expectations
        expect(storageFacadeConversionComponent.buildFileStoreDtoFromUploadFileModel(uploadModel)).andReturn(dto);
        expect(storageService.create(dto)).andReturn(uploadModel.getUuid());
        expect(storageService.getByMetaUuid(uuid)).andReturn(fileStoreData);
        expect(storageFacadeValidationComponent.validateFileMaxLength(fileStoreData, request.getMaxFileLength())).andReturn(Collections.emptyMap());
        expect(storageFacadeConversionComponent.buildStoredFileInfoModelFromFileStoreData(fileStoreData)).andReturn(infoModel);
        // Replay
        replayAll();
        // Run test scenario
        final ResultResponseModel<UploadFileResponse> result = storageFacade.upload(request);
        // Verify
        verifyAll();
        assertNotNull(result);
        assertNotNull(result.getResponse());
        assertEquals(result.getResponse().getFileInfo(), infoModel);
        assertEquals(companyUuid, dto.getFileMetaDataDto().getCompanyUuid());
    }
    //endregion

    //region getFileInfoByUuid
    @Test
    public void testGetFileInfoByUuidWithInvalidArguments() {
        // Test data
        // Reset
        resetAll();
        // Expectations
        // Replay
        replayAll();
        // Run test scenario
        try {
            storageFacade.getFileInfoByUuid(null);
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        try {
            storageFacade.getFileInfoByUuid(new GetFileInfoByUuidRequest(null));
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        // Verify
        verifyAll();
    }

    @Test
    public void testGetFileInfoByUuid() {
        // Test data
        final GetFileInfoByUuidRequest request = new GetFileInfoByUuidRequest(UUID.randomUUID().toString());
        final FileStoreData fileStoreData = getHelper().createFileStoreData();
        final StoredFileInfoModel model = getHelper().createStoredFileInfoModel();
        // Reset
        resetAll();
        // Expectations
        expect(storageService.getByMetaUuid(request.getUuid())).andReturn(fileStoreData);
        expect(storageFacadeConversionComponent.buildStoredFileInfoModelFromFileStoreData(fileStoreData)).andReturn(model);
        // Replay
        replayAll();
        // Run test scenario
        final ResultResponseModel<GetFileInfoByUuidResponse> result = storageFacade.getFileInfoByUuid(request);
        assertNotNull(result);
        assertNotNull(result.getResponse());
        assertEquals(model, result.getResponse().getFileInfo());
        // Verify
        verifyAll();
    }
    //endregion

    //region getFileInfoByUuids
    @Test
    public void testGetFileInfoByUuidsWithInvalidArguments() {
        // Test data
        // Reset
        resetAll();
        // Expectations
        // Replay
        replayAll();
        // Run test scenario
        try {
            storageFacade.getFileInfoByUuids(null);
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        try {
            storageFacade.getFileInfoByUuids(new GetFileInfoByUuidListRequest(null));
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        try {
            storageFacade.getFileInfoByUuids(new GetFileInfoByUuidListRequest(Collections.singletonList(null)));
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        // Verify
        verifyAll();
    }

    @Test
    public void testGetFileInfoByUuids() {
        // Test data
        final List<FileStoreData> fileStoreDataList = Collections.singletonList(getHelper().createFileStoreData());
        final List<String> uuids = fileStoreDataList.stream().map(FileStoreData::getUuid).collect(Collectors.toList());
        final GetFileInfoByUuidListRequest request = new GetFileInfoByUuidListRequest(uuids);
        final List<StoredFileInfoModel> models = Collections.singletonList(getHelper().createStoredFileInfoModel());
        // Reset
        resetAll();
        // Expectations
        expect(storageService.getByMetaUuids(request.getUuids())).andReturn(fileStoreDataList);
        expect(storageFacadeConversionComponent.buildStoredFileInfoModelsFromFileStoreDataList(fileStoreDataList)).andReturn(models);
        // Replay
        replayAll();
        // Run test scenario
        final ResultResponseModel<GetFileInfoByUuidListResponse> result = storageFacade.getFileInfoByUuids(request);
        assertNotNull(result);
        assertNotNull(result.getResponse());
        assertEquals(models, result.getResponse().getFilesInfo());
        // Verify
        verifyAll();
    }
    //endregion

    //region loadFileByUuid
    @Test
    public void testLoadFileByUuidWithInvalidArguments() {
        // Test data
        // Reset
        resetAll();
        // Expectations
        // Replay
        replayAll();
        // Run test scenario
        try {
            storageFacade.loadFileByUuid(null);
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        try {
            storageFacade.loadFileByUuid(new LoadFileByUuidRequest(null));
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        // Verify
        verifyAll();
    }

    @Test
    public void testLoadFileByUuid() {
        // Test data
        final FileStoreData fileStoreData = getHelper().createFileStoreData();
        final LoadFileByUuidRequest request = new LoadFileByUuidRequest(UUID.randomUUID().toString());
        final FileLoadModel fileLoadModel = getHelper().createFileLoadModel();
        // Reset
        resetAll();
        // Expectations
        expect(storageService.getByMetaUuid(request.getUuid())).andReturn(fileStoreData);
        // Replay
        replayAll();
        // Run test scenario
        final ResultResponseModel<LoadFileByUuidResponse> result = storageFacade.loadFileByUuid(request);
        assertNotNull(result);
        assertNotNull(result.getResponse());
        assertEquals(fileLoadModel, result.getResponse().getLoadFileModel());
        // Verify
        verifyAll();
    }
    //endregion

    //region checkImportAlreadyUploaded
    @Test
    public void testCheckImportAlreadyUploadedWithInvalidArguments() {
        // Test data
        final CheckImportAlreadyUploadedRequest validRequest = new CheckImportAlreadyUploadedRequest(
                UUID.randomUUID().toString(),
                "fileName",
                new Date()
        );
        CheckImportAlreadyUploadedRequest invalidRequest;
        // Reset
        resetAll();
        // Expectations
        // Replay
        replayAll();
        // Run test scenario
        try {
            storageFacade.checkImportAlreadyUploaded(null);
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ignore) {
        }
        invalidRequest = SerializationUtils.clone(validRequest);
        invalidRequest.setCompanyUuid(null);
        try {
            storageFacade.checkImportAlreadyUploaded(invalidRequest);
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ignore) {
        }
        invalidRequest = SerializationUtils.clone(validRequest);
        invalidRequest.setCreatedAfter(null);
        try {
            storageFacade.checkImportAlreadyUploaded(invalidRequest);
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ignore) {
        }
        invalidRequest = SerializationUtils.clone(validRequest);
        invalidRequest.setFileName(null);
        try {
            storageFacade.checkImportAlreadyUploaded(invalidRequest);
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ignore) {
        }
        // Verify
        verifyAll();
    }

    @Test
    public void testCheckImportAlreadyUploadedWhenDoesNotExist() {
        // Test data
        final String companyUuid = UUID.randomUUID().toString();
        final String fileName = "fileName";
        final Date createdAfter = new Date();
        final CheckImportAlreadyUploadedRequest request = new CheckImportAlreadyUploadedRequest(
                companyUuid,
                fileName,
                createdAfter
        );
        // Reset
        resetAll();
        // Expectations
        expect(storageService.getByCompanyUuidAndFileNameAndCreatedAfterAndOrigin(companyUuid, fileName, createdAfter, FileOrigin.IMPORT_CSV)).andReturn(Collections.emptyList());
        // Replay
        replayAll();
        // Run test scenario
        final ResultResponseModel<CheckImportAlreadyUploadedResponse> response = storageFacade.checkImportAlreadyUploaded(request);
        // Verify
        verifyAll();
        assertNotNull(response);
        assertFalse(response.hasErrors());
        assertNotNull(response.getResponse());
        assertEquals(response.getResponse().getUuidList().size(), 0);
    }

    @Test
    public void testCheckImportAlreadyUploadedWhenExists() {
        // Test data
        final String companyUuid = UUID.randomUUID().toString();
        final String fileName = "fileName";
        final Date createdAfter = new Date();
        final CheckImportAlreadyUploadedRequest request = new CheckImportAlreadyUploadedRequest(
                companyUuid,
                fileName,
                createdAfter
        );
        final List<FileStoreData> fileStoreDatas = Collections.singletonList(
                getHelper().createFileStoreData()
        );
        // Reset
        resetAll();
        // Expectations
        expect(storageService.getByCompanyUuidAndFileNameAndCreatedAfterAndOrigin(companyUuid, fileName, createdAfter, FileOrigin.IMPORT_CSV)).andReturn(fileStoreDatas);
        // Replay
        replayAll();
        // Run test scenario
        final ResultResponseModel<CheckImportAlreadyUploadedResponse> response = storageFacade.checkImportAlreadyUploaded(request);
        // Verify
        verifyAll();
        assertNotNull(response);
        assertFalse(response.hasErrors());
        assertNotNull(response.getResponse());
        assertEquals(response.getResponse().getUuidList(), fileStoreDatas.stream().map(FileStoreData::getUuid).collect(Collectors.toList()));
    }
    //endregion

    //endregion
}