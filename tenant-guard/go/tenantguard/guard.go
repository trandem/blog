// Package tenantguard chặn câu SQL chạm vào bảng có cột tenant_id mà thiếu điều kiện tenant.
// Guard cài vào driver qua sqlhooks, nên mọi câu đi qua database/sql đều bị kiểm tra:
// câu GORM tự sinh, db.Raw, db.Exec, câu chạy trong transaction.
package tenantguard

import (
	"context"
	"database/sql"
	"fmt"
	"log"
	"strings"
	"sync"

	"github.com/pingcap/tidb/pkg/parser"
	"github.com/pingcap/tidb/pkg/parser/ast"
	"github.com/pingcap/tidb/pkg/parser/opcode"
	_ "github.com/pingcap/tidb/pkg/parser/test_driver"
)

type Mode int

const (
	Enforce Mode = iota // câu vi phạm trả về lỗi, không chạy
	LogOnly             // câu vi phạm chỉ ghi log, vẫn chạy (dùng lúc mới rollout)
)

type Guard struct {
	tenantTables map[string]bool
	mode         Mode
	cache        sync.Map  // chuỗi SQL -> error (nil là hợp lệ); production nên dùng LRU có giới hạn
	parsers      sync.Pool // parser.Parser không an toàn khi dùng chung giữa goroutine
}

func New(tenantTables []string, mode Mode) *Guard {
	g := &Guard{tenantTables: map[string]bool{}, mode: mode}
	for _, t := range tenantTables {
		g.tenantTables[strings.ToLower(t)] = true
	}
	g.parsers.New = func() any { return parser.New() }
	return g
}

