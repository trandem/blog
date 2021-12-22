package blog.common.messenger;

import blog.serialize.base.DMarshaller;

import java.util.concurrent.Future;

public interface DMessengerPublisher<T> {

    void setMarshaller(TransportTopic topic, DMarshaller dMarshaller);

    Future<Void> send(T message,TransportTopic topic);

}
