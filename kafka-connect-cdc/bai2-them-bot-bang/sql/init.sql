CREATE USER 'debezium'@'%' IDENTIFIED BY 'dbz';
GRANT SELECT, RELOAD, SHOW DATABASES, LOCK TABLES, REPLICATION SLAVE, REPLICATION CLIENT
  ON *.* TO 'debezium'@'%';

CREATE DATABASE shop;
USE shop;

CREATE TABLE products (
  id         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sku        VARCHAR(32)   NOT NULL UNIQUE,
  name       VARCHAR(255)  NOT NULL,
  price      DECIMAL(12,2) NOT NULL,
  stock      INT           NOT NULL,
  updated_at TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

-- Bang da co san tu truoc, luc dau connector KHONG capture.
CREATE TABLE orders (
  id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT       NOT NULL,
  qty        INT          NOT NULL,
  status     VARCHAR(16)  NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

-- Bang nhan lenh (signal) cua Debezium: ba cot id, type, data dung ten nay.
CREATE TABLE debezium_signal (
  id   VARCHAR(64) PRIMARY KEY,
  type VARCHAR(32) NOT NULL,
  data VARCHAR(2048) NULL
);
-- Incremental snapshot tren MySQL can connector ghi watermark vao bang signal.
GRANT INSERT ON shop.debezium_signal TO 'debezium'@'%';

INSERT INTO products (sku, name, price, stock) VALUES
  ('KB-01', 'Ban phim co', 1290000.00, 50),
  ('MS-02', 'Chuot khong day', 450000.00, 120),
  ('HP-03', 'Tai nghe', 890000.00, 30);

INSERT INTO orders (product_id, qty, status) VALUES
  (1, 1, 'NEW'),
  (2, 3, 'PAID');
