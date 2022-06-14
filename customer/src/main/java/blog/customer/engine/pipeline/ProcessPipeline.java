package blog.customer.engine.pipeline;

import blog.common.glosory.ReferenceLifeCycle;
import blog.customer.engine.signal.CustomerShardManager;
import blog.customer.engine.signal.Signal;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;


public class ProcessPipeline<T extends Signal> extends ReferenceLifeCycle implements PipeLine<T> {

    private RequestSignalStep.factory<T> stepFactory;
    private CustomerShardManager shardManager;
    private final Map<Shard, Queue<T>> queueMap = new HashMap<>();
    private final List<ProcessThread> processThreads = new ArrayList<>();
    protected final Class<T> clazz;

    public ProcessPipeline(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public boolean submit(T t) {
        Shard shard = t.shard();
        // submit to all queue
        boolean rs = true;
        if (shard == null) {
            for (Queue<T> queue : queueMap.values()) {
                rs &= queue.offer(t);
            }
        } else {
            rs = queueMap.get(shard).offer(t);
        }
        return rs;
    }

    @Override
    protected void doStart() throws Exception {

        for (Shard shard : shardManager.getShards()) {
            queueMap.put(shard, new ConcurrentLinkedQueue<>());
        }

        for (Map.Entry<Shard, Queue<T>> data : queueMap.entrySet()) {
            RequestSignalStep<T> step = stepFactory.create(data.getKey());
            ProcessThread worker = new ProcessThread(step, data.getValue(), data.getKey());
            worker.start();
            processThreads.add(worker);
        }
    }

    @Override
    protected long doStop(long timeout, TimeUnit unit) throws Exception {
        for (ProcessThread thread : processThreads) {
            thread.stop();
        }
        queueMap.clear();
        processThreads.clear();
        return 0;
    }

    public void setStepFactory(RequestSignalStep.factory<T> stepFactory) {
        this.stepFactory = stepFactory;
    }

    public void setShardManager(CustomerShardManager shardManager) {
        this.shardManager = shardManager;
    }


    public class ProcessThread extends ReferenceLifeCycle implements Runnable {
        private final RequestSignalStep<T> step;

        private final Queue<T> queues;

        private Thread worker;

        private boolean isStop;

        private final Shard shard;

        public ProcessThread(RequestSignalStep<T> step, Queue<T> queues, Shard shard) {
            this.step = step;
            this.queues = queues;
            this.shard = shard;
        }

        @Override
        public void run() {
            while (!isRunning()) Thread.yield();
            step.start();
            @SuppressWarnings("unchecked") final T[] works = (T[]) Array.newInstance(clazz, step.getBatch());

            while (!isStop) {
                int count = 0;
                for (int i = 0; i < works.length; i++) {
                    T request = queues.poll();
                    if (request == null) break;
                    works[i] = request;
                    count++;
                }

                if (count == 0) Thread.yield();

                try {
                    step.process(works, count);
                } catch (Throwable th) {
                    th.printStackTrace();
                } finally {
                    //System.out.println("proceed");
                }
            }
        }

        @Override
        protected void doStart() throws Exception {
            this.worker = new Thread(this);
            this.worker.setName("process request shard " + this.shard.getId());
            this.worker.start();

        }

        @Override
        protected long doStop(long timeout, TimeUnit unit) throws Exception {
            isStop = true;
            worker.join(1000);
            return 0;
        }
    }

}
