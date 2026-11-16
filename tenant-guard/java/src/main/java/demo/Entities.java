package demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

/** Hai bộ entity cùng map vào một schema: bộ thường, và bộ có @TenantId của Hibernate. */
public final class Entities {

    private Entities() { }

    @Entity(name = "Customer")
    @Table(name = "customers")
    public static class Customer {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(name = "tenant_id") public Long tenantId;
        public String name;
        @Column(name = "owner_id") public Long ownerId;
        public String getName() { return name; }
    }

    @Entity(name = "Order")
    @Table(name = "orders")
    public static class Order {
        @Id public Long id;
        @Column(name = "tenant_id") public Long tenantId;
        @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id") public Customer customer;
        public Long amount;
    }

    @Entity(name = "Country")
    @Table(name = "countries")
    public static class Country {
        @Id public Long id;
        public String name;
    }

    @Entity(name = "TCustomer")
    @Table(name = "customers")
    public static class TCustomer {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @TenantId @Column(name = "tenant_id") public Long tenantId;
        public String name;
        @Column(name = "owner_id") public Long ownerId;
        public String getName() { return name; }
    }

    @Entity(name = "TOrder")
    @Table(name = "orders")
    public static class TOrder {
        @Id public Long id;
        @TenantId @Column(name = "tenant_id") public Long tenantId;
        @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id") public TCustomer customer;
        public Long amount;
    }
}
