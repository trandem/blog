package blog.serilize;

import blog.proto.Blog;
import blog.serilize.base.DInput;
import blog.serilize.base.DOutput;
import blog.serilize.impl.DByteArrayInput;
import blog.serilize.impl.DByteArrayOutput;
import com.google.protobuf.InvalidProtocolBufferException;

public class Test {
    public static void main(String[] args) throws InvalidProtocolBufferException {
//        Blog.SimpleObject data = Blog.SimpleObject.newBuilder()
//                .setCount(-10)
//                .setName("ânhdem976")
//                .build();
//
//
//        Blog.SimpleObject x=  Blog.SimpleObject.parseFrom(data.toByteArray());

        DOutput output = new DByteArrayOutput(1024);
        output.writeIntOptimise(1);
        output.writeString("ấ");

        DInput input = new DByteArrayInput(output.toArrayBytes());
        System.out.println(input.readIntPositiveOptimise());
        System.out.println(input.readString());
    }
}
