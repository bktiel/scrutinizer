ALTER TABLE posture_run ADD COLUMN review_status VARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE posture_run ADD COLUMN reviewer_notes TEXT;
ALTER TABLE posture_run ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_posture_run_review_status ON posture_run (review_status);
