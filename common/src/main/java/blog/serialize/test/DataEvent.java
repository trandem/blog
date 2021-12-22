package blog.serialize.test;

import blog.serialize.Event;
import blog.serialize.base.DInput;
import blog.serialize.base.DInstance;
import blog.serialize.base.DMarshaller;
import blog.serialize.base.DOutput;

public class DataEvent implements Event {

    private Object someTestData;
    private long id;

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public Type getType() {
        return Type.DATA;
    }

    @Override
    public String toString() {
        return "DataEvent{" +
                "someTestData=" + someTestData +
                ", id=" + id +
                '}';
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output) {
        marshaller.write(someTestData, output);
        output.writeLong(id);
    }

    @Override
    public void read(DMarshaller marshaller, DInput input) {
        this.someTestData = marshaller.read(input);
        this.id = input.readLong();
    }

    public Object getSomeTestData() {
        return someTestData;
    }

    public void setSomeTestData(Object someTestData) {
        this.someTestData = someTestData;
    }

    public static class DataEventInstance implements DInstance<DataEvent> {

        @Override
        public DataEvent instance() {
            return new DataEvent();
        }
    }

}
