package org.shanoir.ng.utils;

import org.shanoir.ng.acquisitionequipment.AcquisitionEquipment;
import org.shanoir.ng.center.Center;
import org.shanoir.ng.manufacturermodel.DatasetModalityType;
import org.shanoir.ng.manufacturermodel.Manufacturer;
import org.shanoir.ng.manufacturermodel.ManufacturerModel;
import org.shanoir.ng.study.Study;
import org.shanoir.ng.subject.Subject;

/**
 * Utility class for test. Generates models.
 * 
 * @author msimon
 *
 */
public final class ModelsUtil {

	// Acquisition equipment data
	public static final String ACQ_EQPT_SERIAL_NUMBER = "123456789";

	// Center data
	public static final String CENTER_NAME = "center";

	// Manufacturer data
	public static final String MANUFACTURER_NAME = "manufacturer";

	// Manufacturer model data
	public static final String MANUFACTURER_MODEL_NAME = "manufacturerModel";

	// Study data
	public static final String STUDY_NAME = "study";

	// Subject data
	public static final String SUBJECT_NAME = "subject";

	/**
	 * Create a center.
	 * 
	 * @return center.
	 */
	public static Center createCenter() {
		final Center center = new Center();
		center.setName(CENTER_NAME);
		return center;
	}

	/**
	 * Create an acquisition equipment.
	 * 
	 * @return acquisition equipment.
	 */
	public static AcquisitionEquipment createAcquisitionEquipment() {
		final AcquisitionEquipment equipment = new AcquisitionEquipment();
		equipment.setCenter(createCenter());
		equipment.setManufacturerModel(createManufacturerModel());
		equipment.setSerialNumber(ACQ_EQPT_SERIAL_NUMBER);
		return equipment;
	}

	/**
	 * Create a manufacturer model.
	 * 
	 * @return manufacturer model.
	 */
	public static ManufacturerModel createManufacturerModel() {
		final ManufacturerModel manufacturerModel = new ManufacturerModel();
		manufacturerModel.setDatasetModalityType(DatasetModalityType.MR_DATASET);
		manufacturerModel.setMagneticField(3D);
		final Manufacturer manufacturer = createManufacturer();
		manufacturer.setId(1L);
		manufacturerModel.setManufacturer(manufacturer);
		manufacturerModel.setName(MANUFACTURER_MODEL_NAME);
		return manufacturerModel;
	}

	/**
	 * Create a manufacturer.
	 * 
	 * @return manufacturer.
	 */
	public static Manufacturer createManufacturer() {
		final Manufacturer manufacturer = new Manufacturer();
		manufacturer.setName(MANUFACTURER_NAME);
		return manufacturer;
	}

	/**
	 * Create a study.
	 * 
	 * @return study.
	 */
	public static Study createStudy() {
		final Study study = new Study();
		study.setName(STUDY_NAME);
		return study;
	}

	public static Subject createSubject() {
		final Subject subject = new Subject();
		subject.setName(SUBJECT_NAME);
		/*
		 * subject.setBirthDate(null); subject.setIdentifier(null);
		 * subject.setPseudonymusHashValues(null); subject.setSex(null);
		 * subject.setLinks(null); subject.setSubjectStudyList(null);
		 */
		return subject;
	}

}
