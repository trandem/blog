package blog.common.messenger;

import blog.serialize.base.DMarshaller;

import java.util.concurrent.ExecutorService;

public interface DMessengerSubscriber<T> {

    void setMarshaller(TransportTopic topic, DMarshaller dMarshaller);

    void setExecutor(TransportTopic topic,ExecutorService executor);

    void addListener(TransportTopic topic, DMessengerListener listener);

}
