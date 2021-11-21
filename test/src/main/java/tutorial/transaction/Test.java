package tutorial.transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test {
    static final String DB_URL = "jdbc:mysql://127.0.0.1/";
    static final String USER = "root";
    static final String PASS = "anhdem96";

    public static void main(String[] args) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.submit(() -> {
            try {
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                try {
                    int number = 100;

                    Statement statement = conn.createStatement();

                    ResultSet resultSet = statement.executeQuery("select * from dev.`users` where id = 1");

                    resultSet.next();

                    int amount1 = resultSet.getInt("amount");

                    String update1 = "UPDATE `dev`.`users` SET `amount` = " + (amount1 - number) + " WHERE (`id` = '1');";
                    System.out.println("1 +" +update1);
                    statement.executeUpdate(update1);

                    resultSet = statement.executeQuery("select * from dev.`users` where id =2");
                    resultSet.next();

                    int amount2 = resultSet.getInt("amount");
                    System.out.println("1 amount2 " + amount2);
                    countDownLatch.countDown();
                    String update2 = "UPDATE `dev`.`users` SET `amount` = " + (amount2 + number) + "  WHERE (`id` = '2');";
                    System.out.println("1 " +update2);
                    statement.executeUpdate("UPDATE `dev`.`users` SET `amount` = " + (amount2 + number) + "  WHERE (`id` = '2');");

                    conn.commit();
                } catch (Exception e) {
                    conn.rollback();
                    System.out.println("1 rollback");
                    e.printStackTrace();
                }
                System.out.println("1 done");
                conn.close();
            } catch (Exception throwables) {
                throwables.printStackTrace();
            }
        });

        service.submit(() -> {
            try {
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

                conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

                conn.setAutoCommit(false);
                try {
                    int number = 100;
                    Statement statement = conn.createStatement();
                    countDownLatch.await();
                    ResultSet resultSet = statement.executeQuery("select * from dev.`users` where id =2");
                    resultSet.next();

                    int amount2 = resultSet.getInt("amount");
                    System.out.println("2 amount2 " + amount2);


                    String update2 = "UPDATE `dev`.`users` SET `amount` = " + (amount2 - number) + "  WHERE (`id` = '2');";
                    System.out.println(update2);

                    statement.executeUpdate(update2);

                    resultSet = statement.executeQuery("select * from dev.`users` where id = 3");

                    resultSet.next();

                    int amount3 = resultSet.getInt("amount");

                    String  update3 = "UPDATE `dev`.`users` SET `amount` = " + (amount3 + number) + " WHERE (`id` = '3');";
                    System.out.println(update3);

                    statement.executeUpdate(update3);
                    conn.commit();


                } catch (Exception e) {
                    conn.rollback();
                    System.out.println("2 rollback");
                    e.printStackTrace();
                }

                conn.close();
                System.out.println("2 done");
            } catch (Exception throwables) {
                throwables.printStackTrace();
            }
        });
        service.shutdown();
    }
}
