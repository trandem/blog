package blog.customer.storage;

import blog.core.storage.Event;

import java.util.Iterator;

public interface Snapshot {

    Iterator<Event> iterate();

}
