package blog.common.transaction.impl;

import blog.common.concurrent.FastThreadLocal;
import blog.common.transaction.base.TransactionBase;

import java.util.HashMap;
import java.util.function.Supplier;

public class TransactionManager {

    public <T> T execute(Supplier<T> supplier) {
        TransactionInfo info = createTransaction();
        TransactionBase transactionBase = info.current;

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
        TransactionBase base = new TransactionActionImpl();
        TransactionInfo prev = STX.get();
        TransactionInfo next = new TransactionInfo(base, prev);
        STX.set(next);
        return next;
    }

    static class TransactionInfo {
        private TransactionBase current;
        private TransactionInfo previous;

        public TransactionInfo(TransactionBase current, TransactionInfo info) {
            this.current = current;
            this.previous = info;
        }

        public TransactionBase getCurrent() {
            return current;
        }

        public void setCurrent(TransactionBase current) {
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

    public static final TransactionBase current() {
        if (STX.get() == null) return null;
        return STX.get().current;
    }

    public static void main(String[] args) {
        TransactionManager manager = new TransactionManager();
        manager.execute(()->{
            System.out.println(STX.get());
            manager.execute(()->{
                System.out.println(STX.get());
                return -1;
            });
            return 1;
        });
        System.out.println(STX.get());

        HashMap<Integer,Long> x = new HashMap<>();
        HashMap<Integer,Long> y = new HashMap<>(x);

    }
}
