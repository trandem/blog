package demo;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Hibernate gọi inspect() với câu SQL cuối cùng trước khi prepare: HQL, Criteria, native query, lazy load. */
public final class TenantGuardInspector implements StatementInspector {

    private final TenantGuard guard;

    public TenantGuardInspector(TenantGuard guard) {
        this.guard = guard;
    }

    @Override
    public String inspect(String sql) {
        if (Boolean.getBoolean("trace.sql")) {
            System.out.println("sql: " + sql);
        }
        guard.check(sql); // vi phạm thì throw, câu lệnh không được gửi xuống database
        return sql;
    }
}
