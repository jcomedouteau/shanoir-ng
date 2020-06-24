package org.shanoir.ng.importer.eeg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.shanoir.ng.importer.AbstractImporterService;
import org.shanoir.ng.importer.eeg.brainvision.BrainVisionReader;
import org.shanoir.ng.importer.eeg.edf.EDFAnnotation;
import org.shanoir.ng.importer.eeg.edf.EDFParser;
import org.shanoir.ng.importer.eeg.edf.EDFParserResult;
import org.shanoir.ng.importer.model.Channel;
import org.shanoir.ng.importer.model.EegDataset;
import org.shanoir.ng.importer.model.Event;
import org.shanoir.ng.shared.exception.ShanoirImportException;
import org.springframework.stereotype.Service;

/**
 * This class contains all the methods used to import EEG datasets
 * @author JCome
 *
 */
@Service
public class EEGImporterService extends AbstractImporterService {

	/**
	 * Reads a list of .edf files to generate a bunch of datasets.
	 * 
	 * @param datasets         the list of datasets to import
	 * @param dataFileDir      the file directory where we are working
	 * @param edfMatchingFiles the list of .edf files
	 * @throws ShanoirImportException when parsing fails
	 */
	public void readEdfFiles(final File[] edfMatchingFiles, final File dataFileDir, final List<EegDataset> datasets)
			throws ShanoirImportException {
		for (File edfFile : edfMatchingFiles) {

			// Parse the file
			try (FileInputStream edfStream = new FileInputStream(edfFile)) {
				EDFParserResult result = EDFParser.parseEDF(edfStream);

				// Create channels
				List<Channel> channels = new ArrayList<>();
				for (int i = 0; i < result.getHeader().getNumberOfChannels(); i++) {
					Channel chan = new Channel();
					Pattern p = Pattern.compile("HP:(\\d+)k?Hz\\sLP:(\\d+)k?Hz(\\sN:(\\d+)k?Hz)?");
					Matcher m = p.matcher(result.getHeader().getPrefilterings()[i].trim());
					if (m.matches()) {
						chan.setHighCutoff(Integer.parseInt(m.group(1)));
						chan.setLowCutoff(Integer.parseInt(m.group(2)));
						if (m.groupCount() > 2) {
							chan.setNotch(Integer.parseInt(m.group(4)));
						}
					}
					chan.setName(result.getHeader().getChannelLabels()[i].trim());
					chan.setReferenceUnits(result.getHeader().getDimensions()[i].trim());

					channels.add(chan);
				}

				double samplingfrequency = result.getHeader().getNumberOfRecords()
						/ result.getHeader().getDurationOfRecords();

				// Create events
				List<Event> events = new ArrayList<>();
				for (EDFAnnotation annotation : result.getAnnotations()) {
					Event event = new Event();

					// This is done by default
					event.setChannelNumber(0);
					event.setPosition(String.valueOf((float) (samplingfrequency / annotation.getOnSet())));
					event.setPoints((int) annotation.getDuration());
					events.add(event);
				}

				EegDataset dataset = new EegDataset();
				dataset.setEvents(events);
				dataset.setChannels(channels);
				dataset.setChannelCount(result.getHeader().getNumberOfChannels());

				// Get dataset name from EDF file name
				String fileNameWithOutExt = FilenameUtils.removeExtension(edfFile.getName());
				dataset.setName(fileNameWithOutExt);

				dataset.setSamplingFrequency((int) samplingfrequency);

				// Get the list of file to save from reader
				List<String> files = new ArrayList<>();

				File[] filesToSave = dataFileDir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(final File dir, final String name) {
						return name.startsWith(fileNameWithOutExt);
					}
				});
				for (File fi : filesToSave) {
					files.add(fi.getCanonicalPath());
				}
				dataset.setFiles(files);
				datasets.add(dataset);
			} catch (IOException e) {
				throw new ShanoirImportException("Error while parsing file. Please contact an amdinistrator", e);
			}
		}
	}

	/**
	 * Reads a list of .vhdr files to generate a bunch of datasets.
	 * 
	 * @param dataFileDir     the file directory where we are working
	 * @param bvMatchingFiles the list of vhdr files
	 * @param datasets        the list of datasets to import
	 * @return a list of datasets generated from the informations of the .vhdr files
	 * @throws ShanoirImportException when parsing fails
	 */
	public void readBrainvisionFiles(final File[] bvMatchingFiles, final File dataFileDir,
			final List<EegDataset> datasets) throws ShanoirImportException {
		for (File vhdrFile : bvMatchingFiles) {

			// Parse the file
			BrainVisionReader bvr = new BrainVisionReader(vhdrFile);

			EegDataset dataset = new EegDataset();
			dataset.setEvents(bvr.getEvents());
			dataset.setChannels(bvr.getChannels());
			dataset.setChannelCount(bvr.getNbchan());
			// Get dataset name from VHDR file name
			String fileNameWithOutExt = FilenameUtils.removeExtension(vhdrFile.getName());
			dataset.setName(fileNameWithOutExt);

			// Manage when we have a sampling interval but no sampling frequency
			int samplingFrequency = bvr.getSamplingFrequency();
			if (samplingFrequency == 0 && bvr.getSamplingIntervall() != 0) {
				samplingFrequency = Math.round(1000 / bvr.getSamplingIntervall());
			}

			dataset.setSamplingFrequency(samplingFrequency);
			dataset.setCoordinatesSystem(bvr.getHasPosition() ? "true" : null);

			try {
				bvr.close();
			} catch (IOException e) {
				throw new ShanoirImportException("Error while parsing file. Please contact an administrator.", e);
			}

			// Get the list of file to save from reader
			List<String> files = new ArrayList<>();

			File[] filesToSave = dataFileDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(final File dir, final String name) {
					return name.startsWith(fileNameWithOutExt);
				}
			});
			try {
				for (File fi : filesToSave) {
					files.add(fi.getCanonicalPath());

				}
			} catch (IOException e) {
				throw new ShanoirImportException("Error while parsing file. Please contact an administrator.", e);
			}
			dataset.setFiles(files);
			datasets.add(dataset);
		}
	}
}
