package com.example.web;

import com.alibaba.fastjson.JSON;
import com.example.util.MySQLUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebServer {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new StaticHandler());
        server.createContext("/api/data", new DataHandler());
        server.setExecutor(null);
        System.out.println("Web Server started on port 8080");
        server.start();
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String path = t.getRequestURI().getPath();
            if ("/".equals(path)) {
                path = "/index.html";
            }

            // 从 resources/web 提供文件
            String content = "";
            try {
                // 为了开发方便优先尝试从文件系统读取（热重载）
                // 假设从项目根目录运行
                java.nio.file.Path filePath = Paths.get("src/main/resources/web", path);
                if (Files.exists(filePath)) {
                    content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
                } else {
                    // 打包为 jar 时回退到 classpath
                    try (java.io.InputStream is = WebServer.class.getClassLoader().getResourceAsStream("web" + path)) {
                        if (is == null) {
                            throw new IOException("File not found in classpath: web" + path);
                        }
                        try (java.util.Scanner s = new java.util.Scanner(is, StandardCharsets.UTF_8.name())) {
                            s.useDelimiter("\\A");
                            content = s.hasNext() ? s.next() : "";
                        }
                    }
                }
            } catch (Exception e) {
                String response = "404 Not Found";
                t.sendResponseHeaders(404, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            byte[] responseBytes = content.getBytes(StandardCharsets.UTF_8);
            t.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = t.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }

    static class DataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            List<Map<String, Object>> list = new ArrayList<>();

            // 加载配置
            java.util.Properties prop = new java.util.Properties();
            try (java.io.InputStream input = WebServer.class.getClassLoader()
                    .getResourceAsStream("config.properties")) {
                if (input == null) {
                    System.out.println("Sorry, unable to find config.properties");
                    return;
                }
                prop.load(input);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            String url = prop.getProperty("jdbc.url");
            String user = prop.getProperty("jdbc.user");
            String pass = prop.getProperty("jdbc.password");
            String table = prop.getProperty("jdbc.table.hot");

            try (Connection conn = DriverManager.getConnection(url, user, pass);
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " ORDER BY count DESC LIMIT 10")) {

                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", rs.getString("keyword")); // 前端通常期望 ECharts 使用 "name"，但是
                                                              // 数据库列名是 "keyword"
                    map.put("value", rs.getInt("count")); // "value" 是 ECharts 的标准字段
                    list.add(map);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String json = JSON.toJSONString(list);
            t.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
            t.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = t.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }
}
