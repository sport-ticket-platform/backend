package com.backend.handler;

import lombok.Getter;
import org.springframework.security.authentication.LockedException;

@Getter
public class CustomLockedException extends LockedException {

  private final long seconds;
  private final long minutes;
  private final long hours;

  public CustomLockedException(String message, long seconds, long minutes, long hours) {
    super(message);
    this.seconds = seconds;
    this.minutes = minutes;
    this.hours = hours;
  }
}