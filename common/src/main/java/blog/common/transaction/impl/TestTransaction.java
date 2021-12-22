package blog.common.transaction.impl;

import blog.common.transaction.base.DPropagation;
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

        manager.executeTransaction(() -> {
            test.buySuccess("demtv", "iphone");
            test.buySuccess("demtv", "ipad");
            test.buySuccess("maitv", "ipad");
            manager.executeTransaction(() -> {
                test.buySuccess("maitv", "ipad");
                test.buyFail();
                return null;
            }, DPropagation.SUPPORT);
            return null;
        }, DPropagation.REQUIRES_NEW);

        manager.executeTransaction(() -> {
            test.buySuccess("maipm", "iphone");
            test.buyFail();
            return null;
        }, DPropagation.REQUIRES_NEW);

        System.out.println(test.getUserData().getValue());
    }

}
