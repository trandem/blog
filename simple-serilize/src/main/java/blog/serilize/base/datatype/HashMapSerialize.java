package blog.serilize.base.datatype;

import blog.serilize.base.DInput;
import blog.serilize.base.DMarshaller;
import blog.serilize.base.DOutput;
import blog.serilize.base.DSerialize;
import blog.serilize.base.anotation.Marshaller;

import java.util.HashMap;
import java.util.Map;

import static blog.serilize.impl.DMarshallerIml.cast;

@Marshaller(name = HashMap.class,number = 4)
public class HashMapSerialize<K, V> implements DSerialize<Map<K, V>> {


    @Override
    public void write(DMarshaller marshaller, DOutput output, Map<K, V> x) {
        int size = x.size();
        output.writeIntOptimise(size);
        for (Map.Entry<K, V> data : x.entrySet()) {
            marshaller.write(data.getKey(), output);
            marshaller.write(data.getValue(), output);
        }
    }

    @Override
    public Map<K, V> read(DMarshaller marshaller, DInput input) {
        int size = input.readIntPositiveOptimise();
        Map<K, V> output = new HashMap<>();
        for (int i = 0; i < size; i++) {
            output.put(marshaller.read(cast(input)), marshaller.read(cast(input)));
        }
        return output;
    }

    @Override
    public Class<?> getClasses() {
        return HashMap.class;
    }
}
