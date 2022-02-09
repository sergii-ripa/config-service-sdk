package com.symphony.conf.sdk.lib.config;

import lombok.Data;

/**
 * Properties for GCS (Google Cloud Storage) backed content storage.
 */
@Data
public class AwsSymConfigProperties {

  private String region;
  private String url;
}
