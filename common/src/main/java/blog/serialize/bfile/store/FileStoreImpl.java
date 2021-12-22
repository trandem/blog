package blog.serialize.bfile.store;

import blog.serialize.bfile.io.BFIleInputImpl;
import blog.serialize.bfile.io.BFIleOutputImpl;
import blog.serialize.bfile.io.BFileInput;
import blog.serialize.bfile.io.BFileOutput;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class FileStoreImpl extends AbstractFileStore {

    private int position;
    private File file;
    private String mode;

    private BFIleInputImpl bInput;
    private BFIleOutputImpl bOutput;
    private RandomAccessFile randomAccessFile;

    public FileStoreImpl(File file, String mode, int capacity) {
        this.file = file;
        this.mode = mode;
        this.capacity = capacity;
        this.position = 0;
    }

    @Override
    protected void doStart() throws Exception {
        this.randomAccessFile = new RandomAccessFile(file, mode);
        this.seek(0);
        this.bInput = new BFIleInputImpl(this);
        this.bOutput = new BFIleOutputImpl(this);
    }

    @Override
    protected long doStop(long timeout, TimeUnit unit) throws Exception {
        close(bInput);
        bInput = null;
        close(bOutput);
        bOutput = null;
        close(randomAccessFile);
        randomAccessFile = null;
        return 0;
    }


    public void seek(int position) {

        if (position > getCapacity()) {
            throw new BufferOverflowException();
        }

        try {
            this.randomAccessFile.seek(position);
            this.position = position;
        } catch (IOException e) {
            throw new RuntimeException("seek error");
        }
    }

    public int read(ByteBuffer buffer) {
        try {
            int r = this.randomAccessFile.getChannel().read(buffer);
            this.position += r;
            return r;
        } catch (IOException e) {
            throw new RuntimeException("read buffer error");
        }
    }

    public int getPosition() {
        return position;
    }

    public int getRemainCap() {
        return getCapacity() - getPosition();
    }

    public int write(ByteBuffer buffer) {
        if (buffer.remaining() > getRemainCap()) {
            throw new BufferOverflowException();
        }

        try {
            final int r = this.randomAccessFile.getChannel().write(buffer);
            this.position += r;
            return r;
        } catch (IOException e) {
            throw new RuntimeException("write buffer error");
        }
    }

    @Override
    public BFileInput getInput() {
        return bInput;
    }

    @Override
    public BFileOutput getOutput() {
        return bOutput;
    }

    @Override
    public void reserve(int size) {
        setReserve(size);
    }

}
