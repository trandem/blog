package blog.serilize.base;

public class MagicSerialize<T> implements DSerialize<T> {

    private int magic;
    private DSerialize<T> serialize;


    public MagicSerialize(int magic, DSerialize<T> serialize) {
        this.magic = magic;
        this.serialize = serialize;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output, T data) {
        this.serialize.write(marshaller, output, data);
    }

    @Override
    public T read(DMarshaller marshaller, DInput input) {
        return this.serialize.read(marshaller, input);
    }

    @Override
    public Class<?> getClasses() {
        return this.serialize.getClasses();
    }

    public int getMagic() {
        return magic;
    }

    public void setMagic(int magic) {
        this.magic = magic;
    }

    public DSerialize<T> getSerialize() {
        return serialize;
    }

    public void setSerialize(DSerialize<T> serialize) {
        this.serialize = serialize;
    }
}
