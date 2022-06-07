package blog.customer.storage.service;

import blog.customer.storage.model.glosory.CustomerStatus;
import blog.customer.storage.model.po.CustomerPo;

import java.util.List;

public interface CustomerService {

    List<CustomerPo> iterateCustomer(Integer id, boolean inclusive, int step);

    List<CustomerPo> getAll();

    List<CustomerPo> findByStatus(CustomerStatus status);

    void insertOrUpdate(List<CustomerPo> customerPos);

    void update(CustomerPo customerPo);



}
