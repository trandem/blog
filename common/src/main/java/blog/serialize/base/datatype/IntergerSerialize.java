package blog.serialize.base.datatype;

import blog.serialize.base.DInput;
import blog.serialize.base.DMarshaller;
import blog.serialize.base.DOutput;
import blog.serialize.base.DSerialize;

public class IntergerSerialize implements DSerialize<Integer> {
    @Override
    public void write(DMarshaller marshaller, DOutput output, Integer data) {
        output.writeInt(data);
    }

    @Override
    public Integer read(DMarshaller marshaller, DInput input) {
        return input.readInt();
    }

    @Override
    public Class<?> getClasses() {
        return Integer.class;
    }
}
