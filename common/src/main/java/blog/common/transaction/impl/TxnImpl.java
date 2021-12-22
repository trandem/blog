package blog.common.transaction.impl;

import blog.common.Utils;
import blog.common.transaction.base.TxnElement;
import blog.common.transaction.base.Txn;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class TxnImpl implements Txn {
    private final Map<TxnElement, Object> cookies;

    public TxnImpl() {
        this.cookies = new HashMap<>(8);
    }

    @Override
    public void commit() {
        try {
            for (Map.Entry<TxnElement, Object> entry : cookies.entrySet()) {
                entry.getKey().onCommit(entry.getValue());
            }
        } finally {
            dispose();
        }
    }

    @Override
    public void begin() {
//        this.cookies.clear();
    }

    @Override
    public void dispose() {
        cookies.clear();
    }

    @Override
    public void rollback() {
        try {
            for (Map.Entry<TxnElement, Object> entry : cookies.entrySet()) {
                entry.getKey().onRollback(entry.getValue());
            }
        } finally {
            dispose();
        }
    }

    @Override
    public <T> T get(TxnElement key) {
        return Utils.cast(cookies.get(key));
    }

    @Override
    public void put(TxnElement element, Object value) {
        cookies.put(element, value);
    }

    @Override
    public boolean contain(TxnElement element) {
        return cookies.containsKey(element);
    }

    private final static class RecycleTxn extends TxnImpl {
        private Queue<RecycleTxn> cacheTxn;

        public void setCacheTxn(Queue<RecycleTxn> cacheTxn) {
            this.cacheTxn = cacheTxn;
        }

        @Override
        public void dispose() {
            super.dispose();
            cacheTxn.add(this);
        }
    }


    public static class RecycleFactory implements Txn.factory {

        Queue<RecycleTxn> cacheTxn = new LinkedList<>();

        @Override
        public Txn create() {
            RecycleTxn  txn = cacheTxn.poll();
            if (txn ==null){
                txn = new RecycleTxn();
                txn.setCacheTxn(cacheTxn);
            }
            return txn;
        }
    }
}
