package blog.customer;

import blog.customer.storage.model.po.CustomerPo;
import blog.customer.storage.service.CustomerService;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

public class Test {
    public static void main(String[] args) {
        final String[] config = new String[]{"customer-orm.xml"};
        ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(config);
        CustomerService service = context.getBean("customer.service",CustomerService.class);

//        List<CustomerPo> customerPoList  =  service.findByStatus(CustomerStatus.ENABLE);

        Integer customerId = null;

        while (true){
            List<CustomerPo> customerPoList  =  service.iterateCustomer(customerId,false,1);
            if (customerPoList.isEmpty()) break;
            for (CustomerPo po : customerPoList){
                System.out.println(po);
                customerId = po.getId();
                po.setAge((short)44);
                po.setVersion(po.getVersion()+1);
                service.update(po);
            }

        }

        //List<CustomerPo> customerPoList = service.getAll();

//        System.out.println(customerPoList);
    }
}
