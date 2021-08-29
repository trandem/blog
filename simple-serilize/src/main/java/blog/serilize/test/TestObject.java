package blog.serilize.test;

import blog.serilize.base.DInput;
import blog.serilize.base.DMarshaller;
import blog.serilize.base.DOutput;
import blog.serilize.base.DSerialize;
import blog.serilize.base.anotation.Marshaller;
import blog.serilize.base.datatype.HashMapSerialize;
import blog.serilize.base.datatype.StringSerialize;
import blog.serilize.impl.DByteBufferInput;
import blog.serilize.impl.DByteBufferOutput;
import blog.serilize.impl.DMarshallerIml;

import java.util.HashMap;
import java.util.Map;

@Marshaller(name = "test.TestObject",number = 1)
public class TestObject implements DSerialize<TestObject> {
    private String userName;
    private int age;
    private User someOne;

    public TestObject(String userName, int age, User someOne) {
        this.userName = userName;
        this.age = age;
        this.someOne = someOne;
    }

    public TestObject() {
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public User getSomeOne() {
        return someOne;
    }

    public void setSomeOne(User someOne) {
        this.someOne = someOne;
    }

    public void write(DMarshaller marshaller, DOutput output, TestObject data) {
        output.writeString(data.getUserName());
        output.writeInt(data.getAge());
        marshaller.write(data.getSomeOne(), output);
    }

    public TestObject read(DMarshaller marshaller, DInput input) {
        TestObject object = new TestObject();
        object.setUserName(input.readString());
        object.setAge(input.readInt());
        object.setSomeOne(marshaller.read(input));
        return object;
    }

    @Override
    public Class<?> getClasses() {
        return TestObject.class;
    }


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

//        TestObject object = new TestObject("phuongmai", 23, demtv);

        DOutput output = new DByteBufferOutput(1000);

        marshaller.write(data, output);
        TestMapData y = marshaller.read(new DByteBufferInput(output.toArrayBytes()));
        System.out.println(y.getData().get("demtv").getName());






//        Kryo kryo = new Kryo();
//        kryo.register(User.class);
//        Output output = new ByteBufferOutput(1000);
//        User user = new User("demtv");
//        kryo.writeObject(output,user);
//
//        Input input = new ByteBufferInput(output.toBytes());
//        User demtv = kryo.readObject(input,User.class);
//        System.out.println(demtv.getName());
    }
}
