package blog.common.cluster;

import blog.common.Utils;
import blog.common.cluster.glosory.MethodListener;
import blog.common.cluster.glosory.RemoteEvent;
import blog.common.cluster.glosory.ServiceBus;
import blog.common.cluster.glosory.ServiceCluster;
import blog.common.messenger.DMessenger;
import blog.common.messenger.KafkaMessenger;
import blog.common.messenger.TransportTopic;
import blog.serialize.impl.DMarshallers;
import blog.serialize.base.DMarshaller;
import blog.serialize.impl.AllMarshaller;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

public class ServiceClusterImpl implements ServiceCluster {

    private final DMessenger<Object> messenger = KafkaMessenger.getInstance();
    private final DMarshaller marshaller = AllMarshaller.DEFAULT;

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final TransportTopic transportTopic;

    private ServiceBus serviceBus;


    public ServiceClusterImpl() {
        this.transportTopic = messenger.registerTopic("cluster");
    }

    public void start() {
        messenger.addListener(transportTopic, this);
        messenger.setMarshaller(transportTopic, marshaller);
        messenger.setExecutor(transportTopic, Executors.newSingleThreadExecutor());

        serviceBus.registerListener(this);

        send("CLUSTER", "JOIN_REQUEST", "stes");
    }

    @MethodListener(topic = "CLUSTER", type = "JOIN_REQUEST")
    public void joinRequest(String t) {
        System.out.println("join request " + t);
    }

    @MethodListener(topic = "CLUSTER", type = "JOIN_RESPONSE")
    public void joinResponse() {

    }

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void send(String topic, String type, Object... args) {
        RemoteEvent event = new RemoteEvent(topic, type, serviceBus.getId(), DMarshallers.marshaller(args, marshaller));
        messenger.send(event, transportTopic);
    }

    public ServiceBus getServiceBus() {
        return serviceBus;
    }

    public void setServiceBus(ServiceBus serviceBus) {
        this.serviceBus = serviceBus;
    }

    @Override
    public void onMessage(Object data) {
        if (data instanceof RemoteEvent) {
            for (Listener listener : listeners) {
                listener.onEvent(Utils.cast(data));
            }
        }
    }
}
