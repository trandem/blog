package blog.serilize.base;

public interface DMarshaller {

    void write(Object data, DOutput output);

    <T> T read(DInput input);

    <T extends DMarshallable> void register(Class<T> c, DInstance<T> ins);

    <T extends DMarshallable> void register(DInstance<T> ins);

    void register(DSerialize<?> serialize);
}
