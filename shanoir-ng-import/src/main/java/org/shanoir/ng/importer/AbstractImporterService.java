package org.shanoir.ng.importer;

import java.io.File;
import java.security.SecureRandom;

import org.shanoir.ng.utils.KeycloakUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

/**
 * This class is an abstract service containing all the common methods of importer services
 * @author fli
 *
 */
public abstract class AbstractImporterService {


	private static final SecureRandom RANDOM = new SecureRandom();
	
	private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

	private static final String ZIP_FILE_SUFFIX = ".zip";

	private static final String APPLICATION_ZIP = "application/zip";

	@Value("${shanoir.import.directory}")
	private String importDir;

	public File getUserImportDir() {
		final Long userId = KeycloakUtil.getTokenUserId();
		final String userImportDirFilePath = importDir + File.separator + Long.toString(userId);
		final File userImportDir = new File(userImportDirFilePath);
		if (!userImportDir.exists()) {
			userImportDir.mkdirs(); // create if not yet existing
		} // else is wanted case, user has already its import directory
		return userImportDir;
	}

	/**
	 * This method creates a random long number.
	 * 
	 * @return long: random number
	 */
	public long createRandomLong() {
		long n = RANDOM.nextLong();
		if (n == Long.MIN_VALUE) {
			n = 0; // corner case
		} else {
			n = Math.abs(n);
		}
		return n;
	}

	/**
	 * Check if sent file is of type .zip.
	 *
	 * @param file
	 */
	public boolean isZipFile(final MultipartFile file) {
		return file.getOriginalFilename().endsWith(ZIP_FILE_SUFFIX) || file.getContentType().equals(APPLICATION_ZIP)
				|| file.getContentType().equals(APPLICATION_OCTET_STREAM);
	}
}
