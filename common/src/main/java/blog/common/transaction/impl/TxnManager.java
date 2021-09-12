package blog.common.transaction.impl;

import blog.common.concurrent.FastThreadLocal;
import blog.common.transaction.base.Txn;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class TxnManager {

    public <T> T executeTransaction(Supplier<T> supplier) {
        TransactionInfo info = createTransaction();
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

    private TransactionInfo createTransaction() {
        Txn base = new TxnImpl();
        TransactionInfo prev = STX.get();
        TransactionInfo next = new TransactionInfo(base, prev);
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
