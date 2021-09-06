package blog.serilize.base.datatype;

import blog.serilize.base.DInput;
import blog.serilize.base.DMarshaller;
import blog.serilize.base.DOutput;
import blog.serilize.base.DSerialize;
import blog.serilize.base.anotation.Marshaller;

@Marshaller(name = String.class,number = 5)
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
