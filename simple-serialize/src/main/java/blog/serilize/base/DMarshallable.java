package blog.serilize.base;

public interface DMarshallable {

    void write(DMarshaller marshaller, DOutput output);

    void read(DMarshaller marshaller, DInput input);

}
