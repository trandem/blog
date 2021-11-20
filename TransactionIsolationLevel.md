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
Bên trên là các **Anomaly** có thể sảy ra trong trường hợp có 2 hay nhiều **transaction** chạy song song. Để tránh sảy ra các loại **Anomaly** này thì người dùng cần phải chọn các loại **Isolation** cho phù hợp.

| Type                         | Dirty Reads | Nonrepeatable Reads | Phantom Reads |
| ----                         | ----        | ----                | ---           |
| TRANSACTION_READ_UNCOMMITTED | no          | no                  | no            |
| TRANSACTION_READ_COMMITTED   | yes         | no                  | no            |
| TRANSACTION_REPEATABLE_READ  | yes         | yes                 | no            |
| TRANSACTION_SERIALIZABLE     | yes         | yes                 | yes           |

## Thực hành các loại isolation

### TRANSACTION_READ_UNCOMMITTED 
Đây là dạng **isolation** cho chúng ta hiệu năng cao nhất. Để kiểm chứng lý thuyết thì ta sẽ
thực hiện các câu lệnh java sau.




