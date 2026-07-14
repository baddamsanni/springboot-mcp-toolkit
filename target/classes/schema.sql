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

CREATE TABLE IF NOT EXISTS tool_invocation (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id  VARCHAR(64) NOT NULL,
    tool_name   VARCHAR(128) NOT NULL,
    input_tokens_est  BIGINT NOT NULL,
    output_tokens_est BIGINT NOT NULL,
    duration_ms BIGINT NOT NULL,
    called_at   TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tool_invocation_session
    ON tool_invocation(session_id);

CREATE INDEX IF NOT EXISTS idx_tool_invocation_called_at
    ON tool_invocation(called_at);

