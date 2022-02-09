package com.symphony.conf.sdk.lib.config;

import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okio.Okio;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Okhttp signer that signs the requests with aws4 signature. */
@Log4j2
class AwsSignerInterceptor implements Interceptor {

  public static final String EXECUTE_API = "execute-api";
  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String X_AMZ_DATE_HEADER = "X-Amz-Date";

  private final Aws4Signer signer;
  private final String region;
  private final AwsCredentialsProvider credentialsProvider;

  /**
   * Create a new instance of {@link AwsSignerInterceptor}.
   *
   * @param region AWS the region where the config service is located
   */
  public AwsSignerInterceptor(String region) {
    this.region = region;
    this.signer = Aws4Signer.create();
    this.credentialsProvider = DefaultCredentialsProvider.builder().build();
  }

  @NotNull
  @Override
  public Response intercept(@NotNull Chain chain) throws IOException {

    Aws4SignerParams params =
        Aws4SignerParams.builder()
            .signingName(EXECUTE_API)
            .signingRegion(Region.of(region))
            .awsCredentials(credentialsProvider.resolveCredentials())
            .build();
    SdkHttpFullRequest awsRequest = toAwsRequest(chain.request());

    final SdkHttpFullRequest awsSignedRequest = signer.sign(awsRequest, params);

    Request okHttpSignedRequest =
        chain
            .request()
            .newBuilder()
            .removeHeader(AUTHORIZATION_HEADER)
            .addHeader(
                AUTHORIZATION_HEADER,
                awsSignedRequest.firstMatchingHeader(AUTHORIZATION_HEADER).orElse(""))
            .addHeader(
                X_AMZ_DATE_HEADER,
                awsSignedRequest.firstMatchingHeader(X_AMZ_DATE_HEADER).orElse(""))
            .build();

    return chain.proceed(okHttpSignedRequest);
  }

  private SdkHttpFullRequest toAwsRequest(Request request) {
    return SdkHttpFullRequest.builder()
        .method(SdkHttpMethod.valueOf(request.method()))
        .uri(request.url().uri())
        .rawQueryParameters(toParams(request))
        .headers(request.headers().toMultimap())
        // otherwise AWS adds this header twice and canonical headers are incorrect
        .removeHeader("host")
        .contentStreamProvider(toBody(request))
        .build();
  }

  private ContentStreamProvider toBody(Request request) {
    if (request.body() != null) {
      try {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        request.body().writeTo(Okio.buffer(Okio.sink(outputStream)));
        return RequestBody.fromBytes(outputStream.toByteArray()).contentStreamProvider();
      } catch (IOException e) {
        LOG.warn("Failed to copy body: {}", e.getMessage());
      }
    }
    return null;
  }

  private Map<String, List<String>> toParams(Request request) {
    final Map<String, List<String>> result = new HashMap<>();
    final Set<String> parameterNames = request.url().queryParameterNames();
    parameterNames.forEach(
        name ->
            result.put(name, List.of(Objects.requireNonNull(request.url().queryParameter(name)))));
    return result;
  }
}
