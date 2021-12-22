package blog.common.cluster;

import blog.common.cluster.glosory.*;
import blog.serialize.impl.DMarshallers;
import blog.serialize.base.DMarshaller;
import blog.serialize.impl.AllMarshaller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * store and notify all service
 */

public class ServiceBusImpl implements ServiceBus, ServiceCluster.Listener {
    private final Listener.Factory listenerFactory = new ListenerImpl.Factory();

    private final Map<String, Map<String, List<Listener>>> registerListenerMap = new ConcurrentHashMap<>();

    private final DMarshaller marshaller = AllMarshaller.DEFAULT;
    private final ExecutorService service = Executors.newFixedThreadPool(3);

    private final int id = Integer.parseInt(System.getProperty("APP_ID"));

    private final ServiceClusterImpl serviceCluster = new ServiceClusterImpl();


    public void start() {
        serviceCluster.addListener(this);
        serviceCluster.setServiceBus(this);
        serviceCluster.start();
    }

    @Override
    public int getId() {
        return id;
    }

    public void registerListener(Object target) {
        for (Listener listener : listenerFactory.create(target)) {
            if (!registerListenerMap.containsKey(listener.getTopic())) {
                registerListenerMap.put(listener.getTopic(), new HashMap<>());
            }
            Map<String, List<Listener>> listenerMap = registerListenerMap.get(listener.getTopic());
            if (!listenerMap.containsKey(listener.getType())) {
                listenerMap.put(listener.getType(), new ArrayList<>());
            }
            listenerMap.get(listener.getType()).add(listener);
        }
    }

    @Override
    public void unRegisterListener(Object target) {
        for (Listener listener : listenerFactory.create(target)) {
            registerListenerMap.remove(listener);
        }
    }

    @Override
    public void notify(Event event, Object... args) {
        System.out.println("bus notify");
        service.submit(() -> {
            try {
                serviceCluster.send(event.getTopic(), event.getType(), args);
            }catch (Throwable e){
                e.printStackTrace();
            }

        });
    }

    @Override
    public void onEvent(RemoteEvent event) {
        System.out.println(" receive event " + event);
        if (event.getBusId() != getId()) service.submit(() -> {
            Map<String, List<Listener>> listenerMap = registerListenerMap.get(event.getTopic());
            if (listenerMap == null || listenerMap.isEmpty()) return;

            List<Listener> listeners = listenerMap.get(event.getType());
            if (listeners == null || listeners.isEmpty()) return;

            for (Listener listener : listeners) {
                try {
                    Object[] x = DMarshallers.unMarshaller(marshaller, event.getArgs());
                    listener.invoke(x);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) {
        ServiceBusImpl serviceBus = new ServiceBusImpl();
        serviceBus.start();
    }
}
