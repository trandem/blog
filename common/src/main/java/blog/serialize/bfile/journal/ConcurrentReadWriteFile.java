package blog.serialize.bfile.journal;

import blog.common.glosory.ReferenceLifeCycle;
import blog.serialize.Event;
import blog.serialize.base.DMarshaller;
import blog.serialize.bfile.io.BFileInput;
import blog.serialize.bfile.io.BFileOutput;
import blog.serialize.bfile.store.MMapFileStore;
import blog.serialize.impl.AllMarshaller;
import blog.serialize.test.DataEvent;
import blog.serialize.test.StartEvent;
import blog.serialize.test.StopEvent;
import blog.serialize.test.UserModel;

import java.io.File;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentReadWriteFile extends ReferenceLifeCycle implements JournalFs.ConcurrentReadWriter {

    private ReentrantLock lock = new ReentrantLock();
    private Condition notEmptyCondition = lock.newCondition();
    protected short magic = 12;
    private MMapFileStore mMapStore;
    private File file;
    protected DMarshaller marshaller;

    private WriterIml writer = new WriterIml();

    public ConcurrentReadWriteFile(File file) {
        this.file = file;
        this.mMapStore = new MMapFileStore(file);
    }

    @Override
    protected void doStart() throws Exception {
        //No Op
    }

    @Override
    protected long doStop(long timeout, TimeUnit unit) throws Exception {
        return 0;
    }

    public void run(Lock lock, Runnable runnable) {
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public JournalFs.Writer createWriter() {
        return writer;
    }

    @Override
    public JournalFs.Reader createReader() {
        return new ReaderIml();
    }

    public class ReaderIml extends ReferenceLifeCycle implements JournalFs.Reader {
        private int position = 0;

        @Override
        public Event seek(long id) {
            lock.lock();
            try {
                BFileInput input = mMapStore.getInput();
                input.reset(position);
                Event event = seek(input, id);
                position = input.mark();
                return event;
            } finally {
                lock.unlock();
            }
        }

        public Event seek(BFileInput input, long seekId) {
            while (true) {
                try {
                    int mark = input.mark();
                    short magic = input.readShort();
                    if (magic == 0) {
                        input.reset(mark);// because read is faster and write is slower, so we need reset to position
                        return null;
                    }
                    long id = input.readLong();
                    int size = input.readInt();
                    if (id >= seekId) {
                        return marshaller.read(input);//event
                    } else {
                        input.skipBytes(size);
                    }
                } catch (BufferUnderflowException e) {
                    System.out.println("overflow");
                    return null;
                }
            }
        }

        @Override
        public Event read(long timeout, TimeUnit unit) {
            lock.lock();
            try {
                long wt = unit.toNanos(timeout);
                while (position == writer.position) {
                    if (wt < 0) {
                        return null;
                    } else {
                        wt = notEmptyCondition.awaitNanos(wt);
                    }
                }

                BFileInput input = mMapStore.getInput();
                input.reset(position);
                Event event = read(input);
                this.position = input.mark();
                return event;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); /* 'interrupt' */
                return null;
            } finally {
                lock.unlock();
            }
        }

        @Override
        protected void doStart() throws Exception {
            run(lock, () -> mMapStore.start());
        }

        @Override
        protected long doStop(long timeout, TimeUnit unit) throws Exception {
            run(lock, () -> mMapStore.start());
            return 0;
        }

        public Event read(BFileInput input) {
            try {
                short magic = input.readShort();
                if (magic == 0) {
                    return new StopEvent();
                }
                input.readLong();//id
                input.readInt();//size
                return marshaller.read(input);//event
            } catch (BufferUnderflowException e) {
                return new StopEvent();
            }
        }
    }

    public class WriterIml extends ReferenceLifeCycle implements JournalFs.Writer {
        private int position = 0;

        @Override
        public boolean write(Event event) {
            lock.lock();
            try {
                BFileOutput output = mMapStore.getOutput();
                output.reset(position);// reset to position to write data
                boolean b = write(output, event);
                if (b) {
                    position = output.mark();
                }
                notEmptyCondition.signalAll();
                return b;
            } finally {
                lock.unlock();
            }
        }

        public boolean write(BFileOutput output, Event event) {
            try {
                output.writeShort(magic);
                output.writeLong(event.getId());
                //prepare write size
                int posS = output.mark();

                output.writeInt(0);
                //write data
                marshaller.write(event, output);

                // write size
                int posE = output.mark();
                int size = posE - posS - 4;
                output.reset(posS);
                output.writeInt(size);
                //reset
                output.reset(posE);
                return true;
            } catch (BufferOverflowException e) {
                return false;
            }
        }

        @Override
        public void flush() {
            run(lock, () -> mMapStore.getOutput().flush());
        }

        @Override
        protected void doStart() throws Exception {
            run(lock, () -> mMapStore.start());
            mMapStore.reserve(64);
            write(new StartEvent());
        }

        @Override
        protected long doStop(long timeout, TimeUnit unit) throws Exception {
            mMapStore.reserve(0);
            write(new StopEvent());
            run(lock, () -> mMapStore.stop());
            return 0;
        }
    }

    public void setMarshaller(DMarshaller marshaller) {
        this.marshaller = marshaller;
    }

    public static void main(String[] args) {
        ConcurrentReadWriteFile cReadWrite = new ConcurrentReadWriteFile(new File("concurrent-rw.dev"));
        cReadWrite.start();
        cReadWrite.setMarshaller(AllMarshaller.DEFAULT);
        JournalFs.Writer writer = cReadWrite.createWriter();
        writer.start();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger id = new AtomicInteger(100);
        executorService.scheduleAtFixedRate(() -> {
            DataEvent event = new DataEvent();
            int tId = id.getAndIncrement();
            event.setId(tId);
            UserModel userModel = new UserModel("demtv" + tId, tId);
            event.setSomeTestData(userModel);
            writer.write(event);
        }, 1000, 40, TimeUnit.MILLISECONDS);

        Thread t = new Thread(new Runnable() {
            JournalFs.Reader reader = cReadWrite.createReader();

            @Override
            public void run() {
                reader.start();
                int seekId = 200;
                boolean sought = false;
                while (true) {
                    if (!sought) {
                        Event event = reader.seek(seekId);
                        if (event == null) continue;

                        sought = true;
                        System.out.println("ThreadId " + Thread.currentThread().getId() + " " + event);
                    } else {
                        Event event = reader.read(100, TimeUnit.MILLISECONDS);
                        System.out.println("ThreadId " + Thread.currentThread().getId() + " " + event);
                    }
                }
            }
        });

        t.start();


        Thread t1 = new Thread(new Runnable() {
            JournalFs.Reader reader = cReadWrite.createReader();

            @Override
            public void run() {
                reader.start();
                while (true) {
                    Event event = reader.read(100, TimeUnit.MILLISECONDS);
                    System.out.println("ThreadId " + Thread.currentThread().getId() + " " + event);
                }
            }
        });

        t1.start();

    }
}
