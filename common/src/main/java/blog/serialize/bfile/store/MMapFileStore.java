package blog.serialize.bfile.store;

import blog.common.Utils;
import blog.serialize.bfile.io.BFileInput;
import blog.serialize.bfile.io.BFileOutput;
import blog.serialize.bfile.io.MMapInput;
import blog.serialize.bfile.io.MMapOutput;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;

public class MMapFileStore extends AbstractFileStore{
    private MMapInput input;
    private MMapOutput output;
    private MappedByteBuffer buffer;
    private final File file;

    private RandomAccessFile randomAccessFile;

//    protected int capacity = 150; /* for test */

    public MMapFileStore(String fileName)  {
        this.file = new File(fileName);
    }

    public MMapFileStore(File file)  {
        this.file = file;
    }

    public void doStart() throws Exception {
        this.randomAccessFile = new RandomAccessFile(file, "rw");
        this.randomAccessFile.setLength(capacity);
        this.buffer = this.randomAccessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, this.randomAccessFile.length());
        this.input = new MMapInput(buffer);
        this.output = new MMapOutput(this);
        setReserve(64);//for stop event
    }

    @Override
    protected long doStop(long timeout, TimeUnit unit) throws Exception {
        close(input); input = null;
        close(output);output = null;
        close(randomAccessFile); randomAccessFile =null;
        Utils.disposeDirectByteBuf(buffer); buffer =null;
        return 0;
    }

    public BFileInput getInput() {
        return input;
    }

    public void setInput(MMapInput input) {
        this.input = input;
    }

    public BFileOutput getOutput() {
        return output;
    }

    @Override
    public void reserve(int size) {
        setReserve(size);
    }

    public void setOutput(MMapOutput output) {
        this.output = output;
    }

    public RandomAccessFile getRandomAccessFile() {
        return randomAccessFile;
    }

    public void setRandomAccessFile(RandomAccessFile randomAccessFile) {
        this.randomAccessFile = randomAccessFile;
    }

    public MappedByteBuffer getBuffer() {
        return buffer;
    }

}
