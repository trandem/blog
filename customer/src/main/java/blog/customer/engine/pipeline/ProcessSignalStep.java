package blog.customer.engine.pipeline;

import blog.common.glosory.ReferenceLifeCycle;
import blog.customer.engine.signal.RequestSignal;

import java.util.concurrent.TimeUnit;

public class ProcessSignalStep extends ReferenceLifeCycle implements RequestSignalStep<RequestSignal> {

    private Shard shard;
    private int batch = 32;


    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected long doStop(long timeout, TimeUnit unit) throws Exception {
        return 0;
    }

    @Override
    public Shard getShard() {
        return shard;
    }

    @Override
    public void process(RequestSignal[] signals, int count) {

    }

    @Override
    public int getBatch() {
        return 32;
    }

    public class ProcessSignalStepFactory implements RequestSignalStep.factory<RequestSignal> {

        private int batch;

        @Override
        public RequestSignalStep<RequestSignal> create(Shard shard) {

            ProcessSignalStep step = new ProcessSignalStep();
            if (this.batch != 0) step.batch = this.batch;
            step.shard = shard;

            return step;
        }

        public void setBatch(int batch) {
            this.batch = batch;
        }
    }
}
