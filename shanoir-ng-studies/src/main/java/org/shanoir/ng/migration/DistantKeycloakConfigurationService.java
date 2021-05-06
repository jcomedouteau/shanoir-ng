package org.shanoir.ng.migration;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * This class is used to connect to a distant Shanoir using an URL, a username and a password, and to keep open the connection
 * @author fli
 *
 */
@Service
public class DistantKeycloakConfigurationService {

	private static final Logger LOG = LoggerFactory.getLogger(DistantKeycloakConfigurationService.class);
	
	@Autowired
	RestTemplate restTemplate;
	
	private String token;

	private ScheduledExecutorService executor;

	/**
	 * Connects to a distant keycloak and keeps the connection alive
	 * @param shanoirUrl
	 * @param username
	 * @param userPassword
	 */
	public void connectToDistantKeycloak(String shanoirUrl, String username, String userPassword) {
		// Connect
		String keycloakURL = shanoirUrl + "/auth/realms/shanoir-ng/protocol/openid-connect/token";
		try {
			final StringBuilder postBody = new StringBuilder();
			postBody.append("client_id=shanoir-uploader");
			postBody.append("&grant_type=password");
			postBody.append("&username=").append(URLEncoder.encode(username, "UTF-8"));
			postBody.append("&password=").append(URLEncoder.encode(userPassword, "UTF-8"));
			postBody.append("&scope=offline_access");
			
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
			headers.set("Content-type", "application/x-www-form-urlencoded");

			ResponseEntity<String> response = restTemplate.exchange(keycloakURL, HttpMethod.POST, new HttpEntity<>(postBody.toString(), headers), String.class);
			// Keep connection alive
			final int statusCode = response.getStatusCodeValue();
			if (HttpStatus.SC_OK == statusCode) {
				JSONObject responseEntityJson = new JSONObject(response);
				String refreshToken = responseEntityJson.getString("refresh_token");
				this.refreshToken(keycloakURL, refreshToken);
			}
		} catch (Exception e) {
			System.err.println("Could not connect to keycloak");
			e.printStackTrace();
		}
	}


	/**
	 * Start job, that refreshes the access token every 240 seconds.
	 * The default access token lifetime of Keycloak is 5 min (300 secs),
	 * we update after 4 min (240 secs) to use the time frame, but not to
	 * be to close to the end.
	 */
	private void refreshToken(String keycloakURL, String refreshToken) {
		final StringBuilder postBody = new StringBuilder();
		postBody.append("client_id=shanoir-uploader");
		postBody.append("&grant_type=refresh_token");
		postBody.append("&refresh_token=").append(refreshToken);
		executor = Executors.newScheduledThreadPool(1);
		Runnable task = () -> {
			try {
				HttpHeaders headers = new HttpHeaders();
				headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
				headers.set("Content-type", "application/x-www-form-urlencoded");

				ResponseEntity<String> response = restTemplate.exchange(keycloakURL, HttpMethod.POST, new HttpEntity<>(postBody.toString(), headers), String.class);

				// Keep connection alive
				final int statusCode = response.getStatusCodeValue();
				if (HttpStatus.SC_OK == statusCode) {
					JSONObject responseEntityJson = new JSONObject(response);
					String newAccessToken = responseEntityJson.getString("access_token");
					if (newAccessToken != null) {
						token = newAccessToken;
					} else {
						LOG.info("ERROR: with access token refresh.");
					}
					LOG.info("Access token has been refreshed.");
				} else {
					LOG.info("ERROR: Access token could NOT be refreshed: HttpStatus-" + statusCode);
				}
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		};
		executor.scheduleAtFixedRate(task, 0, 240, TimeUnit.SECONDS);
	}

	/**
	 * Get the access token
	 * @return the access token
	 */
	public String getAccessToken() {
		return this.token;
	}

	/**
	 * Stop the distant keycloak connection
	 */
	public void stop() {
		this.executor.shutdown();
	}
}
