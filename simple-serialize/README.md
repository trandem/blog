# How to serialize data in java like protobuf
Khi dữ liệu được chuyển qua mạng qua các hệ thống rpc, msg queue,internal service,... Tùy vào các hệ thống chúng ta cần 
serialize dữ liệu thành dạng **json** hoặc **array byte**. Tuy nhiên để đảm bảo tốc đố của hệ thống thì phương pháp
 **serialize** dữ liệu thành **array byte** sẽ được sử dụng rộng rãi hơn.
 
Hiện nay việc serialize đã có các công ty lớn tạo ra các thư viện khác nhau với chất lượng và tốc độ rất cao như :
 - [Protobuf](https://developers.google.com/protocol-buffers) của google.
 - [Thrift](https://thrift.apache.org/) của facebook.
 - [Kryo](https://github.com/EsotericSoftware/kryo) một framework mạnh mẽ của java.

Việc sử dụng các framework trên là khá dễ dàng nên lập trình viên thường sẽ coi việc serialize dữ liệu là một hộp đen.
Điều này khá nguy hiểm vì trên thực tế có những project đội ngũ lập trình thường sẽ không dùng các framework có sẵn
vì một số lý do như : nhiều dependency, tốc độ chưa đảm bảo, mất công tạo file (.proto, .thrift) khiến thay đổi các
**object** gây khó khăn. Tại project này sẽ giới thiệu cho mọi người một cách để **serialize** dữ liệu thành **array byte**.

## BIG_ENDIAN VS LITTLE_ENDIAN
Để serialize dữ liệu sang dạng **array byte** trong máy tính chúng ta có 2 cách **serialize** chính là BIG_ENDIAN và LITTLE_ENDIAN 
mọi người tham khảo lý thuyế tại wiki: https://en.wikipedia.org/wiki/Endianness

Chúng ta sẽ hiểu đơn gian như sau :
- BIG_ENDIAN : sẽ ghi dấu trước ghi dữ liệu khác sau
- LITTLE_ENDIAN : sẽ ghi dữ liệu trước và ghi dấu sau.

Ví dụ ta sẽ biến số interger 32 và -32 thành array byte ta code như sau:
```java
public class Test {
    public static void main(String[] args){
        int number = 32;
        ByteBuffer x = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        x.putInt(number);
        System.out.println("LITTLE_ENDIAN");
        for (int i = 0 ; i < 4 ; i++){
            System.out.printf("%d ", x.array()[i]);
        }
        System.out.println();
        System.out.println("BIG_ENDIAN");
        ByteBuffer y = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        y.putInt(number);
        for (int i = 0 ; i < 4 ; i++){
            System.out.printf("%d ", y.array()[i]);

        }
    }
}
```
Ta sẽ được kết quả sau:
```
LITTLE_ENDIAN
32 0 0 0 
BIG_ENDIAN
0 0 0 32 
```
Thay number = -32 ta được kết quả sau:
```
LITTLE_ENDIAN
-32 -1 -1 -1 
BIG_ENDIAN
-1 -1 -1 -32 
```

## Protobuf Serialize Data
Tiếp đến ta sẽ tìm hiểu cách protobuf serialize các kiểu đơn giản của chúng ta như thế nào. Tôi có file .proto sau
```
syntax = "proto3";

package proto;

option java_package = "blog.proto";

message SimpleObject {
    int32 count = 1;
    string name = 2;
    repeated DataObject dataList = 3;
}

message DataObject {
     int32 num1 = 1;
     int32 num2 = 2;
}
```
Tiếp theo ta sẽ sử dụng API của protobuf để serialize
```java
public class Test {
    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
        Blog.SimpleObject data = Blog.SimpleObject.newBuilder()
                .setCount(10)
                .setName("ânhdem976")
                .build();

        System.out.println(data.toByteArray().length); 
    }
}
``` 
Sau khi debug vào sâu bên trong framework (hàm `data.toByteArray()`) ta sẽ tìm thấy cách protobuf serialize object của chúng ta như sau:
```java
 public static final class SimpleObject extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:proto.SimpleObject)
      SimpleObjectOrBuilder {
//.........
    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (count_ != 0) {
        output.writeInt32(1, count_);
      }
      if (!getNameBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, name_);
      }
      for (int i = 0; i < dataList_.size(); i++) {
        output.writeMessage(3, dataList_.get(i));
      }
      unknownFields.writeTo(output);
    }
//.........

}
```
Với kiển interger như sau :
```java
//.........
    @Override
    public void writeInt32(int fieldNumber, int value) throws IOException {
      writeTag(fieldNumber, WireFormat.WIRETYPE_VARINT);
      writeInt32NoTag(value);
    }



 @Override
    public void writeInt32NoTag(int value) throws IOException {
      if (value >= 0) {
        writeUInt32NoTag(value);
      } else {
        // Must sign-extend.
        writeUInt64NoTag(value);
      }
    }

    @Override
    public void writeUInt32NoTag(int value) throws IOException {
      if (position <= oneVarintLimit) {
        // Optimization to avoid bounds checks on each iteration.
        while (true) {
          if ((value & ~0x7F) == 0) {
            UnsafeUtil.putByte(position++, (byte) value);
            return;
          } else {
            UnsafeUtil.putByte(position++, (byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
          }
        }
      } else {
        while (position < limit) {
          if ((value & ~0x7F) == 0) {
            UnsafeUtil.putByte(position++, (byte) value);
            return;
          } else {
            UnsafeUtil.putByte(position++, (byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
          }
        }
        throw new OutOfSpaceException(
            String.format("Pos: %d, limit: %d, len: %d", position, limit, 1));
      }
    }

    @Override
    public void writeUInt64NoTag(long value) throws IOException {
      if (position <= oneVarintLimit) {
        // Optimization to avoid bounds checks on each iteration.
        while (true) {
          if ((value & ~0x7FL) == 0) {
            UnsafeUtil.putByte(position++, (byte) value);
            return;
          } else {
            UnsafeUtil.putByte(position++, (byte) (((int) value & 0x7F) | 0x80));
            value >>>= 7;
          }
        }
      } else {
        while (position < limit) {
          if ((value & ~0x7FL) == 0) {
            UnsafeUtil.putByte(position++, (byte) value);
            return;
          } else {
            UnsafeUtil.putByte(position++, (byte) (((int) value & 0x7F) | 0x80));
            value >>>= 7;
          }
        }
        throw new OutOfSpaceException(
            String.format("Pos: %d, limit: %d, len: %d", position, limit, 1));
      }
    }
//.........
```
Dễ dàng thấy đối với kiểu interger > 0. protobuf sẽ chỉ mất từ 1-5 byte để serialize thành byte array chú ý hàm (writeUInt32NoTag)
ngược lại đối với số mà nhỏ hơn 0 thì protobuf sẽ mất 10 byte để serialize thành byte array. Điều này suy ra protobuf sẽ tạo ra
một array byte lớn hơn bình thường nếu số của chúng ta muốn serialize nhỏ hơn 0. Bình thường thì chỉ mất 4 bytes cho mọi thể loại số interger.

Tiếp tục ta sẽ kiểm tra tiếp đến kiểu dữ liệu string, debug chán chê các bạn sẽ đến được hàm sau trong class `com.google.protobuf.Utf8`, rảnh các bạn vào đọc source 
của nó cũng khá nhiều thứ hay ho.
```java
final class Utf8 {
//.........
  @Override
    int encodeUtf8(final CharSequence in, final byte[] out, final int offset, final int length) {
      long outIx = offset;
      final long outLimit = outIx + length;
      final int inLimit = in.length();
      if (inLimit > length || out.length - length < offset) {
        // Not even enough room for an ASCII-encoded string.
        throw new ArrayIndexOutOfBoundsException(
            "Failed writing " + in.charAt(inLimit - 1) + " at index " + (offset + length));
      }

      // Designed to take advantage of
      // https://wiki.openjdk.java.net/display/HotSpotInternals/RangeCheckElimination
      int inIx = 0;
      for (char c; inIx < inLimit && (c = in.charAt(inIx)) < 0x80; ++inIx) {
        UnsafeUtil.putByte(out, outIx++, (byte) c);
      }
      if (inIx == inLimit) {
        // We're done, it was ASCII encoded.
        return (int) outIx;
      }

      for (char c; inIx < inLimit; ++inIx) {
        c = in.charAt(inIx);
        if (c < 0x80 && outIx < outLimit) {
          UnsafeUtil.putByte(out, outIx++, (byte) c);
        } else if (c < 0x800 && outIx <= outLimit - 2L) { // 11 bits, two UTF-8 bytes
          UnsafeUtil.putByte(out, outIx++, (byte) ((0xF << 6) | (c >>> 6)));
          UnsafeUtil.putByte(out, outIx++, (byte) (0x80 | (0x3F & c)));
        } else if ((c < MIN_SURROGATE || MAX_SURROGATE < c) && outIx <= outLimit - 3L) {
          // Maximum single-char code point is 0xFFFF, 16 bits, three UTF-8 bytes
          UnsafeUtil.putByte(out, outIx++, (byte) ((0xF << 5) | (c >>> 12)));
          UnsafeUtil.putByte(out, outIx++, (byte) (0x80 | (0x3F & (c >>> 6))));
          UnsafeUtil.putByte(out, outIx++, (byte) (0x80 | (0x3F & c)));
        } else if (outIx <= outLimit - 4L) {
          // Minimum code point represented by a surrogate pair is 0x10000, 17 bits, four UTF-8
          // bytes
          final char low;
          if (inIx + 1 == inLimit || !isSurrogatePair(c, (low = in.charAt(++inIx)))) {
            throw new UnpairedSurrogateException((inIx - 1), inLimit);
          }
          int codePoint = toCodePoint(c, low);
          UnsafeUtil.putByte(out, outIx++, (byte) ((0xF << 4) | (codePoint >>> 18)));
          UnsafeUtil.putByte(out, outIx++, (byte) (0x80 | (0x3F & (codePoint >>> 12))));
          UnsafeUtil.putByte(out, outIx++, (byte) (0x80 | (0x3F & (codePoint >>> 6))));
          UnsafeUtil.putByte(out, outIx++, (byte) (0x80 | (0x3F & codePoint)));
        } else {
          if ((MIN_SURROGATE <= c && c <= MAX_SURROGATE)
              && (inIx + 1 == inLimit || !isSurrogatePair(c, in.charAt(inIx + 1)))) {
            // We are surrogates and we're not a surrogate pair.
            throw new UnpairedSurrogateException(inIx, inLimit);
          }
          // Not enough space in the output buffer.
          throw new ArrayIndexOutOfBoundsException("Failed writing " + c + " at index " + outIx);
        }
      }

      // All bytes have been encoded.
      return (int) outIx;
    }
//.........
}
```
Code khá dài nên anh em nào ngại đọc thì sẽ giải thích như sau. Bình thường kiểu Character sẽ mất 2 byte để lưu trữ cũng như serialize.
Nhưng trong code của google chúng ta thấy họ đã tối ưu khá nhiều khiến kiểu Character sẽ chỉ còn mất từ 1-3 byte nhưng trường hợp 3
byte thì chắc sẽ rất hiếm gặp. Từ đây kết luận protobuf họ làm khá tốt đối với kiểu dữ liệu **String**

Mình chỉ phân tích 2 kiểu dữ liệu đó thôi các kiểu còn lại mọi người chịu khó đọc tìm hiểu sẽ ra :)) chúc may mắn.

## Tự xây dựng phương pháp serialize, deserialize cho project
Vì protobuf không có kiểu dữ liệu Map một kiểu rất hay được dùng, tiếp đến việc có thêm object mới lại phải chỉnh sửa lại file proto
kiến công việc khá rắc rối và thêm dependency mới vào project mà không dùng tính năng **grpc** là những điểm mình không thích thư viện protobuf này.
Tất nhiên nếu dùng serialize, deserialize có sẵn của java thì sẽ khắc phục được những nhược điểm bên trên nhưng nó lại gây chậm cho hệ thống và
khó có thể dùng một ngôn ngữ khác dịch được các byte này thành object.

Tại project này mình sẽ kết hợp protobuf, Kryo và các project khác đã được trải nghiệm để xây dựng lên cách serialize riêng vẫn đảm bảo được 
tốc dộ cũng như vẫn có thể sử dụng một ngôn ngữ khác để Deserialize được.

Project sẽ được xây dựng theo khung sau: 
- Marshaller : là một anotation của java dùng để đánh dấu các class sẽ được serialize, deserialze, chứa một số thông tin để thực hiện quá trình này
- DInput : Một Interface chứa các phương thức dùng để **Deserialize** dữ liệu (Các class implement DByteArrayInput, DByteBufferInput)
- DOutput : Một Interface chứa các phương pháp dùng để **Serialize** dữ liệu (Các class implement DByteArrayOutput, DByteBufferOutput)
- DSerialize : Một Interface mà các class cần implement để  serialize, deserialize
- DMarshaller : Một Interface điều khiển quá trình serialize,deserialize

Trong project có viết sẵn các hàm giống như protobuf để ghi một số integer mất từ 1-5 byte. ghi một dữ liệu character mất từ 1-3 byte. Với cách tự serialize,
deserialize này chúng ta hoàn toàn có thể sử dụng một ngôn ngữ khác để serialize,deserialize dữ liệu một cách bình thường.

## Cách sử dụng
Tất cả các class muốn được serialize, deserialize cần phải implement interface DSerialize. Ví dụ trong project là `User,TestMapData,TestObject`

```java

import blog.serilize.base.DInput;
import blog.serilize.base.DMarshaller;
import blog.serilize.base.DOutput;
import blog.serilize.base.DSerialize;
import blog.serilize.base.anotation.Marshaller;

@Marshaller(name = User.class,number = 2)
public class User implements DSerialize<User> {
    private String name;
    private int age;


    public User(String name) {
        this.name = name;
    }

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public User() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output, User data) {
        output.writeString(data.getName());
        output.writeInt(data.getAge());
    }

    @Override
    public User read(DMarshaller marshaller, DInput input) {
        User user = new User();
        user.name = input.readString();
        user.age =input.readInt();
        return user;
    }

    @Override
    public Class<?> getClasses() {
        return User.class;
    }
}
```

Để sử dụng được thì các class này cần phải được đăng ký với `DMarshaller`. Ta có ví dụ về cách sử dụng như sau :
```java
public class Test {
  public static void main(String[] args) throws InstantiationException, IllegalAccessException {
          DMarshaller marshaller = new DMarshallerIml();
          marshaller.register(TestObject.class);
          marshaller.register(User.class);
          marshaller.register(TestMapData.class);
          marshaller.register(HashMapSerialize.class);
          marshaller.register(StringSerialize.class);
  
  
          User demtv = new User("demtv");
          Map<String ,User> map = new HashMap<>();
          map.put(demtv.getName(), demtv);
          TestMapData data = new TestMapData();
          data.setData(map);
    
          DOutput output = new DByteBufferOutput(100);
  
          marshaller.write(data, output);
          TestMapData y = marshaller.read(new DByteBufferInput(output.toArrayBytes()));
          System.out.println(y.getData().get("demtv").getName());
      }
}
```
