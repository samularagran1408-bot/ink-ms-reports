CREATE DATABASE analytics_ms;
USE analytics_ms;

-- Registro de eventos para auditoría y KPIs
CREATE TABLE analytics_events (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    event_type VARCHAR(50) NOT NULL,
    user_id CHAR(36),
    module VARCHAR(50) NOT NULL,
    metadata JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Configuraciones de reportes personalizados
CREATE TABLE report_configs (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    report_name VARCHAR(100) NOT NULL,
    filters JSON NOT NULL,
    owner_id CHAR(36) NOT NULL,
    last_run DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de agregación para optimización de Dashboards
CREATE TABLE daily_metrics_summary (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    summary_date DATE NOT NULL,
    metric_key VARCHAR(50) NOT NULL,
    metric_value INT NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_metric_date (summary_date, metric_key)
);