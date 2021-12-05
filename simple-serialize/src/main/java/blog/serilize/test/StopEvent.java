package blog.serilize.test;

import blog.serilize.base.DInput;
import blog.serilize.base.DMarshaller;
import blog.serilize.base.DOutput;
import blog.serilize.base.DSerialize;

public class StopEvent implements DSerialize<StopEvent> {
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output, StopEvent data) {
        output.writeLong(data.getId());
    }

    @Override
    public StopEvent read(DMarshaller marshaller, DInput input) {
        StopEvent event = new StopEvent();
        event.setId(input.readLong());
        return event;
    }

    @Override
    public Class<?> getClasses() {
        return StopEvent.class;
    }
}
