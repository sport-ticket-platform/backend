package com.backend.handler;

import com.backend.common.ApiMessage;
import lombok.Getter;
import org.springframework.security.authentication.LockedException;

@Getter
public class CustomLockedException extends LockedException {

  private final ApiMessage apiMessage;
  private final long seconds;
  private final long minutes;
  private final long hours;

  public CustomLockedException(ApiMessage apiMessage, String message, long seconds, long minutes, long hours) {
    super(message);
    this.apiMessage = apiMessage;
    this.seconds = seconds;
    this.minutes = minutes;
    this.hours = hours;
  }
}