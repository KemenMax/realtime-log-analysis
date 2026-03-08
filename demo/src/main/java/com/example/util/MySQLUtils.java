package com.example.util;

import scala.Tuple2;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class MySQLUtils {
    private static volatile MySQLUtils instance;
    private static final String URL = "jdbc:mysql://localhost:3306/bigdata?useSSL=false&characterEncoding=utf-8&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "hadoop";

    private MySQLUtils() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static MySQLUtils getInstance() {
        if (instance == null) {
            synchronized (MySQLUtils.class) {
                if (instance == null) {
                    instance = new MySQLUtils();
                }
            }
        }
        return instance;
    }

    public void upsertHotStats(List<Tuple2<String, Integer>> stats) {
        String sql = "INSERT INTO hot_stats (keyword, count) VALUES (?, ?) ON DUPLICATE KEY UPDATE count = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Tuple2<String, Integer> stat : stats) {
                pstmt.setString(1, stat._1);
                pstmt.setInt(2, stat._2);
                pstmt.setInt(3, stat._2);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
