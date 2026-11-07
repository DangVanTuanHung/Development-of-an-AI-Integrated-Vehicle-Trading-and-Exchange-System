-- Validate every grant, including SQL writes outside the Admin API.
CREATE FUNCTION ebike_auth.enforce_canonical_staff() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE username TEXT; role_name TEXT;
BEGIN
 SELECT u.username INTO username FROM ebike_auth.users u WHERE u.id=NEW.user_id;
 SELECT r.name INTO role_name FROM ebike_auth.roles r WHERE r.id=NEW.role_id;
 IF (username='manager.user' AND role_name<>'MANAGER')
 OR (username='admin.user' AND role_name<>'ADMIN')
 OR (username NOT IN ('manager.user','admin.user') AND role_name<>'CUSTOMER') THEN
  RAISE EXCEPTION 'Chỉ manager.user là Manager, admin.user là Admin; tài khoản khác là Customer' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER enforce_canonical_staff BEFORE INSERT OR UPDATE ON ebike_auth.user_roles
FOR EACH ROW EXECUTE FUNCTION ebike_auth.enforce_canonical_staff();
