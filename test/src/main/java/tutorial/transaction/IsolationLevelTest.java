package tutorial.transaction;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IsolationLevelTest {

    static final String DB_URL = "jdbc:mysql://localhost/blog";
    static final String USER = "demtv";
    static final String PASS = "Anhdem96!@";

    public static void main(String[] args) throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
        connection.setTransactionIsolation(Connection.TRANSACTION_NONE);
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM blog.user where id =1;");
        resultSet.next();
        System.out.println(resultSet.getString("name"));

        ExecutorService service = Executors.newFixedThreadPool(2);

    }
}
