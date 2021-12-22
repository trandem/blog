package blog.serialize.base.datatype;

import blog.serialize.base.DInput;
import blog.serialize.base.DMarshaller;
import blog.serialize.base.DOutput;
import blog.serialize.base.DSerialize;

import java.util.HashMap;
import java.util.Map;

import static blog.common.Utils.cast;

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
