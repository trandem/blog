package blog.model;

import blog.serilize.base.*;

public class UserModel implements DMarshallable {
    private String name;
    private int age;

    public UserModel() {
    }

    public UserModel(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output) {
        output.writeString(name);
        output.writeIntOptimise(age);
    }

    @Override
    public void read(DMarshaller marshaller, DInput input) {
        this.name = input.readString();
        this.age = input.readIntPositiveOptimise();
    }

    @Override
    public String toString() {
        return "UserModel{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }

    public static class UserModelInstanceIml implements DInstance<UserModel> {

        @Override
        public UserModel instance() {
            return new UserModel();
        }

    }

}
