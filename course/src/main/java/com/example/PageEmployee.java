package com.example;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public class PageEmployee {

    public static String render(String fullPath) {
        try {
            if (!fullPath.contains("name=")) {
                return "<h1>Имя сотрудника не указано</h1><a href='/login'>Назад</a>";
            }

            String name = URLDecoder.decode(
                    fullPath.split("name=", 2)[1],
                    StandardCharsets.UTF_8
            ).replace("+", " ").trim();

            Employee emp = CsvRepository.loadEmployees()
                    .stream()
                    .filter(e -> e.name.equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);

            if (emp == null) {
                return "<h1>Сотрудник не найден: " + name + "</h1>";
            }

            int id = emp.id;

            List<AttendanceRecord> records = CsvRepository.getAttendance(id);

            AttendanceRecord today = records.stream()
                    .filter(r -> r.date.equals(LocalDate.now()))
                    .reduce((first, second) -> second)
                    .orElse(null);
            System.out.println("[DEBUG] today=" + today);
            if (today != null) {
                System.out.println("[DEBUG] arrival=" + today.arrival);
                System.out.println("[DEBUG] lunchStart=" + today.lunchStart);
                System.out.println("[DEBUG] lunchEnd=" + today.lunchEnd);
                System.out.println("[DEBUG] departure=" + today.departure);
                System.out.println("[DEBUG] isAtWork=" + today.isAtWork());
                System.out.println("[DEBUG] isAtLunch=" + today.isAtLunch());
            }

            boolean atWork = today != null && today.isAtWork();
            boolean atLunch = today != null && today.isAtLunch() && atWork;

            String status;
            if (atLunch) {
                status = "На обеде";
            } else if (atWork) {
                status = "На работе";
            } else {
                status = "Не на работе";
            }

            String todayTime = "0 мин";
            if (today != null && today.departure != null) {
                long mins = Duration.between(today.arrival, today.departure).toMinutes();
                todayTime = mins + " мин";
            }

            String totalTime = emp.totalMinutes + " мин";

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
            String question = "";
            String questionBtn = "";

            if (atWork  && !atLunch && PresenceControl.shouldAsk(id)) {
                question = "Подтвердите присутствие на рабочем месте";
                questionBtn = """
                    <form action="/confirm" method="post">
                        <input type="hidden" name="id" value="%d">
                        <button type="submit">Я на месте</button>
                    </form>
                """.formatted(id);
            }

            String lunchStartBtn = "";
            String lunchEndBtn = "";

            // Уйти на обед — если на работе и НЕ на обеде
            if (atWork && !atLunch) {
                lunchStartBtn = """
                    <form action="/lunch/start" method="post">
                        <input type="hidden" name="id" value="%d">
                        <button type="submit">Уйти на обед</button>
                    </form>
                """.formatted(id);
            }

            // Вернуться с обеда — если на обеде
            if (atLunch) {
                lunchEndBtn = """
                    <form action="/lunch/end" method="post">
                        <input type="hidden" name="id" value="%d">
                        <button type="submit">Вернуться с обеда</button>
                    </form>
                """.formatted(id);
            }

            String html = Main.loadTemplate("employee.html");

            return html
                    .replace("{{NAME}}", emp.name)
                    .replace("{{STATUS}}", status)
                    .replace("{{TODAY}}", todayTime)
                    .replace("{{TOTAL}}", totalTime)
                    .replace("{{ARRIVAL_BUTTON}}", arrivalBtn)
                    .replace("{{DEPARTURE_BUTTON}}", departureBtn)
                    .replace("{{QUESTION}}", question)
                    .replace("{{QUESTION_BUTTON}}", questionBtn)
                    .replace("{{LUNCH_END_BUTTON}}",lunchEndBtn)
                    .replace("{{LUNCH_START_BUTTON}}",lunchStartBtn);

        } catch (Exception e) {
            e.printStackTrace();
            return "<h1>Ошибка загрузки сотрудника</h1>";
        }
    }

}
