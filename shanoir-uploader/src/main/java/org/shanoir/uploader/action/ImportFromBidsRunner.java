package org.shanoir.uploader.action;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.shanoir.dicom.importer.Serie;
import org.shanoir.dicom.importer.UploadJob;
import org.shanoir.dicom.importer.UploadJobManager;
import org.shanoir.dicom.importer.UploadState;
import org.shanoir.dicom.model.DicomTreeNode;
import org.shanoir.ng.exchange.imports.subject.IdentifierCalculator;
import org.shanoir.ng.shared.exception.ShanoirException;
import org.shanoir.uploader.ShUpOnloadConfig;
import org.shanoir.uploader.dicom.IDicomServerClient;
import org.shanoir.uploader.dicom.query.Media;
import org.shanoir.uploader.dicom.query.Patient;
import org.shanoir.uploader.dicom.query.Study;
import org.shanoir.uploader.gui.ImportFromBidsWindow;
import org.shanoir.uploader.gui.ImportFromCSVWindow;
import org.shanoir.uploader.model.CsvImport;
import org.shanoir.uploader.model.rest.AcquisitionEquipment;
import org.shanoir.uploader.model.rest.Examination;
import org.shanoir.uploader.model.rest.IdList;
import org.shanoir.uploader.model.rest.ImagedObjectCategory;
import org.shanoir.uploader.model.rest.Sex;
import org.shanoir.uploader.model.rest.StudyCard;
import org.shanoir.uploader.model.rest.Subject;
import org.shanoir.uploader.model.rest.SubjectType;
import org.shanoir.uploader.model.rest.importer.ImportJob;
import org.shanoir.uploader.nominativeData.NominativeDataUploadJob;
import org.shanoir.uploader.nominativeData.NominativeDataUploadJobManager;
import org.shanoir.uploader.service.rest.ShanoirUploaderServiceClient;
import org.shanoir.uploader.utils.ImportUtils;
import org.shanoir.uploader.utils.Util;
import org.shanoir.util.ShanoirUtil;

public class ImportFromBidsRunner extends SwingWorker<Void, Integer> {

	private static Logger logger = Logger.getLogger(ImportFromBidsRunner.class);

	private ResourceBundle resourceBundle;
	private ImportFromBidsWindow importFromBidsWindow;
	private IdentifierCalculator identifierCalculator;
	private IDicomServerClient dicomServerClient;
	private ShanoirUploaderServiceClient shanoirUploaderServiceClientNG;

	public ImportFromBidsRunner(ResourceBundle ressourceBundle, ImportFromBidsWindow importFromBidsWindow, IDicomServerClient dicomServerClient, ShanoirUploaderServiceClient shanoirUploaderServiceClientNG) {
		this.resourceBundle = ressourceBundle;
		this.importFromBidsWindow = importFromBidsWindow;
		this.identifierCalculator = new IdentifierCalculator();
		this.dicomServerClient = dicomServerClient;
		this.shanoirUploaderServiceClientNG = shanoirUploaderServiceClientNG;
	}

	@Override
	protected Void doInBackground() throws Exception {
		// Iterate over import to import them one by one
		Set<Long> idList = new HashSet<>();
		Map<String, ArrayList<StudyCard>> studyCardsByStudy = new HashMap<>();

		importFromBidsWindow.openButton.setEnabled(false);
		importFromBidsWindow.uploadButton.setEnabled(false);

		importFromBidsWindow.progressBar.setStringPainted(true);
		importFromBidsWindow.progressBar.setString("Preparing import...");
		importFromBidsWindow.progressBar.setVisible(true);

		List<org.shanoir.uploader.model.rest.Study> studies = new ArrayList<>();
		try {
			studies = shanoirUploaderServiceClientNG.findStudiesNamesAndCenters();
		} catch (Exception e1) {
			//TODO:	this.importFromBidsWindow.error.setText(resourceBundle.getString("shanoir.uploader.import.csv.error.studycard"));
			return null;
		}
		
		if (studies.isEmpty()) {
			return null;
		}
		// Get selector

		boolean success = true;

		if (success) {
			importFromBidsWindow.progressBar.setString("Success !");
			importFromBidsWindow.progressBar.setValue(100);

			// Open current import tab and close import panel
			((JTabbedPane) this.importFromBidsWindow.scrollPaneUpload.getParent().getParent()).setSelectedComponent(this.importFromBidsWindow.scrollPaneUpload.getParent());

			this.importFromBidsWindow.frame.setVisible(false);
			this.importFromBidsWindow.frame.dispose();
		} else {
			importFromBidsWindow.openButton.setEnabled(true);
			importFromBidsWindow.uploadButton.setEnabled(false);
		}
		return null;
	}

}
