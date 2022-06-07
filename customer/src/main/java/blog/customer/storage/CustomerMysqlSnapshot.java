package blog.customer.storage;

import blog.common.glosory.ReferenceLifeCycle;
import blog.core.storage.Event;
import blog.core.storage.ResponseEvent;
import blog.core.storage.StartEvent;
import blog.core.storage.StopEvent;
import blog.customer.storage.model.po.CustomerPo;
import blog.customer.storage.service.CustomerService;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CustomerMysqlSnapshot extends ReferenceLifeCycle implements Snapshot{
    private CustomerService customerService;



    private int step = 1024;

    @Override
    public Iterator<Event> iterate() {
        return new CustomerIterator();
    }

    public void setStep(int step) {
        this.step = step;
    }

    public void setCustomerService(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected long doStop(long timeout, TimeUnit unit) throws Exception {
        return 0;
    }

    public class CustomerIterator implements Iterator<Event> {

        private int phase = 0;
        private Integer userId;
        private Map<Integer, CustomerPo> result = new ConcurrentHashMap<>();


        @Override
        public boolean hasNext() {
            return phase != 2;
        }

        @Override
        public Event next() {
            if (phase == 0) {
                phase++;
                return new StartEvent();
            }

            if (phase == 1) {
                if (result.isEmpty()) load();
                if (result.isEmpty()) {
                    phase = 2;
                } else {
                    for (Object id : result.keySet()) {
                        return new ResponseEvent<>(33, result.remove(id));
                    }
                }
            }

            return new StopEvent();
        }

        public void load() {
            List<CustomerPo> data = customerService.iterateCustomer(userId, false, step);
            for (CustomerPo cus : data) {
                result.put(cus.getId(), cus);
                userId = cus.getId();
            }
        }
    }

}
