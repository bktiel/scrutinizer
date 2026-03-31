CREATE TABLE policy (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,
    description TEXT,
    policy_yaml TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_policy_name ON policy (name);

CREATE TABLE policy_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL REFERENCES policy(id) ON DELETE CASCADE,
    policy_yaml TEXT NOT NULL,
    changed_by VARCHAR(255),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_policy_history_policy ON policy_history (policy_id);
CREATE INDEX idx_policy_history_changed_at ON policy_history (changed_at DESC);

ALTER TABLE posture_run ADD COLUMN policy_id UUID REFERENCES policy(id);
CREATE INDEX idx_posture_run_policy ON posture_run (policy_id);
