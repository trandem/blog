package blog.core.storage;

import blog.serialize.base.*;

public class StopEvent implements Event, DMarshallable {
    @Override
    public long getId() {
        return -1;
    }

    @Override
    public Type getType() {
        return Type.STOP;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output) {

    }

    @Override
    public void read(DMarshaller marshaller, DInput input) {

    }

    public static class StopEventInstance implements DInstance<StopEvent>{

        @Override
        public StopEvent instance() {
            return new StopEvent();
        }
    }

}
