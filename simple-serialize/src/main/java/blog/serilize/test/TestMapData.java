package blog.serilize.test;

import blog.serilize.base.DInput;
import blog.serilize.base.DMarshaller;
import blog.serilize.base.DOutput;
import blog.serilize.base.DSerialize;

import java.util.Map;

public class TestMapData implements DSerialize<TestMapData> {
    private Map<String, User> data;


    public Map<String, User> getData() {
        return data;
    }

    public void setData(Map<String, User> data) {
        this.data = data;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output, TestMapData data) {
        marshaller.write(data.getData(), output);
    }

    @Override
    public TestMapData read(DMarshaller marshaller, DInput input) {
        TestMapData data = new TestMapData();
        data.setData(marshaller.read(input));
        return data;
    }

    @Override
    public Class<?> getClasses() {
        return TestMapData.class;
    }
}
