package blog.common.transaction.impl;

import blog.common.Utils;
import blog.common.transaction.base.TransactionElement;
import blog.common.transaction.base.TransactionBase;

import java.util.HashMap;
import java.util.Map;

public class TransactionActionImpl implements TransactionBase {
    private final Map<TransactionElement, Object> cookies;

    public TransactionActionImpl() {
        this.cookies = new HashMap<>(8);
    }

    @Override
    public void commit() {
        try {
            for (Map.Entry<TransactionElement, Object> entry : cookies.entrySet()) {
                entry.getKey().onCommit(entry.getValue());
            }
        } finally {
            cookies.clear();
        }
    }

    @Override
    public void rollback() {
        try {
            for (Map.Entry<TransactionElement, Object> entry : cookies.entrySet()) {
                entry.getKey().onRollback(entry.getValue());
            }
        }finally {
            cookies.clear();
        }
    }

    @Override
    public <T> T get(TransactionElement key) {
        return Utils.cast(cookies.get(key));
    }

    @Override
    public void put(TransactionElement element, Object value) {
        cookies.put(element, value);
    }

    @Override
    public boolean contain(TransactionElement element) {
        return cookies.containsKey(element);
    }
}
