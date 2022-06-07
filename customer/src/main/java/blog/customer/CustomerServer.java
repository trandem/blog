package blog.customer;

import blog.common.glosory.ReferenceLifeCycle;
import blog.customer.engine.pipeline.PipeLine;
import blog.customer.ha.HaManager;
import blog.customer.storage.Snapshot;

import java.util.concurrent.TimeUnit;

public class CustomerServer extends ReferenceLifeCycle implements HaManager.Listener {
    private HaManager haManager;
    private Snapshot customerSnapshot;


    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected long doStop(long timeout, TimeUnit unit) throws Exception {
        return 0;
    }

    @Override
    public void onMaster() {

    }

    @Override
    public void onSlave(Throwable th) {

    }


}
