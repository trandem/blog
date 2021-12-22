package blog.serialize.test;

import blog.serialize.Event;
import blog.serialize.base.*;

public class StopEvent implements Event {
    private long id = 5;

    public long getId() {
        return id;
    }

    @Override
    public Type getType() {
        return Type.STOP;
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
        return "StopEvent{" +
                "id=" + id +
                '}';
    }

    public static class StopEventInstance implements DInstance<StopEvent> {

        @Override
        public StopEvent instance() {
            return new StopEvent();
        }
    }
}
