package org.shanoir.ng.processing.vip;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.aspectj.util.FileUtil;
import org.shanoir.ng.processing.carmin.model.CarminDatasetProcessing;
import org.shanoir.ng.processing.carmin.model.Execution;
import org.shanoir.ng.processing.carmin.model.ExecutionStatus;
import org.shanoir.ng.processing.carmin.service.CarminDatasetProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiParam;

@Controller
public class VipFakeApiController implements VipFakeApi {

	private static final Logger LOG = LoggerFactory.getLogger(VipFakeApiController.class);

	@Autowired
	ObjectMapper mapper;
	
	@Autowired
	CarminDatasetProcessingService carminDatasetProcessingService;

	String contentStr;
	
	public ResponseEntity<List<Pipeline>> getProcessings() throws JsonMappingException, JsonProcessingException {
		contentStr = 
		"{"
		+ "\"identifier\":\"ct-tiqua1.4\","
		+ "\"name\":\"ct-tiqua\","
		+ "\"description\":\"Computed Tomography based Traumatic Brain Injury Quantification. This software takes as input a CT-scan of a head injured patient and returns: 1/ the segmentation of 7 types of lesions typical from TBI, 2/ a structural atlas dividing the input brain in 10 zones, and 3/ a csv file containing the volume in mm3 of all type on lesion in all zone of the brain. It was developed by Clement Brossard and Benjamin Lemasson (benjamin.lemasson@univ-grenoble-alpes.fr). We are currently writing a scientific article descibing the whole process. If you use our software, please cite our work!\","
		+ "\"version\":\"1.4\","
		+ "\"parameters\":"
		+ "[{"
		+ "\"name\":\"ensemble\","
		+ "\"type\":\"Boolean\","
		+ "\"description\":\"For the segmentation step: Whether to use all the 12 CNN models rather than only one (slower but more precise).\","
		+ "\"isOptional\":true,"
		+ "\"isReturnedValue\":false"
		+ "},{"
		+ "\"name\":\"keep_tmp_files\","
		+ "\"type\":\"Boolean\","
		+ "\"description\":\"Do not remove temporary files at the end of the pipeline, ie. include them in the output archive.\","
		+ "\"isOptional\":true,"
		+ "\"isReturnedValue\":false"
		+ "},{"
		+ "\"name\":\"Localizer\","
		+ "\"type\":\"File\","
		+ "\"description\":\"Input image (e.g. img.nii, img.nii.gz) to be processed. It must be a brain CT-scan.\","
		+ "\"isOptional\":false,"
		+ "\"isReturnedValue\":false"
		+ "},{"
		+ "\"name\":\"TSE-X\","
		+ "\"type\":\"File\","
		+ "\"description\":\"Input 2 image (e.g. img.nii, img.nii.gz) to be processed. It must be a Muscular CT-scan.\","
		+ "\"isOptional\":false,"
		+ "\"isReturnedValue\":false"
		+ "}],"
		+ "\"canExecute\":true"
		+ "}";
		Pipeline pipeline = mapper.readValue(contentStr, Pipeline.class);
		return new ResponseEntity<List<Pipeline>>(Collections.singletonList(pipeline), HttpStatus.OK);
	}

    public ResponseEntity<Pipeline> getProcessing(@ApiParam(value = "id of the pipeline", required = true) @PathVariable("pipelineId") String pipelineId) throws JsonMappingException, JsonProcessingException {
		Pipeline pipeline = mapper.readValue(contentStr, Pipeline.class);
		return new ResponseEntity<Pipeline>(pipeline, HttpStatus.OK);
    }

    public ResponseEntity<Execution> createExecution(@ApiParam(value = "execution to create", required = true) @RequestBody Execution execution) {
    	execution.setIdentifier("localIdentifier" + UUID.randomUUID());
    	return new ResponseEntity<Execution>(execution, HttpStatus.OK);
    }

	public ResponseEntity<Execution> getExecution(@ApiParam(value = "identifier of the execution", required = true) @PathVariable("identifier") String identifier) throws IOException {
		
		CarminDatasetProcessing processing = carminDatasetProcessingService.findByIdentifier(identifier).get();
		
		Execution execution = new Execution();
		execution.setStatus(ExecutionStatus.FINISHED);
		execution.setIdentifier(identifier);
		execution.setResultsLocation(processing.getResultsLocation());
		
		File fileWhereToCopy = new File("/tmp/temp/" + processing.getResultsLocation());
		fileWhereToCopy.mkdirs();
		FileUtil.copyDir(new File("/tmp/temp/result"), fileWhereToCopy);
		
		return new ResponseEntity<Execution>(execution, HttpStatus.OK);
	}

}
