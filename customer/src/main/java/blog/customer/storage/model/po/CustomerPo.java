package blog.customer.storage.model.po;

import blog.customer.storage.model.glosory.CustomerStatus;

import org.apache.ibatis.type.Alias;

import javax.persistence.*;

@Table(name = "customer")
@Entity
@Alias("Customer")
public class CustomerPo {

    public CustomerPo() {
    }

    @Id
    @Column(columnDefinition = "ID")
    private Integer id;

    @Column(columnDefinition = "CUSTOMER_NAME")
    private String customerName;

    @Column(columnDefinition = "AGE")
    private Short age;

    @Column(columnDefinition = "STATUS")
    private CustomerStatus status;

    @Column(columnDefinition = "VERSION")
    @Version
    private Integer version;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String name) {
        this.customerName = name;
    }

    public Short getAge() {
        return age;
    }

    public void setAge(Short age) {
        this.age = age;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }


    @Override
    public String toString() {
        return "CustomerPo{" +
                "id=" + id +
                ", customerName='" + customerName + '\'' +
                ", age=" + age +
                ", status=" + status +
                ", version=" + version +
                '}';
    }
}
