CREATE TABLE customer (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(200)
);

CREATE TABLE order_record (
  id BIGINT PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

