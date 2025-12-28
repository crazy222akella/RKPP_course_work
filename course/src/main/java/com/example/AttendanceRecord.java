package com.example;

import java.time.LocalDate;
import java.time.LocalTime;

public class AttendanceRecord {
    public int employeeId;
    public LocalDate date;
    public LocalTime arrival;
    public LocalTime lunchStart;
    public LocalTime lunchEnd;
    public LocalTime departure;

    public boolean isAtWork() {
        return arrival != null && departure == null;
    }

    public boolean isAtLunch() {
        return lunchStart != null && lunchEnd == null;
    }
}
