package org.shanoir.ng.datasetfile;

import java.io.File;
import java.time.LocalDateTime;
import javax.validation.Valid;

import org.apache.commons.io.FileUtils;
import org.shanoir.ng.datasetfile.service.DatasetFileApi;
import org.shanoir.ng.datasetfile.service.DatasetFileService;
import org.shanoir.ng.migration.DatasetMigrationService;
import org.shanoir.ng.shared.exception.ErrorModel;
import org.shanoir.ng.shared.exception.RestServiceException;
import org.shanoir.ng.shared.migration.MigrationConstants;
import org.shanoir.ng.shared.service.DicomServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiParam;

@Controller
public class DatasetFileApiController implements DatasetFileApi {

	@Autowired
	private DatasetFileService datasetFileService;

	@Autowired
	@Qualifier("stowrs")
	private DicomServiceApi stowRsService;

	@Autowired
	@Qualifier("cstore")
	private DicomServiceApi cStoreService;

	@Value("${dcm4chee-arc.protocol}")
	private String dcm4cheeProtocol;

	@Value("${dcm4chee-arc.host}")
	private String dcm4cheeHost;

	@Value("${dcm4chee-arc.port.web}")
	private String dcm4cheePortWeb;

	@Value("${dcm4chee-arc.dicom.web}")
	private boolean dicomWeb;

	@Value("${migration-folder}")
	private String migrationFolder;

	@Value("${dcm4chee-arc.dicom.wado.uri}")
	private String dicomWADOURI;

	@Value("${dcm4chee-arc.dicom.web.rs}")
	private String dicomWebRS;

	private static final Logger LOG = LoggerFactory.getLogger(DatasetFileApiController.class);

	@Override
	public 	ResponseEntity<DatasetFile> saveNewDatasetFile(
			@ApiParam(value = "datasetfile to create", required = true) @RequestBody DatasetFile datasetFile,
			BindingResult result) throws RestServiceException  {
		try {
			// Save file
			if (datasetFile.isPacs()) {

				String path = datasetFile.getPath();
				path = path.replace(MigrationConstants.DCM4CHEE_PROTOCOL_CONSTANT, dcm4cheeProtocol);
				path = path.replace(MigrationConstants.DCM4CHEE_HOST_CONSTANT, dcm4cheeHost);
				path = path.replace(MigrationConstants.DCM4CHEE_PORT_CONSTANT, dcm4cheePortWeb);

				// This may have to be changes in case old is dicom and new is not
				if (path.contains(MigrationConstants.DCM4CHEE_WADO_URI_CONSTANT)) {
					if (dicomWeb) {
						path = path.replace(MigrationConstants.DCM4CHEE_WADO_URI_CONSTANT, dicomWebRS);
					} else {
						path = path.replace(MigrationConstants.DCM4CHEE_WADO_URI_CONSTANT, dicomWADOURI);
					}
				} else if (path.contains(MigrationConstants.DCM4CHEE_WEB_RS_CONSTANT)){
					if (dicomWeb) {
						path = path.replace(MigrationConstants.DCM4CHEE_WEB_RS_CONSTANT, dicomWebRS);
					} else {
						path = path.replace(MigrationConstants.DCM4CHEE_WEB_RS_CONSTANT, dicomWADOURI);
					}
				}
				datasetFile.setPath(path);
			}

			DatasetFile createdFile = datasetFileService.create(datasetFile);

			return new ResponseEntity<>(createdFile, HttpStatus.OK);
		} catch (Exception e) {
			throw new RestServiceException(e, new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error while adding dataset file."));
		}
	}

	public ResponseEntity<Void> addFilesToPacs(
			@ApiParam(value = "id of the dataset file", required = true) @PathVariable("datasetFileId") Long datasetFileId)
					throws RestServiceException {
		// Transfer to pacs
		DatasetFile datasetFile = datasetFileService.findById(datasetFileId).orElse(null);

		File expressionFolder = new File(migrationFolder + "/migration-" + datasetFile.getDatasetExpression().getId());
		try {
			if (dicomWeb) {
				stowRsService.sendDicomFilesToPacs(expressionFolder);
			} else {
				cStoreService.sendDicomFilesToPacs(expressionFolder);
			}
		}
		catch (Exception e) {
			throw new RestServiceException(e, new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Could not load files to PACS."));
		} finally {
			FileUtils.deleteQuietly(expressionFolder);
		}
		return new ResponseEntity<Void>(HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Void> addFile(
			@ApiParam(value = "id of the dataset file", required = true) @PathVariable("datasetFileId") Long datasetFileId,
			@ApiParam(value = "file to upload", required = true) @Valid @RequestBody MultipartFile file)
					throws RestServiceException {
		DatasetFile datasetFile = datasetFileService.findById(datasetFileId).orElse(null);
		File destination = null;
		try {
			destination = new File(migrationFolder + "/migration-" + datasetFile.getDatasetExpression().getId() + File.separator +  LocalDateTime.now() + file.getName());
			if (datasetFile.isPacs()) {
				// Copy file to load it in the PACS
				destination.getParentFile().mkdirs();
				file.transferTo(destination);
			} else {
				// Get the dataset file then copy the file to path
				// MOVE nifti (and others) on disc
				destination = new File(datasetFile.getPath().replace("file://", ""));
				destination.getParentFile().mkdirs();
				file.transferTo(destination);
			}

			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			throw new RestServiceException(e, new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error while adding dataset file."));
		}
	}
}
