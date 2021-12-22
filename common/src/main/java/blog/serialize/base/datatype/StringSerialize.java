package blog.serialize.base.datatype;

import blog.serialize.base.DInput;
import blog.serialize.base.DMarshaller;
import blog.serialize.base.DOutput;
import blog.serialize.base.DSerialize;

public class StringSerialize implements DSerialize<String> {

    @Override
    public void write(DMarshaller marshaller, DOutput output, String data) {
        output.writeString(data);
    }

    @Override
    public String read(DMarshaller marshaller, DInput input) {
        return input.readString();
    }

    @Override
    public Class<?> getClasses() {
        return String.class;
    }
}
