package com.DanceFengBot.init;

import com.DanceFengBot.config.AbstractConfig;
import com.Tools.DatabaseConnection;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
public class DatabaseInit {
    public static void init() {
        if (!DatabaseConnection.connect()) {
            System.err.println("数据库连接失败，无法初始化表");
            return;
        }

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            System.err.println("获取数据库连接失败");
            return;
        }

        String tableName = "officialmusics";

        try {
            // 1. 检查表是否存在
            if (tableExists(conn, tableName)) {
                System.out.println("表 " + tableName + " 已存在");

                // 2. 验证表结构是否正确
                if (validateTableStructure(conn, tableName)) {
                    System.out.println("表结构验证通过，无需重新初始化");
                    return;
                } else {
                    System.out.println("表结构不匹配，将重新创建表");
                    System.err.println("请手动处理表结构不一致问题，或删除现有表后重新运行初始化");
                    return;
                }
            }

            // 3. 表不存在，创建表
            System.out.println("表 " + tableName + " 不存在，开始创建...");
            createTable(conn, tableName);

        } catch (SQLException | IOException e) {
            System.err.println("初始化数据库表时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean checkInitMark() {
        DatabaseConnection.connect();
        String sql = "SELECT COUNT(*) FROM initmark WHERE id = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }
        } catch (SQLException e) {
            System.err.println("检查初始化标记失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 检查表是否存在
     */
    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * 验证表结构是否正确
     */
    private static boolean validateTableStructure(Connection conn, String tableName) throws SQLException {
        // 预期的列定义
        Set<String> expectedColumns = new HashSet<>();
        expectedColumns.add("id");           // bigint, NOT NULL, PRIMARY KEY
        expectedColumns.add("name");         // longtext, NOT NULL
        expectedColumns.add("coverLink");    // longtext, NOT NULL
        expectedColumns.add("audioLink");    // longtext, NOT NULL

        // 预期的列类型映射（简化验证，只检查关键属性）
        String expectedIdType = "bigint";
        String expectedNameType = "longtext";
        String expectedCoverLinkType = "longtext";
        String expectedTypeType = "bigint";

        // 检查列是否存在且类型正确
        String columnCheckSql =
                "SELECT column_name, column_type, is_nullable, column_key " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ?";

        Set<String> foundColumns = new HashSet<>();
        boolean structureValid = true;

        try (PreparedStatement pstmt = conn.prepareStatement(columnCheckSql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("column_name");
                    String columnType = rs.getString("column_type").toLowerCase();
                    String isNullable = rs.getString("is_nullable");
                    String columnKey = rs.getString("column_key");

                    foundColumns.add(columnName);

                    // 验证特定列的属性
                    if ("id".equals(columnName)) {
                        if (!columnType.startsWith(expectedIdType) ||
                                !"NO".equals(isNullable) ||
                                !"PRI".equals(columnKey)) {
                            System.err.println("id列结构不匹配: 类型=" + columnType +
                                    ", 可空=" + isNullable + ", 键=" + columnKey);
                            structureValid = false;
                        }
                    } else if ("name".equals(columnName)) {
                        if (!columnType.startsWith(expectedNameType) ||
                                !"NO".equals(isNullable)) {
                            System.err.println("name列结构不匹配: 类型=" + columnType +
                                    ", 可空=" + isNullable);
                            structureValid = false;
                        }
                    } else if ("coverLink".equals(columnName.toLowerCase())) {
                        if (!columnType.startsWith(expectedCoverLinkType) ||
                                !"NO".equals(isNullable)) {
                            System.err.println("coverLink列结构不匹配: 类型=" + columnType +
                                    ", 可空=" + isNullable);
                            structureValid = false;
                        }
                    } else if ("audioLink".equals(columnName)) {
                        if (!columnType.startsWith(expectedTypeType) ||
                                !"NO".equals(isNullable)) {
                            System.err.println("audioLink列结构不匹配: 类型=" + columnType +
                                    ", 可空=" + isNullable);
                            structureValid = false;
                        }
                    }
                }
            }
        }

        // 检查是否缺少必需的列
        for (String expectedColumn : expectedColumns) {
            if (!foundColumns.contains(expectedColumn)) {
                System.err.println("缺少必需的列: " + expectedColumn);
                structureValid = false;
            }
        }

        // 检查主键约束
        boolean hasPrimaryKey = checkPrimaryKey(conn, tableName, "id");
        if (!hasPrimaryKey) {
            System.err.println("主键约束验证失败: id列应该为主键");
            structureValid = false;
        }

        return structureValid;
    }

    /**
     * 检查主键约束
     */
    private static boolean checkPrimaryKey(Connection conn, String tableName, String primaryKeyColumn)
            throws SQLException {
        String primaryKeySql =
                "SELECT column_name " +
                        "FROM information_schema.key_column_usage " +
                        "WHERE table_schema = DATABASE() " +
                        "AND table_name = ? " +
                        "AND constraint_name = 'PRIMARY'";

        try (PreparedStatement pstmt = conn.prepareStatement(primaryKeySql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("column_name");
                    if (primaryKeyColumn.equals(columnName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 创建表
     */
    private static void createTable(Connection conn, String tableName) throws SQLException, IOException {
        ScriptRunner runner = new ScriptRunner(conn);
        runner.setAutoCommit(true);
        runner.setStopOnError(true);

        String path = AbstractConfig.configPath + "initTables.sql";
        System.out.println("读取脚本文件: " + path);

        // 规范化路径
        File file = new File(path);
        if (!file.exists()) {
            // 尝试不同的路径格式
            String altPath = path.replace("\\", "/").replace("//", "/");
            file = new File(altPath);
            System.out.println("尝试备用路径: " + altPath);

            if (!file.exists()) {
                throw new FileNotFoundException("找不到SQL脚本文件: " + path);
            }
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            String script = IOUtils.toString(reader);
            script = script.replace("%table_name%", tableName);

            runner.runScript(new StringReader(script));
            System.out.println("表 " + tableName + " 创建成功");
        }
    }

    private static String readSqlScript(String filename) throws IOException {
        // 从类路径或文件系统读取SQL脚本
        try (InputStream is = DatabaseInit.class.getClassLoader().getResourceAsStream(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    private static String[] parseSqlScript(String sqlScript) {
        // 简单的SQL语句分割（按分号分割）
        // 注意：这种方法不支持分号在字符串或注释中的情况
        return sqlScript.split(";\\s*");
    }

    public static void createInitMark() {
        DatabaseConnection.connect();
        String sql = "INSERT INTO initmark (id, time) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE id = VALUES(id)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, 1);
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("创建初始化标记失败: " + e.getMessage());
        }
    }
}