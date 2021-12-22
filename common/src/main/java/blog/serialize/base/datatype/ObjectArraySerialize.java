package blog.serialize.base.datatype;

import blog.serialize.base.DInput;
import blog.serialize.base.DMarshaller;
import blog.serialize.base.DOutput;
import blog.serialize.base.DSerialize;

public class ObjectArraySerialize implements DSerialize<Object[]> {
    @Override
    public void write(DMarshaller marshaller, DOutput output, Object[] data) {
        output.writeIntOptimise(data.length);
        for (Object object : data) {
            marshaller.write(object, output);
        }
    }

    @Override
    public Object[] read(DMarshaller marshaller, DInput input) {
        int size = input.readIntPositiveOptimise();
        Object[] x = new Object[size];
        for (int i = 0; i < size; i++) {
            x[i] = marshaller.read(input);
        }
        return x;
    }

    @Override
    public Class<?> getClasses() {
        return Object[].class;
    }
}
