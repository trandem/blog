package blog.serilize.test;

import blog.serilize.base.DInput;
import blog.serilize.base.DMarshaller;
import blog.serilize.base.DOutput;
import blog.serilize.base.DSerialize;

public class StartEvent implements DSerialize<StartEvent> {
    private long id =4;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output, StartEvent data) {
        output.writeLong(data.getId());
    }

    @Override
    public StartEvent read(DMarshaller marshaller, DInput input) {
        StartEvent event = new StartEvent();
        event.setId(input.readLong());
        return event;
    }

    @Override
    public Class<?> getClasses() {
        return StartEvent.class;
    }
}
