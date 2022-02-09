package com.symphony.conf.sdk.lib.config.exceptions;

/**
 * Exception aised if there is some http communication issue.
 */
public class CommunicationException extends RuntimeException {

  public CommunicationException(String message) {
    super(message);
  }
}
