/**
< * Shanoir NG - Import, manage and share neuroimaging data
 * Copyright (C) 2009-2019 Inria - https://www.inria.fr/
 * Contact us on https://project.inria.fr/shanoir/
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/gpl-3.0.html
 */

package org.shanoir.ng.importer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipOutputStream;

import javax.validation.Valid;

import org.shanoir.ng.exchange.imports.dicom.DicomDirGeneratorService;
import org.shanoir.ng.exchange.model.ExExamination;
import org.shanoir.ng.exchange.model.ExStudy;
import org.shanoir.ng.exchange.model.ExStudyCard;
import org.shanoir.ng.exchange.model.ExSubject;
import org.shanoir.ng.exchange.model.Exchange;
import org.shanoir.ng.importer.dicom.DicomDirCreator;
import org.shanoir.ng.importer.dicom.DicomImporterService;
import org.shanoir.ng.importer.dicom.ImagesCreatorAndDicomFileAnalyzerService;
import org.shanoir.ng.importer.dicom.query.DicomQuery;
import org.shanoir.ng.importer.dicom.query.QueryPACSService;
import org.shanoir.ng.importer.dto.CommonIdNamesDTO;
import org.shanoir.ng.importer.dto.CommonIdsDTO;
import org.shanoir.ng.importer.dto.ExaminationDTO;
import org.shanoir.ng.importer.eeg.EEGImporterService;
import org.shanoir.ng.importer.model.EegDataset;
import org.shanoir.ng.importer.model.EegImportJob;
import org.shanoir.ng.importer.model.ImportJob;
import org.shanoir.ng.importer.model.Patient;
import org.shanoir.ng.importer.model.Serie;
import org.shanoir.ng.importer.model.Study;
import org.shanoir.ng.importer.model.Subject;
import org.shanoir.ng.importer.nifti.NiftiImporterService;
import org.shanoir.ng.shared.configuration.RabbitMQConfiguration;
import org.shanoir.ng.shared.core.model.IdName;
import org.shanoir.ng.shared.exception.ErrorModel;
import org.shanoir.ng.shared.exception.RestServiceException;
import org.shanoir.ng.shared.exception.ShanoirException;
import org.shanoir.ng.shared.exception.ShanoirImportException;
import org.shanoir.ng.study.rights.StudyUser;
import org.shanoir.ng.study.rights.StudyUserInterface;
import org.shanoir.ng.utils.ImportUtils;
import org.shanoir.ng.utils.KeycloakUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.swagger.annotations.ApiParam;

/**
 * This is the main component of the import of Shanoir-NG. The front-end in
 * Angular only communicates with this service. The import ms itself is calling
 * the ms datasets service.
 * 
 * The Import MS returns only a random ID to the outside world for one import.
 * Internally each user has its own folder in the importDirectory. So, when the
 * workFolder in the ImportJob is set to be returned, there is only the random
 * ID. When the requests arrive MS Import is adding the userId and the real path
 * value.
 * 
 * @author mkain
 *
 */
@Controller
public class ImporterApiController implements ImporterApi {

	private static final String WRONG_CONTENT_FILE_UPLOAD = "Wrong content type of file upload, .zip required.";

	private static final String ERROR_WHILE_SAVING_UPLOADED_FILE = "Error while saving uploaded file.";

	private static final String NO_FILE_UPLOADED = "No file uploaded.";

	private static final Logger LOG = LoggerFactory.getLogger(ImporterApiController.class);

	private static final String APPLICATION_ZIP = "application/zip";

	/** The Constant KB. */
	private static final int KB = 1024;

	/** The Constant BUFFER_SIZE. */
	private static final int BUFFER_SIZE = 10 * KB;

	@Value("${ms.url.shanoir-ng-datasets-eeg}")
	private String datasetsEegMsUrl;

	@Value("${ms.url.shanoir-ng-datasets-nifti}")
	private String datasetsNiftiMsUrl;

	@Value("${ms.url.shanoir-ng-studies-commons}")
	private String studiesCommonMsUrl;

	@Value("${ms.url.shanoir-ng-studies-subjects-names}")
	private String studiesSubjectsNamesMsUrl;

	@Value("${ms.url.shanoir-ng-create-examination}")
	private String createExaminationMsUrl;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private DicomDirGeneratorService dicomDirGeneratorService;

