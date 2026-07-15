UPDATE hdbhms.maintenance_costs
SET cost_responsibility = 'OWNER'
WHERE cost_responsibility = 'PROPERTY';
