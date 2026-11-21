-- User cho connector: doc binlog nhu mot replica (REPLICATION SLAVE, REPLICATION CLIENT),
-- SELECT de snapshot, RELOAD + LOCK TABLES de khoa luc snapshot, SHOW DATABASES de liet ke schema.
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

-- Bang outbox theo ten cot mac dinh cua Outbox Event Router.
CREATE TABLE outbox (
  id            CHAR(36)     NOT NULL PRIMARY KEY,
  aggregatetype VARCHAR(64)  NOT NULL,
  aggregateid   VARCHAR(64)  NOT NULL,
  type          VARCHAR(64)  NOT NULL,
  payload       JSON         NOT NULL,
  created_at    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

INSERT INTO products (sku, name, price, stock) VALUES
  ('KB-01', 'Ban phim co', 1290000.00, 50),
  ('MS-02', 'Chuot khong day', 450000.00, 120),
  ('HP-03', 'Tai nghe', 890000.00, 30);
