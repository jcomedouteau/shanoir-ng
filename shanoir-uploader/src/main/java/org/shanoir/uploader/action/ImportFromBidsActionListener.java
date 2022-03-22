package org.shanoir.uploader.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.shanoir.ng.shared.exception.ShanoirException;
import org.shanoir.uploader.dicom.IDicomServerClient;
import org.shanoir.uploader.gui.ImportFromBidsWindow;
import org.shanoir.uploader.gui.ImportFromCSVWindow;
import org.shanoir.uploader.model.CsvImport;
import org.shanoir.uploader.model.rest.AcquisitionEquipment;
import org.shanoir.uploader.model.rest.IdList;
import org.shanoir.uploader.model.rest.StudyCard;
import org.shanoir.uploader.service.rest.ShanoirUploaderServiceClient;

/**
 * This class is used after 'import' button from CSV importer.
 * It loads the list of imports to do, then imports them one by one
 * Managing errors and displays
 * @author fli
 *
 */
public class ImportFromBidsActionListener implements ActionListener {

	ImportFromBidsWindow importFromBidsWindow;
	IDicomServerClient dicomServerClient;
	File shanoirUploaderFolder;

	ShanoirUploaderServiceClient shanoirUploaderServiceClientNG;
	private ResourceBundle resourceBundle;

	public ImportFromBidsActionListener(ImportFromBidsWindow importFromBidsWindow, ResourceBundle resourceBundle, IDicomServerClient dicomServerClient, File shanoirUploaderFolder, ShanoirUploaderServiceClient shanoirUploaderServiceClientNG) {
		this.importFromBidsWindow = importFromBidsWindow;
		this.dicomServerClient = dicomServerClient;
		this.shanoirUploaderFolder = shanoirUploaderFolder;
		this.shanoirUploaderServiceClientNG = shanoirUploaderServiceClientNG;
		this.resourceBundle = resourceBundle;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ImportFromBidsRunner importer = new ImportFromBidsRunner(resourceBundle, importFromBidsWindow, dicomServerClient, shanoirUploaderServiceClientNG);
		importer.execute();
	}

}
