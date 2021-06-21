package org.shanoir.ng.migration;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.shanoir.ng.datasetacquisition.model.DatasetAcquisition;
import org.shanoir.ng.datasetfile.DatasetFile;
import org.shanoir.ng.examination.model.Examination;
import org.shanoir.ng.shared.exception.ShanoirException;
import org.shanoir.ng.shared.migration.DistantKeycloakConfigurationService;
import org.shanoir.ng.studycard.model.StudyCard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class DistantDatasetShanoirService {

	

	private static final String CREATE_EXAMINATION = "/shanoir-ng/datasets/examinations/";
	
	private static final String ADD_EXTRA_DATA = "/shanoir-ng/datasets/examinations/";

	private static final String CREATE_STUDY_CARD = "/shanoir-ng/datasets/studycards/";
	
	private static final String CREATE_DATASET_ACQUISITION = "/shanoir-ng/datasets/datasetacquisition/";

	private static final String ADD_FILE = "/shanoir-ng/datasets/datasets/";

	RestTemplate restTemplate;

	@Autowired
	DistantKeycloakConfigurationService distantKeycloak;

	private static final TrustManager[] UNQUESTIONING_TRUST_MANAGER = new TrustManager[]{
			new X509TrustManager() {
				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers(){
					return null;
				}
				@Override
				public void checkClientTrusted( X509Certificate[] certs, String authType ){}
				@Override
				public void checkServerTrusted( X509Certificate[] certs, String authType ){}
			}
	};

	public DistantDatasetShanoirService() {
		// Instanciate a "weak" rest template.
		TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

		SSLContext sslContext;
		try {
			sslContext = org.apache.http.ssl.SSLContexts.custom()
					.loadTrustMaterial(null, acceptingTrustStrategy)
					.build();

			SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

			CloseableHttpClient httpClient = HttpClients.custom()
					.setSSLSocketFactory(csf)
					.build();

			HttpComponentsClientHttpRequestFactory requestFactory =
					new HttpComponentsClientHttpRequestFactory();

			requestFactory.setHttpClient(httpClient);
			restTemplate = new RestTemplate(requestFactory);
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			// POUTOU
		}
	}

	
	public Examination createExamination(Examination examination) throws ShanoirException {
		try {
			ResponseEntity<Examination> response = this.restTemplate.exchange(getURI(CREATE_EXAMINATION), HttpMethod.POST, new HttpEntity<>(examination, getHeader()), Examination.class);
			if (HttpStatus.OK.equals(response.getStatusCode())) {
				return response.getBody();
			} else {
				throw new ShanoirException("Could not create a new distant examination {} {}" + response.getStatusCode() + response.getBody());
			}
		} catch (Exception e) {
			throw new ShanoirException("Could not create a new distant examination: ", e);
		}
	}

	public void moveDatasetFile(DatasetFile dsFile, File file) throws ShanoirException {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			headers.add("Authorization", "Bearer " + distantKeycloak.getAccessToken());

			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("file", file);

			HttpEntity<MultiValueMap<String, Object>> requestEntity	= new HttpEntity<>(body, headers);

			restTemplate.postForEntity(getURI(ADD_FILE + dsFile.getId() + File.separator + dsFile.getDatasetExpression().getDataset().getId()), requestEntity, Void.class);
		} catch (Exception e) {
			throw new ShanoirException("Could not add dataset file on dataset: ", e);
		}
	}

	public StudyCard createStudyCard(StudyCard sc) throws ShanoirException {
		try {
			ResponseEntity<StudyCard> response = this.restTemplate.exchange(getURI(CREATE_STUDY_CARD), HttpMethod.POST, new HttpEntity<>(sc, getHeader()), StudyCard.class);
			if (HttpStatus.OK.equals(response.getStatusCode())) {
				return response.getBody();
			} else {
				throw new ShanoirException("Could not create a new distant studyCard {} {}" + response.getStatusCode() + response.getBody());
			}
		} catch (Exception e) {
			throw new ShanoirException("Could not create a new distant studyCard: ", e);
		}
	}

	public DatasetAcquisition createAcquisition(DatasetAcquisition acq) throws ShanoirException {
		try {
			ResponseEntity<DatasetAcquisition> response = this.restTemplate.exchange(getURI(CREATE_DATASET_ACQUISITION), HttpMethod.POST, new HttpEntity<>(acq, getHeader()), DatasetAcquisition.class);
			if (HttpStatus.OK.equals(response.getStatusCode())) {
				return response.getBody();
			} else {
				throw new ShanoirException("Could not create a new distant acquisition {} {}" + response.getStatusCode() + response.getBody());
			}
		} catch (Exception e) {
			throw new ShanoirException("Could not create a new distant acquisition: ", e);
		}
	}
	
	public void addExminationExtraData(File file, String examId) throws ShanoirException {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			headers.add("Authorization", "Bearer " + distantKeycloak.getAccessToken());

			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("file", file);

			HttpEntity<MultiValueMap<String, Object>> requestEntity	= new HttpEntity<>(body, headers);

			restTemplate.postForEntity(getURI(ADD_EXTRA_DATA + examId), requestEntity, Void.class);
		} catch (Exception e) {
			throw new ShanoirException("Could not add protocol file on study: ", e);
		}
	}

	private HttpHeaders getHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + distantKeycloak.getAccessToken());
		return headers;
	}

	public URI getURI(String apiHeader) throws URISyntaxException {
		return new URI(distantKeycloak.getServer() + apiHeader);
	}


}
