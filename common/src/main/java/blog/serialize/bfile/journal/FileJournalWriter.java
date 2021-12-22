package blog.serialize.bfile.journal;

import blog.common.glosory.ReferenceLifeCycle;
import blog.serialize.Event;
import blog.serialize.base.DMarshaller;
import blog.serialize.bfile.io.BFileOutput;
import blog.serialize.bfile.store.BFileStore;
import blog.serialize.bfile.store.FileStoreImpl;
import blog.serialize.test.StartEvent;
import blog.serialize.test.StopEvent;

import java.io.File;
import java.nio.BufferOverflowException;
import java.util.concurrent.TimeUnit;

public class FileJournalWriter extends ReferenceLifeCycle implements JournalFs.Writer {

    protected short magic = 12;
    protected BFileStore store;
    protected File file;
    protected DMarshaller marshaller;

    public FileJournalWriter(File file) {
        this.file = file;
        store = new FileStoreImpl(file, "rwd", Integer.MAX_VALUE);
    }

    @Override
    protected void doStart() throws Exception {
        store.reserve(64);
        store.start();
        write(new StartEvent()/* begin with StartEvent */);
        flush();
    }

    @Override
    protected long doStop(long timeout, TimeUnit unit) throws Exception {
        this.store.reserve(0);
        write(new StopEvent());
        flush();
        store.stop();
        return 0;
    }

    @Override
    public boolean write(Event event) {
        BFileOutput output = store.getOutput();
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
            int size = posE - posS -4;
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
        store.getOutput().flush();
    }

    public void setMarshaller(DMarshaller marshaller) {
        this.marshaller = marshaller;
    }
}
