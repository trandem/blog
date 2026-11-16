package demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import demo.Entities.Country;
import demo.Entities.Customer;
import demo.Entities.Order;
import demo.Entities.TCustomer;
import demo.Entities.TOrder;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class Main {

    private static final Logger HIBERNATE_LOG = Logger.getLogger("org.hibernate");
    private static final String URL = "jdbc:mysql://tg-mysql:3306/crm";

    public static void main(String[] args) throws Exception {
        HIBERNATE_LOG.setLevel(Level.SEVERE);

        Set<String> tables = loadTenantTables();
        System.out.println("bang co tenant_id: " + tables);

        TenantGuard guard = new TenantGuard(tables, TenantGuard.Mode.ENFORCE);
        try (SessionFactory sf = build(guard, Customer.class, Order.class, Country.class)) {
            step("[1] HQL: from Customer, quen dieu kien tenant");
            run(sf, s -> names(s.createQuery("from Customer", Customer.class).list()));

            step("[2] HQL: from Customer c where c.tenantId = :t");
            run(sf, s -> names(s.createQuery("from Customer c where c.tenantId = :t", Customer.class)
                    .setParameter("t", 1L).list()));

            step("[3] session.find(Customer.class, 3L)");
            run(sf, s -> "doc duoc: " + s.find(Customer.class, 3L).name);

            step("[4] HQL update theo id");
            run(sf, s -> {
                s.beginTransaction();
                try {
                    return "updated " + s.createMutationQuery("update Customer set name = :n where id = :id")
                            .setParameter("n", "Doi ten").setParameter("id", 3L).executeUpdate();
                } finally {
                    s.getTransaction().rollback();
                }
            });

            step("[5] native query: WHERE tenant_id = ? OR 1 = 1");
            run(sf, s -> names(s.createNativeQuery("select * from customers where tenant_id = :t or 1 = 1",
                    Customer.class).setParameter("t", 1L).list()));

            step("[6] HQL LEFT JOIN dat dieu kien tenant nham bang");
            run(sf, s -> rows(s.createQuery(
                    "select o.id, o.amount, c.name from Order o left join o.customer c on c.tenantId = :t",
                    Object[].class).setParameter("t", 1L).list()));

            step("[7] HQL JOIN da them o.tenantId = :t");
            run(sf, s -> rows(s.createQuery(
                    "select o.id, o.amount, c.name from Order o left join o.customer c on c.tenantId = :t "
                            + "where o.tenantId = :t", Object[].class).setParameter("t", 1L).list()));

            step("[8] lazy load: order.customer.name");
            run(sf, s -> {
                Order o = s.createQuery("from Order o where o.tenantId = :t and o.id = 1", Order.class)
                        .setParameter("t", 1L).getSingleResult();
                return "customer cua order 1: " + o.customer.getName();
            });

            step("[9] bang dung chung countries");
            run(sf, s -> "so country: " + s.createQuery("from Country", Country.class).list().size());

            step("[10] withoutTenantCheck cho job xuyen tenant");
            run(sf, s -> TenantGuard.withoutTenantCheck(
                    () -> names(s.createQuery("from Customer", Customer.class).list())));
        }

        try (SessionFactory sf = build(guard, TCustomer.class, TOrder.class)) {
            step("[11] @TenantId: from TCustomer, session cua tenant 1");
            runTenant(sf, s -> tnames(s.createQuery("from TCustomer", TCustomer.class).list()));

            System.setProperty("trace.sql", "true");
            step("[12] @TenantId: session.find(TCustomer.class, 3L)");
            runTenant(sf, s -> {
                TCustomer c = s.find(TCustomer.class, 3L);
                return c == null ? "find tra ve null" : "doc duoc: " + c.name;
            });

            step("[13] @TenantId: lazy load order.customer.name");
            runTenant(sf, s -> {
                TOrder o = s.createQuery("from TOrder o where o.id = 1", TOrder.class).getSingleResult();
                return "customer cua order 1: " + o.customer.getName();
            });

            System.setProperty("trace.sql", "false");
            step("[14] @TenantId: native query khong co tenant");
            runTenant(sf, s -> "so dong: " + s.createNativeQuery("select * from customers", TCustomer.class)
                    .list().size());
        }

        TenantGuard logOnly = new TenantGuard(tables, TenantGuard.Mode.LOG_ONLY);
        try (SessionFactory sf = build(logOnly, Customer.class, Order.class, Country.class)) {
            step("[15] che do LOG_ONLY");
            run(sf, s -> names(s.createQuery("from Customer", Customer.class).list()));
        }
    }

    private static SessionFactory build(TenantGuard guard, Class<?>... entities) {
        Configuration cfg = new Configuration()
                .setProperty("hibernate.connection.url", URL)
                .setProperty("hibernate.connection.username", "root")
                .setProperty("hibernate.connection.password", "pw");
        for (Class<?> e : entities) {
            cfg.addAnnotatedClass(e);
        }
        // Spring Boot: đăng ký qua HibernatePropertiesCustomizer, key "hibernate.session_factory.statement_inspector"
        cfg.setStatementInspector(new TenantGuardInspector(guard));
        return cfg.buildSessionFactory();
    }

    private interface Action {
        String apply(Session s);
    }

    private static void run(SessionFactory sf, Action action) {
        exec(() -> sf.openSession(), action);
    }

    private static void runTenant(SessionFactory sf, Action action) {
        exec(() -> sf.withOptions().tenantIdentifier(1L).openSession(), action);
    }

    private static void exec(Supplier<Session> open, Action action) {
        try (Session s = open.get()) {
            System.out.println(action.apply(s));
        } catch (RuntimeException e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String wrap = root == e ? "" : " (boc trong " + e.getClass().getSimpleName() + ")";
            System.out.println("ERROR: " + root.getMessage() + wrap);
        }
    }

    private static String names(List<Customer> cs) {
        StringBuilder sb = new StringBuilder();
        for (Customer c : cs) {
            sb.append("tenant ").append(c.tenantId).append(" | ").append(c.name).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private static String tnames(List<TCustomer> cs) {
        StringBuilder sb = new StringBuilder();
        for (TCustomer c : cs) {
            sb.append("tenant ").append(c.tenantId).append(" | ").append(c.name).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private static String rows(List<Object[]> rs) {
        StringBuilder sb = new StringBuilder();
        for (Object[] r : rs) {
            sb.append("order ").append(r[0]).append(" | amount ").append(r[1])
                    .append(" | customer ").append(r[2]).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private static Set<String> loadTenantTables() throws Exception {
        Set<String> out = new HashSet<>();
        try (Connection c = DriverManager.getConnection(URL, "root", "pw");
             ResultSet rs = c.createStatement().executeQuery(
                     "SELECT table_name FROM information_schema.columns "
                             + "WHERE table_schema = DATABASE() AND column_name = 'tenant_id'")) {
            while (rs.next()) {
                out.add(rs.getString(1));
            }
        }
        return out;
    }

    private static void step(String s) {
        System.out.println("\n=== " + s);
    }
}
