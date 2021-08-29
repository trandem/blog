package blog.serilize;

import blog.proto.Blog;
import blog.serilize.base.DMarshaller;
import blog.serilize.base.DOutput;
import blog.serilize.impl.DByteBufferOutput;
import blog.serilize.impl.DMarshallerIml;
import blog.serilize.test.User;

public class Test {
    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
        Blog.SimpleObject data = Blog.SimpleObject.newBuilder()
                .setCount(10)
                .setName("ânhdem976")
                .build();

        System.out.println(data.toByteArray().length);

        DMarshaller marshaller = new DMarshallerIml();
        marshaller.register(User.class);

        User demtv = new User("ânhdem976",10);
        DOutput output = new DByteBufferOutput(10);
        marshaller.write(demtv, output);

        System.out.println(output.toArrayBytes().length);
    }
}
