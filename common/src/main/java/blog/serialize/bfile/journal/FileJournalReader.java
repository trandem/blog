package blog.serialize.bfile.journal;

import blog.common.glosory.ReferenceLifeCycle;
import blog.serialize.Event;
import blog.serialize.base.DMarshaller;
import blog.serialize.bfile.io.BFileInput;
import blog.serialize.bfile.store.BFileStore;
import blog.serialize.bfile.store.FileStoreImpl;
import blog.serialize.test.StopEvent;

import java.io.File;
import java.nio.BufferUnderflowException;
import java.util.concurrent.TimeUnit;

public class FileJournalReader extends ReferenceLifeCycle implements JournalFs.Reader {
    protected short magic = 12;
    protected BFileStore store;
    protected File file;
    protected DMarshaller marshaller;

    public FileJournalReader(File file) {
        this.file = file;
        store = new FileStoreImpl(file, "r", Integer.MAX_VALUE);
    }

    @Override
    protected void doStart() throws Exception {
        store.start();
    }

    @Override
    protected long doStop(long timeout, TimeUnit unit) throws Exception {
        store.stop();
        return 0;
    }

    @Override
    public Event seek(long seekId) {
        BFileInput input = store.getInput();
        while (true) {
            try {
                short magic = input.readShort();
                if (magic == 0) return null;
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
        BFileInput input = store.getInput();
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

    public void setMarshaller(DMarshaller marshaller) {
        this.marshaller = marshaller;
    }
}
