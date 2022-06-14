package blog.customer.engine.pipeline;

import blog.common.concurrent.DFuture;
import blog.common.glosory.ReferenceLifeCycle;
import blog.common.transaction.impl.TxnManager;
import blog.customer.engine.action.CustomerAction;
import blog.customer.engine.action.impl.CreateAccountAction;
import blog.customer.engine.action.impl.QueryCustomerAction;
import blog.customer.engine.action.impl.ReplicateAction;
import blog.customer.engine.action.impl.UpdateCustomerAction;
import blog.customer.engine.enitty.CustomerEntityManager;
import blog.customer.engine.enitty.CustomerEntityManagerImpl;
import blog.customer.engine.pipeline.request.CustomerRequest;
import blog.customer.engine.pipeline.request.CustomerRequestType;
import blog.customer.engine.pipeline.response.CustomerResponse;
import blog.customer.engine.signal.RequestSignal;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ProcessSignalStep extends ReferenceLifeCycle implements RequestSignalStep<RequestSignal> {

    private Shard shard;
    private int batch = 32;

    private Map<CustomerRequestType, CustomerAction> actionMap;
    private TxnManager transactionManager;
    protected CustomerEntityManager entityManager;

    @Override
    protected void doStart() throws Exception {
        this.transactionManager = new TxnManager();
        this.entityManager = new CustomerEntityManagerImpl();
        if (actionMap == null) {
            actionMap = new EnumMap<>(CustomerRequestType.class);
            initAction();
        }
    }

    private void initAction() {
        actionMap.put(CustomerRequestType.CREATE, new CreateAccountAction());
        actionMap.put(CustomerRequestType.REPLICATE, new ReplicateAction());
        actionMap.put(CustomerRequestType.GET_CUSTOMER, new QueryCustomerAction());
        actionMap.put(CustomerRequestType.UPDATE, new UpdateCustomerAction());

        for (CustomerAction action : actionMap.values()) {
            action.setEntityManager(entityManager);
            action.setTracsactionManager(transactionManager);
        }
    }

    @Override
    protected long doStop(long timeout, TimeUnit unit) throws Exception {
        return 0;
    }

    @Override
    public Shard getShard() {
        return shard;
    }

    @Override
    public void process(RequestSignal[] signals, int count) {
        for (int i = 0; i < count; i++) {
            CustomerRequest request = signals[i].getRequest();
            CustomerResponse response = actionMap.get(request.getRequestType()).doAction(request);
            DFuture<CustomerResponse> future = signals[i].getCustomerFuture();
            if (future != null) {
                future.setResult(response);
            }else{
                System.out.println("proceed " +response);
            }
        }
    }

    public void setActionMap(Map<CustomerRequestType, CustomerAction> actionMap) {
        this.actionMap = actionMap;
    }

    @Override
    public int getBatch() {
        return batch;
    }

    public static class ProcessSignalStepFactory implements RequestSignalStep.factory<RequestSignal> {

        private int batch;

        @Override
        public RequestSignalStep<RequestSignal> create(Shard shard) {

            ProcessSignalStep step = new ProcessSignalStep();
            if (this.batch != 0) step.batch = this.batch;
            step.shard = shard;

            return step;
        }

        public void setBatch(int batch) {
            this.batch = batch;
        }
    }
}
