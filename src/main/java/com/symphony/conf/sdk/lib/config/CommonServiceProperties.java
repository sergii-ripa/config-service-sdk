package com.symphony.conf.sdk.lib.config;

import lombok.Data;

/**
 * Service properties that are provided as environment variables during service startup.
 *
 * <p>Those are standard properties for most of the services run in Symphony infrastructure.
 */
@Data
public class CommonServiceProperties {

  private String env;

  private String region;

  private String platform;

  private String serviceName;
}
