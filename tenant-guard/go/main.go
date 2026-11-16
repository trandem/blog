package main

import (
	"context"
	"database/sql"
	"fmt"
	"log"
	"os"

	"github.com/go-sql-driver/mysql"
	"github.com/qustavo/sqlhooks/v2"
	gormmysql "gorm.io/driver/mysql"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"

	"tenantguard-demo/tenantguard"
)

type Customer struct {
	ID       int64
	TenantID int64
	Name     string
	OwnerID  int64
	Orders   []Order
}

type Order struct {
	ID         int64
	TenantID   int64
	CustomerID int64
	Amount     int64
}

type Country struct {
	ID   int64
	Name string
}

func main() {
	log.SetFlags(0)
	log.SetOutput(os.Stdout)
	dsn := "root:pw@tcp(tg-mysql:3306)/crm"

	// Danh sách bảng có tenant_id đọc bằng connection thường, chưa qua hook.
	raw, err := sql.Open("mysql", dsn)
	must(err)
	tables, err := tenantguard.LoadTenantTables(raw)
	must(err)
	fmt.Println("bang co tenant_id:", tables)

	// Bọc driver MySQL bằng guard, rồi cho GORM dùng driver đã bọc.
	guard := tenantguard.New(tables, tenantguard.Enforce)
	sql.Register("mysql-tenant-guard", sqlhooks.Wrap(&mysql.MySQLDriver{}, guard))
	db, err := gorm.Open(gormmysql.New(gormmysql.Config{DriverName: "mysql-tenant-guard", DSN: dsn}),
		&gorm.Config{Logger: logger.Discard})
	must(err)

	ctx := context.Background()
	var cs []Customer

	step("[1] db.Find(&cs), quen dieu kien tenant")
	show(db.Find(&cs).Error, func() { names(cs) })

	step("[2] db.Where(\"tenant_id = ?\", 1).Find(&cs)")
	cs = nil
	show(db.Where("tenant_id = ?", 1).Find(&cs).Error, func() { names(cs) })

	step("[3] db.First(&c, 3): doc khach hang theo id, khong co tenant")
	var c Customer
	show(db.First(&c, 3).Error, func() { fmt.Println("doc duoc:", c.Name) })

	step("[4] db.Model(&Customer{}).Where(\"id = ?\", 3).Update(...)")
	show(db.Model(&Customer{}).Where("id = ?", 3).Update("name", "Doi ten").Error, nil)

	step("[5] db.Raw: WHERE tenant_id = ? OR 1 = 1")
	cs = nil
	show(db.Raw("SELECT * FROM customers WHERE tenant_id = ? OR 1 = 1", 1).Scan(&cs).Error, func() { names(cs) })

	step("[6] LEFT JOIN dat dieu kien tenant nham bang")
	type row struct {
		ID     int64
		Amount int64
		Name   *string
	}
	var rs []row
	leftJoin := `SELECT o.id, o.amount, c.name FROM orders o
		LEFT JOIN customers c ON c.id = o.customer_id AND c.tenant_id = ?`
	show(db.Raw(leftJoin, 1).Scan(&rs).Error, nil)
	step("[6b] cung cau do, chay bang connection khong co guard")
	rows, err := raw.Query(leftJoin, 1)
	must(err)
	for rows.Next() {
		var r row
		must(rows.Scan(&r.ID, &r.Amount, &r.Name))
		n := "NULL"
		if r.Name != nil {
			n = *r.Name
		}
		fmt.Printf("order %d | amount %d | customer %s\n", r.ID, r.Amount, n)
	}
	rows.Close()

	step("[7] subquery tinh tong doanh thu khong loc tenant")
	type rev struct {
		Name  string
		Total int64
	}
	var revs []rev
	show(db.Raw(`SELECT name, (SELECT SUM(amount) FROM orders) AS total
		FROM customers WHERE tenant_id = ?`, 1).Scan(&revs).Error, nil)

	step("[8] cau JOIN da them o.tenant_id = ?")
	rs = nil
	show(db.Raw(`SELECT o.id, o.amount, c.name FROM orders o
		LEFT JOIN customers c ON c.id = o.customer_id AND c.tenant_id = ?
		WHERE o.tenant_id = ?`, 1, 1).Scan(&rs).Error, func() {
		for _, r := range rs {
			fmt.Printf("order %d | amount %d | customer %s\n", r.ID, r.Amount, *r.Name)
		}
	})

	step("[9] cau chay trong transaction")
	cs = nil
	show(db.Transaction(func(tx *gorm.DB) error { return tx.Find(&cs).Error }), nil)

	step("[10] Preload: GORM tu sinh cau doc orders")
	cs = nil
	show(db.Where("tenant_id = ?", 1).Preload("Orders").Find(&cs).Error, nil)
	step("[10b] Preload kem dieu kien tenant")
	cs = nil
	show(db.Where("tenant_id = ?", 1).Preload("Orders", "tenant_id = ?", 1).Find(&cs).Error, func() {
		for _, x := range cs {
			fmt.Printf("%s: %d order\n", x.Name, len(x.Orders))
		}
	})

	step("[11] bang dung chung countries")
	var countries []Country
	show(db.Find(&countries).Error, func() { fmt.Println("so country:", len(countries)) })

	step("[12] WithoutTenantCheck cho job xuyen tenant")
	cs = nil
	show(db.WithContext(tenantguard.WithoutTenantCheck(ctx)).Find(&cs).Error, func() { names(cs) })

	step("[13] che do LogOnly")
	sql.Register("mysql-tenant-guard-log", sqlhooks.Wrap(&mysql.MySQLDriver{}, tenantguard.New(tables, tenantguard.LogOnly)))
	dbLog, err := gorm.Open(gormmysql.New(gormmysql.Config{DriverName: "mysql-tenant-guard-log", DSN: dsn}),
		&gorm.Config{Logger: logger.Discard})
	must(err)
	cs = nil
	show(dbLog.Find(&cs).Error, func() { names(cs) })
}

func step(s string) { fmt.Println("\n=== " + s) }

func show(err error, onOK func()) {
	if err != nil {
		fmt.Println("ERROR:", err)
		return
	}
	if onOK != nil {
		onOK()
	} else {
		fmt.Println("OK")
	}
}

func names(cs []Customer) {
	for _, c := range cs {
		fmt.Printf("tenant %d | %s\n", c.TenantID, c.Name)
	}
}

func must(err error) {
	if err != nil {
		panic(err)
	}
}
