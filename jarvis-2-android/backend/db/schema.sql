CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE IF NOT EXISTS jarvis_memory (
  id uuid PRIMARY KEY,
  text text NOT NULL,
  embedding vector(1536),
  metadata jsonb NOT NULL DEFAULT '{}',
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS jarvis_memory_embedding_idx ON jarvis_memory USING hnsw (embedding vector_cosine_ops);
CREATE TABLE IF NOT EXISTS jarvis_tasks (id uuid PRIMARY KEY, title text NOT NULL, status text NOT NULL, priority text NOT NULL, created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE IF NOT EXISTS jarvis_approvals (id uuid PRIMARY KEY, action text NOT NULL, status text NOT NULL, created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE IF NOT EXISTS jarvis_audit (id bigserial PRIMARY KEY, trace_id uuid NOT NULL, type text NOT NULL, payload jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX IF NOT EXISTS jarvis_audit_created_idx ON jarvis_audit(created_at DESC);
