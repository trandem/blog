package blog.common.transaction.base;

public interface TransactionBase {
    void commit();

    void rollback();

    <T> T get(TransactionElement key);

    void put(TransactionElement element, Object value);

    boolean contain (TransactionElement element);

}
