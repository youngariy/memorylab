-- Add parent_id column to comment table for reply functionality
ALTER TABLE comment ADD COLUMN parent_id BIGINT NULL;

-- Add foreign key constraint
ALTER TABLE comment ADD CONSTRAINT fk_comment_parent
    FOREIGN KEY (parent_id) REFERENCES comment(id) ON DELETE CASCADE;

-- Add index for better query performance
CREATE INDEX idx_comment_parent_id ON comment(parent_id);
