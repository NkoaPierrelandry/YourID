ALTER TABLE products DROP CONSTRAINT IF EXISTS products_content_type_check;
ALTER TABLE products ADD CONSTRAINT products_content_type_check
    CHECK (content_type IN ('YOUTUBE','VIMEO','GDRIVE','DROPBOX','ICLOUD','URL','FILE'));

ALTER TABLE products ADD COLUMN content_file_path VARCHAR(500);
ALTER TABLE products ADD COLUMN content_file_name VARCHAR(255);