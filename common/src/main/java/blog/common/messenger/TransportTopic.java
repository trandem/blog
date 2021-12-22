package blog.common.messenger;

import blog.serialize.base.DMarshallable;

public interface TransportTopic extends DMarshallable {
    String getName();
}
