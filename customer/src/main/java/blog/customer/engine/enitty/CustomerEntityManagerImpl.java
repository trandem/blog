package blog.customer.engine.enitty;

import blog.common.glosory.ReferenceLifeCycle;
import blog.common.transaction.element.TxnHashMap;
import blog.customer.storage.model.po.CustomerPo;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class CustomerEntityManagerImpl extends ReferenceLifeCycle implements CustomerEntityManager {
    private final TxnHashMap<Integer, CustomerPo> customerStorage = new TxnHashMap<>();

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected long doStop(long timeout, TimeUnit unit) throws Exception {
        return 0;
    }

    @Override
    public CustomerPo getCustomer(int id) {
        return customerStorage.get(id);
    }

    @Override
    public void putCustomer(CustomerPo customerPo) {
        customerStorage.put(customerPo.getId(), customerPo);
    }

    @Override
    public Iterator<CustomerPo> customerIterator() {
        return customerStorage.getValue().values().iterator();
    }
}
