package blog.rpc.support;

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
