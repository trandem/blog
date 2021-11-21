# Transaction 
**Transaction** là một khái niệm rất quen thuộc với các lập trình viên **backend**. Nhưng trong thực tế làm
việc không ít các lập trình viên **backend** chỉ biết transaction dùng để thực hiện một chuỗi câu lệnh insert,update,...
với **sql**. **Database** thực sự thay đổi khi tất cả các lệnh trên đều thực hiện thành công ngược lại thì sẽ không 
có bất cứ thay đổi nào trên database. Các loại **SQL** còn cung cấp cho chúng ta các loại **Isolation** để tùy chỉnh
**Transaction** theo ý muốn của mình.

Khi thực hiện 1 **Transaction** các cơ sở dữ liệu quan hệ (**SQL**) cung cấp cho chúng ta rất 4 loại **Isolation** khác
nhau với nhiều mục đích sử dụng khác nhau. Loại **Isolation** càng cao thì sẽ có mức độ toàn vẹn dữ liệu càng cao nhưng ngược 
lại sẽ có hiệu năng thấp hơn với các loại khác.
- **TRANSACTION_READ_UNCOMMITTED** mức độ 1
- **TRANSACTION_READ_COMMITTED** mức độ 2
- **TRANSACTION_REPEATABLE_READ** mức độ 3
- **TRANSACTION_SERIALIZABLE** mức độ 4

Trong bài này tôi sẽ trình bày về các loại **Isolation** cũng như các **Anomaly** về dữ liệu khi thực hiện đồng thời **Transaction**

## Phân biệt các loại Anomaly
### Dirty Reads 
**Dirty Read** là việc ransaction (T1) đọc được dữ liệu đang được thay đổi từ một transaction (T2) khác 
mặc dù transaction chưa được commit nghĩa là T2 đó có thể bị rollback và dữ liệu được đọc từ và dùng ở T1 đã bị sai.

Ví dụ : 

```sql
Transaction T1 begins.
UPDATE post SET title = 'ACID'
WHERE id = '1'

Transaction T2 begins.
SELECT * FROM post where id = '1'
```

T2 sẽ nhìn thấy title = 'ACID' cái được update từ T1 mặc dù T1 chưa commit.


