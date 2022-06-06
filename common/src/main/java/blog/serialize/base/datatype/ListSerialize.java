package blog.serialize.base.datatype;

import blog.serialize.base.DInput;
import blog.serialize.base.DMarshaller;
import blog.serialize.base.DOutput;
import blog.serialize.base.DSerialize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListSerialize<T> implements DSerialize<List<T>> {
    @Override
    public void write(DMarshaller marshaller, DOutput output, List<T> data) {
        int size = data.size();
        output.writeIntOptimise(size);
        for (T ele : data) {
            marshaller.write(ele, output);
        }
    }

    @Override
    public List<T> read(DMarshaller marshaller, DInput input) {
        List<T> rs = new ArrayList<>();
        int size = input.readIntPositiveOptimise();
        for (int i = 0; i < size; i++) {
            rs.add(marshaller.read(input));
        }
        return rs;
    }

    @Override
    public Class<?> getClasses() {
        return ArrayList.class;
    }
}
