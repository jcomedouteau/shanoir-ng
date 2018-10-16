package org.shanoir.ng.preclinical.anesthetics.examination_anesthetics;

import java.util.List;

import org.shanoir.ng.preclinical.anesthetics.anesthetic.Anesthetic;
import org.shanoir.ng.shared.exception.ShanoirException;
import org.shanoir.ng.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Examination anesthetic service implementation.
 * 
 * @author sloury
 *
 */
@Service
public class ExaminationAnestheticServiceImpl implements ExaminationAnestheticService {

	/**
	 * Logger
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ExaminationAnestheticServiceImpl.class);

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private ExaminationAnestheticRepository examAnestheticsRepository;

	@Override
	public void deleteById(final Long id) throws ShanoirException {
		examAnestheticsRepository.delete(id);
	}

	@Override
	public List<ExaminationAnesthetic> findAll() {
		return Utils.toList(examAnestheticsRepository.findAll());
	}

	@Override
	public List<ExaminationAnesthetic> findByExaminationId(Long examinationId) {
		return Utils.toList(examAnestheticsRepository.findByExaminationId(examinationId));
	}

	@Override
	public List<ExaminationAnesthetic> findBy(final String fieldName, final Object value) {
		return examAnestheticsRepository.findBy(fieldName, value);
	}

	@Override
	public ExaminationAnesthetic findById(final Long id) {
		return examAnestheticsRepository.findOne(id);
	}

	@Override
	public ExaminationAnesthetic save(final ExaminationAnesthetic examAnesthetic) throws ShanoirException {
		ExaminationAnesthetic savedExamAnesthetic = null;
		try {
			savedExamAnesthetic = examAnestheticsRepository.save(examAnesthetic);
		} catch (DataIntegrityViolationException dive) {
			LOG.error("Error while creating an  examination anesthetic:  ", dive);
			throw new ShanoirException("Error while creating an  examination anesthetic:  ", dive);
		}
		return savedExamAnesthetic;
	}

	@Override
	public ExaminationAnesthetic update(final ExaminationAnesthetic examAnesthetic) throws ShanoirException {
		final ExaminationAnesthetic examAnestheticDb = examAnestheticsRepository.findOne(examAnesthetic.getId());
		updateModelValues(examAnestheticDb, examAnesthetic);
		try {
			examAnestheticsRepository.save(examAnestheticDb);
		} catch (Exception e) {
			LOG.error("Error while updating an  examination anesthetic:  ", e);
			throw new ShanoirException("Error while updating an  examination anesthetic:  ", e);
		}
		return examAnestheticDb;
	}

	@Override
	public List<ExaminationAnesthetic> findByAnesthetic(Anesthetic anesthetic) {
		return Utils.toList(examAnestheticsRepository.findByAnesthetic(anesthetic));
	}

	private ExaminationAnesthetic updateModelValues(final ExaminationAnesthetic examAnestheticDb,
			final ExaminationAnesthetic examAnesthetic) {
		examAnestheticDb.setAnesthetic(examAnesthetic.getAnesthetic());
		examAnestheticDb.setDose(examAnesthetic.getDose());
		examAnestheticDb.setDoseUnit(examAnesthetic.getDoseUnit());
		examAnestheticDb.setInjectionInterval(examAnesthetic.getInjectionInterval());
		examAnestheticDb.setInjectionSite(examAnesthetic.getInjectionSite());
		examAnestheticDb.setInjectionType(examAnesthetic.getInjectionType());
		examAnestheticDb.setStartDate(examAnesthetic.getStartDate());
		examAnestheticDb.setEndDate(examAnesthetic.getEndDate());
		return examAnestheticDb;
	}

}
