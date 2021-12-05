package blog.serilize.test;


import blog.serilize.base.DInput;
import blog.serilize.base.DMarshaller;
import blog.serilize.base.DOutput;
import blog.serilize.base.DSerialize;

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
