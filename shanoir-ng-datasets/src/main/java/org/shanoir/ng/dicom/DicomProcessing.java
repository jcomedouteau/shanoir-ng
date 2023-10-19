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

package org.shanoir.ng.dicom;

import java.io.File;
import java.io.IOException;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.shanoir.ng.download.AcquisitionAttributes;
import org.shanoir.ng.download.ExaminationAttributes;
import org.shanoir.ng.importer.dto.Dataset;
import org.shanoir.ng.importer.dto.DatasetFile;
import org.shanoir.ng.importer.dto.Serie;
import org.shanoir.ng.importer.dto.Study;
import org.shanoir.ng.shared.exception.PacsException;
import org.springframework.stereotype.Service;

@Service
public class DicomProcessing {
	
	public Attributes getDicomObjectAttributes(DatasetFile image, Boolean isEnhancedMR) throws IOException {
		File dicomFile = new File(image.getPath());
		try (DicomInputStream dIS = new DicomInputStream(dicomFile)) {
			Attributes datasetAttributes;
			if (isEnhancedMR != null && isEnhancedMR) {
				// In case of Enhanced MR, we need to the pixel data in order to use Dcm4chee emf extract method.
				datasetAttributes = dIS.readDataset(-1);
			} else {
				// Else we do not load the picture in Ram for faster performance.
				datasetAttributes = dIS.readDataset(Tag.PixelData);
			}
			return datasetAttributes;
		}
	}

    public ExaminationAttributes getDicomExaminationAttributes(Study study, Boolean isEnhanced) throws PacsException {
		ExaminationAttributes attributes = new ExaminationAttributes();
		if (study != null) {
			for (Serie serie : study.getSeries()) {
				attributes.addAcquisitionAttributes(serie.hashCode(), getDicomAcquisitionAttributes(serie, isEnhanced));
			}
		}
		return attributes;
    }

	public ExaminationAttributes getDicomExaminationAttributes(Study study) throws PacsException {
		ExaminationAttributes attributes = new ExaminationAttributes();
		if (study != null) {
			for (Serie serie : study.getSeries()) {
				attributes.addAcquisitionAttributes(serie.hashCode(), getDicomAcquisitionAttributes(serie));
			}
		}
		return attributes;
    }

	public AcquisitionAttributes getDicomAcquisitionAttributes(Serie serie, Boolean isEnhanced) throws PacsException {
		AcquisitionAttributes attributes = new AcquisitionAttributes();
		for (Dataset dataset : serie.getDatasets()) {
			try {
				attributes.addDatasetAttributes(dataset.hashCode(), getDicomObjectAttributes(serie.getFirstDatasetFileForCurrentSerie(), isEnhanced));
			} catch (IOException e) {
				throw new PacsException("Could not get dicom metadata for serie " + serie.getSopClassUID(), e);
			}
		}
		return attributes;
	}

	public AcquisitionAttributes getDicomAcquisitionAttributes(Serie serie) throws PacsException {
		return getDicomAcquisitionAttributes(serie, serie.getIsEnhanced());
	}

}
