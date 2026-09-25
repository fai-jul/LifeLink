package com.lifelink.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtil {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    public static final DateTimeFormatter TIME_ONLY_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private DateUtil() {
    }

    public static String format(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FORMAT);
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_TIME_FORMAT);
    }

    public static String formatTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(TIME_ONLY_FORMAT);
    }

    /** Days remaining until the given date (negative if already past). */
    public static long daysUntil(LocalDate date) {
        if (date == null) return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date);
    }
}
