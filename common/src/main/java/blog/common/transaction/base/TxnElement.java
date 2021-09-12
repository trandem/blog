package blog.common.transaction.base;

public interface TxnElement {

    void onCommit(Object data);

    void onRollback(Object data);

}
