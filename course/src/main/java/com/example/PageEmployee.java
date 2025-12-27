package com.example;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public class PageEmployee {

    public static String render(String fullPath) {
        try {
            // ===== 1. Проверка имени =====
            if (!fullPath.contains("name=")) {
                return "<h1>Имя сотрудника не указано</h1><a href='/login'>Назад</a>";
            }

            String name = URLDecoder.decode(
                    fullPath.split("name=", 2)[1],
                    StandardCharsets.UTF_8
            ).replace("+", " ").trim();

            if (name.isEmpty()) {
                return "<h1>Имя не может быть пустым</h1>";
            }

            // ===== 2. Поиск сотрудника =====
            Employee emp = CsvRepository.loadEmployees()
                    .stream()
                    .filter(e -> e.name.equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);

            if (emp == null) {
                return "<h1>Сотрудник не найден: " + name + "</h1>";
            }

            int id = emp.id;

            // ===== 3. Посещения =====
            List<AttendanceRecord> records = CsvRepository.getAttendance(id);

            AttendanceRecord today = records.stream()
                    .filter(r -> r.date.equals(LocalDate.now()))
                    .reduce((first, second) -> second) // ← последняя запись
                    .orElse(null);

            boolean atWork = today != null && today.isAtWork();

            String status = atWork ? "На работе" : "Не на работе";

            long todayMinutes = 0;

            for (AttendanceRecord r : records) {
                if (!r.date.equals(LocalDate.now())) continue;

                if (r.departure != null) {
                    todayMinutes += Duration.between(r.arrival, r.departure).toMinutes();
                }
            }

            String todayTime = todayMinutes + " мин";


            String totalTime = emp.totalMinutes + " мин";

            // ===== 4. Кнопки =====
            String arrivalBtn = "";
            String departureBtn = "";

            if (!atWork) {
                arrivalBtn = """
                    <form action="/arrival" method="post">
                        <input type="hidden" name="id" value="%d">
                        <button type="submit">На работе</button>
                    </form>
                """.formatted(id);
            }

            if (atWork) {
                departureBtn = """
                    <form action="/departure" method="post">
                        <input type="hidden" name="id" value="%d">
                        <button type="submit">Закончил работу</button>
                    </form>
                """.formatted(id);
            }

            // ===== 5. HTML =====
            String html = Main.loadTemplate("employee.html");

            html = html
                    .replace("{{NAME}}", emp.name)
                    .replace("{{STATUS}}", status)
                    .replace("{{TODAY}}", todayTime)
                    .replace("{{TOTAL}}", totalTime)
                    .replace("{{ARRIVAL_BUTTON}}", arrivalBtn)
                    .replace("{{DEPARTURE_BUTTON}}", departureBtn);
            System.out.println("[DEBUG] employee=" + emp.name);
            System.out.println("[DEBUG] today record=" + today);
            System.out.println("[DEBUG] atWork=" + atWork);

            return html;

        } catch (Exception e) {
            e.printStackTrace();
            return "<h1>Ошибка загрузки сотрудника</h1>";
        }
    }
}
