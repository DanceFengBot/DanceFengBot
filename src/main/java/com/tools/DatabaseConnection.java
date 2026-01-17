package com.tools;

import java.sql.*;
import com.mirai.config.AbstractConfig;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

public class DatabaseConnection {
    private static String driver = "com.mysql.cj.jdbc.Driver";
    private static String url;
    private static String user;
    private static String pass;
    private static Connection conn = null;
    private static boolean isConnected = false;
    public static ResultSet executeResult;

    /**
     * 从YML配置文件加载数据库配置
     */
    private static boolean loadConfigFromYaml(String configPath) {
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = new FileInputStream(configPath + "db.yml");
            Map<String, Object> config = yaml.load(inputStream);

            if (config == null) {
                System.err.println("配置文件db.yml不存在。");
                return false;
            }

            // 获取数据库配置
            Map<String, Object> dbConfig = (Map<String, Object>) config.get("database");
            if (dbConfig == null) {
                System.err.println("找不到'database'配置节。");
            }
            url = "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC".formatted(
                    dbConfig.get("host"),
                    dbConfig.get("port"),
                    dbConfig.get("database")
            );
            user = (String) dbConfig.get("user");
            pass = (String) dbConfig.get("password");

            if (url == null || user == null || pass == null) {
                System.err.println("数据库配置不完整，请检查host、port、user和password配置");
                return false;
            }

            System.out.println("数据库配置加载成功");
            return true;
        } catch (Exception e) {
            System.err.println("加载配置文件失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 连接数据库，返回连接状态
     */
    public static boolean connect() {
        return connect(AbstractConfig.configPath);
    }

    /**
     * 指定配置文件路径连接数据库
     */
    public static boolean connect(String configPath) {
        try {
            // 从YML文件加载配置
            if (!loadConfigFromYaml(configPath)) {
                System.err.println("加载数据库配置失败");
                return false;
            }

            // Register JDBC driver
            Class.forName(driver);

            // Open connection
            conn = DriverManager.getConnection(url, user, pass);
            isConnected = true;
            System.out.println("已连接到数据库。");
            return true;
        } catch (SQLException se) {
            // Handle JDBC errors
            System.err.println("数据库连接失败: " + se.getMessage());
            se.printStackTrace();
            isConnected = false;
            return false;
        } catch (Exception e) {
            System.err.println("连接数据库时发生未知错误: " + e.getMessage());
            e.printStackTrace();
            isConnected = false;
            return false;
        }
    }

    /**
     * 获取数据库连接
     */
    public static Connection getConnection() {
        if (!isConnected || conn == null) {
            System.err.println("数据库未连接，请先调用connect()方法");
            return null;
        }
        return conn;
    }

    /**
     * 检查数据库是否已连接
     */
    public static boolean isConnected() {
        return isConnected && conn != null;
    }

    /**
     * 断开数据库连接
     */
    public static void disconnect() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("数据库连接已关闭。");
            }
            isConnected = false;
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            conn = null;
        }
    }

    /**
     * Run sql execute
     */
    public static boolean executeSql(String sql) {
        if (!isConnected()) {
            System.err.println("数据库未连接，无法执行SQL语句");
            return false;
        }
        try (Statement stmt = conn.createStatement()) {
            executeResult =  stmt.executeQuery(sql);
            return true;
        } catch (SQLException se) {
            System.err.println("执行SQL语句失败: " + se.getMessage());
            se.printStackTrace();
            return false;
        }
    }
    @Test
    public void testConnection() {
        if (connect()) {
            System.out.println("测试连接成功！");
            disconnect();
        } else {
            System.err.println("测试连接失败！");
        }
    }
}