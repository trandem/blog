package blog.serialize;

import blog.serialize.base.DMarshallable;

public interface Event extends DMarshallable {

    long getId();

    Type getType();



    enum Type {
        START, STOP, DATA
    }
}
