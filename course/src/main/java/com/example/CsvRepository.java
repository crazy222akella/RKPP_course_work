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

                r.lunchStart = p.length > 3 && !p[3].isEmpty()
                        ? LocalTime.parse(p[3]) : null;

                r.lunchEnd = p.length > 4 && !p[4].isEmpty()
                        ? LocalTime.parse(p[4]) : null;

                r.departure = p.length > 5 && !p[5].isEmpty()
                        ? LocalTime.parse(p[5]) : null;
                System.out.println("[DEBUG] parsed lunchStart=" + r.lunchStart);
                System.out.println("[DEBUG] parsed lunchEnd=" + r.lunchEnd);


                list.add(r);
            }
        }

        System.out.println("[DEBUG] attendance records count = " + list.size());
        return list;
    }
    public static void logLunchStart(int employeeId) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(ATT_FILE));
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        for (int i = lines.size() - 1; i >= 1; i--) {
            String[] p = lines.get(i).split(",", -1);

            if (Integer.parseInt(p[0]) == employeeId &&
                    LocalDate.parse(p[1]).equals(today) &&
                    !p[2].isEmpty() &&          // есть приход
                    p[3].isEmpty()) {           // обед не начат

                lines.set(i,
                        p[0] + "," + p[1] + "," + p[2] + "," + now + ",," + p[5]
                );
                break;
            }
        }

        Files.write(Path.of(ATT_FILE), lines);
    }
    public static void logLunchEnd(int employeeId) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(ATT_FILE));
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        for (int i = lines.size() - 1; i >= 1; i--) {
            String[] p = lines.get(i).split(",", -1);

            if (Integer.parseInt(p[0]) == employeeId &&
                    LocalDate.parse(p[1]).equals(today) &&
                    !p[3].isEmpty() &&
                    p[4].isEmpty()) {

                lines.set(i,
                        p[0] + "," + p[1] + "," + p[2] + "," + p[3] + "," + now + "," + p[5]
                );
                break;
            }
        }

        Files.write(Path.of(ATT_FILE), lines);
    }

    public static void logArrival(int employeeId) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(ATT_FILE));
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        boolean found = false;

        // Ищем последнюю запись за сегодня
        for (int i = lines.size() - 1; i >= 1; i--) {
            String[] p = lines.get(i).split(",", -1);

            if (Integer.parseInt(p[0]) == employeeId &&
                    LocalDate.parse(p[1]).equals(today)) {

                // Если последняя запись без ухода — сотрудник уже на работе
                if (p.length >= 6 && p[5].isEmpty()) {
                    System.out.println("[DEBUG] employee already at work, arrival ignored");
                    return;
                }

                found = true;
                break;
            }
        }

        // Создаем новую запись на сегодня, очищаем старые обеды и уходы
        String newLine = employeeId + "," + today + "," + now + ",,,";
        lines.add(newLine);
        Files.write(Path.of(ATT_FILE), lines);

        System.out.println("[DEBUG] new arrival line = " + newLine);
    }


    public static void logDeparture(int employeeId) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(ATT_FILE));
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        for (int i = lines.size() - 1; i >= 1; i--) {
            String[] p = lines.get(i).split(",", -1);

            if (Integer.parseInt(p[0]) == employeeId &&
                    LocalDate.parse(p[1]).equals(today) &&
                    (p.length < 6 || p[5].isEmpty())) {

                p[5] = now.toString();   // departure
                p[3] = "";               // очищаем lunchStart
                p[4] = "";               // очищаем lunchEnd
                lines.set(i, String.join(",", p));

                int minutes = (int) Duration.between(LocalTime.parse(p[2]), now).toMinutes();
                addMinutesToEmployee(employeeId, minutes);
                break;
            }
        }

        Files.write(Path.of(ATT_FILE), lines);
    }



    public static void startLunch(int employeeId) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(ATT_FILE));
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        for (int i = lines.size() - 1; i >= 1; i--) {
            String[] p = lines.get(i).split(",", -1);

            if (Integer.parseInt(p[0]) == employeeId &&
                    LocalDate.parse(p[1]).equals(today)) {

                // гарантируем 6 колонок
                while (p.length < 6) {
                    p = Arrays.copyOf(p, p.length + 1);
                    p[p.length - 1] = "";
                }

                // Обновляем lunchStart и очищаем lunchEnd
                p[3] = now.toString();
                p[4] = "";
                p[5] = "";

                lines.set(i, String.join(",", p));
                break;
            }
        }

        Files.write(Path.of(ATT_FILE), lines);
    }



    public static void endLunch(int employeeId) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(ATT_FILE));
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        System.out.println("[DEBUG] endLunch id=" + employeeId);

        for (int i = lines.size() - 1; i >= 1; i--) {
            String[] p = lines.get(i).split(",", -1);

            if (Integer.parseInt(p[0]) == employeeId &&
                    LocalDate.parse(p[1]).equals(today) &&
                    !p[3].isEmpty() && // lunchStart != null
                    p[4].isEmpty() &&  // lunchEnd == null
                    p[5].isEmpty()) {

                p[4] = now.toString();
                lines.set(i, String.join(",", p));

                System.out.println("[DEBUG] lunch ended at " + now);
                break;
            }
        }

        Files.write(Path.of(ATT_FILE), lines);
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
