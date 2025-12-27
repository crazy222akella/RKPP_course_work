package com.example;

import java.util.List;

public class PageIndex {

    public static String render() {
        try {
            List<Employee> employees = CsvRepository.loadEmployees();

            StringBuilder rows = new StringBuilder();

            for (Employee e : employees) {
                rows.append("<tr>")
                        .append("<td>").append(e.name).append("</td>")
                        .append("<td>")
                        .append("<a href='/employee?id=").append(e.id).append("'>Открыть</a>")
                        .append("</td>")
                        .append("<td>").append(e.totalMinutes).append(" мин</td>")
                        .append("</tr>");
            }

            String html = Main.loadTemplate("index.html");
            html = html.replace("{{ROWS}}", rows.toString());

            return html;

        } catch (Exception e) {
            e.printStackTrace();
            return "<h1>Ошибка загрузки</h1>";
        }
    }
}
