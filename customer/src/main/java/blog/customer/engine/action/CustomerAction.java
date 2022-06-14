package blog.customer.engine.action;

import blog.common.transaction.impl.TxnManager;
import blog.customer.engine.enitty.CustomerEntityManager;
import blog.customer.engine.pipeline.request.CustomerRequest;
import blog.customer.engine.pipeline.response.CustomerResponse;

public interface CustomerAction extends Action<CustomerRequest, CustomerResponse> {
    public void setEntityManager(CustomerEntityManager entityManager);

    public TxnManager getTracsactionManager();

    public void setTracsactionManager(TxnManager tracsactionManager);
}
