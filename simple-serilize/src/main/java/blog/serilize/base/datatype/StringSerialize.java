package blog.serilize.base.datatype;

import blog.serilize.base.DInput;
import blog.serilize.base.DMarshaller;
import blog.serilize.base.DOutput;
import blog.serilize.base.DSerialize;
import blog.serilize.base.anotation.Marshaller;

import java.util.HashMap;
import java.util.Map;

import static blog.serilize.impl.DMarshallerIml.cast;

@Marshaller(name = "test.TestObject",number = 5)
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
