# Simple transaction in memory
Khi xây dựng hệ thống backend chúng ta thường xuyên phải làm việc với **transaction** của các loại cơ sở dữ liệu quan 
hệ khác nhau. Việc nắm bắt và sử dụng thành thạo các loại này là việc cần thiêt để xây dựng một hệ thống backend tốt.
Tuy nhiên trong nhiều trường hợp **database** chúng ta sử dụng lại không hỗ trợ transaction hoặc việc **insert** vào
các loại cơ sở dữ liệu quan hệ được xủ lý bỏi các hệ thống **acsync** điều này khiến chúng ta phải xủ lý các **transaction**
trên  logic trong **application** của mình. Những hệ thống trước đây tôi xây dựng đều phải xử lý điều này và sau khi 
tìm được một cách hay ho có thể sử dụng lại thì tôi quết định viết bài chia sẻ cho mọi người.

## Cài đặt transaction in memory
Ta sẽ chú ý ví dụ lưu các sản phẩm khách hàng mua thành công trên RAM sau :

```java

import blog.common.transaction.element.TxnHashMap;

import java.util.ArrayList;
import java.util.List;


public class TestTransaction {
    private TxnHashMap<String, List<String>> userData;

    public TestTransaction() {
        this.userData = new TxnHashMap<>();
    }

    private void buySuccess(String userName, String productName) {
        if (userData.containKey(userName)) {
            userData.put(userName, new ArrayList<>());
        }
        userData.get(userName).add(productName);
    }

    private void buyFail() {
        throw new RuntimeException(" something is error");
    }

    public TxnHashMap<String, List<String>> getUserData() {
        return userData;
    }

    public void setUserData(TxnHashMap<String, List<String>> userData) {
        this.userData = userData;
    }

    public static void main(String[] args) {

        TestTransaction test = new TestTransaction();

        TxnManager manager = new TxnManager();

        manager.executeTransaction(() -> {
            test.buySuccess("demtv", "iphone");
            test.buySuccess("demtv", "ipad");
            test.buySuccess("maitv", "ipad");
            manager.executeTransaction(() -> {
                test.buySuccess("maitv", "ipad");
                test.buyFail();
                return null;
            });
            return null;
        });

        manager.executeTransaction(() -> {
            test.buySuccess("maipm", "iphone");
            test.buyFail();
            return null;
        });

        System.out.println(test.getUserData().getValue());
    }

}
```
Tại đây tôi sẽ lưu các sản phẩm khách hàng mua thành công vào một `TxnHashMap` là implement của một **HashMap**
nhưng áp dụng cho transaction. Kết quả của lệnh in là

`
 {maitv=[ipad], demtv=[iphone, ipad]}
`
### Cài đặt transaction
Tại đây tôi có các class chính sau :
- `interface Txn` : Class này đại diện cho một transaction trong hệ thống, đảm bảo các `TxnElement` được hoạt động. Chứa
tất cả thay đổi của `TxnElement` trong một tracsaction để thực hiện commit hoặc rollback khi cần.
- `TxnManager` : Class quản lý cách transaction của hệ thống hoạt động.
- `interface TxnElement` : Muốn sử dụng được transaction thì cần phải implement lại **interface** này. Trong ví dụ trên 
là `TxnHashMap`. 

Cài đặt chi tiết mọi người tham khảo trong project nhé. 

### Ứng dụng
Tại đây tôi chỉ implement `TxnHashMap` để sử dụng trên RAM của chương trình trong thực tế mọi người có thể tự 
impl các class khác nhau để phục vụ cho các mục đích khác nhau. Ví dụ thao tác với 1 key trên redis, gọi hệ thống
ngoài,... 

Nếu mọi người dùng được cách implement này thì cho mình 1 sao nhé.
