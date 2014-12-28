package com.jorge.thesis;

import java.sql.*;

public abstract class DerbyTest {

    public static void test() {
        try {
            Class.forName("org.apache.derby.jdbc.Driver");
        } catch (ClassNotFoundException cnfe) {
            System.err.println("Derby driver not found.");
        }
        try {
            Connection conn = DriverManager.getConnection("jdbc:derby://localhost:1527/test;create=true;user=APP;" +
                    "pass=APP");
            Statement s = conn.createStatement();
            s.execute("CREATE TABLE test (id integer primary key not null, text varchar(32))");
            s.execute("INSERT INTO test VALUES (1, 'hello world!')");
            s.execute("SELECT * FROM test");
            ResultSet rs = s.getResultSet();
            while (rs.next()) {
                System.out.println("Derby says: " + rs.getString("text"));
            }
            s.execute("DROP TABLE test");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }
}
