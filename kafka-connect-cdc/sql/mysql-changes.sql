USE shop;

-- 1. INSERT mot san pham moi
INSERT INTO products (sku, name, price, stock) VALUES ('MN-04', 'Man hinh 27 inch', 4590000.00, 15);

-- 2. Doi gia va ghi event vao outbox trong CUNG mot transaction
START TRANSACTION;
UPDATE products SET price = 1190000.00 WHERE sku = 'KB-01';
INSERT INTO outbox (id, aggregatetype, aggregateid, type, payload)
VALUES (UUID(), 'product', '1', 'ProductPriceChanged',
        JSON_OBJECT('productId', 1, 'oldPrice', 1290000, 'newPrice', 1190000));
COMMIT;

-- 3. DELETE
DELETE FROM products WHERE sku = 'HP-03';
