package blog.common.transaction.base;

public interface Txn {
    void commit();
    void begin();

    void rollback();

    <T> T get(TransactionElement key);

    void put(TransactionElement element, Object value);

    boolean contain (TransactionElement element);

}
