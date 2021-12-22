package blog.common.messenger;

public interface DMessenger<T> extends DMessengerPublisher<T>, DMessengerSubscriber<T> {
    TransportTopic registerTopic(String topic);
}
