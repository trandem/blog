# Bitwise in Action
Trong chương trình giảng dạy IT của các trường đại học, cao đẳng hay trung tâm dạy nghề chúng ta đều được học các phép tính
toán sau : 
- Số học (+,-*,/,%)
- Logic (&&,|| )
- Bitwise (&,|,~,^,>>,<<)

Trong khi phép toán **Số học** và **Logic** được ưng dụng rộng rãi trên hầu hết các hệ thống phần mềm
cũng như bài tập trên trường lớp. Phép toan **Bitwise** có vẻ được ít các bạn lập trình viên quan tâm 
và rất ít người áp dụng được phép toán này trong các hệ thống phần mềm.

**Bitwise** là một phép toán cực quan trọng nếu nắm bắt được nó bạn có thể hiểu được một số tư tưởng của
một số thuật toán tạo ID cũng như các opensource sử dụng nó. Áp dụng **Bitwise** vào trong hệ thống phần
mêm của bạn sẽ giúp phần mềm của bạn trở lên tốt hơn cũng như giải quyết được nhiều bài toán khó.

## Bài toán tạo ID
Ứng dụng hay được sử dụng nhất và cũng là ứng dụng nổi bật nhất của phép tính toán **Bitwise** là tạo một ID chứa nhiều thông tin bên trong.

Thông thường mọi người sẽ lưu ID dưới dạng số tăng dần vào các RDMS như : mysql, oracle , mariadb, postgresql ,... Sử dụng cơ chế **auto increment** hay
**sequence** của các loại **DB** này để thực hiện tạo ID. Cách này sẽ làm tăng tải cho DB khi thực hiện **insert** dữ liệu. Kèm theo cách này thì thông 
tin của **ID** cung cấp cho chúng ta là không nhiều chỉ là đã tạo được bao nhiêu bản ghi nhưng nếu chúng ta xóa 1 **row** đi thì thông tin của ID mang
lại hầu như không sử dụng được.

Trên thực tế với các phầm mềm mình đã làm và được nhận lại từ các chuyên gia thì họ thường sẽ không tạo **ID** kiểu vậy và sẽ sử dụng phép tính toán
**Bitwise** để lưu thông tin. 

_Trong bài blog này mình sẽ lấy ví dụ và giải thích chi tiết thuật toán snowflake để các bạn có thể hiểu được tầm quan trọng của phép toán **bitwise** cũng
như mình sẽ cung cấp một số common function để làm việc với **bitwise**. Hy vọng sau bài này các bạn sẽ luyện tập và tạo được **ID** theo ý và chứa nhiều thông
tin hơn một số tự tăng_.

## Tạo ID theo thuật toán snowflake

Đối với các hệ thống phân tán ( **distributed system** ) việc tạo **ID** sao cho **unique** giữa các **service** cũng như **ID** chứa nhiều thông tin
là một thách thức rất lớn nếu chúng ta không sử dụng phép  **Bitwise**. 

