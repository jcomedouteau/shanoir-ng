package org.shanoir.ng.importer.dicom;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.shanoir.ng.importer.AbstractImporterService;
import org.shanoir.ng.importer.model.ImportJob;
import org.shanoir.ng.importer.model.Patient;
import org.shanoir.ng.importer.model.Serie;
import org.shanoir.ng.importer.model.Study;
import org.shanoir.ng.shared.exception.ErrorModel;
import org.shanoir.ng.shared.exception.RestServiceException;
import org.shanoir.ng.utils.ImportUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * This class contains all the method to allow import of DICOM in Shanoir
 * @author fli
 *
 */
@Service
public class DicomImporterService extends AbstractImporterService {

	private static final String DICOMDIR = "DICOMDIR";

	private static final String FILE_POINT = ".";

	private static final String UPLOAD_FILE_SUFFIX = ".upload";

	@Value("${shanoir.import.directory}")
	private String importDir;

	@Autowired
	private DicomDirToModelService dicomDirToModel;

	/**
	 * 1. STEP: read DICOMDIR and create Shanoir model from it (== Dicom model):
	 * Patient - Study - Serie - Instance 2. STEP: split instances into non-images
	 * and images and get additional meta-data from first dicom file of each serie,
	 * meta-data missing in dicomdir.
	 * 
	 * @param dirWithDicomDir
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public List<Patient> preparePatientsForImportJob(File dirWithDicomDir) throws IOException {
		List<Patient> patients = null;
		File dicomDirFile = new File(dirWithDicomDir.getAbsolutePath() + File.separator + DICOMDIR);
		if (dicomDirFile.exists()) {
			patients = dicomDirToModel.readDicomDirToPatients(dicomDirFile);
		}
		return patients;
	}

	/**
	 * This method stores an uploaded zip file in a temporary file, creates a new
	 * folder with the same name and unzips the content into this folder, and gives
	 * back the folder with the content.
	 * 
	 * @param userImportDir
	 * @param dicomZipFile
	 * @return
	 * @throws IOException
	 * @throws RestServiceException
	 */
	public File saveTempFileCreateFolderAndUnzip(final File userImportDir, final MultipartFile dicomZipFile,
			final boolean fromDicom) throws IOException, RestServiceException {
		File tempFile = saveTempFile(userImportDir, dicomZipFile);
		if (fromDicom && !ImportUtils.checkZipContainsFile(DICOMDIR, tempFile)) {
			throw new RestServiceException(
					new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), "DICOMDIR is missing in .zip file.", null));
		}
		String fileName = tempFile.getName();
		int pos = fileName.lastIndexOf(FILE_POINT);
		if (pos > 0) {
			fileName = fileName.substring(0, pos);
		}
		File unzipFolderFile = new File(tempFile.getParentFile().getAbsolutePath() + File.separator + fileName);
		if (!unzipFolderFile.exists()) {
			unzipFolderFile.mkdirs();
		} else {
			throw new RestServiceException(new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(),
					"Error while unzipping file: folder already exists.", null));
		}
		ImportUtils.unzip(tempFile.getAbsolutePath(), unzipFolderFile.getAbsolutePath());
		tempFile.delete();
		return unzipFolderFile;
	}

	/**
	 * This method takes a multipart file and stores it in a configured upload
	 * directory in relation with the userId with a random name and the suffix
	 * .upload
	 *
	 * @param file
	 * @throws IOException
	 */
	public File saveTempFile(final File userImportDir, final MultipartFile file) throws IOException {
		long n = createRandomLong();
		File uploadFile = new File(userImportDir.getAbsolutePath(), Long.toString(n) + UPLOAD_FILE_SUFFIX);
		file.transferTo(uploadFile);
		return uploadFile;
	}

	/**
	 * This methods removes not selected series during import from the list of patients/study/series
	 * @param importJob the import job to remove the series from
	 */
	public void removeUnselectedSeries(final ImportJob importJob) {
		for (Iterator<Patient> patientIt = importJob.getPatients().iterator(); patientIt.hasNext();) {
			Patient patient = patientIt.next();
			List<Study> studies = patient.getStudies();
			for (Iterator<Study> studyIt = studies.iterator(); studyIt.hasNext();) {
				Study study = studyIt.next();
				List<Serie> series = study.getSeries();
				for (Iterator<Serie> serieIt = series.iterator(); serieIt.hasNext();) {
					Serie serie = serieIt.next();
					if (!serie.getSelected()) {
						serieIt.remove();
					}
				}
			}
		}
	}

}
