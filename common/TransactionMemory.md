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

import blog.common.concurrent.FastThreadLocal;
import blog.common.transaction.base.Txn;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class TxnManager {

    public <T> T executeTransaction(Supplier<T> supplier) {
        TransactionInfo info = createTransaction();
        Txn transactionBase = info.current;

        try {
            transactionBase.begin();

            T r = supplier.get();

            transactionBase.commit();
            return r;

        } catch (Exception e) {
            transactionBase.rollback();
            return null;
        } finally {
            STX.set(info.getPrevious());
        }
    }

    private TransactionInfo createTransaction() {
        Txn base = new TxnImpl();
        TransactionInfo prev = STX.get();
        TransactionInfo next = new TransactionInfo(base, prev);
        STX.set(next);
        return next;
    }

    static class TransactionInfo {
        private Txn current;
        private TransactionInfo previous;

        public TransactionInfo(Txn current, TransactionInfo info) {
            this.current = current;
            this.previous = info;
        }

        public Txn getCurrent() {
            return current;
        }

        public void setCurrent(Txn current) {
            this.current = current;
        }

        public TransactionInfo getPrevious() {
            return previous;
        }

        public void setPrevious(TransactionInfo previous) {
            this.previous = previous;
        }
    }

    private static final FastThreadLocal<TransactionInfo> STX = new FastThreadLocal<>();

    public static Txn current() {
        if (STX.get() == null) return null;
        return STX.get().current;
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