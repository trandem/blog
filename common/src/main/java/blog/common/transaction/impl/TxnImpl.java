package blog.common.transaction.impl;

import blog.common.Utils;
import blog.common.transaction.base.TxnElement;
import blog.common.transaction.base.Txn;

import java.util.HashMap;
import java.util.Map;

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
            cookies.clear();
        }
    }

    @Override
    public void begin() {
        this.cookies.clear();
    }

    @Override
    public void rollback() {
        try {
            for (Map.Entry<TxnElement, Object> entry : cookies.entrySet()) {
                entry.getKey().onRollback(entry.getValue());
            }
        } finally {
            cookies.clear();
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
}
