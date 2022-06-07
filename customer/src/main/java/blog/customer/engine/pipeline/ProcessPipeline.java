package blog.customer.engine.pipeline;

import blog.common.glosory.ReferenceLifeCycle;
import blog.customer.engine.signal.RequestSignal;

import java.util.concurrent.TimeUnit;

public class ProcessPipeline extends ReferenceLifeCycle implements PipeLine {

    @Override
    public boolean submit(RequestSignal t) {
        return false;
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected long doStop(long timeout, TimeUnit unit) throws Exception {
        return 0;
    }
}
