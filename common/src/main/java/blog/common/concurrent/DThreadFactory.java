package blog.common.concurrent;

import java.util.concurrent.ThreadFactory;

public class DThreadFactory implements ThreadFactory {

    private String name;
    public DThreadFactory() {
    }

    public DThreadFactory(String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new DThread(r);
    }
}
