package blog.common.glosory;

import java.util.concurrent.TimeUnit;

/**
 * copy from: https://github.com/nextopcn/lite-pool/blob/master/src/main/java/cn/nextop/lite/pool/glossary/Lifecyclet.java
 */
public interface LifeCycle {

    boolean start();

    boolean stop();

    boolean isRunning();

    boolean stop(long timeout, TimeUnit unit);
}
