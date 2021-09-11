package blog.common.transaction.base;

public interface TransactionElement {

    void onCommit(Object data);

    void onRollback(Object data);

}