// LoadTenantTables đọc danh sách bảng có cột tenant_id trong schema hiện tại.
func LoadTenantTables(db *sql.DB) ([]string, error) {
	rows, err := db.Query(`SELECT table_name FROM information_schema.columns
		WHERE table_schema = DATABASE() AND column_name = 'tenant_id'`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []string
	for rows.Next() {
		var t string
		if err := rows.Scan(&t); err != nil {
			return nil, err
		}
		out = append(out, t)
	}
	return out, rows.Err()
}

type bypassKey struct{}

// WithoutTenantCheck đánh dấu context được bỏ qua kiểm tra (job admin, migration, báo cáo xuyên tenant).
func WithoutTenantCheck(ctx context.Context) context.Context {
	return context.WithValue(ctx, bypassKey{}, true)
}

// Before và After thỏa mãn interface sqlhooks.Hooks.
func (g *Guard) Before(ctx context.Context, query string, args ...interface{}) (context.Context, error) {
	if ctx.Value(bypassKey{}) != nil {
		log.Printf("tenant guard: bypass: %s", query)
		return ctx, nil
	}
	if err := g.Check(query); err != nil {
		if g.mode == LogOnly {
			log.Printf("tenant guard (log only): %v", err)
			return ctx, nil
		}
		return ctx, err
	}
	return ctx, nil
}

func (g *Guard) After(ctx context.Context, query string, args ...interface{}) (context.Context, error) {
	return ctx, nil
}

// Check parse câu SQL một lần cho mỗi chuỗi SQL khác nhau, các lần sau lấy kết quả từ cache.
func (g *Guard) Check(query string) error {
	if v, ok := g.cache.Load(query); ok {
		if v == nil {
			return nil
		}
		return v.(error)
	}
	err := g.check(query)
	if err == nil {
		g.cache.Store(query, nil)
	} else {
		g.cache.Store(query, err)
	}
	return err
}

func (g *Guard) check(query string) error {
	p := g.parsers.Get().(*parser.Parser)
	defer g.parsers.Put(p)
	stmts, _, err := p.ParseSQL(query)
	if err != nil {
		return fmt.Errorf("tenant guard: cannot parse SQL: %w", err)
	}
	v := &visitor{g: g}
	for _, s := range stmts {
		s.Accept(v)
	}
	return v.err
}

// visitor đi qua toàn bộ cây cú pháp, nên subquery ở FROM, WHERE hay SELECT đều được kiểm tra như một scope riêng.
type visitor struct {
	g   *Guard
	err error
}

func (v *visitor) Enter(n ast.Node) (ast.Node, bool) {
	if v.err != nil {
		return n, true
	}
	switch s := n.(type) {
	case *ast.SelectStmt:
		v.err = v.g.checkScope(s.From, s.Where)
	case *ast.UpdateStmt:
		v.err = v.g.checkScope(s.TableRefs, s.Where)
	case *ast.DeleteStmt:
		v.err = v.g.checkScope(s.TableRefs, s.Where)
	case *ast.InsertStmt:
		v.err = v.g.checkInsert(s)
	}
	return n, false
}

func (v *visitor) Leave(n ast.Node) (ast.Node, bool) { return n, true }

type tableRef struct{ name, alias string }

// collect lấy các bảng trong FROM/JOIN và điều kiện ON của từng JOIN.
func collect(refs *ast.TableRefsClause) (tables []tableRef, ons []ast.ExprNode) {
	var walk func(n ast.ResultSetNode)
	walk = func(n ast.ResultSetNode) {
		switch x := n.(type) {
		case *ast.Join:
			if x.Left != nil {
				walk(x.Left)
			}
			if x.Right != nil {
				walk(x.Right)
			}
			if x.On != nil {
				ons = append(ons, x.On.Expr)
			}
		case *ast.TableSource:
			if t, ok := x.Source.(*ast.TableName); ok {
				alias := x.AsName.L
				if alias == "" {
					alias = t.Name.L
				}
				tables = append(tables, tableRef{t.Name.L, alias})
			}
		}
	}
	if refs != nil && refs.TableRefs != nil {
		walk(refs.TableRefs)
	}
	return tables, ons
}

// checkScope: mỗi bảng có tenant_id phải có "alias.tenant_id = ?" trong chuỗi AND ngoài cùng của WHERE hoặc ON.
func (g *Guard) checkScope(refs *ast.TableRefsClause, where ast.ExprNode) error {
	tables, ons := collect(refs)
	conds := flattenAnd(nil, where)
	for _, on := range ons {
		conds = flattenAnd(conds, on)
	}
	var tenantRefs []tableRef
	for _, t := range tables {
		if g.tenantTables[t.name] {
			tenantRefs = append(tenantRefs, t)
		}
	}
	for _, t := range tenantRefs {
		if !hasTenantFilter(conds, t, len(tenantRefs) == 1) {
			return fmt.Errorf("tenant guard: table %s (alias %s) has no %s.tenant_id = ? in top-level AND",
				t.name, t.alias, t.alias)
		}
	}
	return nil
}

func (g *Guard) checkInsert(s *ast.InsertStmt) error {
	tables, _ := collect(s.Table)
	for _, t := range tables {
		if !g.tenantTables[t.name] {
			continue
		}
		for _, c := range s.Columns {
			if c.Name.L == "tenant_id" {
				return nil
			}
		}
		return fmt.Errorf("tenant guard: INSERT into %s has no tenant_id column", t.name)
	}
	return nil
}

// flattenAnd tách "a AND (b AND c)" thành [a, b, c]. Vế OR được giữ nguyên một khối nên không bao giờ được tính.
func flattenAnd(out []ast.ExprNode, e ast.ExprNode) []ast.ExprNode {
	switch x := e.(type) {
	case nil:
		return out
	case *ast.ParenthesesExpr:
		return flattenAnd(out, x.Expr)
	case *ast.BinaryOperationExpr:
		if x.Op == opcode.LogicAnd {
			return flattenAnd(flattenAnd(out, x.L), x.R)
		}
	}
	return append(out, e)
}

func hasTenantFilter(conds []ast.ExprNode, t tableRef, onlyTenantTable bool) bool {
	for _, c := range conds {
		b, ok := c.(*ast.BinaryOperationExpr)
		if !ok || b.Op != opcode.EQ {
			continue
		}
		if (isTenantColumn(b.L, t, onlyTenantTable) && isValue(b.R)) ||
			(isTenantColumn(b.R, t, onlyTenantTable) && isValue(b.L)) {
			return true
		}
	}
	return false
}

func isTenantColumn(e ast.ExprNode, t tableRef, onlyTenantTable bool) bool {
	c, ok := e.(*ast.ColumnNameExpr)
	if !ok || c.Name.Name.L != "tenant_id" {
		return false
	}
	q := c.Name.Table.L
	return q == t.alias || (q == "" && onlyTenantTable)
}

// isValue nhận placeholder (?) hoặc hằng số; so với cột khác (o.tenant_id = c.tenant_id) thì không tính.
func isValue(e ast.ExprNode) bool {
	switch e.(type) {
	case ast.ParamMarkerExpr, ast.ValueExpr:
		return true
	}
	return false
}
