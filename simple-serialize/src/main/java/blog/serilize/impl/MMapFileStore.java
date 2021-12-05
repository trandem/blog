package blog.serilize.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MMapFileStore {
    private MMapInput input;
    private MMapOutput output;
    private MappedByteBuffer buffer;

    private RandomAccessFile randomAccessFile;
    private int reserve;
//    protected int capacity = 32 * 1024; /* 32K */
    protected int capacity = 150; /* 32K */

    public MMapFileStore(String fileName) throws IOException {
        File file = new File(fileName);
        this.randomAccessFile = new RandomAccessFile(file, "rw");
        this.randomAccessFile.setLength(capacity);
        this.buffer = this.randomAccessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, this.randomAccessFile.length());
        this.input = new MMapInput(buffer);
        this.output = new MMapOutput(buffer, this);
        setReserve(64);//for stop event
    }

    public MMapInput getInput() {
        return input;
    }

    public void setInput(MMapInput input) {
        this.input = input;
    }

    public MMapOutput getOutput() {
        return output;
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

    public int getReserve() {
        return reserve;
    }

    public void setReserve(int reserve) {
        this.reserve = reserve;
    }
}
