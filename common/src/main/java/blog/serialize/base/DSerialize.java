package blog.serialize.base;

public interface DSerialize<T> {

    void write(DMarshaller marshaller, DOutput output, T data);

    T read(DMarshaller marshaller, DInput input);

    Class<?> getClasses();
}
