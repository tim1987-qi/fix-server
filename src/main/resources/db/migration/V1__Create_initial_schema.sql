-- Create sessions table
CREATE TABLE sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL UNIQUE,
    sender_comp_id VARCHAR(100) NOT NULL,
    target_comp_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    incoming_sequence_number INTEGER NOT NULL DEFAULT 1,
    outgoing_sequence_number INTEGER NOT NULL DEFAULT 1,
    last_heartbeat TIMESTAMP,
    session_start_time TIMESTAMP,
    heartbeat_interval INTEGER DEFAULT 30,
    last_error VARCHAR(500),
    total_messages_received BIGINT NOT NULL DEFAULT 0,
    total_messages_sent BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMP
);

-- Create indexes for sessions table
CREATE INDEX idx_session_id ON sessions(session_id);
CREATE INDEX idx_sender_target ON sessions(sender_comp_id, target_comp_id);
CREATE INDEX idx_status ON sessions(status);

-- Create messages table
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    sequence_number INTEGER NOT NULL,
    direction VARCHAR(20) NOT NULL,
    message_type VARCHAR(10) NOT NULL,
    sender_comp_id VARCHAR(100) NOT NULL,
    target_comp_id VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    raw_message TEXT NOT NULL,
    client_ip_address VARCHAR(50),
    error_message VARCHAR(500),
    processed BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMP
);

-- Create indexes for messages table
CREATE INDEX idx_session_seq ON messages(session_id, sequence_number, direction);
CREATE INDEX idx_session_time ON messages(session_id, timestamp);
CREATE INDEX idx_timestamp ON messages(timestamp);

-- Create audit_records table
CREATE TABLE audit_records (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_type VARCHAR(50) NOT NULL,
    message_type VARCHAR(10),
    raw_message TEXT,
    direction VARCHAR(20),
    client_ip_address VARCHAR(50),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for audit_records table
CREATE INDEX idx_audit_session ON audit_records(session_id);
CREATE INDEX idx_audit_timestamp ON audit_records(timestamp);
CREATE INDEX idx_audit_event_type ON audit_records(event_type);

-- Create audit_additional_data table for storing key-value pairs
CREATE TABLE audit_additional_data (
    audit_record_id BIGINT NOT NULL,
    data_key VARCHAR(255) NOT NULL,
    data_value VARCHAR(1000),
    PRIMARY KEY (audit_record_id, data_key),
    FOREIGN KEY (audit_record_id) REFERENCES audit_records(id) ON DELETE CASCADE
);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_sessions_updated_at 
    BEFORE UPDATE ON sessions 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();