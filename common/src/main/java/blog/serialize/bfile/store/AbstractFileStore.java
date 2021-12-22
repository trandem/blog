package blog.serialize.bfile.store;

import blog.common.glosory.ReferenceLifeCycle;

import java.io.Closeable;


public abstract class AbstractFileStore extends ReferenceLifeCycle implements BFileStore {
    private int reserve;
    protected int capacity = 32 * 1024; /* 32K */

    public int getReserve() {
        return reserve;
    }

    public void setReserve(int reserve) {
        this.reserve = reserve;
    }

    public int getCapacity() {
        return capacity - reserve;
    }


    public void close(Closeable element){
        try {
            element.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
