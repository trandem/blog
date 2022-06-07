package blog.customer.storage.repo;

import blog.customer.storage.model.glosory.CustomerStatus;
import blog.customer.storage.model.po.CustomerPo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CustomerRepo extends RegularRepo<CustomerPo> {

    List<CustomerPo> findByStatus(CustomerStatus status);

    List<CustomerPo> iterateCustomer(@Param("id") Integer id, @Param("inclusive") boolean inclusive, @Param("step") int step);

}
