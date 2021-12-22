package blog.serialize.bfile.journal;

import blog.common.glosory.LifeCycle;
import blog.serialize.Event;

import java.util.concurrent.TimeUnit;

public interface JournalFs extends LifeCycle {

    interface Writer extends LifeCycle{
        boolean write(Event event);

        void flush();
    }

    interface Reader  extends LifeCycle {
        Event seek(long id);

        Event read(long timeout, TimeUnit unit);
    }

    interface ConcurrentReadWriter extends LifeCycle{

        Writer createWriter();

        Reader createReader();
    }
}
