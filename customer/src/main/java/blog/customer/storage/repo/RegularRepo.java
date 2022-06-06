package blog.customer.storage.repo;

import java.util.List;

public interface RegularRepo<T> {
    T get(Object id);

    List<T> getAll();

    int insert(T productPo);

    void insertOrUpdates(List<T> data);

    int update(T t);
}
