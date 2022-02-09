package com.symphony.conf.sdk.lib.config;

import com.symphony.conf.sdk.lib.config.exceptions.CommunicationException;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

/**
 * Client SDK to communicate with Symphony Configuration Service deployed in AWS.
 */
@Log4j2
public class SymConfigurationServiceClient {

  private static final String KEYS_QUERY_PARAM = "keys";
  private static final String ENV_PARAM = "env";
  private static final String REGION_PARAM = "region";
  private static final String PLATFORM_PARAM = "platform";
  private static final String CONFIGURATION_PARAM = "configuration";

  private final ObjectMapper objectMapper;
  private final URIBuilder uriBuilder;
  private final OkHttpClient httpClient;

  /**
   * Create a new instance of {@link SymConfigurationServiceClient}.
   *
   * @param commonServiceProperties mostly environment properties of the running service
   * @param awsProperties           AWS related properties of application
   * @param objectMapper            object mapper to be used to parse JSON
   */
  public SymConfigurationServiceClient(
      CommonServiceProperties commonServiceProperties,
      AwsSymConfigProperties awsProperties,
      ObjectMapper objectMapper) throws URISyntaxException {
    this.objectMapper = objectMapper;

    this.uriBuilder =

        new URIBuilder(awsProperties.getUrl())
            .setPathSegments(CONFIGURATION_PARAM, "v1", "kvs")
            .addParameter(ENV_PARAM, commonServiceProperties.getEnv())
            .addParameter(REGION_PARAM, commonServiceProperties.getRegion())
            .addParameter(PLATFORM_PARAM, commonServiceProperties.getPlatform());
    httpClient =
        new OkHttpClient.Builder()
            .addNetworkInterceptor(new AwsSignerInterceptor(awsProperties.getRegion()))
            .build();
  }

  /**
   * Get properties for the product/client/key.
   *
   * @param service service to get config for
   * @param tenant  tenant to get config for
   * @param keys    keys to get values for
   * @return values for the key
   * @throws CommunicationException in case response from config service wasn't successful, or the request to it failed
   */
  public List<KeyValue> getAllKeyValuesForKey(String service, String tenant, String... keys) throws URISyntaxException {
    final String uri =
        new URIBuilder(uriBuilder.toString())
            .addParameter("tenant", tenant)
            .addParameter("service", service)
            .addParameter(KEYS_QUERY_PARAM, String.join(",", keys))
            .toString();
    Request request = new Request.Builder().url(uri).build();
    try {
      Response response = httpClient.newCall(request).execute();
      if (response.isSuccessful()) {
        return List.of(
            objectMapper.readValue(
                Objects.requireNonNull(response.body()).string(), KeyValue[].class));
      } else {
        logAwsErrors(response);
        LOG.warn(
            "Error response form Config Service while trying to read key '{}'. "
                + "Code: {}, Message: '{}'",
            keys,
            response.code(),
            response.code());
        throw new CommunicationException(response.message());
      }
    } catch (IOException e) {
      throw new CommunicationException(e.getMessage());
    }
  }

  private void logAwsErrors(Response response) {
    try {
      LOG.debug("AWS RESP payload: {}", new String(response.body().byteStream().readAllBytes()));
    } catch (IOException e) {
      // ignore
    }
    StreamSupport.stream(response.headers().spliterator(), false)
        .forEach(pair -> LOG.debug("AWS RESP: {}:{}", pair.getFirst(), pair.getSecond()));
  }
}
