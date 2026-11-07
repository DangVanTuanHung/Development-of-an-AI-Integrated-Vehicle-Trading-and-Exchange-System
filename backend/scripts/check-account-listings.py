"""Integration checks using a temporary database/container, never the application's DB.

Prerequisites: docker compose services postgres, redis and mailpit running; backend built.
Run from repository root: python backend/scripts/check-account-listings.py
No provider API keys are used. OTPs are read only for the generated @example.test users.
"""
import base64
from image_fixture import photo_fixture
import http.client
import json
import re
import secrets
import subprocess
import time
import tempfile
from pathlib import Path
import urllib.error
import urllib.request
import uuid

suffix = uuid.uuid4().hex[:10]
database = "motionx_check_" + suffix
container = "motionx-check-" + suffix
assert re.fullmatch(r"motionx_check_[a-f0-9]{10}", database)
assert re.fullmatch(r"motionx-check-[a-f0-9]{10}", container)
base = "http://127.0.0.1:18081/api/v1"
created = False
started = False
passed = 0


def docker(*args, check=True):
    result = subprocess.run(["docker", *args], capture_output=True, text=True, encoding="utf-8", errors="replace")
    if check and result.returncode:
        raise RuntimeError("Docker operation failed: " + " ".join(args[:2]) + " (details suppressed)")
    return result.stdout.strip()


def sql(statement):
    # The target database is always the uniquely named temporary database.
    return docker("exec", "ebike-postgres", "psql", "-U", "ebike_user", "-d", database, "-v", "ON_ERROR_STOP=1", "-Atc", statement)


def request(path, body=None, token=None, expected=200, method=None, raw=None, content_type=None):
    headers = {}
    if token:
        headers["Authorization"] = "Bearer " + token
    if body is not None:
        raw = json.dumps(body).encode()
        content_type = "application/json"
    if content_type:
        headers["Content-Type"] = content_type
    req = urllib.request.Request(base + path, data=raw, headers=headers, method=method or ("POST" if raw is not None else "GET"))
    try:
        with urllib.request.urlopen(req, timeout=20) as response:
            status, data = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, data = error.code, error.read()
    if status != expected:
        raise AssertionError(f"{req.method} {path}: expected {expected}, received {status}")
    return json.loads(data) if data else None


def note(message):
    global passed
    passed += 1
    print("PASS " + message, flush=True)


def otp(email, subject):
    for _ in range(30):
        with urllib.request.urlopen("http://127.0.0.1:8025/api/v1/messages?limit=100", timeout=10) as response:
            items = json.load(response)["messages"]
        for item in items:
            if subject not in item.get("Subject", "") or not any(x.get("Address") == email for x in item.get("To", [])):
                continue
            with urllib.request.urlopen("http://127.0.0.1:8025/api/v1/message/" + item["ID"], timeout=10) as response:
                text = json.load(response).get("Text", "")
            match = re.search(r"\b[0-9]{6}\b", text)
            if match:
                return match.group()
        time.sleep(.2)
    raise AssertionError("Generated test email was not received by Mailpit")


