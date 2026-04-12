-- Project table: registered applications tracked by Scrutinizer
CREATE TABLE project (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    repository_url VARCHAR(500),
    gitlab_project_id VARCHAR(100),
    default_branch VARCHAR(100) DEFAULT 'main',
    policy_id UUID REFERENCES policy(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_project_name ON project(name);

-- Link posture_run to project
ALTER TABLE posture_run ADD COLUMN project_id UUID REFERENCES project(id) ON DELETE SET NULL;
CREATE INDEX idx_posture_run_project ON posture_run(project_id);

-- Policy exception table: temporary exceptions for rejected packages/rules
CREATE TABLE policy_exception (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID REFERENCES project(id) ON DELETE CASCADE,
    policy_id UUID REFERENCES policy(id) ON DELETE CASCADE,
    rule_id VARCHAR(255),
    package_name VARCHAR(500),
    package_version VARCHAR(100),
    justification TEXT NOT NULL,
    created_by VARCHAR(255) NOT NULL DEFAULT 'system',
    approved_by VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    scope VARCHAR(20) NOT NULL DEFAULT 'PROJECT',
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_exception_project ON policy_exception(project_id);
CREATE INDEX idx_exception_status ON policy_exception(status);
CREATE INDEX idx_exception_package ON policy_exception(package_name);
