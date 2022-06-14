package blog.customer;

import blog.common.concurrent.DFuture;
import blog.common.concurrent.ListenableFuture;
import blog.common.glosory.ReferenceLifeCycle;
import blog.core.storage.Event;
import blog.core.storage.ResponseEvent;
import blog.customer.engine.pipeline.PipeLine;
import blog.customer.engine.pipeline.request.CustomerRequest;
import blog.customer.engine.pipeline.request.CustomerSharableRequest;
import blog.customer.engine.pipeline.request.impl.QueryCustomerRequest;
import blog.customer.engine.pipeline.request.impl.ReplicateCustomerRequest;
import blog.customer.engine.pipeline.response.CustomerResponse;
import blog.customer.engine.signal.CustomerShardManager;
import blog.customer.engine.signal.CustomerShardSignal;
import blog.customer.engine.signal.RequestSignal;
import blog.customer.engine.signal.Signal;
import blog.customer.ha.HaManager;
import blog.customer.storage.Snapshot;
import blog.customer.storage.model.po.CustomerPo;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public final class CustomerServer extends ReferenceLifeCycle implements HaManager.Listener {
    private HaManager haManager;
    private Snapshot customerSnapshot;
    private int numRetry = 2;

    private PipeLine<RequestSignal> requestPipeline;
    private CustomerShardManager customerShardManager;

    @Override
    protected void doStart() throws Exception {
        haManager.start();
        haManager.addListener(this);
        requestPipeline.start();
        int retry = 0;
        while (retry < numRetry) {
            HaManager.Status status = haManager.getStatus();
            if (status == HaManager.Status.MASTER || status == HaManager.Status.SLAVE) {
                break;
            }
            retry++;
            new Object().wait(100);
        }


        if (haManager.getStatus() == HaManager.Status.MASTER) {
            lead();
        } else if (haManager.getStatus() == HaManager.Status.SLAVE) {
            System.out.println("slave");
        } else {
            throw new RuntimeException("voting master get exception, please check");
        }
    }

    @Override
    protected long doStop(long timeout, TimeUnit unit) throws Exception {
        haManager.delListener(this);
        return 0;
    }

    private void lead() {
        customerSnapshot.start();
        Iterator<Event> iter = customerSnapshot.iterate();
        while (iter.hasNext()) {
            Event event = iter.next();

            if (event instanceof ResponseEvent) {
                ResponseEvent<CustomerPo> responseEvent = (ResponseEvent<CustomerPo>) event;
                CustomerPo customerPo = responseEvent.getResponse();
                ReplicateCustomerRequest request = new ReplicateCustomerRequest(customerPo.getId(), 1, customerPo);
                submit(request, null);
            }
        }
    }

    public void submit(CustomerRequest request, DFuture<CustomerResponse> future) {
        if (request instanceof CustomerSharableRequest) {
            RequestSignal signal = new CustomerShardSignal(customerShardManager.getShards(((CustomerSharableRequest) request).getCustomerId()), request);
            signal.setCustomerFuture(future);
            requestPipeline.submit(signal);
        } else {
            throw new RuntimeException("not support yet");
        }
    }


    @Override
    public void onMaster() {

    }

    @Override
    public void onSlave(Throwable th) {

    }

    public void setHaManager(HaManager haManager) {
        this.haManager = haManager;
    }

    public void setCustomerSnapshot(Snapshot customerSnapshot) {
        this.customerSnapshot = customerSnapshot;
    }

    public void setNumRetry(int numRetry) {
        this.numRetry = numRetry;
    }

    public void setRequestPipeline(PipeLine<RequestSignal> requestPipeline) {
        this.requestPipeline = requestPipeline;
    }

    public void setCustomerShardManager(CustomerShardManager customerShardManager) {
        this.customerShardManager = customerShardManager;
    }

    public static void setConfig(String[] config) {
        CustomerServer.config = config;
    }

    public static String[] config = {"classpath:customer-server-start.xml"};

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(config);
        CustomerServer service = context.getBean("customer.server", CustomerServer.class);
        service.start();

        QueryCustomerRequest request = new QueryCustomerRequest(4,10);
        ListenableFuture<CustomerResponse> future = new ListenableFuture<>();
        future.setListener((response) -> {
            try {
                System.out.println("get respone " +response.get());
            }catch (Exception e){
                System.out.println("lol");
            }

        });

        service.submit(request,future);


    }
}
