package blog.common.messenger;

import blog.serialize.base.DMarshaller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public abstract class AbstractMessenger<T> implements DMessenger<T> {

    protected Map<String, TransportTopic> transportTopicMap = new ConcurrentHashMap<>();
    protected Map<TransportTopic, DMarshaller> marshallerMap = new ConcurrentHashMap<>();
    protected Map<TransportTopic, ExecutorService> executorServiceMap = new ConcurrentHashMap<>();
    protected Map<TransportTopic, List<DMessengerListener>> listenerMap = new ConcurrentHashMap<>();


    @Override
    public TransportTopic registerTopic(String topic) {
        if (!transportTopicMap.containsKey(topic)){
            transportTopicMap.put(topic,new DTransportTopic(topic));
        }
        return transportTopicMap.get(topic);
    }

    @Override
    public void setMarshaller(TransportTopic topic, DMarshaller dMarshaller) {
        marshallerMap.putIfAbsent(topic, dMarshaller);
    }

    @Override
    public void setExecutor(TransportTopic topic, ExecutorService executor) {
        executorServiceMap.putIfAbsent(topic, executor);
    }

    @Override
    public void addListener(TransportTopic topic, DMessengerListener listener) {
        if (!listenerMap.containsKey(topic)) {
            listenerMap.put(topic, new ArrayList<>());
        }
        listenerMap.get(topic).add(listener);
    }
}
