package blog.customer;

import blog.customer.storage.model.po.CustomerPo;
import blog.customer.storage.service.CustomerService;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test {

    private Map<Integer, Dog> data;


    public static class TestFactory {
        private Map<Integer, Dog> config;

        public Test create() {
            Test t = new Test();
            t.data = new HashMap<>(config);
            return t;
        }

        public void setConfig(Map<Integer, Dog> config) {
            this.config = config;
        }
    }

    public static void main(String[] args) {
//        final String[] config = new String[]{"customer-orm.xml"};
//        ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(config);
//        CustomerService service = context.getBean("customer.service", CustomerService.class);
//
////        List<CustomerPo> customerPoList  =  service.findByStatus(CustomerStatus.ENABLE);
//
//        Integer customerId = null;
//
//        while (true) {
//            List<CustomerPo> customerPoList = service.iterateCustomer(customerId, false, 1);
//            if (customerPoList.isEmpty()) break;
//            for (CustomerPo po : customerPoList) {
//                System.out.println(po);
//                customerId = po.getId();
//                po.setAge((short) 44);
//                po.setVersion(po.getVersion() + 1);
//                service.update(po);
//            }
//
//        }

        final String[] config = new String[]{"test-init.xml"};
        ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(config);
        Test.TestFactory factory = context.getBean("testFatory",  Test.TestFactory.class);

        Test t1 = factory.create();
        Test t2 = factory.create();
        System.out.println(t1.data);
        System.out.println(t2.data);

        t2.data.get(3).setAge(0);

        System.out.println(t1.data);

        //List<CustomerPo> customerPoList = service.getAll();

        //System.out.println(customerPoList);
    }
}
