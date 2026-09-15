-- Rebrand showroom display names without changing identifiers or historical migrations.
UPDATE ebike_order.showrooms
SET name = regexp_replace(name, '(?i)(kinetic([[:space:]]+e-bike)?|e-?bike([[:space:]]*store)?)', 'MOTIONX', 'g')
WHERE name ~* '(kinetic|e-?bike)';