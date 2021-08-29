package blog.serilize.test;


import blog.serilize.base.DInput;
import blog.serilize.base.DMarshaller;
import blog.serilize.base.DOutput;
import blog.serilize.base.DSerialize;
import blog.serilize.base.anotation.Marshaller;

@Marshaller(name = "test.User",number = 2)
public class User implements DSerialize<User> {
    private String name;


    public User(String name) {
        this.name = name;
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
    }

    @Override
    public User read(DMarshaller marshaller, DInput input) {
        User user = new User();
        user.name = input.readString();
        return user;
    }

    @Override
    public Class<?> getClasses() {
        return User.class;
    }
}
