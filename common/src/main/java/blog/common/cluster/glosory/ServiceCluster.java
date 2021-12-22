package blog.common.cluster.glosory;

import blog.common.messenger.DMessengerListener;

public interface ServiceCluster extends DMessengerListener {


    void addListener(Listener listener);

    void removeListener(Listener listener);

    void send(String topic,String type,Object... args);

    interface Listener {
        void onEvent(RemoteEvent event);
    }
}
