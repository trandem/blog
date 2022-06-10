package blog.common.transaction.element;

import blog.common.Utils;
import blog.common.transaction.base.TnxError;
import blog.common.transaction.base.TxnElement;
import blog.common.transaction.base.Txn;
import blog.common.transaction.impl.TxnManager;

import java.util.HashMap;
import java.util.Map;

public class TxnHashMap<K, V> implements TxnElement {
    private Map<K, V> value;

    public TxnHashMap() {
        this.value = new HashMap<>();
    }

    public void put(K k, V v) {
        Map<K, V> cookies = getCookies(true);
        if (cookies == null) {
            throw new TnxError("Transaction not found");
        }
        cookies.put(k, v);
    }

    public V get(K k) {
        V data = value.get(k);
        if (data != null) return data;
        Map<K, V> cookies = getCookies(false);
        if (cookies == null) {
            return null;
        }
        return cookies.get(k);
    }

    public boolean containKey(K k) {
        boolean isExist = value.containsKey(k);
        if (isExist) return false;
        Map<K, V> cookies = getCookies(false);
        if (cookies == null) {
            return true;
        }
        return !cookies.containsKey(k);
    }


    private Map<K, V> getCookies(boolean force) {
        Txn txn = TxnManager.current();
        if (txn == null) return null;
        Map<K, V> cookies = txn.get(this);
        if (cookies == null && force) {
            cookies = new HashMap<>();
            txn.put(this, cookies);
        }
        return cookies;
    }

    @Override
    public void onCommit(Object data) {
        Map<K, V> cookie = Utils.cast(data);
        if (cookie != null) {
            this.value.putAll(cookie);
        }
    }

    @Override
    public void onRollback(Object data) {
        System.out.println("do nothing because value is not be change");
    }

    public Map<K, V> getValue() {
        return value;
    }
}
