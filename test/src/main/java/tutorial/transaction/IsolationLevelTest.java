package tutorial.transaction;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static tutorial.transaction.Test.*;
import static tutorial.transaction.Test.PASS;

public class IsolationLevelTest {

    public static void main(String[] args) {
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.submit(() -> {
            try {
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                try {
                    int number = 100;

                    Statement statement = conn.createStatement();

                    ResultSet resultSet = statement.executeQuery("select * from dev.`users` where id = 1");

                    resultSet.next();

                    int amount1 = resultSet.getInt("amount");

                    String update1 = "UPDATE `dev`.`users` SET `amount` = " + (amount1 - number) + " WHERE (`id` = '1');";

                    statement.executeUpdate(update1);

                    resultSet = statement.executeQuery("select * from dev.`users` where id = 2");
                    resultSet.next();

                    int amount2 = resultSet.getInt("amount");

                    String update2 = "UPDATE `dev`.`users` SET `amount` = " + (amount2 + number) + "  WHERE (`id` = '2');";

                    statement.executeUpdate(update2);

                    conn.commit();
                } catch (Exception e) {
                    conn.rollback();
                    System.out.println("1 rollback");
                    e.printStackTrace();
                }
                conn.close();
            } catch (Exception throwables) {
                throwables.printStackTrace();
            }
        });

        service.submit(() -> {
            try {
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

                conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

                conn.setAutoCommit(false);
                try {
                    int number = 50;

                    Statement statement = conn.createStatement();

                    ResultSet resultSet = statement.executeQuery("select * from dev.`users` where id = 1");

                    resultSet.next();

                    int amount1 = resultSet.getInt("amount");

                    String update1 = "UPDATE `dev`.`users` SET `amount` = " + (amount1 - number) + " WHERE (`id` = '1');";

                    statement.executeUpdate(update1);

                    resultSet = statement.executeQuery("select * from dev.`users` where id = 3");
                    resultSet.next();

                    int amount2 = resultSet.getInt("amount");

                    String update2 = "UPDATE `dev`.`users` SET `amount` = " + (amount2 + number) + "  WHERE (`id` = '3');";

                    statement.executeUpdate(update2);

                    conn.commit();

                } catch (Exception e) {
                    conn.rollback();
                    System.out.println("2 rollback");
                    e.printStackTrace();
                }
                conn.close();
            } catch (Exception throwables) {
                throwables.printStackTrace();
            }
        });
        service.shutdown();
    }
}
