package blog.common.concurrent;

import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unchecked")
@State(Scope.Benchmark)
public class RingOneConMulProBuffer<T> extends RingOneConOneProBuffer<T> {
    private final AtomicBoolean canWrite;

    public RingOneConMulProBuffer() {
        this(10000);
    }

    public RingOneConMulProBuffer(int k) {
        super(k);
        this.canWrite = new AtomicBoolean(true);
    }

    @Override
    public boolean offer(T value) {
        //this is not blocking but only one producer can write to resource
        while (!canWrite.compareAndSet(true, false)) {

        }
        boolean rs =  super.offer(value);
        canWrite.compareAndSet(false, true);
        return rs;
    }



    public synchronized boolean offer1(T value) {
        return super.offer(value);
    }

    public static void main(String[] args) throws IOException {
        org.openjdk.jmh.Main.main(args);
    }


    @Benchmark
    @BenchmarkMode({Mode.Throughput,Mode.AverageTime})
    public void test11() throws ExecutionException, InterruptedException {
        RingOneConMulProBuffer<String> buffer = new RingOneConMulProBuffer<>(100000);
        AtomicInteger numwrite = new AtomicInteger(0);
        ExecutorService service = Executors.newFixedThreadPool(3);

        Callable<Set<String>> cw = () -> {
            Set<String> x = new HashSet<>();
            int count = 0;
            for (int i = 0; i < 10000; i++) {
                if (buffer.offer(i + "")) {
                    count++;
                    x.add(i + "");
                }
            }
            numwrite.getAndIncrement();
            return x;
        };


        Callable<Set<String>> cr = () -> {
            Set<String> x = new HashSet<>();
            int count = 0;
            while (numwrite.get() != 2) {
                String data = buffer.poll();
                if (data != null) {
                    x.add(data);
                    count++;
                }
            }

            while (true) {
                String data = buffer.poll();
                if (data != null) {
                    x.add(data);
                    count++;
                } else {
                    break;
                }
            }
            return x;
        };

        Future<Set<String>> fw1 = service.submit(cw);
        Future<Set<String>> fw2 = service.submit(cw);
        Future<Set<String>> fr = service.submit(cr);

        fw2.get();
        fw1.get();
        fr.get();
        service.shutdown();
    }


    @Benchmark
    @BenchmarkMode({Mode.Throughput,Mode.AverageTime})
    public void test31() throws ExecutionException, InterruptedException {
        RingOneConMulProBuffer<String> buffer = new RingOneConMulProBuffer<>(100000);
        AtomicInteger numwrite = new AtomicInteger(0);
        ExecutorService service = Executors.newFixedThreadPool(3);

        Callable<Set<String>> cw = () -> {
            Set<String> x = new HashSet<>();
            int count = 0;
            for (int i = 0; i < 10000; i++) {
                if (buffer.offer1(i + "")) {
                    count++;
                    x.add(i + "");
                }
            }
            numwrite.getAndIncrement();
            return x;
        };


        Callable<Set<String>> cr = () -> {
            Set<String> x = new HashSet<>();
            int count = 0;
            while (numwrite.get() != 2) {
                String data = buffer.poll();
                if (data != null) {
                    x.add(data);
                    count++;
                }
            }

            while (true) {
                String data = buffer.poll();
                if (data != null) {
                    x.add(data);
                    count++;
                } else {
                    break;
                }
            }
            return x;
        };

        Future<Set<String>> fw1 = service.submit(cw);
        Future<Set<String>> fw2 = service.submit(cw);
        Future<Set<String>> fr = service.submit(cr);

        fw2.get();
        fw1.get();
        fr.get();
        service.shutdown();
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput,Mode.AverageTime})
    public void test21() throws ExecutionException, InterruptedException {
        BlockingQueue<String> buffer = new ArrayBlockingQueue<>(100000);
        AtomicInteger numwrite = new AtomicInteger(0);
        ExecutorService service = Executors.newFixedThreadPool(3);

        Callable<Set<String>> cw = () -> {
            Set<String> x = new HashSet<>();
            int count = 0;
            for (int i = 0; i < 10000; i++) {
                if (buffer.offer(i + "")) {
                    count++;
                    x.add(i + "");
                }
            }
            numwrite.getAndIncrement();
            return x;
        };


        Callable<Set<String>> cr = () -> {
            Set<String> x = new HashSet<>();
            int count = 0;
            while (numwrite.get() != 2) {
                String data = buffer.poll();
                if (data != null) {
                    x.add(data);
                    count++;
                }
            }

            while (true) {
                String data = buffer.poll();
                if (data != null) {
                    x.add(data);
                    count++;
                } else {
                    break;
                }
            }
            return x;
        };

        Future<Set<String>> fw1 = service.submit(cw);
        Future<Set<String>> fw2 = service.submit(cw);
        Future<Set<String>> fr = service.submit(cr);

        fw2.get();
        fw1.get();
        fr.get();
        service.shutdown();
    }
}
