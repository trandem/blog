package blog.serilize.test;

//import com.esotericsoftware.kryo.kryo5.Kryo;
//import com.esotericsoftware.kryo.kryo5.io.ByteBufferInput;
//import com.esotericsoftware.kryo.kryo5.io.ByteBufferOutput;
//import com.esotericsoftware.kryo.kryo5.io.Input;
//import com.esotericsoftware.kryo.kryo5.io.Output;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CopyKyro {
    protected byte[] buffer;

    protected int position;

    public CopyKyro() {
        this.buffer = new byte[1000];
        position =0;
    }

    public void writeInt(int value) {

        byte[] buffer = this.buffer;
        int p = this.position;
        this.position = p + 4;
        buffer[p] = (byte)value;
        buffer[p + 1] = (byte)(value >> 8);
        buffer[p + 2] = (byte)(value >> 16);
        buffer[p + 3] = (byte)(value >> 24);
    }
    public int readInt() {
        byte[] buffer = this.buffer;
        int p = this.position;
        this.position = p + 4;
        return buffer[p] & 255 | (buffer[p + 1] & 255) << 8 | (buffer[p + 2] & 255) << 16 | (buffer[p + 3] & 255) << 24;
    }

    public int readIntLITTLE(byte[] arr) {
        int p =0;
        return arr[p] & 255 | (arr[p + 1] & 255) << 8 | (arr[p + 2] & 255) << 16 | (arr[p + 3] & 255) << 24;
    }

    public int readIntBig(byte[] arr) {
        int p =0;
        return ((arr[0] & 0xFF) << 24) | ((arr[1] & 0xFF) << 16) | ((arr[2] & 0xFF) << 8) | (arr[3] & 0xFF);
    }

    public char readChar(byte[] arr)  {
        return (char)(arr[0] & 255 | (arr[1] & 255) << 8);
    }


    public static void main(String[] args) throws IOException {
//        Test test = new Test();
////        test.writeInt(-10);
////        test.position =0;
////        System.out.println(test.readInt());
//        ByteBuffer x = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN);
////        x.putChar('c');
////        int y = test.readIntLITTLE(x.array());
////        System.out.println(y);
//        System.out.println(test.readChar    (x.array()));
//
//        Socket s=new Socket("localhost",9999);
//        DataInputStream din=new DataInputStream(s.getInputStream());
//        DataOutputStream dout=new DataOutputStream(s.getOutputStream());
//        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
//
//        dout.write(x.array());
//        dout.flush();
//        dout.close();
//        s.close();

//        Output output = new ByteBufferOutput(1024);
//        output.writeInt(-10,true);
//
//        Input input = new ByteBufferInput(output.toBytes());
//        System.out.println(input.readInt(true));

    }
}
