package com.symphony.conf.sdk.lib.config;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;

/** Data model of values stored in AWS-backed configuration service. */
@Data
@JsonSerialize
@Builder
public class KeyValue {

  /** The configuration property key. */
  String key;

  /** The configuration property value. */
  String value;

  /** The namespace from the list of accepted values. */
  String namespace;

  /** The namespace value from which the value was retrieved. */
  String namespaceValue;

  /**
   * The data type for the value. Field cannot be set, just returned in responses. It inherits the
   * KeyDefinition key type associated with this KeyValue.
   */
  String type;

  /**
   * Optional field indicating the service for service-specific keys which can have different values
   * in different namespaces (other than the 'service' namespace).
   */
  String serviceName;

  /**
   * Field indicating where the key for a given namespace is actually defined, when the value is
   * inherited from a higher level namespace. It's only set if value was inherited. If this value is
   * returned, common fields like 'id', 'etag', etc. are not available. This field is always
   * returned if it has a value and cannot be set on the "fields" parameter.
   */
  String inheritedFrom;

  /**
   * Flag indicating that value of this KeyValue is a staged (work-in-progress) values. Staged
   * values are only returned when the 'staged' query string argument is present and set to true on
   * the request. If this field is not present, the value is NOT staged.
   */
  String staged;
}
