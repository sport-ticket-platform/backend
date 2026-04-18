package com.backend.config;

public class RedisKeyNames {
    public static String BLACKLISTED_IPS_KEY = "security:ips:blacklist";
    public static String IPS_ILLEGALS_COUNT = "security:ips:illegal_count";
    public static String LOCKED_USERS_KEY = "security:lock:users";
}
