package demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;

/** Chặn câu SQL chạm vào bảng có cột tenant_id mà thiếu điều kiện tenant. */
public final class TenantGuard {

    public enum Mode { ENFORCE, LOG_ONLY }

    private static final ThreadLocal<Boolean> BYPASS = ThreadLocal.withInitial(() -> false);

    private final Set<String> tenantTables;
    private final Mode mode;
    // chuỗi SQL -> lỗi (rỗng là hợp lệ); production nên dùng cache có giới hạn (Caffeine)
    private final Map<String, Optional<String>> cache = new ConcurrentHashMap<>();

    public TenantGuard(Set<String> tenantTables, Mode mode) {
        this.tenantTables = tenantTables.stream().map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        this.mode = mode;
    }

    /** Chạy action mà không kiểm tra tenant (job admin, migration, báo cáo xuyên tenant). */
    public static <T> T withoutTenantCheck(Supplier<T> action) {
        BYPASS.set(true);
        try {
            return action.get();
        } finally {
            BYPASS.set(false);
        }
    }

    public void check(String sql) {
        if (BYPASS.get()) {
            System.out.println("tenant guard: bypass: " + sql);
            return;
        }
        Optional<String> violation = cache.computeIfAbsent(sql, this::findViolation);
        if (violation.isEmpty()) {
            return;
        }
        if (mode == Mode.LOG_ONLY) {
            System.out.println("tenant guard (log only): " + violation.get());
            return;
        }
        throw new TenantGuardException(violation.get());
    }

    private Optional<String> findViolation(String sql) {
        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            return Optional.of("cannot parse SQL | sql: " + sql);
        }
        try {
            // TablesNamesFinder đi qua toàn bộ cây cú pháp, kể cả subquery ở FROM, WHERE, SELECT;
            // mỗi PlainSelect/UPDATE/DELETE/INSERT gặp trên đường đi được kiểm tra như một scope riêng.
            new ScopeVisitor().getTables(statement);
            return Optional.empty();
        } catch (Violation v) {
            return Optional.of(v.getMessage() + " | sql: " + sql);
        }
    }

    private final class ScopeVisitor extends TablesNamesFinder<Void> {
        @Override
        public <S> Void visit(PlainSelect select, S context) {
            List<TableRef> tables = new ArrayList<>();
            List<Expression> conds = new ArrayList<>();
            addFromItem(select.getFromItem(), tables);
            addJoins(select.getJoins(), tables, conds);
            flattenAnd(select.getWhere(), conds);
            requireTenantFilter(tables, conds);
            return super.visit(select, context);
        }

        @Override
        public <S> Void visit(Update update, S context) {
            List<TableRef> tables = new ArrayList<>();
            List<Expression> conds = new ArrayList<>();
            addFromItem(update.getTable(), tables);
            addJoins(update.getStartJoins(), tables, conds);
            addJoins(update.getJoins(), tables, conds);
            flattenAnd(update.getWhere(), conds);
            requireTenantFilter(tables, conds);
            return super.visit(update, context);
        }

        @Override
        public <S> Void visit(Delete delete, S context) {
            List<TableRef> tables = new ArrayList<>();
            List<Expression> conds = new ArrayList<>();
            addFromItem(delete.getTable(), tables);
            addJoins(delete.getJoins(), tables, conds);
            flattenAnd(delete.getWhere(), conds);
            requireTenantFilter(tables, conds);
            return super.visit(delete, context);
        }

        @Override
        public <S> Void visit(Insert insert, S context) {
            String table = clean(insert.getTable().getName());
            if (tenantTables.contains(table)) {
                boolean hasColumn = insert.getColumns() != null && insert.getColumns().stream()
                        .anyMatch(c -> clean(c.getColumnName()).equals("tenant_id"));
                if (!hasColumn) {
                    throw new Violation("INSERT into " + table + " has no tenant_id column");
                }
            }
            return super.visit(insert, context);
        }
    }

    private record TableRef(String name, String alias) { }

    private static void addFromItem(FromItem item, List<TableRef> tables) {
        // subquery trong FROM là một PlainSelect riêng, ScopeVisitor kiểm tra khi đi tới
        if (item instanceof Table t) {
            String name = clean(t.getName());
            String alias = t.getAlias() != null ? clean(t.getAlias().getName()) : name;
            tables.add(new TableRef(name, alias));
        }
    }

    private static void addJoins(List<Join> joins, List<TableRef> tables, List<Expression> conds) {
        if (joins == null) {
            return;
        }
        for (Join join : joins) {
            addFromItem(join.getFromItem(), tables);
            for (Expression on : join.getOnExpressions()) {
                flattenAnd(on, conds);
            }
        }
    }

    /** Mỗi bảng có tenant_id phải có "alias.tenant_id = ?" trong chuỗi AND ngoài cùng của WHERE hoặc ON. */
    private void requireTenantFilter(List<TableRef> tables, List<Expression> conds) {
        List<TableRef> tenantRefs = tables.stream().filter(t -> tenantTables.contains(t.name())).toList();
        for (TableRef t : tenantRefs) {
            boolean ok = conds.stream().anyMatch(c -> isTenantFilter(c, t, tenantRefs.size() == 1));
            if (!ok) {
                throw new Violation("table " + t.name() + " (alias " + t.alias() + ") has no "
                        + t.alias() + ".tenant_id = ? in top-level AND");
            }
        }
    }

    /** Tách "a AND (b AND c)" thành [a, b, c]. Vế OR được giữ nguyên một khối nên không bao giờ được tính. */
    private static void flattenAnd(Expression e, List<Expression> out) {
        if (e == null) {
            return;
        }
        if (e instanceof AndExpression and) {
            flattenAnd(and.getLeftExpression(), out);
            flattenAnd(and.getRightExpression(), out);
        } else if (e instanceof ParenthesedExpressionList<?> p && p.size() == 1) {
            flattenAnd(p.get(0), out);
        } else {
            out.add(e);
        }
    }

    private static boolean isTenantFilter(Expression e, TableRef t, boolean onlyTenantTable) {
        if (!(e instanceof EqualsTo eq)) {
            return false;
        }
        return (isTenantColumn(eq.getLeftExpression(), t, onlyTenantTable) && isValue(eq.getRightExpression()))
                || (isTenantColumn(eq.getRightExpression(), t, onlyTenantTable) && isValue(eq.getLeftExpression()));
    }

    private static boolean isTenantColumn(Expression e, TableRef t, boolean onlyTenantTable) {
        if (!(e instanceof Column c) || !clean(c.getColumnName()).equals("tenant_id")) {
            return false;
        }
        String q = c.getTable() != null && c.getTable().getName() != null ? clean(c.getTable().getName()) : "";
        return q.equals(t.alias()) || (q.isEmpty() && onlyTenantTable);
    }

    /** Placeholder (?) hoặc hằng số; so với cột khác (o.tenant_id = c.tenant_id) thì không tính. */
    private static boolean isValue(Expression e) {
        return e instanceof JdbcParameter || e instanceof JdbcNamedParameter
                || e instanceof LongValue || e instanceof StringValue;
    }

    private static String clean(String identifier) {
        return identifier.replace("`", "").replace("\"", "").toLowerCase(Locale.ROOT);
    }

    private static final class Violation extends RuntimeException {
        Violation(String message) {
            super(message, null, false, false);
        }
    }

    public static final class TenantGuardException extends RuntimeException {
        TenantGuardException(String message) {
            super("tenant guard: " + message);
        }
    }
}
