package blog.core.storage;

import blog.serialize.base.*;

public class StartEvent implements Event, DMarshallable {

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public Type getType() {
        return Type.START;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output) {
    }

    @Override
    public void read(DMarshaller marshaller, DInput input) {
    }

    public static class StartEventInstance implements DInstance<StartEvent>{

        @Override
        public StartEvent instance() {
            return new StartEvent();
        }
    }
}
