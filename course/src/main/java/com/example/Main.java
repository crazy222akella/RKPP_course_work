package com.example;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        System.out.println(
                Main.class.getClassLoader().getResource("templates/index.html")
        );
        try (ServerSocket server = new ServerSocket(8080)) {
            System.out.println("Server started on http://localhost:8080");

            while (true) {
                Socket client = server.accept();
                new Thread(() -> handleClient(client)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                OutputStream out = socket.getOutputStream()
        ) {
            String requestLine = in.readLine();
            if (requestLine == null) return;

            System.out.println("REQUEST: " + requestLine);

            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];
            String fullPath = requestParts[1];
            String path = fullPath.split("\\?")[0];

            // ===== headers =====
            int contentLength = 0;
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                }
            }

            // ===== body =====
            String body = "";
            if ("POST".equals(method) && contentLength > 0) {
                char[] buf = new char[contentLength];
                in.read(buf);
                body = new String(buf);
            }

            Map<String, String> params = parseParams(
                    "GET".equals(method) ? fullPath : body
            );

            // ===== POST ACTIONS =====
            if ("POST".equals(method)) {
                int id = params.containsKey("id") ? Integer.parseInt(params.get("id")) : -1;
                Employee emp = null;
                if (id != -1) {
                    emp = CsvRepository.loadEmployees()
                            .stream()
                            .filter(e -> e.id == id)
                            .findFirst()
                            .orElse(null);
                }

                if (path.equals("/arrival") && emp != null) {
                    CsvRepository.logArrival(id);
                    EmployeeLogger.log(id, "Пришел на работу");
                } else if (path.equals("/departure") && emp != null) {
                    CsvRepository.logDeparture(id);
                    EmployeeLogger.log(id, "Ушел с работы");
                } else if (path.equals("/confirm") && emp != null) {
                    PresenceControl.confirm(id);
                    System.out.println("[CONTROL] employee " + id + " confirmed presence");
                    EmployeeLogger.log(id, "Подтвердил присутствие");
                } else if (path.equals("/lunch/start") && emp != null) {
                    CsvRepository.startLunch(id);
                    EmployeeLogger.log(id, "Ушел на обед");
                } else if (path.equals("/lunch/end") && emp != null) {
                    CsvRepository.endLunch(id);
                    EmployeeLogger.log(id, "Вернулся с обеда");
                }

                if (emp != null) {
                    String encodedName = URLEncoder.encode(emp.name, StandardCharsets.UTF_8);
                    sendRedirect(out, "/employee?name=" + encodedName);
                } else {
                    sendRedirect(out, "/");
                }
                return;
            }

            // ===== PAGES =====
            String response;
            String contentType = "text/html; charset=utf-8";

            if (path.equals("/") || path.equals("/index")) {
                response = PageIndex.render();
            } else if (path.equals("/login")) {
                response = PageLogin.render();
            } else if (path.startsWith("/employee")) {
                response = PageEmployee.render(fullPath);
            } else if (path.equals("/style.css")) {
                response = loadStatic("public/style.css");
                contentType = "text/css; charset=utf-8";
            } else {
                response = "<h1>404 Not Found</h1>";
            }

            byte[] bodyBytes = response.getBytes(StandardCharsets.UTF_8);
            String headers =
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + contentType + "\r\n" +
                            "Content-Length: " + bodyBytes.length + "\r\n" +
                            "Connection: close\r\n\r\n";

            out.write(headers.getBytes(StandardCharsets.UTF_8));
            out.write(bodyBytes);
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    // ===== УТИЛИТЫ =====

    static String loadTemplate(String name) throws IOException {
        InputStream is = Main.class
                .getClassLoader()
                .getResourceAsStream("templates/" + name);

        if (is == null) {
            return "<h1>Template not found: " + name + "</h1>";
        }

        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }


    private static Map<String, String> parseParams(String source) {
        Map<String, String> map = new HashMap<>();

        if (source.contains("?")) {
            source = source.substring(source.indexOf("?") + 1);
        }

        for (String pair : source.split("&")) {
            if (!pair.contains("=")) continue;
            String[] p = pair.split("=", 2);
            map.put(
                    URLDecoder.decode(p[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(p[1], StandardCharsets.UTF_8)
            );
        }
        return map;
    }
    static String loadStatic(String path) throws IOException {
        InputStream is = Main.class
                .getClassLoader()
                .getResourceAsStream(path);

        if (is == null) {
            return "";
        }
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void sendRedirect(OutputStream out, String location) throws IOException {
        String headers =
                "HTTP/1.1 303 See Other\r\n" +
                        "Location: " + location + "\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n\r\n";

        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
