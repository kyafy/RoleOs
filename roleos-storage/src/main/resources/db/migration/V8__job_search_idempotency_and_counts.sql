-- Feature 002 convergence: persist start/resume commands and observable search totals.
ALTER TABLE job_search
    ADD COLUMN start_command_id VARCHAR(128),
    ADD COLUMN resume_command_id VARCHAR(128),
    ADD COLUMN discovered_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN normalized_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN rejected_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN ranked_count INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_job_search_counts_nonnegative CHECK (
        discovered_count >= 0 AND normalized_count >= 0 AND rejected_count >= 0 AND ranked_count >= 0
    );

CREATE UNIQUE INDEX uq_job_search_start_command
    ON job_search(user_id, start_command_id) WHERE start_command_id IS NOT NULL;
CREATE UNIQUE INDEX uq_job_search_resume_command
    ON job_search(user_id, resume_command_id) WHERE resume_command_id IS NOT NULL;
