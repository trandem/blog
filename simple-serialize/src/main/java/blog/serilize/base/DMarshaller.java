package blog.serilize.base;

public interface DMarshaller {

    void register(DSerialize<?> x);

    void register(Class<?> x) throws IllegalAccessException, InstantiationException;

    void write(Object x,DOutput output);

    <T> T read(DInput input);
}
