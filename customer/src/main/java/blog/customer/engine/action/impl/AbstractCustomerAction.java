package blog.customer.engine.action.impl;

import blog.common.transaction.base.DPropagation;
import blog.common.transaction.impl.TxnManager;
import blog.customer.engine.action.CustomerAction;
import blog.customer.engine.enitty.CustomerEntityManager;
import blog.customer.engine.pipeline.request.CustomerRequest;
import blog.customer.engine.pipeline.request.CustomerSharableRequest;
import blog.customer.engine.pipeline.response.CustomerFactoryManager;
import blog.customer.engine.pipeline.response.CustomerResponse;

public abstract class AbstractCustomerAction<Req extends CustomerRequest, Res extends CustomerResponse> implements CustomerAction {

    private CustomerFactoryManager factoryManager;

    protected CustomerEntityManager entityManager;

    private TxnManager manager;

    protected  Res doActionTransaction(Req req) {
        throw new RuntimeException("not implement");
    }

    @Override
    public CustomerResponse doAction(CustomerRequest customerRequest) {
        if (customerRequest instanceof CustomerSharableRequest) {
            return manager.executeTransaction(() -> doActionTransaction((Req) customerRequest), DPropagation.REQUIRES_NEW);
        } else {
            throw new RuntimeException("not implement yet");
        }
    }

    public CustomerFactoryManager getFactoryManager() {
        return factoryManager;
    }

    public void setFactoryManager(CustomerFactoryManager factoryManager) {
        this.factoryManager = factoryManager;
    }

    public CustomerEntityManager getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(CustomerEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public TxnManager getManager() {
        return manager;
    }

    public void setManager(TxnManager manager) {
        this.manager = manager;
    }
}
