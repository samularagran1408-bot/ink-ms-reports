-- Índices recomendados para producción (sección 9.10 del manual)
-- Ejecutar manualmente si la BD ya existía antes de ddl-auto update

USE analytics_ms;

CREATE INDEX IF NOT EXISTS idx_events_type_date
ON analytics_events(event_type, created_at);

CREATE INDEX IF NOT EXISTS idx_metrics_key_date
ON daily_metrics_summary(metric_key, summary_date);
