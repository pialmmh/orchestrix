-- Update resource groups to set only the first one (Cloud Computing) as active
UPDATE resource_groups 
SET is_active = FALSE 
WHERE name != 'cloud_computing';

-- Ensure Cloud Computing is active
UPDATE resource_groups 
SET is_active = TRUE 
WHERE name = 'cloud_computing';