CREATE TABLE posture_run (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_name VARCHAR(255) NOT NULL,
    sbom_hash VARCHAR(64) NOT NULL,
    policy_name VARCHAR(255) NOT NULL,
    policy_version VARCHAR(50) NOT NULL,
    overall_decision VARCHAR(10) NOT NULL,
    posture_score DOUBLE PRECISION NOT NULL,
    summary_json JSONB,
    run_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_posture_run_app_name ON posture_run (application_name);
CREATE INDEX idx_posture_run_timestamp ON posture_run (run_timestamp DESC);

CREATE TABLE component_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    posture_run_id UUID NOT NULL REFERENCES posture_run(id) ON DELETE CASCADE,
    component_ref VARCHAR(255) NOT NULL,
    component_name VARCHAR(500),
    component_version VARCHAR(100),
    purl VARCHAR(500),
    is_direct BOOLEAN NOT NULL DEFAULT false,
    decision VARCHAR(10) NOT NULL
);

CREATE INDEX idx_component_result_run ON component_result (posture_run_id);

CREATE TABLE finding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    component_result_id UUID NOT NULL REFERENCES component_result(id) ON DELETE CASCADE,
    rule_id VARCHAR(255) NOT NULL,
    decision VARCHAR(10) NOT NULL,
    severity VARCHAR(10) NOT NULL,
    field VARCHAR(255) NOT NULL,
    actual_value VARCHAR(500),
    expected_value VARCHAR(500),
    description TEXT,
    remediation TEXT
);

CREATE INDEX idx_finding_component ON finding (component_result_id);
CREATE INDEX idx_finding_decision ON finding (decision);
