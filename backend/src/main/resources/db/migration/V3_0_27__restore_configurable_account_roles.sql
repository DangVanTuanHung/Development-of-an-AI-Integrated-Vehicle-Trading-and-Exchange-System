-- The two-account staff setup is local test data, not a product restriction.
-- Supersedes V3_0_26, which may already be applied on a local database.
DROP TRIGGER IF EXISTS enforce_canonical_staff ON ebike_auth.user_roles;
DROP FUNCTION IF EXISTS ebike_auth.enforce_canonical_staff();