![DirtyRead.png](https://cdn.hashnode.com/res/hashnode/image/upload/v1637380642990/cftruGVs90.png)

### Nonrepeatable Reads
**Nonrepeatable Reads** sảy ra khi trong cùng một **transaction** (T2) query trả lại các quả khác nhau.

**Nonrepeatable Reads** sảy ra khi một **transaction**(T1) đã sửa đổi dữ liệu cái mà được đọc từ **transaction** (T2)

Ví dụ :
```sql
Transaction T2 begins.
SELECT * FROM post
WHERE id = '1' 

Transaction T1 begins.
UPDATE post SET title = 'ACID'WHERE id = '1' 


```
Transaction T1 thay đổi giá trị cảu **title** thành **ACID** cái mà đang được **transaction** T2 đọc ra
nếu **transaction** T2 tiếp tục đọc lại giá trị này thì sẽ được kết quả khác với kết quả lần đầu tiên đọc ra.


![nonereapeat.png](https://cdn.hashnode.com/res/hashnode/image/upload/v1637380668581/yvVIJgrkx.png)

##Phantom Reads
**Phantom Reads** xảy ra khi một transaction (T1) đọc ra một tập kết quả dữ liệu sau thỏa mãn điều kiện **where** của mình sau đó một transaction (T2) thực hiện thêm dữ liệu vào table. Transaction T1 chưa commit và thực hiện lại query 1 lần nữa thì được tập kết quả khác với tập đầu tiên đọc ra (nhiều bản ghi hơn).


Ví dụ
```sql
Transaction T1 begin
select * from post_comment where post_id =1;

Transaction T2 begin
insert into post_comment (id,post_id) Values(4,1)

```
Nếu **Transaction** T1 thực hiện lại câu query `select * from post_comment where post_id =1;` sẽ thu được số lượng kết quả lớn hơn so với ban đầu.


![PhantomRead.png](https://cdn.hashnode.com/res/hashnode/image/upload/v1637381029323/MXSieKNYC.png)


## Cách sử dụng isolation
Bên trên là các **Anomaly** có thể sảy ra trong trường hợp có 2 hay nhiều **transaction** chạy song song.
Để tránh sảy ra các loại **Anomaly** này thì người dùng cần phải chọn các loại **Isolation** cho phù hợp.

Đối với **oracle** chúng ta tham khảo **link** sau :
https://docs.oracle.com/javadb/10.8.3.0/devguide/cdevconcepts15366.html


| Type                         | Dirty Reads | Nonrepeatable Reads | Phantom Reads |
| ----                         | ----        | ----                | ---           |
| TRANSACTION_READ_UNCOMMITTED | no          | no                  | no            |
| TRANSACTION_READ_COMMITTED   | yes         | no                  | no            |
| TRANSACTION_REPEATABLE_READ  | yes         | yes                 | no            |
| TRANSACTION_SERIALIZABLE     | yes         | yes                 | yes           |

Bảng với mode locking là **Table Level**

| Type                         | Dirty Reads | Nonrepeatable Reads | Phantom Reads |
| ----                         | ----        | ----                | ---           |
| TRANSACTION_READ_UNCOMMITTED | no          | no                  | no            |
| TRANSACTION_READ_COMMITTED   | yes         | no                  | no            |
| TRANSACTION_REPEATABLE_READ  | yes         | yes                 | yes            |
| TRANSACTION_SERIALIZABLE     | yes         | yes                 | yes           |


## Thực hành các loại isolation
Tại rất khó tìm được 1 bảng thống kê các loại **Isolation** với mysql nên chúng ta sẽ thực hành để biết
loại **Isolation** sẽ chống lại loại **Anomaly** nào.
### TRANSACTION_READ_UNCOMMITTED 
Đây là dạng **isolation** cho chúng ta hiệu năng cao nhất. Để kiểm chứng lý thuyết thì ta sẽ thực hiện 
các câu lệnh java sau.
```java
package tutorial.transaction;

import java.sql.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static tutorial.transaction.Test.*;
import static tutorial.transaction.Test.PASS;

public class IsolationLevelTest {

    public static void main(String[] args) {
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

                    System.out.println("Transaction1 get amount = " + amount1 );

                    String update1 = "UPDATE `dev`.`users` SET `amount` = " + (amount1 - number) + " WHERE (`id` = '1');";

                    statement.executeUpdate(update1);

                    countDownLatch.countDown();
                    Thread.sleep(5000);
                    conn.rollback();
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

                conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

                conn.setAutoCommit(false);
                try {
                    Statement statement = conn.createStatement();
                    countDownLatch.await();
                    ResultSet resultSet = statement.executeQuery("select * from dev.`users` where id =1");
                    resultSet.next();

                    int amount2 = resultSet.getInt("amount");
                    System.out.println("Transaction2 get amount = " + amount2);

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
```
Tại ví dụ trên ta tạo 2 transaction chạy đồng thời. Transaction T1 chạy trước và sửa đổi giá trị amount, Sau đó
chúng ta chạy Transaction T2. Nếu để mode trên sẽ thu được **TRANSACTION_READ_UNCOMMITTED** kết quả sau.

```
Transaction1 get amount = 13004200
Transaction2 get amount = 13004100
```
Mặc dù T1 đã rollback nhưng T2 vẫn đọc ra kết quả được T1 thay đổi. Điều này chứng tỏ  **TRANSACTION_READ_UNCOMMITTED** 
không thể chống được **Dirty Read**.

### TRANSACTION_READ_COMMITTED 

Vẫn xét ví dụ trên nhưng ta sẽ thay đổi **IsolateLevel** của Transaction T2 thành `conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);`

Kết quả ta thu được là :
```
Transaction1 get amount = 13004200
Transaction2 get amount = 13004200
```
Theo kết quả ta thấy được **TRANSACTION_READ_COMMITTED** chống được  **Dirty Read**.

Ta sửa đổi class trên 1 chút thành dạng sau đây.

```java
package tutorial.transaction;

import java.sql.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static tutorial.transaction.Test.*;
import static tutorial.transaction.Test.PASS;

public class IsolationLevelTest {

    public static void main(String[] args) {
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

                    System.out.println("Transaction1 get ammount = " + amount1 );

                    String update1 = "UPDATE `dev`.`users` SET `amount` = " + (amount1 - number) + " WHERE (`id` = '1');";

                    statement.executeUpdate(update1);

                    countDownLatch.countDown();
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

                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

                conn.setAutoCommit(false);
                try {
                    Statement statement = conn.createStatement();
                    countDownLatch.await();
                    ResultSet resultSet = statement.executeQuery("select * from dev.`users` where id =1");
                    resultSet.next();

                    int amount2 = resultSet.getInt("amount");
                    System.out.println("Transaction2 get amount = " + amount2);
                    
                    Thread.sleep(1000);

                    resultSet = statement.executeQuery("select * from dev.`users` where id =1");
                    resultSet.next();

                    amount2 = resultSet.getInt("amount");
                    System.out.println("Transaction2 re read get amount = " + amount2);

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
```

Tại đây ta thay đổi Transaction T2 đọc lại dữ liệu thêm 1 lần nữa. Nhưng lần này Transaction T1 đã commit thay đổi
kết quả của mình. Kết quả sẽ là

```
Transaction1 get ammount = 13003900
Transaction2 get amount = 13003900
Transaction2 re read get amount = 13003800
```
Điều này thấy được **TRANSACTION_READ_COMMITTED** không chống lại được **Nonrepeatable Reads**

### TRANSACTION_REPEATABLE_READ
Tiếp tục với ví dụ trên ta thay đổi **IsolateLevel** của Transaction T2 thành `conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);`

Kết quả sẽ thay đổi thành.

```
Transaction1 get ammount = 13003800
Transaction2 get amount = 13003800
Transaction2 re read get amount = 13003800
```
Đây là **mode IsolateLevel** mặc định của **Jdbc** với **Mysql** nhưng sẽ đưa lại hiệu năng kém hơn 2 mẫu còn lại.

Hiện tại bảng dùng để test đang dùng **innodb** của **Mysql**. Chúng ta cùng test xem liệu rằng **TRANSACTION_REPEATABLE_READ**
có chống được lại **Phantom Reads** được hay không. Cùng xét ví dụ sau

```java
package tutorial.transaction;

import java.sql.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static tutorial.transaction.Test.*;
import static tutorial.transaction.Test.PASS;

public class IsolationLevelTest {

    public static void main(String[] args) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CountDownLatch countLatch = new CountDownLatch(1);
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.submit(() -> {
            try {
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                try {
                    Statement statement = conn.createStatement();

                    String update1 = "INSERT INTO `dev`.`users` (`full_name`, `amount`) VALUES ('lol', '5000');;";

                    statement.executeUpdate(update1);

                    conn.commit();

                } catch (Exception e) {
                    conn.rollback();
                    System.out.println("1 rollback");
                    e.printStackTrace();
                }
                conn.close();
                System.out.println("1 done");
            } catch (Exception throwables) {
                throwables.printStackTrace();
            }
        });

        service.submit(() -> {
            try {
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

                conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

                conn.setAutoCommit(false);
                try {
                    int count =0;
                    Statement statement = conn.createStatement();

                    ResultSet resultSet = statement.executeQuery("select * from dev.`users` where  amount > 2500");
                    while (resultSet.next()){
                        count++;
                    }
                    resultSet.close();
                    System.out.println(" first read = "+ count );

                    Thread.sleep(2000);
                    count =0;

                    ResultSet resultSet1 = statement.executeQuery("select * from dev.`users` where  amount > 2500");
                    while (resultSet1.next()){
                        count++;
                    }
                    System.out.println(" second read = "+ count );
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

```

Kết quả của lệnh in sẽ là :
```
 first read = 1
 second read = 1
```
Như vậy ta kết luận được **TRANSACTION_REPEATABLE_READ** của **mysql** sẽ chống lại **Phantom Reads** với  **innodb**.

Chuyển sang dạng **MyISAM** và test lại code ta được kết quả sau:

```
 first read = 2
 second read = 3
```
Vậy với dạng  **MyISAM** thì mysql sẽ không chống lại được **Phantom Reads**.

### TRANSACTION_SERIALIZABLE
Cũng tương tự như **TRANSACTION_READ_COMMITTED** thì **TRANSACTION_SERIALIZABLE** cũng chống lại  **Phantom Reads** với  **innodb**.

Đến đây câu hỏi sẽ là vậy **Mysql** tạo ra thêm dạng **Isolation** này để làm gì? Hiệu năng của **TRANSACTION_SERIALIZABLE**
mang lại là tệ nhất trong các loại **Isolation**.

Ta xét ví dụ chuyển tiền trong ngân hàng :

```
A -> B (100). A chuyển cho B 100 

A -> C (50). B chuyển cho C 50

```
Ta sẽ xét đoạn code sau của Java

```java
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
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
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

                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

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

```

Khi bạn chạy chương trình không có lỗi gì sảy ra. Bạn sẽ thông báo là đã chuyển đủ tiền từ A sang các tài khoản khác.
Nhưng khi check db thì thấy kết quả đang bị sai. Nó chỉ đang ghi nhận cho 1 transaction. Thay vì Tài khoản của A bị giảm
đi 150 thì tài khoản của A sẽ chỉ giảm 50 hoặc 100. Điều này dẫn đến lỗi sai nghiêm trọng.

Để giải quyết bài toán cần sự tuần tự trong transaction này chúng ta cần sử dụng mode **TRANSACTION_SERIALIZABLE** sau khi 
thay đổi `conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);` chúng ta chạy lại code sẽ được kết quả sau
```
2 rollback
com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction
	at com.mysql.cj.jdbc.exceptions.SQLError.createSQLException(SQLError.java:123)
	at com.mysql.cj.jdbc.exceptions.SQLExceptionsMapping.translateException(SQLExceptionsMapping.java:122)
	at com.mysql.cj.jdbc.StatementImpl.executeUpdateInternal(StatementImpl.java:1340)
	at com.mysql.cj.jdbc.StatementImpl.executeLargeUpdate(StatementImpl.java:2089)
	at com.mysql.cj.jdbc.StatementImpl.executeUpdate(StatementImpl.java:1251)
	at tutorial.transaction.IsolationLevelTest.lambda$main$1(IsolationLevelTest.java:75)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
```
Đến đây thì 1 trong 2 transaction sẽ bị rollback và bắn ra exception. Điều này sẽ giúp bạn chống được lỗi trên.

## Kết luận
Đến đây thì mọi người chắc cũng biết cách chọn loại transaction cho riêng mình rồi mình xin có 1 số ý kiến như sau:
- Thông thường khi đọc kết quả từ 1 câu **select** chúng ta sẽ lưu lại nó trên **RAM** và rất ít khi thực hiện
lại câu **select** tương tự vào **SQL** nên để đảm bảo **performance** thì hãy trọn mode **TRANSACTION_READ_COMMITTED**
  
- Nếu hệ thống bạn cần phải thực hiện transaction tuần tự hóa thì hãy trọn **TRANSACTION_SERIALIZABLE** mặc dù hiệu năng 
nó rất tệ nhưng nó mang lại kết quả tốt.
  
Sử dụng  **TRANSACTION_SERIALIZABLE**  sẽ mang lại cho bạn 1 hệ thống **Strong consistency** nhưng hệ thống của bạn sẽ thật
sự có hiệu năng rất tệ nếu có nhiều người sử dụng. Bạn cũng có thể sử dụng các loại **lock** để đảm bảo được tính **consistency**
hãy nghiên cứu thử **optimistic locking** biết đâu sẽ phù hợp với hệ thống của bạn. 

Một kỹ thuật nữa là sử dụng **eventual consistency** một kỹ thuật tăng được hiệu năng nhưng cũng khá khó implement. 
Nếu có dịp mình sẽ xây 1 hệ thống chuyển tiền bằng kỹ thuật **eventual consistency** trên. Mình nghĩ kỹ thuật này đang
được dùng tại nhiều hệ thống chuyển tiền để tăng trải nghiệm khách hàng.

