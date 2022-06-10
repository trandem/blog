package blog.customer.engine.enitty;

import blog.customer.storage.model.po.CustomerPo;

import java.util.Iterator;

public interface CustomerEntityManager {

    CustomerPo getCustomer(int id);

    void putCustomer(CustomerPo customerPo);

    Iterator<CustomerPo> customerIterator();
}
