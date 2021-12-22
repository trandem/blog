package blog.common.transaction.impl;

import blog.common.concurrent.FastThreadLocal;
import blog.common.transaction.base.DPropagation;
import blog.common.transaction.base.Txn;

import java.util.function.Supplier;

public class TxnManager {

    Txn.factory factory = new TxnImpl.RecycleFactory();

    public <T> T executeTransaction(Supplier<T> supplier, DPropagation propagation) {
        TransactionInfo info = createTransaction(propagation);
        Txn transactionBase = info.current;

        try {
            transactionBase.begin();

            T r = supplier.get();

            transactionBase.commit();
            return r;

        } catch (Exception e) {
            transactionBase.rollback();
            return null;
        } finally {
            STX.set(info.getPrevious());
        }
    }

    private TransactionInfo createTransaction(DPropagation propagation) {
        TransactionInfo currentInfo = STX.get();
        Txn txn = currentInfo == null ? null : currentInfo.current;
        TransactionInfo next;
        switch (propagation) {
            case SUPPORT:
                next = new TransactionInfo(txn, currentInfo);
                break;
            case REQUIRES_NEW:
                next = new TransactionInfo(factory.create(), currentInfo);
                break;
            case REQUIRED:
                next = new TransactionInfo(txn == null ? factory.create() : txn, currentInfo);
                break;
            default:
                throw new RuntimeException("not support this DPropagation");
        }
        STX.set(next);
        return next;
    }

    static class TransactionInfo {
        private Txn current;
        private TransactionInfo previous;

        public TransactionInfo(Txn current, TransactionInfo info) {
            this.current = current;
            this.previous = info;
        }

        public Txn getCurrent() {
            return current;
        }

        public void setCurrent(Txn current) {
            this.current = current;
        }

        public TransactionInfo getPrevious() {
            return previous;
        }

        public void setPrevious(TransactionInfo previous) {
            this.previous = previous;
        }
    }

    private static final FastThreadLocal<TransactionInfo> STX = new FastThreadLocal<>();

    public static Txn current() {
        if (STX.get() == null) return null;
        return STX.get().current;
    }

}
