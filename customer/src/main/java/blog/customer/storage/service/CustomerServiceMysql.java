package blog.customer.storage.service;

import blog.customer.storage.model.glosory.CustomerStatus;
import blog.customer.storage.model.po.CustomerPo;
import blog.customer.storage.repo.CustomerRepo;

import java.util.List;

public class CustomerServiceMysql implements CustomerService {

    private final CustomerRepo repo;

    public CustomerServiceMysql(CustomerRepo repo) {
        this.repo = repo;
    }


    @Override
    public List<CustomerPo> iterateCustomer(Integer id, boolean inclusive, int step) {
        return repo.iterateCustomer(id, inclusive, step);
    }

    @Override
    public List<CustomerPo> getAll() {
        return repo.getAll();
    }

    @Override
    public List<CustomerPo> findByStatus(CustomerStatus status) {
        return repo.findByStatus(status);
    }

    @Override
    public void insertOrUpdate(List<CustomerPo> customerPos) {
        repo.insertOrUpdates(customerPos);
    }

    @Override
    public void update(CustomerPo customerPo) {
        int rs = repo.update(customerPo);
        if (rs ==1) return;
        throw new RuntimeException("update fails, optimistic locking");
    }
}
