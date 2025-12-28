package com.example;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public class PageIndex {

    public static String render() {
        try {
            List<Employee> employees = CsvRepository.loadEmployees();
            StringBuilder rows = new StringBuilder();

            for (Employee e : employees) {
                // получаем запись за сегодня, если есть
                AttendanceRecord today = CsvRepository.getAttendance(e.id).stream()
                        .filter(r -> r.date.equals(LocalDate.now()))
                        .reduce((first, second) -> second)
                        .orElse(null);

                String status;
                long todayMinutes = 0;

                if (today != null) {
                    boolean atWork = today.isAtWork();
                    boolean atLunch = today.isAtLunch() && atWork;

                    if (atLunch) status = "На обеде";
                    else if (atWork) status = "На работе";
                    else status = "Не на работе";

                    // считаем минуты сегодня
                    LocalDate nowDate = LocalDate.now();
                    if (today.arrival != null) {
                        // если сотрудник еще не ушел, берём текущее время
                        var endTime = (today.departure != null) ? today.departure : java.time.LocalTime.now();
                        todayMinutes = Duration.between(today.arrival, endTime).toMinutes();
                        if (today.lunchStart != null) {
                            var lunchEnd = (today.lunchEnd != null) ? today.lunchEnd : java.time.LocalTime.now();
                            todayMinutes -= Duration.between(today.lunchStart, lunchEnd).toMinutes();
                        }
                    }
                } else {
                    status = "Не на работе";
                }

                rows.append("<tr>")
                        .append("<td>").append(e.name).append("</td>")
                        .append("<td>").append(status).append("</td>")
                        .append("<td>").append(todayMinutes).append(" мин</td>")
                        .append("<td>").append(e.totalMinutes).append(" мин</td>")
                        .append("</tr>");
            }

            String html = Main.loadTemplate("index.html");
            return html.replace("{{ROWS}}", rows.toString());

        } catch (Exception ex) {
            ex.printStackTrace();
            return "<h1>Ошибка загрузки</h1>";
        }
    }
}