try:
    with tempfile.TemporaryDirectory(prefix="motionx-env-check-") as temp:
        empty_env=Path(temp)/".env"
        empty_env.write_text("",encoding="utf-8")
        docker("compose","--env-file",str(empty_env),"-f",str(Path(__file__).resolve().parents[1]/"docker-compose.yml"),"config","--quiet")
    note("Docker Compose accepts a completely empty .env file")
    info = json.loads(docker("inspect", "ebike-backend"))[0]
    network = next(iter(info["NetworkSettings"]["Networks"]))
    image = info["Config"]["Image"]
    docker("exec", "ebike-postgres", "createdb", "-U", "ebike_user", database)
    created = True
    args = ["run", "-d", "--name", container, "--network", network, "-p", "127.0.0.1:18081:8080"]
    env = {
        "SPRING_DATASOURCE_URL": "jdbc:postgresql://postgres:5432/" + database,
        "SPRING_DATASOURCE_USERNAME": "ebike_user", "SPRING_DATASOURCE_PASSWORD": "ebike_password_123",
        "SPRING_DATA_REDIS_HOST": "redis", "SPRING_DATA_REDIS_PASSWORD": "redis_password_123",
        "APP_JWT_SECRET": secrets.token_urlsafe(48), "APP_RATE_LIMIT_ENABLED": "false",
        "SMTP_HOST": "mailpit", "SMTP_PORT": "1025", "SMTP_AUTH": "false", "SMTP_STARTTLS_ENABLE": "false",
        "GEMINI_AI_KEY": "", "TWILIO_ACCOUNT_SID": "", "TWILIO_AUTH_TOKEN": "", "TWILIO_VERIFY_SERVICE_SID": "",
        "APP_MARKETPLACE_EXPIRY_CHECK_MS": "1000", "SPRING_JPA_SHOW_SQL": "false",
        "APP_STORAGE_S3_ENDPOINT": "http://minio:9000", "APP_STORAGE_S3_ACCESS_KEY": "marketplace_admin",
        "APP_STORAGE_S3_SECRET_KEY": "marketplace_local_password_123", "APP_STORAGE_S3_BUCKET": "vehicle-marketplace",
        "APP_STORAGE_S3_PATH_STYLE_ACCESS": "true", "APP_STORAGE_S3_INITIALIZE_BUCKET": "false",
    }
    for key, value in env.items():
        args += ["-e", key + "=" + value]
    docker(*args, image)
    started = True
    for _ in range(120):
        try:
            request("/health/ready")
            break
        except (OSError, urllib.error.URLError, http.client.HTTPException, AssertionError):
            time.sleep(1)
    else:
        raise AssertionError("Temporary backend did not become ready")
    request("/marketplace/listings")
    note("Backend boots and serves listings without external API credentials")

    def account(label):
        email = label + suffix + "@example.test"
        phone = {"owner":"0901234567","buyer":"0901234568","admincheck":"0901234569"}[label]
        user = request("/auth/register", {"username": label + suffix, "email": email, "password": "InitialTestPass123!", "firstName": label, "lastName": "Check", "phoneNumber": phone}, expected=201)
        return user, email

    owner, email = account("owner")
    buyer, _ = account("buyer")
    admin = request('/auth/login/enhanced',{'usernameOrEmail':'admin.user','password':'AdminPassword123'})
    owner_id, buyer_id, admin_id = (int(user["userId"]) for user in (owner, buyer, admin))
    request("/auth/register", {"username":"duplicate"+suffix,"email":"duplicate"+suffix+"@example.test","password":"InitialTestPass123!","phoneNumber":"+84901234567"},expected=409)
    token, buyer_token, admin_token = owner["token"], buyer["token"], admin["token"]
    availability = request("/auth/verification/availability", token=token)
    assert availability == {"emailAvailable": True, "phoneAvailable": False}
    request("/auth/verification/phone/request", {}, token, expected=503)
    note("Missing SMS configuration is optional and never marks a phone verified")

    challenge = request("/auth/verification/email/request", {}, token)
    code = otp(email, "email")
    request("/auth/verification/email/confirm", {"challengeId": challenge["challengeId"], "code": code}, buyer_token, expected=400)
    request("/auth/verification/email/confirm", {"challengeId": challenge["challengeId"], "code": code}, token, expected=204)
    assert request("/auth/profile", token=token)["emailVerifiedAt"]
    request("/auth/verification/email/confirm", {"challengeId": challenge["challengeId"], "code": code}, token, expected=400)
    assert request("/auth/profile?usernameOrEmail=" + buyer["username"], token=token)["userId"] == owner_id
    note("Email OTP binds to its owner, consumes once, and profile cannot read another account")

    reset = request("/auth/password-reset/request", {"email": email})
    reset_code = otp(email, "MOTIONX")
    # Choose the reset email by matching the code's hash in the isolated DB if the inbox lists the verification email first.
    with urllib.request.urlopen("http://127.0.0.1:8025/api/v1/messages?limit=100") as response:
        messages = json.load(response)["messages"]
    for message in messages:
        if "mật khẩu" in message.get("Subject", "").lower() and any(x.get("Address") == email for x in message.get("To", [])):
            with urllib.request.urlopen("http://127.0.0.1:8025/api/v1/message/" + message["ID"]) as response:
                reset_code = re.search(r"\b[0-9]{6}\b", json.load(response)["Text"]).group()
            break
    wrong_code = "000000" if reset_code != "000000" else "111111"
    for _ in range(5):
        request("/auth/password-reset/confirm", {"challengeId": reset["challengeId"], "code": wrong_code, "newPassword": "NewTestPass123!"}, expected=400)
    request("/auth/password-reset/confirm", {"challengeId": reset["challengeId"], "code": reset_code, "newPassword": "NewTestPass123!"}, expected=400)
    assert sql("select attempts from ebike_auth.account_challenges where public_id='" + reset["challengeId"] + "'") == "5"
    sql("update ebike_auth.account_challenges set created_at=now()-interval '2 minutes' where public_id='" + reset["challengeId"] + "'")
    reset = request("/auth/password-reset/request", {"email": email})
    reset_code = otp(email, "mật khẩu")
    request("/auth/password-reset/confirm", {"challengeId": reset["challengeId"], "code": reset_code, "newPassword": "NewTestPass123!"}, expected=204)
    request("/auth/profile", token=token, expected=401)
    request("/auth/login/enhanced", {"usernameOrEmail": email, "password": "InitialTestPass123!"}, expected=401)
    token = request("/auth/login/enhanced", {"usernameOrEmail": email, "password": "NewTestPass123!"})["token"]
    request("/auth/password-reset/confirm", {"challengeId": reset["challengeId"], "code": reset_code, "newPassword": "AnotherTestPass123!"}, expected=400)
    request("/auth/password-reset/request", {"email": "unknown" + suffix + "@example.test"})
    note("Five failed OTP guesses persist, reset consumes once and revokes old sessions")

    category = request("/marketplace/categories")[0]["id"]
    brand = request("/marketplace/brands")[0]["id"]
    model_id = int(sql(f"insert into marketplace.vehicle_models(brand_id,category_id,name,slug) values ({int(brand)},{int(category)},'Check Model','check-{suffix}') returning id").splitlines()[0])
    assert any(x["id"] == model_id for x in request(f"/marketplace/models?brandId={brand}&categoryId={category}"))
    payload = {"categoryId": category, "brandId": brand, "modelId": model_id, "title": "Honda City 2022 số tự động, chính chủ", "description": "Temporary listing for isolated integration checks.", "listingType": "SALE", "condition": "USED", "price": 5000000, "negotiable": True, "exchangeAllowed": False, "province": "Test Province", "district": "Test District", "addressText": "Test meeting point", "manufactureYear": 2022, "mileageKm": 1000}
    request("/marketplace/listings", {**payload, "modelId": 999999999}, token, expected=400)
    listing = request("/marketplace/listings", payload, token, expected=201)
    public_id = listing["publicId"]
    path = "/marketplace/listings/" + public_id
    assert listing["status"] == "DRAFT" and listing["modelId"] == model_id and listing["addressText"] == payload["addressText"]
    request(path, expected=404)
    request(path + "/statistics", expected=401)
    request(path + "/statistics", token=buyer_token, expected=404)
    request(path + "/edit", payload, buyer_token, expected=403)
    note("Draft privacy, catalog integrity, address persistence and ownership checks")

    png = photo_fixture()
    boundary = "motionx" + suffix
    upload = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="check.png"\r\nContent-Type: image/png\r\n\r\n'.encode() + png + f"\r\n--{boundary}--\r\n".encode())
    request(path + "/images", token=token, expected=201, raw=upload, content_type="multipart/form-data; boundary=" + boundary)

    def approve():
        request(path + "/publish", {}, token)
        request("/manager/marketplace/listings/" + public_id + "/moderate", {"action": "APPROVE"}, admin_token)

    approve()
    assert request(path)["sellerVerified"] is True
    for _ in range(2):
        request(path + "/engagement", {"type": "VIEW"}, buyer_token, expected=204)
        request(path + "/engagement", {"type": "CONTACT"}, buyer_token, expected=204)
    stats = request(path + "/statistics", token=token)
    assert stats["viewCount"] == 1 and stats["contactCount"] == 1
    request(path + "/favorite", {}, buyer_token)
    assert request(path + "/statistics", token=token)["favoriteCount"] == 1
    request(path + "/edit", {**payload, "price": 5500000, "addressText": "Updated meeting point"}, token)
    assert request(path + "/statistics", token=token)["priceHistory"][0]["price"] == 5500000
    assert request(path, token=token)["addressText"] == "Updated meeting point"
    approve()
    assert request(path + "/manage/hide", {}, token)["status"] == "HIDDEN"
    request(path, expected=404)
    assert request(path + "/manage/relist", {}, token)["status"] == "DRAFT"
    approve()
    request(path + "/manage/renew", {}, token)
    request(path + "/manage/renew", {}, token, expected=429)
    note("Publish/review, daily view/contact deduplication, favorites and price history")

    sql("update marketplace.vehicle_listings set expires_at=now()-interval '1 minute',renewed_at=null where public_id='" + public_id + "'")
    for _ in range(15):
        if request(path, token=token)["status"] == "EXPIRED":
            break
        time.sleep(.5)
    else:
        raise AssertionError("Listing did not expire")
    assert request(path + "/manage/renew", {}, token)["status"] == "PENDING_REVIEW"
    request("/manager/marketplace/listings/" + public_id + "/moderate", {"action": "APPROVE"}, admin_token)
    rep = request(f"/marketplace/sellers/{owner_id}/reputation")
    assert rep["summary"]["activeListingCount"] == 1 and "email" not in rep["seller"]
    assert len(request(f"/marketplace/sellers/{owner_id}/listings")) == 1
    request(path + "/manage/sold", {}, buyer_token, expected=404)
    assert request(path + "/manage/sold", {}, token)["status"] == "SOLD"
    request(path + "/manage/relist", {}, token, expected=409)
    assert len(request(f"/marketplace/sellers/{owner_id}/listings")) == 0
    note("Expiry, renewal review, public seller profile and sold-state protection")

    request(f"/admin/accounts/{owner_id}/status", {"status":"LOCKED"},admin_token,method="PATCH")
    request("/auth/profile",token=token,expected=401)
    request(f"/admin/accounts/{owner_id}/status", {"status":"ACTIVE"},admin_token,method="PATCH")
    request("/auth/profile",token=token,expected=401)
    token=request("/auth/login/enhanced",{"usernameOrEmail":email,"password":"NewTestPass123!"})["token"]
    note("Account locking revokes existing sessions even after unlocking")

    request("/auth/profile", {"firstName": "Owner", "lastName": "Check", "email": "changed" + suffix + "@example.test", "phoneNumber": "0909999999"}, token, method="PUT")
    profile = request("/auth/profile", token=token)
    assert profile["emailVerifiedAt"] is None and profile["phoneVerifiedAt"] is None and not profile["verified"]
    note("Changing contact details clears verification")
    print(f"SUCCESS: {passed} integration groups passed; temporary resources will be removed.", flush=True)
except Exception:
    if started:
        for line in docker("logs", "--tail", "120", container, check=False).splitlines():
            if "ERROR" in line or "Caused by:" in line or "Detail:" in line:
                print("DIAGNOSTIC " + line[:450], flush=True)
    raise
finally:
    if started:
        docker("rm", "-f", container, check=False)
    if created:
        docker("exec", "ebike-postgres", "dropdb", "-U", "ebike_user", "--force", database, check=False)
