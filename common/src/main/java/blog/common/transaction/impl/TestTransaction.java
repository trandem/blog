package blog.common.transaction.impl;

import blog.common.transaction.element.TxnHashMap;

import java.util.ArrayList;
import java.util.List;


public class TestTransaction {
    private TxnHashMap<String, List<String>> userData;

    public TestTransaction() {
        this.userData = new TxnHashMap<>();
    }

    private void buySuccess(String userName, String productName) {
        if (!userData.contain(userName)) {
            userData.put(userName, new ArrayList<>());
        }
        userData.get(userName).add(productName);
    }

    private void buyFail() {
        throw new RuntimeException(" something is error");
    }

    public TxnHashMap<String, List<String>> getUserData() {
        return userData;
    }

    public void setUserData(TxnHashMap<String, List<String>> userData) {
        this.userData = userData;
    }

    public static void main(String[] args) {

        TestTransaction test = new TestTransaction();

        TxnManager manager = new TxnManager();

        manager.execute(() -> {
            test.buySuccess("demtv","iphone");
            test.buySuccess("demtv","ipad");
            test.buySuccess("maitv","ipad");
            return null;
        });

        manager.execute(() -> {
            test.buySuccess("maipm","iphone");
            test.buyFail();
            return null;
        });

        System.out.println(test.getUserData().getValue());
    }

}
