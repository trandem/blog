package blog.customer;

import blog.common.glosory.ReferenceLifeCycle;
import blog.core.storage.Event;
import blog.customer.ha.HaManager;
import blog.customer.storage.Snapshot;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class CustomerServer extends ReferenceLifeCycle implements HaManager.Listener {
    private HaManager haManager;
    private Snapshot customerSnapshot;
    private int numRetry = 2;

    @Override
    protected void doStart() throws Exception {
        haManager.start();
        haManager.addListener(this);

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
            System.out.println("master");
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
    private void lead(){
        customerSnapshot.start();
        Iterator<Event> iter = customerSnapshot.iterate();
        while (iter.hasNext()){
            System.out.println(iter.next());
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

    public static String[] config ={"classpath:customer-server-start.xml"};
    public static void main(String[] args) {
        ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(config);
        CustomerServer service = context.getBean("customer.server",CustomerServer.class);
        service.start();
    }
}