	@Autowired
	private ImagesCreatorAndDicomFileAnalyzerService imagesCreatorAndDicomFileAnalyzer;

	@Autowired
	private ImporterManagerService importerManagerService;

	@Autowired
	private QueryPACSService queryPACSService;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private EEGImporterService eegImporterService;

	@Autowired
	private DicomImporterService dicomImporterService;

	@Autowired
	private NiftiImporterService niftiImporterService;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public ResponseEntity<String> createTempDir() throws RestServiceException {
		final File userImportDir = dicomImporterService.getUserImportDir();
		long n = dicomImporterService.createRandomLong();
		File tempDirForImport = new File(userImportDir, Long.toString(n));
		if (!tempDirForImport.exists()) {
			tempDirForImport.mkdirs();
		} else {
			throw new RestServiceException(new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(),
					"Error while creating temp dir: random number generated twice?", null));
		}
		return new ResponseEntity<>(tempDirForImport.getName(), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ImportJob> uploadDicomZipFile(
			@ApiParam(value = "file detail") @RequestPart("file") final MultipartFile dicomZipFile)
					throws RestServiceException {
		if (dicomZipFile == null) {
			throw new RestServiceException(
					new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), WRONG_CONTENT_FILE_UPLOAD, null));
		}
		if (!dicomImporterService.isZipFile(dicomZipFile)) {
			throw new RestServiceException(new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(),
					"Wrong content type of file upload, .zip required.", null));
		}
		try {
			/**
			 * 1. STEP: Handle file management. Always create a userId specific folder in
			 * the import work folder (the root of everything): split imports to clearly
			 * separate them into separate folders for each user
			 */
			File userImportDir = dicomImporterService.getUserImportDir();
			File importJobDir = dicomImporterService.saveTempFileCreateFolderAndUnzip(userImportDir, dicomZipFile, true);

			/**
			 * 2. STEP: prepare patients list to be put into ImportJob: read DICOMDIR and
			 * complete with meta-data from files
			 */
			List<Patient> patients = dicomImporterService.preparePatientsForImportJob(importJobDir);

			/**
			 * 3. STEP: split instances into non-images and images and get additional meta-data
			 * from first dicom file of each serie, meta-data missing in dicomdir.
			 */
			imagesCreatorAndDicomFileAnalyzer.createImagesAndAnalyzeDicomFiles(patients, importJobDir.getAbsolutePath(), false);

			/**
			 * . STEP: create ImportJob
			 */
			ImportJob importJob = new ImportJob();
			importJob.setFromDicomZip(true);
			// Work folder is always relative to general import directory
			importJob.setWorkFolder(importJobDir.getName());
			importJob.setPatients(patients);
			return new ResponseEntity<>(importJob, HttpStatus.OK);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			throw new RestServiceException(
					new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), ERROR_WHILE_SAVING_UPLOADED_FILE, null));
		}
	}

	@Override
	public ResponseEntity<ImportJob> uploadNiftiZipFile(@ApiParam(value = "file detail") @RequestPart("file") MultipartFile niftiZipFile) throws RestServiceException {
		// Step 1 check zip file
		if (niftiZipFile == null) {
			throw new RestServiceException(
					new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), WRONG_CONTENT_FILE_UPLOAD, null));
		}
		if (!dicomImporterService.isZipFile(niftiZipFile)) {
			throw new RestServiceException(new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(),
					"Wrong content type of file upload, .zip required.", null));
		}
		try {
			/**
			 * STEP 2: Handle file management. Always create a userId specific folder in
			 * the import work folder (the root of everything): split imports to clearly
			 * separate them into separate folders for each user
			 */
			File userImportDir = dicomImporterService.getUserImportDir();
			File importJobDir = dicomImporterService.saveTempFileCreateFolderAndUnzip(userImportDir, niftiZipFile, false);

			/**
			 * STEP 3: prepare patients list to be put into ImportJob: read DICOMDIR and
			 * complete with meta-data from files
			 */
			// TODO: Check that the NIFTI is a nifti, otherwise, send an error ? => Or this is checked later ?

			/**
			 * STEP 4: create ImportJob
			 */
			ImportJob importJob = new ImportJob();
			importJob.setFromDicomZip(true);
			// Work folder is always relative to general import directory
			importJob.setWorkFolder(importJobDir.getName());
			return new ResponseEntity<>(importJob, HttpStatus.OK);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			throw new RestServiceException(
					new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), ERROR_WHILE_SAVING_UPLOADED_FILE, null));
		}
	}

	@Override
	public ResponseEntity<Void> startImportJob(
			@ApiParam(value = "ImportJob", required = true) @Valid @RequestBody final ImportJob importJob)
					throws RestServiceException {
		// TOOD: precise in the title that it's DICOM
		File userImportDir = dicomImporterService.getUserImportDir();
		final Long userId = KeycloakUtil.getTokenUserId();
		String tempDirId = importJob.getWorkFolder();
		final File importJobDir = new File(userImportDir, tempDirId);
		if (importJobDir.exists()) {
			importJob.setWorkFolder(importJobDir.getAbsolutePath());
			if (!importJob.isFromShanoirUploader()) {
				importJob.setAnonymisationProfileToUse("Profile Neurinfo");
			}
			dicomImporterService.removeUnselectedSeries(importJob);
			importerManagerService.manageImportJob(userId, KeycloakUtil.getKeycloakHeader(), importJob);
			return new ResponseEntity<>(HttpStatus.OK);
		} else {
			LOG.error("Missing importJobDir.");
			throw new RestServiceException(
					new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Missing importJobDir.", null));
		}
	}

	@Override
	public ResponseEntity<ImportJob> queryPACS(
			@ApiParam(value = "DicomQuery", required = true) @Valid @RequestBody final DicomQuery dicomQuery)
					throws RestServiceException {
		ImportJob importJob;
		try {
			importJob = queryPACSService.queryCFIND(dicomQuery);
			// the pacs workfolder is empty here, as multiple queries could be asked before
			// string an import
			importJob.setWorkFolder("");
			importJob.setFromPacs(true);
		} catch (ShanoirException e) {
			throw new RestServiceException(
					new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.getMessage(), null));
		}
		if (importJob.getPatients() == null || importJob.getPatients().isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(importJob, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ImportJob> importDicomZipFile(
			@ApiParam(value = "file detail") @RequestBody final String dicomZipFilename) throws RestServiceException {
		// We use this when coming from BRUKER upload
		if (dicomZipFilename == null) {
			throw new RestServiceException(
					new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), NO_FILE_UPLOADED, null));
		}
		File tempFile = new File(dicomZipFilename);
		MockMultipartFile multiPartFile;
		try {
			multiPartFile = new MockMultipartFile(tempFile.getName(), tempFile.getName(), APPLICATION_ZIP, new FileInputStream(tempFile.getAbsolutePath()));

			// Import dicomfile
			return uploadDicomZipFile(multiPartFile);
		} catch (IOException e) {
			LOG.error("ERROR while loading zip fiole, please contact an administrator");
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
		} finally {
			// Delete temp file which is useless now
			tempFile.delete();
		}
	}

	@Override
	/**
	 * This method load an EEG file, unzip it and load an import job with the
	 * informations collected
	 */
	public ResponseEntity<EegImportJob> uploadEEGZipFile(
			@ApiParam(value = "file detail") @RequestPart("file") final MultipartFile eegFile)
					throws RestServiceException {
		try {
			// Do some checks about the file, must be != null and must be a .zip file
			if (eegFile == null) {
				throw new RestServiceException(
						new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), NO_FILE_UPLOADED, null));
			}
			if (!dicomImporterService.isZipFile(eegFile)) {
				throw new RestServiceException(
						new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), WRONG_CONTENT_FILE_UPLOAD, null));
			}
			/**
			 * 1. STEP: Handle file management. Always create a userId specific folder in
			 * the import work folder (the root of everything): split imports to clearly
			 * separate them into separate folders for each user
			 */
			final File userImportDir = eegImporterService.getUserImportDir();
			if (!userImportDir.exists()) {
				userImportDir.mkdirs(); // create if not yet existing
			}

			// Unzip the file and get the elements
			File importJobDir = dicomImporterService.saveTempFileCreateFolderAndUnzip(userImportDir, eegFile, false);

			EegImportJob importJob = new EegImportJob();
			importJob.setWorkFolder(importJobDir.getName());

			List<EegDataset> datasets = new ArrayList<>();

			File dataFileDir = new File(importJobDir.getAbsolutePath() + File.separator
					+ eegFile.getOriginalFilename().replace(".zip", ""));

			// Get .VHDR file
			File[] bvMatchingFiles = dataFileDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(final File dir, final String name) {
					return name.endsWith("vhdr");
				}
			});

			// Get .edf file
			File[] edfMatchingFiles = dataFileDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(final File dir, final String name) {
					return name.endsWith("edf");
				}
			});

			if (bvMatchingFiles != null && bvMatchingFiles.length > 0) {
				// Manage multiple vhdr files
				// read .vhdr files
				eegImporterService.readBrainvisionFiles(bvMatchingFiles, dataFileDir, datasets);
			} else if (edfMatchingFiles != null && edfMatchingFiles.length > 0) {
				// read .edf files
				eegImporterService.readEdfFiles(edfMatchingFiles, dataFileDir, datasets);
			} else {
				throw new RestServiceException(new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(),
						"File does not contains a .vhdr or .edf file."));
			}

			importJob.setDatasets(datasets);

			return new ResponseEntity<>(importJob, HttpStatus.OK);
		} catch (IOException ioe) {
			throw new RestServiceException(ioe, new ErrorModel(HttpStatus.BAD_REQUEST.value(), "Invalid file"));
		} catch (ShanoirImportException e) {
			throw new RestServiceException(e, new ErrorModel(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
		}
	}

	/**
	 * Here we had all the informations we needed (metadata, examination, study,
	 * subject, ect...) so we make a call to dataset API to create it.
	 * @throws RestServiceException
	 */
	@Override
	public ResponseEntity<Void> startImportEEGJob(
			@ApiParam(value = "EegImportJob", required = true) @Valid @RequestBody final EegImportJob importJob) throws RestServiceException {
		// Comment: Anonymisation is not necessary for pure brainvision EEGs data
		// For .EDF, anonymisation could be done here.
		// Comment: BIDS translation will be done during export and not during import.

		try {
	        rabbitTemplate.setBeforePublishPostProcessors(message -> {
	            message.getMessageProperties().setHeader("x-user-id",
	            		KeycloakUtil.getTokenUserId());
	            return message;
	        });
			this.rabbitTemplate.convertAndSend(RabbitMQConfiguration.IMPORTER_QUEUE_DATASET_EEG, objectMapper.writeValueAsString(importJob));
		} catch (AmqpException | JsonProcessingException e) {
			e.printStackTrace();
			LOG.error("Error while sending rabbit MQ message to create a new processed dataset.");
			throw new RestServiceException(e, new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Void> startImportNiftiJob(
			@ApiParam(value = "ImportJob", required = true) @Valid @RequestBody final ImportJob importJob) throws RestServiceException {
		// Post to dataset MS to finish import and create associated datasets
		try {
			importJob.setWorkFolder(niftiImporterService.getUserImportDir() + "/" + importJob.getWorkFolder());
	        rabbitTemplate.setBeforePublishPostProcessors(message -> {
	            message.getMessageProperties().setHeader("x-user-id",
	            		KeycloakUtil.getTokenUserId());
	            return message;
	        });
			this.rabbitTemplate.convertAndSend(RabbitMQConfiguration.IMPORTER_QUEUE_DATASET_NIFTI, objectMapper.writeValueAsString(importJob));
		} catch (AmqpException | JsonProcessingException e) {
			e.printStackTrace();
			LOG.error("Error while sending rabbit MQ message to create a new processed dataset.");
			throw new RestServiceException(e, new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Void> uploadFile(@PathVariable("tempDirId") String tempDirId,
			@RequestParam("file") MultipartFile file) throws RestServiceException, IOException {
		final File userImportDir = eegImporterService.getUserImportDir();
		final File importJobDir = new File(userImportDir, tempDirId);
		// only continue in case of existing temp dir id
		if (importJobDir.exists()) {
			File fileToWrite = new File(importJobDir, file.getOriginalFilename());
			if (fileToWrite.exists()) {
				throw new RestServiceException(new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(),
						"Duplicate file name in tempDir, could not create file as file exists already.", null));
			} else {
				byte[] bytes = file.getBytes();
				Files.write(fileToWrite.toPath(), bytes);
			}
		} else {
			throw new RestServiceException(new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(),
					"Upload file called with not existing tempDirId.", null));
		}
		return null;
	}

	@Override
	public ResponseEntity<Void> startImport(@RequestBody Exchange exchange)
			throws RestServiceException, FileNotFoundException, IOException {
		// 1. Check if uploaded data are complete (to be done a little later)
		final File userImportDir = dicomImporterService.getUserImportDir();
		final File tempDir = new File(userImportDir, exchange.getTempDirId());

		// TODO: constant
		final File dicomDir = new File(tempDir, "DICOMDIR");
		if (!dicomDir.exists()) {
			dicomDirGeneratorService.generateDicomDirFromDirectory(dicomDir, tempDir);
			LOG.info("DICOMDIR generated at path: " + dicomDir.getAbsolutePath());
		}

		/**
		 * 2. STEP: prepare patients list to be put into ImportJob: read DICOMDIR and
		 * complete with meta-data from files
		 */
		ImportJob importJob = new ImportJob();
		List<Patient> patients = dicomImporterService.preparePatientsForImportJob(tempDir);
		importJob.setPatients(patients);
		importJob.setFromDicomZip(true);
		importJob.setAnonymisationProfileToUse(exchange.getAnonymisationProfileToUse());
		// Work folder is always relative to general import directory and userId (not
		// shown to outside world)
		importJob.setWorkFolder(tempDir.getAbsolutePath());
		/**
		 * Handle Study and StudyCard settings:
		 */
		ExStudy exStudy = exchange.getExStudy();
		if (exStudy != null && exStudy.getStudyId() != null) {
			importJob.setStudyId(exStudy.getStudyId());
			ExStudyCard exStudyCard = exStudy.getExStudyCards().get(0);
			importJob.setStudyCardName(exStudyCard.getName());
			int i = 0;
			List<ExSubject> exSubjects = exStudy.getExSubjects();
			for (Iterator<ExSubject> iterator = exSubjects.iterator(); iterator.hasNext();) {
				ExSubject exSubject = iterator.next();
				Subject subject = new Subject();
				subject.setId(exSubject.getSubjectId());
				subject.setName(exSubject.getSubjectName());
				patients.get(i).setSubject(subject);
				if (exSubject != null && exSubject.getSubjectName() != null) {
					List<ExExamination> exExaminations = exSubject.getExExaminations();
					for (Iterator<ExExamination> iterator2 = exExaminations.iterator(); iterator2.hasNext();) {
						ExExamination exExamination = iterator2.next();
						// @TODO: adapt ImportJob later for multiple-exams
						importJob.setExaminationId(exExamination.getId());
					}
				} else {
					// handle creation of subject and exams later here
				}
				i++;
			}
		} else {
			// handle creation of study and study cards later here
		}
		final Long userId = KeycloakUtil.getTokenUserId();
		importerManagerService.manageImportJob(userId, KeycloakUtil.getKeycloakHeader(), importJob);
		return null;
	}

	/**
	 * This methods import a bunch of datasets from a Shanoir Exchange Format (based
	 * on BIDS format)
	 * 
	 * @param bidsFile
	 *            the file
	 * @throws ShanoirException
	 *             when something gets wrong during the import
	 * @throws IOException
	 *             when IO fails
	 * @throws RestServiceException
	 */
	@Override
	public ResponseEntity<ImportJob> importAsBids(
			@ApiParam(value = "file detail") @RequestPart("file") final MultipartFile bidsFile)
					throws RestServiceException, ShanoirException, IOException {
		// Check that the file is not null and well zipped
		try {
			if (bidsFile == null) {
				throw new RestServiceException(
						new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), NO_FILE_UPLOADED, null));
			}
			
			if (!dicomImporterService.isZipFile(bidsFile)) {
				// .SEF ?
				throw new RestServiceException(
						new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), WRONG_CONTENT_FILE_UPLOAD, null));
			}

			// Create tmp folder and unzip archive
			final File userImportDir = dicomImporterService.getUserImportDir();
			File importJobDir = dicomImporterService.saveTempFileCreateFolderAndUnzip(userImportDir, bidsFile, false);
			// Deserialize participants.tsv => Do a call to studies API to create
			// corresponding subjects
			File participantsFile = new File(importJobDir.getAbsolutePath() + "/participants.tsv");
			if (!participantsFile.exists()) {
				throw new ShanoirException("participants.tsv file is mandatory");
			}

			ObjectMapper mapper = new ObjectMapper();

			SimpleModule module = new SimpleModule();
			module.addAbstractTypeMapping(StudyUserInterface.class, StudyUser.class);
			mapper.registerModule(module);
			// Here we wait for the response => to be sure that the subjects are created
			String participantString = (String) rabbitTemplate.convertSendAndReceive(RabbitMQConfiguration.SUBJECTS_QUEUE, participantsFile.getAbsolutePath());
			List<IdName> participants = Arrays.asList(mapper.readValue(participantString, IdName[].class));
			// If we receive a unique subject with no ID => It's an error
			if (participants.size() == 1 && participants.get(0).getId() == null) {
				throw new ShanoirException(participants.get(0).getName());
			}

			File studyDescriptionFile = new File(importJobDir.getAbsolutePath() + "/dataset_description.json");
			if (!studyDescriptionFile.exists()) {
				throw new ShanoirException("studyDescriptionFile file is mandatory");
			}

			// Then import data
			File sourceData = new File(importJobDir.getAbsolutePath() + "/sourcedata");
			if (!sourceData.exists()) {
				throw new ShanoirException("sourcedata folder is mandatory");
			}

			// 2) Import Datasets
			File[] subjectFiles = sourceData.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith("sub-");
				}
			});
			ImportJob job = null;

			for (File subjFile : subjectFiles) {
				// Get subjectName
				String subjectName = subjFile.getName().substring("sub-".length());

				// Read shanoirImportFile
				File shanoirImportFile = new File(subjFile.getAbsolutePath() + "/shanoir-import.json");

				if (!shanoirImportFile.exists()) {
					throw new ShanoirException("shanoir-import.json file is mandatory in subject folder");
				}

				ObjectMapper objectMapper = new ObjectMapper();
				ImportJob sid = objectMapper.readValue(shanoirImportFile, ImportJob.class);
				CommonIdsDTO idsDTO = new CommonIdsDTO(null, sid.getStudyId(), null,
						sid.getAcquisitionEquipmentId());
				final HttpEntity<CommonIdsDTO> requestBody = new HttpEntity<>(idsDTO, KeycloakUtil.getKeycloakHeader());
				// Post to dataset MS to finish import and create associated datasets
				ResponseEntity<CommonIdNamesDTO> response = restTemplate.exchange(studiesCommonMsUrl, HttpMethod.POST,
						requestBody, CommonIdNamesDTO.class);
				// Check that equipement exists
				// Check that study exists
				// All in one with studies MS CommonsApi
				// This is not necessary if we further use the studyCard
				if (response.getBody().getEquipement() == null) {
					throw new ShanoirException(
							"Equipement with ID " + sid.getAcquisitionEquipmentId() + " does not exists.");
				}
				if (response.getBody().getStudy() == null) {
					throw new ShanoirException("Study with ID " + sid.getStudyId() + " does not exists.");
				}
				// Subject based on folder name
				Long subjectId = getSubjectIdByName(subjectName, participants);
				if (subjectId == null) {
					throw new ShanoirException(
							"Subject " + subjectName + " could not be created. Please check participants.tsv file.");
				}

				// If there is no DICOMDIR: create it
				File dicomDir = new File(subjFile.getAbsolutePath() + "/DICOM/DICOMDIR");
				if (!dicomDir.exists()) {
					DicomDirCreator creator = new DicomDirCreator(subjFile.getAbsolutePath() + "/DICOMDIR",
							subjFile.getAbsolutePath() + "/DICOM");
					creator.start();
				}
				// Zip data folders to be able to call ImporterAPIController.uploadDicomZipFile
				FileOutputStream fos = new FileOutputStream(subjFile.getAbsolutePath() + ".zip");
				ZipOutputStream zipOut = new ZipOutputStream(fos);

				ImportUtils.zipFile(subjFile, subjFile.getName(), zipOut, true);

				zipOut.close();
				fos.close();
				MockMultipartFile multiPartFile = new MockMultipartFile(subjFile.getName(), subjFile.getName() + ".zip",
						APPLICATION_ZIP, new FileInputStream(subjFile.getAbsolutePath() + ".zip"));

				// Send data folder to import API and get import job
				ResponseEntity<ImportJob> entity = this.uploadDicomZipFile(multiPartFile);

				// Complete ImportJob to use startImportJob
				job = entity.getBody();

				// Construire l'arborescence
				job.setAcquisitionEquipmentId(sid.getAcquisitionEquipmentId());
				job.setStudyId(sid.getStudyId());

				job.setFromPacs(false);
				job.setFromShanoirUploader(false);
				job.setFromDicomZip(true);
				for (Patient pat : job.getPatients()) {
					pat.setPatientName(subjectName);
					Subject subject = new Subject();
					subject.setId(subjectId);
					subject.setName(subjectName);
					pat.setSubject(subject);

					// Select all series to be imported
					for (Study study : pat.getStudies()) {
						for (Serie serie : study.getSeries()) {
							serie.setSelected(Boolean.TRUE);
						}
					}
				}

				// Create a new examination if not existing
				if (sid.getExaminationId() == null || sid.getExaminationId().equals(Long.valueOf(0l))) {
					// Create examination => We actually need its ID so do a direct API call
					ExaminationDTO examDTO = new ExaminationDTO();
					// Construct DTO
					examDTO.setCenter(new IdName(Long.valueOf(1), null));
					examDTO.setPreclinical(false); // Pour le moment on fait que du DICOM
					examDTO.setStudy(new IdName(sid.getStudyId(), response.getBody().getStudy().getName()));
					examDTO.setSubject(new IdName(subjectId, subjectName));
					examDTO.setExaminationDate(job.getPatients().get(0).getStudies().get(0).getStudyDate());
					examDTO.setComment(job.getPatients().get(0).getStudies().get(0).getStudyDescription());

					final HttpEntity<ExaminationDTO> requestBodyExam = new HttpEntity<>(examDTO,
							KeycloakUtil.getKeycloakHeader());
					ResponseEntity<ExaminationDTO> examResponse = restTemplate.exchange(createExaminationMsUrl,
							HttpMethod.POST, requestBodyExam, ExaminationDTO.class);
					job.setExaminationId(examResponse.getBody().getId());
				}
				// Next API call => StartImportJob
				ResponseEntity<Void> result = this.startImportJob(job);
				if (!result.getStatusCode().equals(HttpStatus.OK)) {
					throw new ShanoirException("Error while importing subject: " + subjectName);
				}
			}
			// TODO ONE DAY: Copy "other" files to the bids folder
			// Copy non datasets elements
			// Don't copy "data" folder
			// Don't copy examination_description.json
			// copy /sourceData??, /code and / files (readme, changes, participants.tsv,
			// participants.json, etc..)
			return new ResponseEntity<>(job, HttpStatus.OK);

		} catch (Exception e) {
			System.err.println("Coucou" + e + e.getMessage() + e.getStackTrace());
			throw e;
		}
	}

	/**
	 * Get the ID of a subject from its name and a list of subject
	 * 
	 * @param name
	 *            the name of the subject to find
	 * @param subjects
	 *            the list of subjects to supply
	 * @return the ID of the subject corresponding to the name, null otherwise
	 */
	public Long getSubjectIdByName(String name, List<IdName> subjects) {
		for (IdName sub : subjects) {
			if (sub.getName().equals(name)) {
				return sub.getId();
			}
		}
		return null;
	}

	/**
	 * This methods returns a dicom file
	 * 
	 * @param path
	 *            the dicom file path
	 * @throws ShanoirException
	 *             when something gets wrong during the import
	 * @throws IOException
	 *             when IO fails
	 * @throws RestServiceException
	 */
	@Override
	public ResponseEntity<ByteArrayResource> getDicomImage(@ApiParam(value = "path", required=true)  @RequestParam(value = "path", required = true) String path)
			throws RestServiceException, IOException {

		final File userImportDir = dicomImporterService.getUserImportDir();
		String pathInfo = userImportDir.getAbsolutePath() + File.separator + path;
		URL url = new URL("file:///" + pathInfo);
		final URLConnection uCon = url.openConnection();
		final InputStream is = uCon.getInputStream();

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[BUFFER_SIZE];
		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		buffer.flush();
		byte[] byteArray = buffer.toByteArray();

		ByteArrayResource resource = new ByteArrayResource(byteArray);

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("application/dicom"))
				.contentLength(uCon.getContentLength())
				.body(resource);
	}
}
