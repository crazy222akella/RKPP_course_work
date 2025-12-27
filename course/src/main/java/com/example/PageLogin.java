package com.example;

public class PageLogin {

    public static String render() {
        try {
            String html = Main.loadTemplate("login.html");
            return html;
        } catch (Exception e) {
            return "<h1>Error loading login page</h1>";
        }
    }
}
