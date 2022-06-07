package blog.customer.storage;

import blog.common.glosory.LifeCycle;
import blog.core.storage.Event;

import java.util.Iterator;

public interface Snapshot extends LifeCycle {

    Iterator<Event> iterate();

}
