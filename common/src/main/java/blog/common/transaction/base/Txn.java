package blog.common.transaction.base;

public interface Txn {
    void commit();
    void begin();

    void rollback();

    <T> T get(TxnElement key);

    void put(TxnElement element, Object value);

    boolean contain (TxnElement element);

}
