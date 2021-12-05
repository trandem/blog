package blog.serilize.base;

public class DObjectSerialize<T extends DMarshallable> implements DSerialize<T> {

    private DInstance<T> instance;

    private Class<?> aClass;

    public DObjectSerialize(DInstance<T> instance, Class<?> aClass) {
        this.instance = instance;
        this.aClass = aClass;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output, T data) {
        data.write(marshaller, output);
    }

    @Override
    public T read(DMarshaller marshaller, DInput input) {
        T output = instance.instance();
        output.read(marshaller, input);
        return output;
    }

    @Override
    public Class<?> getClasses() {
        return aClass;
    }
}
