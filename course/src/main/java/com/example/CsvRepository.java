package com.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.time.LocalDate;
import java.time.LocalTime;
public class CsvRepository {

    private static final String DATA_DIR = "src/main/data/";
    private static final String EMP_FILE = DATA_DIR +"employees.csv";
    private static final String ATT_FILE = DATA_DIR +"attendance.csv";

    // ===== EMPLOYEES =====

    public static List<Employee> loadEmployees() throws IOException {
        List<Employee> list = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(Path.of(EMP_FILE), StandardCharsets.UTF_8)) {
            br.readLine(); // header
            String line;

            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");

                Employee e = new Employee();
                e.id = Integer.parseInt(p[0]);
                e.name = p[1];
                e.totalMinutes = Integer.parseInt(p[2]);

                list.add(e);
            }
        }

        System.out.println("[DEBUG] employees loaded = " + list.size());
        return list;
    }


    // ===== ATTENDANCE =====
    public static List<AttendanceRecord> getAttendance(int employeeId) throws IOException {
        List<AttendanceRecord> list = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(Path.of(ATT_FILE), StandardCharsets.UTF_8)) {
            br.readLine(); // header
            String line;

            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);

                if (Integer.parseInt(p[0]) != employeeId) continue;

                AttendanceRecord r = new AttendanceRecord();
                r.employeeId = employeeId;
                r.date = LocalDate.parse(p[1]);
                r.arrival = LocalTime.parse(p[2]);
                r.departure = p[3].isEmpty() ? null : LocalTime.parse(p[3]);

                list.add(r);
            }
        }

        System.out.println("[DEBUG] attendance records count = " + list.size());
        return list;
    }




    public static void logArrival(int employeeId) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(ATT_FILE));
        LocalDate today = LocalDate.now();

        System.out.println("[DEBUG] logArrival for id=" + employeeId);

        for (int i = lines.size() - 1; i >= 1; i--) {
            String[] p = lines.get(i).split(",", -1);

            if (Integer.parseInt(p[0]) == employeeId &&
                    LocalDate.parse(p[1]).equals(today)) {

                if (p[3].isEmpty()) {
                    System.out.println("[DEBUG] arrival ignored — employee already at work");
                    return;
                }

                break; // последняя запись закрыта — можно новый приход
            }
        }

        String newLine = employeeId + "," + today + "," + LocalTime.now() + ",";
        System.out.println("[DEBUG] new arrival line = " + newLine);

        lines.add(newLine);
        Files.write(Path.of(ATT_FILE), lines);
    }





    public static void logDeparture(int employeeId) throws IOException {
        List<String> lines = java.nio.file.Files.readAllLines(new File(ATT_FILE).toPath());

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        System.out.println("[DEBUG] logDeparture for id=" + employeeId);

        for (int i = lines.size() - 1; i >= 1; i--) {
            String[] p = lines.get(i).split(",", -1);

            System.out.println("[DEBUG] check line: " + Arrays.toString(p));

            if (Integer.parseInt(p[0]) == employeeId &&
                    LocalDate.parse(p[1]).equals(today) &&
                    p[3].isEmpty()) {

                LocalTime arrival = LocalTime.parse(p[2]);
                int minutes = (int) Duration.between(arrival, now).toMinutes();

                lines.set(i, p[0] + "," + p[1] + "," + p[2] + "," + now);

                System.out.println("[DEBUG] departure written, minutes=" + minutes);

                addMinutesToEmployee(employeeId, minutes);
                break;
            }
        }

        java.nio.file.Files.write(new File(ATT_FILE).toPath(), lines);
    }


    private static void addMinutesToEmployee(int employeeId, int minutes) throws IOException {

        List<String> lines = java.nio.file.Files.readAllLines(new File(EMP_FILE).toPath());

        for (int i = 1; i < lines.size(); i++) {
            String[] p = lines.get(i).split(",");

            if (Integer.parseInt(p[0]) == employeeId) {
                int total = Integer.parseInt(p[2]) + minutes;
                lines.set(i, p[0] + "," + p[1] + "," + total);
                break;
            }
        }

        java.nio.file.Files.write(new File(EMP_FILE).toPath(), lines);
    }



    // ===== HELPERS =====

    private static BufferedReader openReader(String path) throws IOException {
        InputStream is = CsvRepository.class
                .getClassLoader()
                .getResourceAsStream(path);

        if (is == null) throw new FileNotFoundException(path);

        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    private static BufferedWriter openWriter(String path, boolean append) throws IOException {
        File file = new File("src/main/resources/" + path);
        return new BufferedWriter(new FileWriter(file, append));
    }

    private static List<String> readAllLines(String path) throws IOException {
        File file = new File("src/main/resources/" + path);
        return new ArrayList<>(java.nio.file.Files.readAllLines(file.toPath()));
    }

    private static void writeAllLines(String path, List<String> lines) throws IOException {
        File file = new File("src/main/resources/" + path);
        java.nio.file.Files.write(file.toPath(), lines);
    }
}
