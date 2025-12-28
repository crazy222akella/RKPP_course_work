package com.example;

import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EmployeeLogger {

    private static final Path LOG_DIR = Path.of("src/main/data/logs");

    public static void log(int employeeId, String message) {
        try {
            // создаём папку logs, если её нет
            if (!Files.exists(LOG_DIR)) {
                Files.createDirectories(LOG_DIR);
            }

            // файл для конкретного сотрудника
            Path file = LOG_DIR.resolve("employee_" + employeeId + ".log");

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            String line = timestamp + " - " + message + System.lineSeparator();

            // создаёт файл, если его нет, и добавляет строку
            Files.write(file, line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
