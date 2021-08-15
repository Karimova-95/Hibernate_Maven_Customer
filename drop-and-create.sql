BEGIN;

DROP TABLE IF EXISTS  manufacturers CASCADE;
CREATE TABLE manufacturers (id serial PRIMARY KEY, title VARCHAR(255));
INSERT INTO manufacturers (title) VALUES ('Coca Cola Company'), ('Danone');

DROP TABLE IF EXISTS  products CASCADE;
CREATE TABLE products (id serial PRIMARY KEY, title VARCHAR(255), price numeric(8, 2), manufacturer_id int, FOREIGN KEY (manufacturer_id) REFERENCES manufacturers(id));
INSERT INTO products (title, manufacturer_id, price) VALUES ('Sprite', 1, 30.00), ('Fanta', 1, 40.00), ('Pepsi', 2, 1000.00);

DROP TABLE IF EXISTS  customers CASCADE;
CREATE TABLE customers (id serial PRIMARY KEY, name VARCHAR(100));
INSERT INTO customers (name) VALUES ('Bob'), ('John'), ('Max');

DROP TABLE IF EXISTS  big_items CASCADE;
CREATE TABLE big_items (id serial PRIMARY KEY, val int, junkField int, version serial);
INSERT INTO big_items (val, junkField) VALUES (10, 30);

COMMIT;
