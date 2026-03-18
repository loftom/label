package com.medlabel.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DbInit {
    public static void main(String[] args) throws Exception {
        String hostUrl = "jdbc:mysql://localhost:3306/" +
                "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        String dbUrl = "jdbc:mysql://localhost:3306/label_system?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        String user = "root";
        String pass = "123456";

        try (Connection c = DriverManager.getConnection(hostUrl, user, pass);
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS label_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            System.out.println("Created database label_system (if not existed)");
        }

        // Run schema.sql against the new database
        File f = new File("schema.sql");
        if (!f.exists()) {
            System.out.println("schema.sql not found in project root: " + f.getAbsolutePath());
            return;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }

        String[] statements = sb.toString().split(";\\s*\\n");
        try (Connection c = DriverManager.getConnection(dbUrl, user, pass);
             Statement s = c.createStatement()) {
            for (String stmt : statements) {
                String t = stmt.trim();
                if (t.isEmpty()) continue;
                s.execute(t);
            }
            System.out.println("Imported schema.sql into label_system");
        }
    }
}