Thuật toán nổi tiếng nhất để tạo **ID** cho hệ thống này là [snowflake](https://github.com/twitter-archive/snowflake). Bạn có thể tìm thấy rất nhiều 
**implement** trên mạng và tại **blog** này tôi cũng có 1 **implement** tại class **IdGenerator** trong project **common** cụ thể như sau:

```java
package blog.common.id;

import java.time.Instant;
import java.util.Date;

public class IdGenerator {

    private static final int NODE_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final int maxSequence = (int) (Math.pow(2, SEQUENCE_BITS) - 1);

    private static final long CUSTOM_EPOCH = 1420070400000L;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public static IdGenerator instance = new IdGenerator();

    public synchronized long nextId() {
        long id = nextId(188);
        return id;
    }


    private synchronized long nextId(int nodeId) {
        long currentTimestamp = timestamp();

        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Invalid System Clock!");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & maxSequence;
            if (sequence == 0) {
                // Sequence Exhausted, wait till next millisecond.
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            // reset sequence to start with zero for the next millisecond
            sequence = 0;
        }

        lastTimestamp = currentTimestamp;

        long id = currentTimestamp << (NODE_ID_BITS + SEQUENCE_BITS);

        id |= (nodeId << SEQUENCE_BITS);
        id |= sequence;
        return id;
    }


    private static long timestamp() {
        return Instant.now().toEpochMilli() - CUSTOM_EPOCH;
    }

    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp == lastTimestamp) {
            currentTimestamp = timestamp();
        }
        return currentTimestamp;
    }

    public short getNodeIdById(long id) {
        return (short) ((id >> SEQUENCE_BITS) & 0x2ff);
    }

    public long getTimestampById(long id) {
        return (id >> 22) + CUSTOM_EPOCH;
    }
}
```

Nếu bạn không hiểu nhiều về phép **bitwise** thì bạn có thể không hiểu lý do tại sao cách tạo ID theo thuật toán **snowflake** sẽ cho
chúng ta các **ID** khác nhau trên một hệ thống phân tán. Và các thông tin chứa trong ID đó là gì. Chúng ta sẽ phân tích **snowflake** bằng cách **implement** trong bài blog này.

Khi tạo **ID** theo **implement** thì trong **ID** sẽ chứa các thông tin sau:
- Thời gian **ID** đó được tạo ra
- Node nào tạo ra **ID** đó theo NodeID
- sequence: thứ tự ID được tạo trong một milliseconds nếu có nhiều ID được tạo trong cùng thời gian. Thông tin này không quá quan trọng với logic sau này.

## Cách kết hợp 3 thông tin trong cùng ID
Như chúng ta biêt số **long** trong java sẽ la **8 bytes** và là **64 bits**. Việc cần làm ở đây là quy định các thông tin chứa trong **ID** sẽ chiếm bao nhiêu bits
và sẽ nàm ở vị trí nào trong **64 bits** này, vì bit đầu là **bit dấu** nên chúng ta chỉ dùng được **63 bits**. Với cách **implement** trên thì chúng ta sắp xếp như sau: 
- Thời gian tạo **ID** : 41 bits đầu tiên Với 41 bits này thì chúng ta có thể tạo **ID** trong khoảng 69 năm tính từ thời điểm **CUSTOM_EPOCH**.
 Bởi vì nếu vượt qua **69 năm** thì ` Instant.now().toEpochMilli() - CUSTOM_EPOCH;` sẽ vượt qua **41 bits** khiến cho **ID** tạo ra không còn đảm bảo đúng được nữa.
- Định danh service tạo **ID** (nodeId) : 10 bits. Có thể chứa tới 1024 định danh của các **service** khác nhau. Mình chưa bao giờ thấy **service** nào có nhiều **instance** đến
thế. 
- Số tăng dần **sequence** : 12 bit . sẽ tạo ra 4096 ID khác nhau trong một millisecond. Nếu thời gian gọi tạo **ID** của service là trùng nhau ở mức **millisecond** thì  **sequence**
sẽ có tác dụng tạo ra các **ID** tăng dần khác nhau. 


### ví dụ
Lý thuyết là thế sau đây mình sẽ làm một ví dụ cho các bạn hiểu thêm về cách hoạt động của **snowflake**.

Thời điểm mình tạo ID tính theo millisecond sẽ là **1649991105180L** sau khi trừ đi **CUSTOM_EPOCH** sẽ được số **229920705180**. Đổi số này thành dạng nhị phân dưới dạng **long** ta sẽ được số
`00000000 00000000 00000000 00110101 10001000 01010111 10001010 10011100`

Sau khi dịch sang trái 22 bit thì ta sẽ được số dạng nhị phân sau:
`00001101 01100010 00010101 11100010 10100111 00000000 00000000 00000000`

Mình đang hard code **NodeID** là : 18, đổi số này sang sạng **binary long** sẽ được số sau :
`00000000 00000000 00000000 00000000 00000000 00000000 00000000 00010010`

Dịch sang trái 10 bit ta được số sau :
`00000000 00000000 00000000 00000000 00000000 00000000 01001000 00000000`

Sequence đang là "0" ta sẽ được số sau :
`00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000`

ta thực hiện phép toán **|** để gộp 3 số sau thành 1 số **long** mới ta được
```
 00001101 01100010 00010101 11100010 10100111 00000000 00000000 00000000
|
 00000000 00000000 00000000 00000000 00000000 00000000 01001000 00000000
|
 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000
-----------------------------------
 00001101 01100010 00010101 11100010 10100111 00000000 01001000 00000000
```
Đổi ra số dạng decimal được : 964357333419313152

### Lấy các thông tin được lưu trong ID ra
Ta chỉ cần làm các động tác ép kiểu và **bitwise** ngược lại thì sẽ lấy ra được thông tin như **thời gian tạo ID**, **service nào tạo ID**. 
Trong code **Demo** bên trên mình đã **code** sẵn các hàm giúp các bạn lấy ra thông tin này. 

```
    public short getNodeIdById(long id) {
        return (short) ((id >> SEQUENCE_BITS) & 0x2ff);
    }

    public long getTimestampById(long id) {
        return (id >> 22) + CUSTOM_EPOCH;
    }
```
Phần giải thích tại sao lại lấy ra thông tin bằng các hàm này mình sẽ nhường các bạn để hiểu hơn về cách hoạt động của **bitwise** sau này có thể
tùy chỉnh thuật toán **snowflake** theo ý.

Việc lấy ra các thông tin này cung cấp cho chúng ta khả năng thực hiện các **validate** dữ liệu mà không cần phải vào **Database** lấy các thông tin này ra.
Trên thực tế nếu tạo **ID** kiểu này mình thường lấy thời gian ra để thực hiện một số nghiệp vụ validate timeout,...

### Tự tạo thuật toán sinh ID
Việc nắm rõ cách hoạt động của một số **bitwise** cơ bản như (<<,>>,|,&) sẽ giúp các bạn không chỉ tạo **ID** theo thuật toán **snowflake** mà giúp các bạn tạo
ra các loại **ID** có nhiều thông tin theo ý muốn của bạn. Ví dụ bạn muốn tạo một **customerID** chứa thông tin của quốc gia của **customer** đó thì bạn có thể tạo 
một **customerID** dạng **integer** với 9 bits đầu để lưu mã quốc gia (vì một bit đầu tiên là bit dấu mà có hơn 200 quốc gia trên toàn thể giới) và 23 bit sau thì lưu **sequence**.
```
    public static int generateCustomerId(short nationalId, int sequence) {
        int id = ((int) (nationalId & 0x1ff)) << 23;
        id |= sequence & 0x007FFFFF;
        return id;
    }

    public static short getNationalId(int customerId) {
        short id = (short) ((short) (customerId >> 23) & 0x1ff);
        return id;
    }
```

### Một số ứng dụng khác khi dùng bitwise
Các ứng dụng này sẽ là các cải thiện về hiệu năng của chương trình khi chạy nhưng nó chỉ cải thiện rất nhỏ vì các cách khác cũng đủ nhanh rồi.

**Số chắn số lẻ**
```
// Returns true if n is even, else odd
static boolean isEven(int n)
{
    // n&1 is 1, then odd, else even
    return ((n & 1)!=1);
}

   // Returns true if n is even, else odd
    static boolean isEven1(int n)
    {
 
        // n^1 is n+1, then even, else odd
        if ((n ^ 1) == n + 1)
            return true;
        else
            return false;
    }
```

**Binary serialize**
Một ứng dụng tuyệt vời khác của bitwise là dùng để **serialize** dữ liệu thành  **array byte** sau đó được ghi vào các file nhị phân 
(các loại **database** đều dùng binary file), hoặc truyền các **array byte** này thông qua mạng đến các **service**. Vì đọc file nhị phân
hay deserialize **array byte** sẽ nhanh hơn và tiết kiệm tài nguyên hơn rất nhiều so với **json** nên nó nâng cao tốc độ của **service** của bạn.

Các thư viện giúp bạn làm điều này như : proto, thrift, kryo,... Hoặc các bạn cũng có thể tự tạo cho mình một cách **serialize** dữ liệu bằng cách
sử dụng **bitwise** và nên tuân theo cách **big-endian (BE) or little-endian (LE)**. Mình cũng đã từng chia sẻ sơ qua về cách này tại **blog** 
[How to serialize data in java like protobuf](https://demtv.hashnode.dev/how-to-serialize-data-in-java-like-protobuf). Nếu các bạn quan tâm 
thì vào đọc ủng hộ giúp mình nhé.

Đây là cách mình dùng **bitwise** để **serialize** một số int thành 1-5 bytes thay vì 4 bytes.
```
default void writeIntOptimise(int value) {
        if ((value < 0)) throw new IllegalArgumentException("pack int: " + value);
        int x;
        while (true) {
            x = value & 0x7F;
            value >>>= 7;
            if (value != 0) {
                this.writeByte((byte) (x | 0x80));
            } else {
                this.writeByte((byte) x);
                break;
            }
        }
    }
```
Chi tiết các bạn tham khảo code phần serilize này tại link [github](https://github.com/trandem/blog/tree/main/common/src/main/java/blog/serialize/impl/io)


## Bitwise common number
Tại hệ thống mình làm khi giao tiếp với phần mềm bên thứ 3 họ chỉ chấp nhận số lớn nhất là **int** nhưng **ID** của hệ thống mình là một số **long** vậy nên bọn mình đã
sử dụng **bitwise** để tách 1 số **long** thành 2 số **int** và khi nhận về 2 số **int** sẽ ghép thành 1 số **long** để 2 hệ thống kết hợp với nhau tốt hơn. Do đó mình
xin chia sẻ cho anh em các hàm common chuyên sử dụng để tách các số như vậy.

```java
package blog.common.id;

public class Numbers {
    
    // phan tach so long thanh int va nguoc lai
    public static final long toLong(final int v1, final int v2) {
        return (((long)v1 << 32) & 0xFFFFFFFF00000000L) | ((long)v2 & 0x00000000FFFFFFFFL);
    }
    
    public static int lowInt(long value) {
        return (int)(value & 0xFFFFFFFFL);
    }

    public static int highInt(final long value) {
        return (int)((value >>> 32) & 0xFFFFFFFFL);
    }


    // phan tach so int thanh short va nguoc lai
    public static final int toInt( short v1, short v2 ) {
        return ((v1 << 16) & 0xFFFF0000) | (v2 & 0x0000FFFF);
    }

    public static short lowShort(int value) {
        return (short)(value & 0xFFFF);
    }
    
    public static short highShort(final int value) {
        return (short)((value >>> 16) & 0xFFFF);
    }

    
    // phan tach so short thanh byte va nguoc lai
    public static final short toShort(byte v1, byte v2) {
        return (short)(((v1 << 8) & 0xFF00) | (v2 & 0x00FF));
    }

    public static byte lowByte(short value) {
        return (byte)(value & 0xFF);
    }

    public static byte highByte(short value) {
        return (byte)((value >>> 8) & 0xFF);
    }
}
```
 
## Tổng kết
Nắm vững kỹ thuật **bitwise** này sẽ giúp ứng dụng của bạn trở lên thông minh hơn. **ID** sẽ trở lên có ý nghĩa hơn. Trong bài mình sử dụng **java** để thực hiện viết
**demo** thuật toán cũng như **common** nhưng **bitwise** sẽ sử dụng trong tất cả các ngôn ngữ lập trình. Hiện tại mình đang học và lập trình ngôn ngữ **C++** và vẫn 
áp dụng phép tính toán này bình thường. Hy vọng sau khi làm quen với **C++** thì mình sẽ có một số bài **blog** chia sẻ về ngôn ngữ này. 

Nếu bài viết này hay thì mình xin một sao github cho có động lực update thêm bài tiếp theo nhé. 

