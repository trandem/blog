package blog.serialize.test;

import blog.serialize.Event;
import blog.serialize.base.*;

public class StartEvent implements Event {
    private long id =4L;

    public long getId() {
        return id;
    }

    @Override
    public Type getType() {
        return Type.START;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output) {
        output.writeLong(id);
    }

    @Override
    public void read(DMarshaller marshaller, DInput input) {
        this.id = input.readLong();
    }


    @Override
    public String toString() {
        return "StartEvent{" +
                "id=" + id +
                '}';
    }

    public static class StartEventInstance implements DInstance<StartEvent>{

        @Override
        public StartEvent instance() {
            return new StartEvent();
        }
    }

}
