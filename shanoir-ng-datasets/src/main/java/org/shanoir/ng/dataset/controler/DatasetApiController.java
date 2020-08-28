/**
 * Shanoir NG - Import, manage and share neuroimaging data
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

package org.shanoir.ng.dataset.controler;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.shanoir.ng.dataset.DatasetDescription;
import org.shanoir.ng.dataset.dto.DatasetDTO;
import org.shanoir.ng.dataset.dto.mapper.DatasetMapper;
import org.shanoir.ng.dataset.modality.EegDataset;
import org.shanoir.ng.dataset.modality.EegDatasetMapper;
import org.shanoir.ng.dataset.modality.MrDataset;
import org.shanoir.ng.dataset.modality.MrDatasetMapper;
import org.shanoir.ng.dataset.modality.MrDatasetNature;
import org.shanoir.ng.dataset.model.Dataset;
import org.shanoir.ng.dataset.model.DatasetExpression;
import org.shanoir.ng.dataset.model.DatasetExpressionFormat;
import org.shanoir.ng.dataset.security.DatasetSecurityService;
import org.shanoir.ng.dataset.service.DatasetService;
import org.shanoir.ng.datasetacquisition.model.DatasetAcquisition;
import org.shanoir.ng.datasetfile.DatasetFile;
import org.shanoir.ng.download.WADODownloaderService;
import org.shanoir.ng.examination.model.Examination;
import org.shanoir.ng.examination.service.ExaminationService;
import org.shanoir.ng.exporter.service.BIDSService;
import org.shanoir.ng.shared.error.FieldErrorMap;
import org.shanoir.ng.shared.event.ShanoirEvent;
import org.shanoir.ng.shared.event.ShanoirEventService;
import org.shanoir.ng.shared.event.ShanoirEventType;
import org.shanoir.ng.shared.exception.EntityNotFoundException;
import org.shanoir.ng.shared.exception.ErrorDetails;
import org.shanoir.ng.shared.exception.ErrorModel;
import org.shanoir.ng.shared.exception.RestServiceException;
import org.shanoir.ng.utils.KeycloakUtil;
import org.shanoir.ng.utils.Utils;
import org.shanoir.ng.shared.model.Subject;
import org.shanoir.ng.shared.repository.SubjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiParam;

@Controller
public class DatasetApiController implements DatasetApi {

	private static final String EEG = "eeg";

	private static final String NII = "nii";

	private static final String DCM = "dcm";

	private static final String ATTACHMENT_FILENAME = "attachment;filename=";

	private static final String ZIP = ".zip";

	private static final String DOWNLOAD = ".download";

	private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

	private static final String T1w = "T1w";

	private static final String SUB_PREFIX = "sub-";

	private static final String SES_PREFIX = "ses-";

	private static final String DATASET_DESCRIPTION_FILE = "dataset_description.json";

	private static final String README_FILE = "README";

	private static final Logger LOG = LoggerFactory.getLogger(DatasetApiController.class);

	@Value("${datasets-data}")
	private String niftiStorageDir;

	@Autowired
	private DatasetMapper datasetMapper;

	@Autowired
	private MrDatasetMapper mrDatasetMapper;

	@Autowired
	private EegDatasetMapper eegDatasetMapper;

	@Autowired
	private DatasetService datasetService;

	@Autowired
	private ExaminationService examinationService;

	private final HttpServletRequest request;

	@Autowired
	private WADODownloaderService downloader;

	@Autowired
	private BIDSService bidsService;

	@Autowired
	private ShanoirEventService eventService;

	@Autowired
	private DatasetSecurityService datasetSecurityService;

	@Value("${maxDownloadSize}")
	private long maxFileSize;

	@Autowired
	private SubjectRepository subjectRepo;

	private static final SecureRandom RANDOM = new SecureRandom();

	@org.springframework.beans.factory.annotation.Autowired
	public DatasetApiController(final HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public ResponseEntity<Void> deleteDataset(
			@ApiParam(value = "id of the dataset", required = true) @PathVariable("datasetId") final Long datasetId)
					throws RestServiceException {

		try {
			Dataset dataset = datasetService.findById(datasetId);
			bidsService.deleteDataset(dataset);
			datasetService.deleteById(datasetId);
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	@Override
	public ResponseEntity<DatasetDTO> findDatasetById(
			@ApiParam(value = "id of the dataset", required = true) @PathVariable("datasetId") final Long datasetId) {

		final Dataset dataset = datasetService.findById(datasetId);
		if (dataset == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (dataset instanceof MrDataset) {
			return new ResponseEntity<>(mrDatasetMapper.datasetToDatasetDTO((MrDataset) dataset), HttpStatus.OK);
		}
		else if (dataset instanceof EegDataset) {
			return new ResponseEntity<>(eegDatasetMapper.datasetToDatasetDTO((EegDataset) dataset), HttpStatus.OK);
		}
		return new ResponseEntity<>(datasetMapper.datasetToDatasetDTO(dataset), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Void> updateDataset(
			@ApiParam(value = "id of the dataset", required = true) @PathVariable("datasetId") final Long datasetId,
			@ApiParam(value = "study to update", required = true) @Valid @RequestBody final Dataset dataset,
			final BindingResult result) throws RestServiceException {

		validate(result);

		try {
			datasetService.update(dataset);
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);

		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	@Override
	public ResponseEntity<Page<DatasetDTO>> findDatasets(final Pageable pageable) throws RestServiceException {
		Page<Dataset> datasets = datasetService.findPage(pageable);
		if (datasets.getContent().isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(datasetMapper.datasetToDatasetDTO(datasets), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<List<Long>> findDatasetIdsBySubjectId(@ApiParam(value = "id of the subject", required = true) @PathVariable("subjectId") Long subjectId) {
		final List<Examination> examinations = examinationService.findBySubjectId(subjectId);

		List<Long> datasetIds = new ArrayList<Long>();
		for(Examination examination : examinations) {
			ResponseEntity<List<Long>> response = findDatasetIdsBySubjectIdStudyId(subjectId, examination.getStudyId());
			if(response.getStatusCode() == HttpStatus.OK) {
				datasetIds.addAll(response.getBody());
			}
		}
		return new ResponseEntity<List<Long>>(datasetIds, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<List<Long>> findDatasetIdsBySubjectIdStudyId(
			@ApiParam(value = "id of the subject", required = true) @PathVariable("subjectId") Long subjectId,
			@ApiParam(value = "id of the study", required = true) @PathVariable("studyId") Long studyId) {
		
		final List<Examination> examinations = examinationService.findBySubjectIdStudyId(subjectId, studyId);
		if (examinations.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		List<Long> datasetIds = new ArrayList<Long>();
		for(Examination examination : examinations) {
			List<DatasetAcquisition> datasetAcquisitions = examination.getDatasetAcquisitions();
			for(DatasetAcquisition datasetAcquisition : datasetAcquisitions) {
				for(Dataset dataset : datasetAcquisition.getDatasets()) {
					datasetIds.add(dataset.getId());
				}
			}
		}
		return new ResponseEntity<>(datasetIds, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ByteArrayResource> downloadDatasetById(
			@ApiParam(value = "id of the dataset", required = true) @PathVariable("datasetId") final Long datasetId,
			@ApiParam(value = "Decide if you want to download dicom (dcm) or nifti (nii) files.", allowableValues = "dcm, nii, eeg", defaultValue = DCM)
			@Valid @RequestParam(value = "format", required = false, defaultValue = DCM) final String format)
					throws RestServiceException, IOException {

		final Dataset dataset = datasetService.findById(datasetId);
		if (dataset == null) {
			throw new RestServiceException(
					new ErrorModel(HttpStatus.NOT_FOUND.value(), "Dataset with id not found.", null));
		}

		/* Create folder and file */
		String tmpDir = System.getProperty(JAVA_IO_TMPDIR);
		long n = RANDOM.nextLong();
		if (n == Long.MIN_VALUE) {
			n = 0; // corner case
		} else {
			n = Math.abs(n);
		}
		String tmpFilePath = tmpDir + File.separator + Long.toString(n);
		File workFolder = new File(tmpFilePath + DOWNLOAD);
		workFolder.mkdirs();
		File zipFile = new File(tmpFilePath + ZIP);
		zipFile.createNewFile();

		try {
			List<URL> pathURLs = new ArrayList<>();
			if (DCM.equals(format)) {
				getDatasetFilePathURLs(dataset, pathURLs, DatasetExpressionFormat.DICOM);
				downloader.downloadDicomFilesForURLs(pathURLs, workFolder);
			} else if (NII.equals(format)) {
				getDatasetFilePathURLs(dataset, pathURLs, DatasetExpressionFormat.NIFTI_SINGLE_FILE);
				copyNiftiFilesForURLs(pathURLs, workFolder, dataset);
			} else if (EEG.equals(format)) {
				getDatasetFilePathURLs(dataset, pathURLs, DatasetExpressionFormat.EEG);
				copyNiftiFilesForURLs(pathURLs, workFolder, dataset);
			} else {
				throw new RestServiceException(
						new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Bad arguments", null));
			}
		} catch (IOException | MessagingException e) {
			throw new RestServiceException(
					new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Error in WADORSDownloader.", null));
		}
		Utils.zip(workFolder.getAbsolutePath(), zipFile.getAbsolutePath());

		// Try to determine file's content type
		String contentType = request.getServletContext().getMimeType(zipFile.getAbsolutePath());

		byte[] data = Files.readAllBytes(zipFile.toPath());
		ByteArrayResource resource = new ByteArrayResource(data);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ATTACHMENT_FILENAME + zipFile.getName())
				.contentType(MediaType.parseMediaType(contentType))
				.contentLength(data.length)
				.body(resource);
	}

	@Override
	public ResponseEntity<ByteArrayResource> massiveDownloadByDatasetIds(
			@ApiParam(value = "ids of the datasets", required=true) @Valid
			@RequestParam(value = "datasetIds", required = true) List<Long> datasetIds,
			@ApiParam(value = "Decide if you want to download dicom (dcm) or nifti (nii) files.", allowableValues = "dcm, nii, eeg", defaultValue = DCM) @Valid
			@RequestParam(value = "format", required = false, defaultValue=DCM) String format) throws RestServiceException, EntityNotFoundException, MalformedURLException, IOException {
		// STEP 0: Check data integrity
		if (datasetIds == null || datasetIds.isEmpty()) {
			throw new RestServiceException(
					new ErrorModel(HttpStatus.FORBIDDEN.value(), "Please use a valid sets of dataset IDs."));
		}
		// STEP 1: Retrieve all datasets all in one with only the one we can see
		List<Dataset> datasets = datasetService.findByIdIn(datasetIds);

		return massiveDownload(format, datasets);
	}

	@Override
	public ResponseEntity<ByteArrayResource> massiveDownloadByStudyId(
			@ApiParam(value = "id of the study", required=true) @Valid
			@RequestParam(value = "studyId", required = true) Long studyId,
			@ApiParam(value = "Decide if you want to download dicom (dcm) or nifti (nii) files.", allowableValues = "dcm, nii, eeg", defaultValue = DCM) @Valid
			@RequestParam(value = "format", required = false, defaultValue=DCM) String format) throws RestServiceException, EntityNotFoundException, IOException {
		// STEP 0: Check data integrity
		if (studyId == null) {
			throw new RestServiceException(
					new ErrorModel(HttpStatus.FORBIDDEN.value(), "Please use a valid study ID."));
		}
		// STEP 1: Retrieve all datasets all in one with only the one we can see
		List<Dataset> datasets = datasetService.findByStudyId(studyId);

		return massiveDownload(format, datasets);
	}

	public ResponseEntity<ByteArrayResource> massiveDownload(String format, List<Dataset> datasets) throws EntityNotFoundException, RestServiceException, IOException {
		// STEP 2: Check rights => Also filters datasets on rights
		datasets = datasetSecurityService.hasRightOnAtLeastOneDataset(datasets, "CAN_DOWNLOAD");
		// STEP 3: Get the data
		// Check rights on at least one of the datasets and filter the datasetIds list
		String tmpDir = System.getProperty(JAVA_IO_TMPDIR);
		String tmpFilePath = tmpDir + File.separator + "Datasets[";
		for (Dataset dataset : datasets) {
			tmpFilePath += dataset.getId() + ",";
		}
		// Remove last comma and close brackets
	    tmpFilePath = tmpFilePath.substring(0, tmpFilePath.length() - 1);
	    tmpFilePath += "]";

		File tmpFile = new File(tmpFilePath);
		tmpFile.mkdirs();
		
		List<ShanoirEvent> events = new ArrayList<>();

		// Get the data
		try {
			for (Dataset dataset : datasets) {
				LOG.info("treating dataset: " + dataset.getName());
				File datasetFile = new File(tmpFile.getAbsolutePath() + File.separator + dataset.getName());
				datasetFile.mkdirs();
				List<URL> pathURLs = new ArrayList<>();
				if (DCM.equals(format)) {
					getDatasetFilePathURLs(dataset, pathURLs, DatasetExpressionFormat.DICOM);
					downloader.downloadDicomFilesForURLs(pathURLs, datasetFile);
				} else if (NII.equals(format)) {
					getDatasetFilePathURLs(dataset, pathURLs, DatasetExpressionFormat.NIFTI_SINGLE_FILE);
					copyNiftiFilesForURLs(pathURLs, datasetFile, dataset);
				}  else if (EEG.equals(format)) {
					getDatasetFilePathURLs(dataset, pathURLs, DatasetExpressionFormat.EEG);
					copyNiftiFilesForURLs(pathURLs, tmpFile, dataset);
				} else {
					throw new RestServiceException(
							new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Bad arguments.", null));
				}
				ShanoirEvent event = new ShanoirEvent(ShanoirEventType.DOWNLOAD_DATASETS_EVENT, dataset.getId().toString(), KeycloakUtil.getTokenUserId(), dataset.getName() + " : " + format, ShanoirEvent.SUCCESS);
				events.add(event);
			}
		} catch (IOException | MessagingException e) {
			e.printStackTrace();
			throw new RestServiceException(
					new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Error while copying files.", e));
		}

		// Zip it
		File zipFile = new File(tmpFilePath + ZIP);
		zipFile.createNewFile();

		Utils.zip(tmpFile.getAbsolutePath(), zipFile.getAbsolutePath());
		
		long fileSize = FileUtils.sizeOf(zipFile);

		// Nginx limit is set to 5Gb => split by dataset if file is bigger
	
		boolean fileIsTooBig = fileSize > maxFileSize;

		if (fileIsTooBig) {
			// Publish events to download files on by one
			
			for (ShanoirEvent event : events) {
				// publish all events for every datasets
				eventService.publishEvent(event);
			}

			// Here delete zip file only
			FileUtils.deleteQuietly(zipFile);
			FileUtils.deleteQuietly(tmpFile);
			return new ResponseEntity<>(HttpStatus.PAYLOAD_TOO_LARGE);
		}

		byte[] data = Files.readAllBytes(zipFile.toPath());
		ByteArrayResource resource = new ByteArrayResource(data);

		FileUtils.deleteDirectory(tmpFile);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + zipFile.getName())
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.contentLength(data.length)
				.body(resource);
	}

	/**
	 * Receives a list of URLs containing file:/// urls and copies the files to a folder named workFolder.
	 * @param urls
	 * @param workFolder
	 * @throws IOException
	 * @throws MessagingException
	 */
	private void copyNiftiFilesForURLs(final List<URL> urls, final File workFolder, Dataset dataset) throws IOException {
		for (Iterator<URL> iterator = urls.iterator(); iterator.hasNext();) {
			URL url =  iterator.next();
			File srcFile = new File(url.getPath());

			// Theorical file name:  NomSujet_SeriesDescription_SeriesNumberInProtocol_SeriesNumberInSequence.nii
			StringBuilder name = new StringBuilder("");
			
			Subject subject = subjectRepo.findOne(dataset.getSubjectId());
			if (subject != null) {
				name.append(subject.getName());
			} else {
				name.append("unknown");
			}
			name.append("_")
			.append(dataset.getUpdatedMetadata().getComment()).append("_")
			.append(dataset.getDatasetAcquisition().getSortingIndex()).append("_")
			.append(dataset.getDatasetAcquisition().getRank()).append(".")
			.append(FilenameUtils.getExtension(srcFile.getName()));

			File destFile = new File(workFolder.getAbsolutePath() + File.separator + name);
			Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * Reads all dataset files depending on the format attached to one dataset.
	 * @param dataset
	 * @param pathURLs
	 * @throws MalformedURLException
	 */
	private void getDatasetFilePathURLs(final Dataset dataset, final List<URL> pathURLs, final DatasetExpressionFormat format) throws MalformedURLException {
		List<DatasetExpression> datasetExpressions = dataset.getDatasetExpressions();
		for (Iterator<DatasetExpression> itExpressions = datasetExpressions.iterator(); itExpressions.hasNext();) {
			DatasetExpression datasetExpression = itExpressions.next();
			if (datasetExpression.getDatasetExpressionFormat().equals(format)) {
				List<DatasetFile> datasetFiles = datasetExpression.getDatasetFiles();
				for (Iterator<DatasetFile> itFiles = datasetFiles.iterator(); itFiles.hasNext();) {
					DatasetFile datasetFile = itFiles.next();
					URL url = new URL(datasetFile.getPath().replaceAll("%20", " "));
					pathURLs.add(url);
				}
			}
		}
	}


	@Override
	public ResponseEntity<ByteArrayResource> exportBIDSBySubjectId(@ApiParam(value = "id of the subject", required = true) @PathVariable("subjectId") Long subjectId,
			@ApiParam(value = "name of the subject", required = true) @PathVariable("subjectName") String subjectName,
			@ApiParam(value = "name of the study", required = true) @PathVariable("studyName") String studyName)
					throws RestServiceException, MalformedURLException, IOException {
		final List<Examination> examinationList = examinationService.findBySubjectId(subjectId);
		if (examinationList.isEmpty()) {
			throw new RestServiceException(
					new ErrorModel(HttpStatus.NOT_FOUND.value(), "No Examination found of subject Id.", null));
		} else {
			// 1. Create folder
			String tmpDir = System.getProperty(JAVA_IO_TMPDIR);
			long n = RANDOM.nextLong();
			if (n == Long.MIN_VALUE) {
				n = 0; // corner case
			} else {
				n = Math.abs(n);
			}
			String tmpFilePath = tmpDir + File.separator + Long.toString(n);
			File workFolder = new File(tmpFilePath + DOWNLOAD);
			workFolder.mkdirs();
			File zipFile = new File(tmpFilePath + ZIP);
			zipFile.createNewFile();

			// 2. Create dataset_description.json and README
			DatasetDescription datasetDesciption = new DatasetDescription();
			datasetDesciption.setName(studyName);
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.writeValue(new File(workFolder.getAbsolutePath() + File.separator + DATASET_DESCRIPTION_FILE), datasetDesciption);
			objectMapper.writeValue(new File(workFolder.getAbsolutePath() + File.separator + README_FILE), studyName);

			// TODO BIDS: 3. Create [ses-<label>/] folder if multi exams
			/*if (examinationList.size() > 1) {
				for (Examination examination: examinationList) {
					String sesLabel = examination.getId().toString();
					final List<DatasetAcquisition> datasetAcquisitionList = examination.getDatasetAcquisitions();
					for (DatasetAcquisition datasetAcquisition : datasetAcquisitionList) {
						// TODO BIDS: 5. multi dataset acquisiton: add [_acq-<label>]
						String acqLabel = datasetAcquisition.getId().toString();
						final List<Dataset> datasetList = datasetAcquisition.getDatasets();
						for (Dataset dataset: datasetList) {
							// TODO BIDS: 6. multi datasets: add [_run-<index>]

							// TODO BIDS: 7. multi MrDatasetNature: add _<modality_label>
						}
					}
				}
			}*/

			// 8. Get modality label, nii and json of dataset
			/* For the demo: one exam, one acq, one dataset, one modality which is T1 */
			final Dataset dataset = examinationList.get(0).getDatasetAcquisitions().get(0).getDatasets().get(0);
			if (dataset == null) {
				throw new RestServiceException(
						new ErrorModel(HttpStatus.NOT_FOUND.value(), "No Dataset found for subject Id.", null));
			}

			// Get modality label
			String modalityLabel = null;
			if (((MrDataset) dataset).getUpdatedMrMetadata().getMrDatasetNature().equals(MrDatasetNature.T1_WEIGHTED_MR_DATASET)
					|| ((MrDataset) dataset).getUpdatedMrMetadata().getMrDatasetNature().equals(MrDatasetNature.T1_WEIGHTED_DCE_MR_DATASET)) {
				modalityLabel = T1w;
			}
			if (StringUtils.isEmpty(modalityLabel)) {
				throw new RestServiceException(
						new ErrorModel(HttpStatus.NOT_FOUND.value(), "No MrDatasetNature, so could not define modality label and export BIDS!", null));
			}

			// Get nii and json files
			try {
				List<URL> pathURLs = new ArrayList<URL>();
				getDatasetFilePathURLs(dataset, pathURLs, DatasetExpressionFormat.NIFTI_SINGLE_FILE);
				copyFilesForBIDSExport(pathURLs, workFolder, subjectName, examinationList.get(0).getId().toString(), modalityLabel);
			} catch (IOException e) {
				throw new RestServiceException(
						new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Error exporting nifti files for subject in BIDS.", null));
			}

			// 9. Create zip file
			Utils.zip(workFolder.getAbsolutePath(), zipFile.getAbsolutePath());

			// Try to determine file's content type
			String contentType = request.getServletContext().getMimeType(zipFile.getAbsolutePath());

			byte[] data = Files.readAllBytes(zipFile.toPath());
			ByteArrayResource resource = new ByteArrayResource(data);

			return ResponseEntity.ok()
					// Content-Disposition
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + zipFile.getName())
					// Content-Type
					.contentType(MediaType.parseMediaType(contentType)) //
					// Content-Length
					.contentLength(data.length) //
					.body(resource);
		}
	}

	/**
	 * This method receives a list of URLs containing file:/// urls and copies the files to a folder named workFolder.
	 * @param urls
	 * @param workFolder
	 * @throws IOException
	 * @throws MessagingException
	 */
	public void copyFilesForBIDSExport(final List<URL> urls, final File workFolder, final String subjectName,
			final String sesId, final String modalityLabel) throws IOException {
		for (Iterator<URL> iterator = urls.iterator(); iterator.hasNext();) {
			URL url =  iterator.next();
			File srcFile = new File(url.getPath());
			String destFilePath = srcFile.getPath().substring(niftiStorageDir.length() + 1, srcFile.getPath().lastIndexOf('/'));
			File destFolder = new File(workFolder.getAbsolutePath() + File.separator + destFilePath);
			destFolder.mkdirs();
			String extensionType = srcFile.getPath().substring(srcFile.getPath().lastIndexOf(".") + 1);
			String destFileNameBIDS = SUB_PREFIX + subjectName + "_" + SES_PREFIX + sesId + "_" + modalityLabel + "." + extensionType;
			File destFile = new File(destFolder.getAbsolutePath() + File.separator + destFileNameBIDS);
			Files.copy(srcFile.toPath(), destFile.toPath());
		}
	}

	/**
	 * Validate a dataset
	 * 
	 * @param result
	 * @throws RestServiceException
	 */
	private void validate(final BindingResult result) throws RestServiceException {
		final FieldErrorMap errors = new FieldErrorMap(result);
		if (!errors.isEmpty()) {
			ErrorModel error = new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Bad arguments", new ErrorDetails(errors));
			throw new RestServiceException(error);
		}
	}
	
	/**
	 * This enum is for coordinates system and associated units
	 */
	public enum CoordinatesSystem {
	    ACPC("mm"),
	    ALLEN("mm"),
	    ANALYZE("mm"),
	    BTI_4D("m"),
	    CTF_MRI("mm"),
	    CTF_GRADIOMETER("cm"),
	    CAPTRAK("mm"),
	    CHIETI("mm"),
	    DICOM("mm"),
	    FREESURFER("mm"),
	    MNI("mm"),
	    NIFTI("mm"),
	    NEUROMAG_ELEKTA("m"),
	    PAXINOS_FRANKLIN("mm"),
	    TALAIRACH_TOURNOUX("mm"),
	    YOKOGAWA("n/a");
	    
	    private String unit;
	    
	    CoordinatesSystem(final String pUnit) {
	    	this.unit = pUnit;
	    }
	    public String getUnit() {
	    	return unit;
	    }
	}
	
}
