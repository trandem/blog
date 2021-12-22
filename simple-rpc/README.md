# Simple RPC implement in java
Gần đây mọi người hay so sánh REST với RPC nên chọn công nghệ 
nào để truyền tải dữ liệu qua các server. Với REST chúng ta có
thể tìm thấy rất nhiều bài hướng dẫn trên internet. Các bài về tìm hiểu
cơ chế chạy, cách truyền dữ liệu thông qua body,... Những bài viết đó khiến
chúng ta quá quen thuộc với REST và nó không còn là hộp đen nữa. Trái ngược với
điều trên thì RPC lại không nhiều bài viết hướng dẫn mọi người implement, mọi 
người thường tìm thấy cách sử dụng của một số framework như gRPC, thrift,... và các
bài so sánh hiệu năng viết RPC nhanh hơn REST và thích hợp với truyền tải thông tin 
liên server hơn REST. Tại đây tôi có 1 implement nhỏ đơn giản về RPC hy vọng thông 
qua bài này mọi người sẽ không còn cảm thấy lạ với loại hình này và có thể giải thích
được tại sao nó lại thích hợp giữa các server.

Bài viết sẽ có các phần sau:
- [Transport](#transport)
- [Serialize data](#serialize-data)
- [Simple rpc server](#simple-rpc-server)
- [Simple rpc client]()

## Transport
Cũng như các hệ thống **client-server** khác để giao tiếp giữa **client** và **server** chúng ta
cần một **network protocol**. Các framework về **RPC** nổi tiếng họ dùng tầng transport sau:
- gRPC sử dụng http2 truyền tải dũ liệu dữa **client - server**. Với Java thì họ dùng **netty** để
dựng server http2. Vì Netty là một **Non Blocking IO Framework**
- thrift sử dụng http1.1 truyền tải dữ liệu giữa **client-server**

Trong lúc đi làm tôi thường thấy mọi người hay so sánh giữa **http** và **rpc**. Hai khái niệm này
không thể so sánh với nhau được vì nó thuộc 2 phạm trù khác nhau. Hy vọng sau bài viết này các bạn sẽ
tự mình phân biệt được.

Trong bài demo này tôi cũng sẽ sử dụng **Netty** để xây dựng tầng **transport** dữ liệu giữa **client server**
cụ thể sẽ dùng **Non Blocking Socket** của **Netty**

Bạn dễ dàng tìm được code hướng dẫn với Netty trên trang chủ. Với server ta sẽ mở 1 socket :
```java
public class NettyTransport {
//..........
    public void server() throws Exception {
        this.group = new NioEventLoopGroup();
        bootstrap = new ServerBootstrap();
        bootstrap.group(group)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY,true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer() {
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(new RpcHandle(listener));
                    }
                });
        ChannelFuture f = bootstrap.bind(port).sync(); // (5)
    }
}
```
Với client ta sẽ kết nối với socket :

```java
    public void connectNetty() {
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap(); // (1)
            b.group(workerGroup); // (2)
            b.channel(NioSocketChannel.class); // (3)
            b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
            b.option(ChannelOption.TCP_NODELAY,true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(transport = new ClientHandle(SimpleRpcClient.this));
                }
            });
            // Start the client.
            ChannelFuture f = b.connect(host, port).sync(); // (5)
            // Wait until the connection is closed.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```

## Serialize data
- Với REST dữ liệu truyền tải giữa client server sẽ nằm trong body của request và được chuyển
sang dưới dạng **json**. Với dạng **Json** sẽ dễ dàng đọc hiểu với mọi người nhưng với máy tính
thì việc đó không dễ dàng. Nó kiến việc Deserialize và Serialize dữ liệu chậm hơn.
- Với RPC dữ liệu truyền tải giữa client và server thì thường sẽ được **serialize** thành array byte việc này
làm tiết kiệm được tài nguyên thực hiện **serialize** và **deserialize**
- Hẹn các bạn ở bài viết sau sẽ giải thích lý do vì sao dạng **byte array** lại nhanh hơn so với json. 
Các bạn có thể tham khảo một bài viết trên [medium](https://medium.com/m/global-identity?redirectUrl=https%3A%2F%2Fbetterprogramming.pub%2Fuse-binary-encoding-instead-of-json-dec745ec09b6)
- Với gRPC họ sử dụng **protobuf** để serialize dữ liệu thành byte array  và ngược lại. Apache thrift cũng sẽ có một bộ serialize và deserialize
riêng để biến dữ liệu truyền tải giữa server và client
- Tại bài viết này tôi sẽ dùng thư viện [kryo](https://github.com/EsotericSoftware/kryo) một thư viện mạnh mẽ với
java về serialize và deserialize dữ liệu. Việc tự xây cho mình một cách serialize dữ liệu cũng hoàn toàn có thể làm được nhưng tôi sẽ để một bài viết khác.

Qua github của **Kryo** ta sử dụng thư viện này đơn giản nhất như sau :
```java
import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.ByteBufferInput;
import com.esotericsoftware.kryo.kryo5.io.ByteBufferOutput;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;

public class RPCSerialize {
    public static RPCSerialize serialize = new RPCSerialize();

    private final Kryo kryo;

    public RPCSerialize() {
        this.kryo = new Kryo();
        kryo.register(RPCRequest.class);
        kryo.register(RPCResponse.class);
        kryo.register(Object[].class);
        kryo.register(User.class);
    }

    public <T> T deSerialize(byte[] arr, Class<T> t) {
        Input input = new ByteBufferInput(arr);
        return this.kryo.readObject(input, t);
    }

    public <T> byte[] serialize(T t) {
        Output output = new ByteBufferOutput(1024);
        kryo.writeObject(output, t);
        return output.toBytes();
    }
}
```
Ta chỉ cần đăng ký các object cần giải serialize và deserialize với **Kryo** sau đó dựa vào **reflection** thư viện có thể serialize, deserialize dữ liệu.

## Simple RPC server
Tại bài này chúng ta sẽ implement một service đơn giản sau :
```java
import blog.rpc.support.User;

public interface SimpleService {

    int add(int a, int b);

    User getUser();

}
```
```java
import blog.rpc.service.SimpleService;
import blog.rpc.support.User;

public class ServerImpl implements SimpleService {
    @Override
    public int add(int a, int b) {
        return a+b;
    }

    @Override
    public User getUser() {
        return new User("demtv");
    }
}
```
Một **RPC server** cần phải lưu được lại các **method** của các service của mình để khi nhận được request chúng sẽ thực hiện gọi các method 
này theo cơ chế RPC. Tôi sẽ sử dụng một **Map** để lưu trữ thông tin này và sử dụng **java reflection** để gọi method.
```java
import blog.rpc.listener.RpcListener;
import blog.rpc.support.*;
import blog.rpc.tranport.imp.NettyTransport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class SimpleRpcServer implements RpcListener<ByteBuf> {

    private Map<Integer, XMethod> methodService;

    private RPCSerialize serialize = RPCSerialize.serialize;

    public SimpleRpcServer(int port) {
        new NettyTransport(this, port);
        this.methodService = new HashMap<>();
    }

    public void addHandle(Object target) {
        Method[] methods = target.getClass().getDeclaredMethods();
        for (Method method : methods) {
            XMethod xMethod = new XMethod(method, target);
            methodService.put(xMethod.getSignature(), xMethod);
        }
    }

    public XFuture<ByteBuf> onMessage(ByteBuf buf) {
        byte[] bytes;
        int length = buf.readableBytes();
        if (buf.hasArray()) {
            bytes = buf.array();
        } else {
            bytes = new byte[length];
            buf.getBytes(buf.readerIndex(), bytes);
        }

        RPCRequest request = serialize.deSerialize(bytes, RPCRequest.class);
        try {
            Object result = this.methodService.get(request.getMethod()).invoke(request.getArgs());
            RPCResponse response = new RPCResponse(request.getId(), result);
            XFuture<ByteBuf> future = new XFuture<>();
            byte[]x = serialize.serialize(response);
            future.setResult(Unpooled.copiedBuffer(x));
            return future;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
```
Trong code sẽ sử dụng một số Class support sau:
- **IdGenerator** chịu trách nhiệm tạo id cho request và response. Vì netty là **Non Blocking IO** nên cần thiết có **id** để đánh dấu **response** này 
được phản hồi cho **request** nào.
- **RPCRequest** là object sẽ được truyền từ **client** lên **server**
- **RPCResponse** là object sẽ được truyên từ **server** lên **client**
- **XFuture** một custom của class Future nhằm đợi kết quả được gửi về server.
- **XMethod** tạo ra signature để đánh dấu method và thực hiện invoke method.
## Simple RPC client
Sau khi implement server, chúng ta thực hiện implement **client**. **Client** khi **implement** cần gửi lên server đúng method và param thực hiện method.
Ta **Override** lại các method `interface SimpleService` đơn giản như sau để gửi lên server và nhận lại kết quả.
```
    @Override
    public int add(int a, int b) {
        int rs = Integer.MAX_VALUE;
        try {
            RPCRequest request = new RPCRequest();
            request.setId(idGenerator.nextId());
            Method method = this.getClass().getMethod("add", int.class, int.class);
            request.setMethod(XMethod.signature(method));
            request.setArgs(new Object[]{a, b});
            byte[] x = serialize.serialize(request);
            transport.sendMsg(Unpooled.copiedBuffer(x));
            XFuture<Integer> future = new XFuture<>();
            futures.put(request.getId(), future);
            rs = future.get(100, TimeUnit.SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return rs;
    }

    @Override
    public User getUser() {
        try {
            RPCRequest request = new RPCRequest();
            request.setId(idGenerator.nextId());
            Method method = this.getClass().getMethod("getUser");
            request.setMethod(XMethod.signature(method));
            request.setArgs(null);
            byte[] x = serialize.serialize(request);
            transport.sendMsg(Unpooled.copiedBuffer(x));
            XFuture<User> future = new XFuture<>();
            futures.put(request.getId(), future);
            return future.get(100, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

```


## Kết luận
- **RPC** là một dạng giao tiếp giữa **client** và **server** thông tin trao đổi gồm có: **method**,**param** của **method** và kết quả. Nó thường được sử dụng hơn so với **REST**
để truyền tải dữ liệu giữa **client** và **server** vì có thể dùng được nhiều loại **transport** khác nhau. Sử dụng **Serialize** dữ liệu hiệu quả hơn so với **REST**
- **RPC server** cần phải có cơ chế lưu lại cái **method** và gọi đúng **method** khi nhận được **request**.
- **RPC client** cần phải gửi lên **method** và **param** và đợi kết quả trả về từ **server**

Hy vọng mọi người cũng có thể hiểu hơn RPC là gì nhưng khi sử dụng thì mọi người nên sử dụng **framework** cho nó nhàn :)).

Nếu bài viết này có ích thì xin mọi người ủng hộ tôi 1 sao trên github để sớm có các bài khác.