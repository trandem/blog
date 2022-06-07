package blog.customer.ha;

import blog.common.glosory.ReferenceLifeCycle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class AutoMasterHaManager extends ReferenceLifeCycle implements HaManager{

    private List<Listener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public Status getStatus() {
        return Status.MASTER;
    }

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void delListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected long doStop(long timeout, TimeUnit unit) throws Exception {
        return 0;
    }

}
