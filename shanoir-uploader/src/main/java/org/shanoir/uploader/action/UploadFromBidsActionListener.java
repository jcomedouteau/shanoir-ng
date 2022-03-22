package org.shanoir.uploader.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JFileChooser;

import org.apache.log4j.Logger;
import org.shanoir.uploader.gui.ImportFromBidsWindow;
import org.shanoir.uploader.gui.ImportFromCSVWindow;
import org.shanoir.uploader.model.CsvImport;

public class UploadFromBidsActionListener implements ActionListener {

	JFileChooser fileChooser;
	ImportFromBidsWindow importFromBidsWindow;
	private ResourceBundle resourceBundle;

	private static Logger logger = Logger.getLogger(UploadFromBidsActionListener.class);

	public UploadFromBidsActionListener(ImportFromBidsWindow importFromBidsWindow, ResourceBundle resourceBundle) {
		this.importFromBidsWindow = importFromBidsWindow;
		this.fileChooser = new JFileChooser();
		this.resourceBundle = resourceBundle;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int result = fileChooser.showOpenDialog(importFromBidsWindow);
		if (result == JFileChooser.APPROVE_OPTION) {
			analyzeFile(fileChooser.getSelectedFile());
		}
	}

	/**
	 * Displays the BIDS file, and checks its integrity
	 * @param selectedFile the selected BIDS folder
	 */
	private void analyzeFile(File selectedFile) {
		return;
	}

}
