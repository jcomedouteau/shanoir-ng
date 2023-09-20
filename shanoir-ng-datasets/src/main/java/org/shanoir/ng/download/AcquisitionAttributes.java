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

package org.shanoir.ng.download;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.dcm4che3.data.Attributes;

public class AcquisitionAttributes {

	private ConcurrentMap<Long, Attributes> datasetMap = new ConcurrentHashMap<>();

	public Attributes getDatasetAttributes(long id) {
		return datasetMap.get(id);
	}

	public List<Attributes> getAllDatasetAttributes() {
		return new ArrayList<>(datasetMap.values());
	}

	public void addDatasetAttributes(long id, Attributes attributes) {
		this.datasetMap.put(id, attributes);
	}

	@Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Long dsId : datasetMap.keySet()) {
            sb.append("dataset ").append(dsId).append("\n");
            for(String line : datasetMap.get(dsId).toString(1000, 1000).split("\n")) {
                sb.append("\t").append(line).append("\n");
            }
        }
        return sb.toString();
    }

	public Set<Long> getDatasetIds() {
        return datasetMap.keySet();
    }

	public void merge(AcquisitionAttributes dicomAcquisitionAttributes) {
		for (Long datasetId : dicomAcquisitionAttributes.getDatasetIds()) {
			addDatasetAttributes(datasetId, dicomAcquisitionAttributes.getDatasetAttributes(datasetId));
		}
	}

    public Attributes getFirstDatasetAttributes() {
        if (datasetMap != null && datasetMap.size() > 0) {
			return datasetMap.entrySet().iterator().next().getValue();
		} else {
			return null;
		}
    }
}