-- Make partner_id mandatory for clouds
ALTER TABLE clouds 
MODIFY COLUMN partner_id INT(11) NOT NULL;

-- Add foreign key constraint if not exists
ALTER TABLE clouds
ADD CONSTRAINT fk_cloud_partner FOREIGN KEY (partner_id) REFERENCES partners(id);