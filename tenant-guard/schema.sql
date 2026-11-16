-- MySQL 8.0. Hai bảng có tenant_id và một bảng dùng chung (countries).
CREATE TABLE customers (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT       NOT NULL,
    name      VARCHAR(100) NOT NULL,
    owner_id  BIGINT       NOT NULL,
    KEY idx_tenant (tenant_id, id)
);

CREATE TABLE orders (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    amount      BIGINT NOT NULL,
    KEY idx_tenant (tenant_id, id)
);

CREATE TABLE countries (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

INSERT INTO customers (tenant_id, name, owner_id) VALUES
    (1, 'Khach A1', 10), (1, 'Khach A2', 11),
    (2, 'Khach B1', 20), (2, 'Khach B2', 21);

INSERT INTO orders (tenant_id, customer_id, amount) VALUES
    (1, 1, 500), (1, 2, 50), (2, 3, 700), (2, 4, 70);

INSERT INTO countries (name) VALUES ('Viet Nam'), ('Singapore');
