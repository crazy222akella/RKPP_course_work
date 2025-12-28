package com.example;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class PresenceControl {

    private static final Map<Integer, LocalDateTime> lastConfirm = new HashMap<>();
    private static final Map<Integer, LocalDateTime> lastQuestion = new HashMap<>();

    // каждые 5 минут
    private static final int INTERVAL_MINUTES = 5;

    public static boolean shouldAsk(int employeeId) {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime lastQ = lastQuestion.get(employeeId);
        LocalDateTime lastC = lastConfirm.get(employeeId);

        if (lastQ == null) {
            lastQuestion.put(employeeId, now);
            return true;
        }

        if (lastC != null && lastC.isAfter(lastQ)) {
            return false;
        }

        if (lastQ.plusMinutes(INTERVAL_MINUTES).isBefore(now)) {
            lastQuestion.put(employeeId, now);
            return true;
        }

        return false;
    }

    public static void confirm(int employeeId) {
        lastConfirm.put(employeeId, LocalDateTime.now());
    }
}
