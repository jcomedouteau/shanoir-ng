package org.shanoir.uploader.gui;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.io.File;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import org.apache.log4j.Logger;
import org.shanoir.uploader.action.ImportFromBidsActionListener;
import org.shanoir.uploader.action.ImportFromCsvActionListener;
import org.shanoir.uploader.action.UploadFromBidsActionListener;
import org.shanoir.uploader.action.UploadFromCsvActionListener;
import org.shanoir.uploader.dicom.IDicomServerClient;
import org.shanoir.uploader.model.CsvImport;
import org.shanoir.uploader.service.rest.ShanoirUploaderServiceClient;

public class ImportFromBidsWindow extends JFrame {

	public JButton uploadButton;
	public JButton openButton;
	public JProgressBar progressBar;

	private static Logger logger = Logger.getLogger(ImportFromBidsWindow.class);

	public File shanoirUploaderFolder;
	public ResourceBundle resourceBundle;
	public JFrame frame;
	public JLabel error = new JLabel();
	public JLabel csvDetail = new JLabel();

	final JPanel masterPanel;

	UploadFromBidsActionListener uploadListener;
	ImportFromBidsActionListener importListener;
	IDicomServerClient dicomServerClient;
	ShanoirUploaderServiceClient shanoirUploaderServiceClient;
	public JScrollPane scrollPaneUpload;

	public ImportFromBidsWindow(File shanoirUploaderFolder, ResourceBundle resourceBundle, JScrollPane scrollPaneUpload, IDicomServerClient dicomServerClient, ShanoirUploaderServiceClient shanoirUploaderServiceClientNG) {
		this.shanoirUploaderFolder = shanoirUploaderFolder;
		this.resourceBundle = resourceBundle;
		this.dicomServerClient = dicomServerClient;
		this.shanoirUploaderServiceClient = shanoirUploaderServiceClientNG;
		this.scrollPaneUpload = scrollPaneUpload;

		// Create the frame.
		frame = new JFrame(resourceBundle.getString("shanoir.uploader.import.bids.title"));
		frame.setSize(1600, 700);
		this.setSize(1600, 700);

		// What happens when the frame closes?
		frame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

		// Panel content
		masterPanel = new JPanel(new GridBagLayout());
		masterPanel.setLayout(new GridBagLayout());
		masterPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		frame.setContentPane(masterPanel);

		// CSV description here
		this.csvDetail.setText(resourceBundle.getString("shanoir.uploader.import.bids.detail"));

		// Potential error here
		GridBagConstraints gBCError = new GridBagConstraints();
		gBCError.anchor = GridBagConstraints.NORTHWEST;
		gBCError.gridx = 0;
		gBCError.gridy = 1;
		this.error.setForeground(Color.RED);
		masterPanel.add(this.error, gBCError);

		// OPEN button here
		openButton = new JButton(resourceBundle.getString("X"));
		GridBagConstraints gBCOpenButton = new GridBagConstraints();
		gBCOpenButton.anchor = GridBagConstraints.NORTHWEST;
		gBCOpenButton.gridx = 0;
		gBCOpenButton.gridy = 2;
		openButton.setEnabled(true);
		masterPanel.add(openButton, gBCOpenButton);

		uploadListener = new UploadFromBidsActionListener(this, resourceBundle);
		openButton.addActionListener(uploadListener);

		// IMPORT button here when necessary
		uploadButton = new JButton(resourceBundle.getString("shanoir.uploader.import.csv.button.import"));
		GridBagConstraints gBCuploadButton = new GridBagConstraints();
		gBCuploadButton.anchor = GridBagConstraints.NORTHWEST;
		gBCuploadButton.gridx = 0;
		gBCuploadButton.gridy = 4;
		uploadButton.setEnabled(false);
		masterPanel.add(uploadButton, gBCuploadButton);
		
		progressBar = new JProgressBar(0);
		GridBagConstraints gBCProgressBar = new GridBagConstraints();
		gBCProgressBar.anchor = GridBagConstraints.NORTHWEST;
		gBCProgressBar.gridx = 0;
		gBCProgressBar.gridy = 5;
		progressBar.setVisible(false);
		masterPanel.add(progressBar, gBCProgressBar);
	
		// importListener = new ImportFromCsvActionListener(this, resourceBundle, dicomServerClient, shanoirUploaderFolder, shanoirUploaderServiceClientNG);

		uploadButton.addActionListener(importListener);

		// center the frame
		// frame.setLocationRelativeTo( null );
		Point center = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		int windowWidth = 1600;
		int windowHeight = 700;
		// set position and size
		frame.setBounds(center.x - windowWidth / 2, center.y - windowHeight / 2, windowWidth, windowHeight);

		// Show it.
		frame.setVisible(true);
	}

	/**
	 * Displays an error in the  located error field
	 * @param string the generated error to display
	 */
	public void displayError(String string) {
		this.error.setText(string);
		this.error.setVisible(true);
	}

	public void displayTree(List<CsvImport> imports) {

	}
}
