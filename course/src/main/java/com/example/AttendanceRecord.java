package com.example;

import java.time.LocalDate;
import java.time.LocalTime;

public class AttendanceRecord {
    public int employeeId;
    public LocalDate date;
    public LocalTime arrival;
    public LocalTime departure;

    public boolean isAtWork() {
        return departure == null;
    }
}

