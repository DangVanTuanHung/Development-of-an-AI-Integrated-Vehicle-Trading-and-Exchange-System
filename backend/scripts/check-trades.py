"""Isolated four-role trading checks. --prepare keeps fixtures for Chrome; --cleanup removes them.
State/ephemeral test credentials are saved only under ignored .local-checks/.
"""
import base64, http.client, json, re, secrets, subprocess, sys, time, urllib.error, urllib.request, uuid
from concurrent.futures import ThreadPoolExecutor
from image_fixture import photo_fixture
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
STATE=ROOT/'.local-checks'/'trade-state.json'
BASE='http://127.0.0.1:18082/api/v1'
PNG=photo_fixture()
passed=[]
def docker(*args,check=True):
    r=subprocess.run(['docker',*args],capture_output=True,text=True,encoding='utf-8',errors='replace')
    if check and r.returncode:raise RuntimeError('Docker command failed: '+' '.join(args[:3]))
    return r.stdout.strip()
def cleanup(state):
    assert re.fullmatch(r'motionx_trade_[a-f0-9]{10}',state['database'])
    assert re.fullmatch(r'motionx-trade-[a-f0-9]{10}',state['container'])
    docker('rm','-f',state['container'],check=False)
    docker('exec','ebike-postgres','dropdb','-U','ebike_user','--force',state['database'],check=False)
    STATE.unlink(missing_ok=True)
def sql(query):return docker('exec','ebike-postgres','psql','-U','ebike_user','-d',state['database'],'-v','ON_ERROR_STOP=1','-Atc',query)
def api(path,body=None,role=None,expected=200,method=None,raw=None,content_type=None):
    headers={}
    if role:headers['Authorization']='Bearer '+state['accounts'][role]['token']
    if body is not None:raw=json.dumps(body).encode();content_type='application/json'
    if content_type:headers['Content-Type']=content_type
    request=urllib.request.Request(BASE+path,data=raw,headers=headers,method=method or ('POST' if raw is not None else 'GET'))
    try:
        with urllib.request.urlopen(request,timeout=20) as r:code,data=r.status,r.read()
    except urllib.error.HTTPError as e:code,data=e.code,e.read()
    if code!=expected:
        try:message=json.loads(data).get('message','')
        except Exception:message=''
        raise AssertionError(f'{request.method} {path}: expected {expected}, got {code}; {message}')
    if not data:return None
    try:return json.loads(data)
    except (ValueError,UnicodeDecodeError):return data
def ok(label):passed.append(label);print('PASS '+label,flush=True)
def upload(path,role):
    boundary='check'+uuid.uuid4().hex
    data=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="handover-check.png"\r\nContent-Type: image/png\r\n\r\n'.encode()+PNG+f'\r\n--{boundary}--\r\n'.encode())
    return api(path,role=role,raw=data,content_type='multipart/form-data; boundary='+boundary,expected=201 if path.endswith('/images') else 200)
def vehicle(title,owner='seller',publish=True):
    body={'categoryId':category,'title':'Toyota Camry 2022 số tự động, chính chủ','description':'Isolated scenario: '+title,'listingType':'SALE_OR_EXCHANGE','condition':'USED','price':10000000,'negotiable':True,'exchangeAllowed':True,'province':'Test Province'}
    item=api('/marketplace/listings',body,owner,201);id=item['publicId']
    upload(f'/marketplace/listings/{id}/images',owner)
    api(f'/marketplace/listings/{id}/publish',{},owner)
    if publish:api(f'/manager/marketplace/listings/{id}/moderate',{'action':'APPROVE'},'manager')
    return id
def offer(listing,deposit=1000000,amount=10000000,exchange=None,credit=0):
    return api(f'/marketplace/listings/{listing}/offers',{'amount':amount,'depositAmount':deposit,'message':'Isolated trade check','exchangeListingId':exchange,'exchangeValue':credit,'policyAccepted':True},'buyer',201)
def accept(o,role='seller'):return api(f'/marketplace/offers/{o["publicId"]}/accept',{'policyAccepted':True},role)
def detail(tx,role='buyer'):return api(f'/marketplace/transactions/{tx["publicId"]}',role=role)
def payment(tx,stage='DEPOSIT',provider='BANK_TRANSFER'):
    payer='buyer' if tx['payerId']==state['accounts']['buyer']['id'] else 'seller'
    payee='seller' if payer=='buyer' else 'buyer'
    tx=api(f'/marketplace/transactions/{tx["publicId"]}/payments',{'stage':stage,'provider':provider},payer)
    p=next(x for x in tx['payments'] if x['status']=='PENDING')
    api(f'/marketplace/payments/{p["publicId"]}/confirm',{},payee,409)
    api(f'/marketplace/payments/{p["publicId"]}/sent',{'reference':'TEST-TRANSFER'},payee,403)
    api(f'/marketplace/payments/{p["publicId"]}/sent',{'reference':'TEST-TRANSFER'},payer)
    tx=api(f'/marketplace/payments/{p["publicId"]}/confirm',{},payee)
    return tx
def plan(tx):
    return api(f'/marketplace/transactions/{tx["publicId"]}/handover/plan',{'scheduledAt':(datetime.now(timezone.utc)+timedelta(hours=1)).isoformat(),'location':'Test handover location','note':'Test only','revision':tx['handover']['revision']},'seller')
def finish(tx):
    tx=plan(tx);rev=tx['handover']['revision'];id=tx['publicId']
    api(f'/marketplace/transactions/{id}/handover/agree',{'revision':rev},'buyer')
    api(f'/marketplace/transactions/{id}/handover/confirm',{'revision':rev},'manager',403)
    api(f'/marketplace/transactions/{id}/handover/confirm',{'revision':rev},'buyer',409)
    proof=upload(f'/marketplace/transactions/{id}/evidence','seller')
    api(f'/marketplace/transactions/{id}/evidence/{proof["publicId"]}',role=None,expected=401)
    api(f'/marketplace/transactions/{id}/evidence/{proof["publicId"]}',role='outsider',expected=403)
    tx=api(f'/marketplace/transactions/{id}/handover/confirm',{'revision':rev},'buyer');assert tx['status']=='FULLY_PAID'
    api(f'/marketplace/transactions/{id}/handover/confirm',{'revision':rev},'buyer',409)
    return api(f'/marketplace/transactions/{id}/handover/confirm',{'revision':rev},'seller')

if '--cleanup' in sys.argv:
    if STATE.exists():cleanup(json.loads(STATE.read_text(encoding='utf-8')))
    print('Temporary trading resources removed.');sys.exit()
if STATE.exists():raise SystemExit('Existing trade check state found. Run --cleanup first.')
suffix=uuid.uuid4().hex[:10]
state={'database':'motionx_trade_'+suffix,'container':'motionx-trade-'+suffix,'accounts':{},'fixtures':{}}
STATE.parent.mkdir(exist_ok=True)
try:
    info=json.loads(docker('inspect','ebike-backend'))[0];network=next(iter(info['NetworkSettings']['Networks']))
    docker('exec','ebike-postgres','createdb','-U','ebike_user',state['database'])
    STATE.write_text(json.dumps(state),encoding='utf-8')
    env={'SPRING_DATASOURCE_URL':'jdbc:postgresql://postgres:5432/'+state['database'],'SPRING_DATASOURCE_USERNAME':'ebike_user','SPRING_DATASOURCE_PASSWORD':'ebike_password_123','SPRING_DATA_REDIS_HOST':'redis','SPRING_DATA_REDIS_PASSWORD':'redis_password_123','APP_JWT_SECRET':secrets.token_urlsafe(48),'APP_RATE_LIMIT_ENABLED':'false','SMTP_HOST':'','GEMINI_AI_KEY':'','TWILIO_ACCOUNT_SID':'','TWILIO_AUTH_TOKEN':'','TWILIO_VERIFY_SERVICE_SID':'','SPRING_JPA_SHOW_SQL':'false','APP_STORAGE_S3_ENDPOINT':'http://minio:9000','APP_STORAGE_S3_ACCESS_KEY':'marketplace_admin','APP_STORAGE_S3_SECRET_KEY':'marketplace_local_password_123','APP_STORAGE_S3_BUCKET':'vehicle-marketplace','APP_STORAGE_S3_PATH_STYLE_ACCESS':'true','APP_STORAGE_S3_INITIALIZE_BUCKET':'false'}
    args=['run','-d','--name',state['container'],'--network',network,'-p','127.0.0.1:18082:8080']
    for key,value in env.items():args+=['-e',key+'='+value]
    docker(*args,info['Config']['Image'])
    for _ in range(120):
        running=docker('inspect','--format','{{.State.Running}}',state['container'])
        if running!='true':raise AssertionError('Temporary backend exited during startup')
        try:api('/health/ready');break
        except (OSError,urllib.error.URLError,http.client.HTTPException,AssertionError):time.sleep(1)
    else:raise AssertionError('Temporary backend failed to start')
    for role in ['buyer','seller','manager','admin','outsider']:
        if role in ['manager','admin']:
            username=role+'.user';password='ManagerPassword123' if role=='manager' else 'AdminPassword123'
            user=api('/auth/login/enhanced',{'usernameOrEmail':username,'password':password})
            state['accounts'][role]={'username':username,'password':password,'id':user['userId'],'token':user['token']};continue
        username='check_'+role+'_'+suffix;password='Test-'+secrets.token_urlsafe(12)
        user=api('/auth/register',{'username':username,'email':username+'@example.test','password':password,'firstName':'Chrome '+role,'lastName':'Check'},expected=201)
        state['accounts'][role]={'username':username,'password':password,'id':user['userId'],'token':user['token']}
    STATE.write_text(json.dumps(state),encoding='utf-8')
    category=api('/marketplace/categories')[0]['id']
    api('/manager/marketplace/trades',role='buyer',expected=403);api('/admin/accounts',role='manager',expected=403)
    api('/manager/marketplace/trades',role='manager');api('/manager/marketplace/trades',role='admin')
    ok('Empty credentials: backend ready; Buyer/Seller and Manager/Admin permissions enforced')
    car=vehicle('API negotiation vehicle');o=offer(car)
    api(f'/marketplace/offers/{o["publicId"]}/accept',{'policyAccepted':True},'buyer',403)
    counter=api(f'/marketplace/offers/{o["publicId"]}/counter',{'amount':9500000,'depositAmount':500000,'exchangeValue':0,'policyAccepted':True},'seller')
    tx=accept(counter,'buyer');api(f'/marketplace/offers/{o["publicId"]}/accept',{'policyAccepted':True},'seller',409)
    api(f'/marketplace/transactions/{tx["publicId"]}',role='outsider',expected=403)
    tx=payment(tx);tx=payment(tx,'FINAL');assert tx['status']=='FULLY_PAID'
    tx=finish(tx);assert tx['status']=='COMPLETED';assert api(f'/marketplace/listings/{car}')['status']=='SOLD'
    ok('Counteroffer, participant authorization, two-step deposit/final payment, private proof and bilateral handover')
    car=vehicle('API offer decisions');o=offer(car);api(f'/marketplace/offers/{o["publicId"]}/withdraw',{},'seller',403);api(f'/marketplace/offers/{o["publicId"]}/withdraw',{},'buyer')
    o=offer(car);api(f'/marketplace/offers/{o["publicId"]}/reject',{},'seller');o=offer(car)
    sql("update marketplace.offers set expires_at=now()-interval '1 minute' where public_id='"+o['publicId']+"'")
    api(f'/marketplace/offers/{o["publicId"]}/accept',{'policyAccepted':True},'seller',409)
    ok('Reject, withdraw and expired offer safeguards')
    car=vehicle('API cancellation refund');tx=accept(offer(car));tx=payment(tx);id=tx['publicId']
    api(f'/marketplace/transactions/{id}/cancellation/request',{'note':'Cancel by agreement'},'buyer')
    api(f'/marketplace/transactions/{id}/cancellation/approve',{},'buyer',403)
    tx=api(f'/marketplace/transactions/{id}/cancellation/approve',{},'seller');assert tx['status']=='REFUND_PENDING'
    ref=tx['refunds'][0]['publicId'];api(f'/marketplace/transactions/{id}/refunds/{ref}/received',{},'buyer',409)
    api(f'/marketplace/transactions/{id}/refunds/{ref}/sent',{'reference':'TEST-REFUND'},'seller')
    tx=api(f'/marketplace/transactions/{id}/refunds/{ref}/received',{},'buyer');assert tx['status']=='REFUNDED';assert api(f'/marketplace/listings/{car}')['status']=='PUBLISHED'
    ok('Mutual cancellation, full refund, recipient acknowledgment and vehicle release')
    car=vehicle('API cash handover');tx=accept(offer(car));tx=payment(tx,'FINAL','CASH_ON_DELIVERY');assert tx['status']=='FULLY_PAID';tx=finish(tx);assert tx['status']=='COMPLETED'
    ok('Cash receipt never completes handover unilaterally')
    target=vehicle('API exchange target');swap=vehicle('API exchange buyer vehicle','buyer');tx=accept(offer(target,deposit=0,exchange=swap,credit=12000000));assert tx['payerId']==state['accounts']['seller']['id']
    tx=payment(tx,'FINAL');tx=finish(tx);assert tx['status']=='COMPLETED'
    ok('Exchange reserves both vehicles, supports seller cash adjustment, and finishes bilaterally')
    for role in ['manager','admin']:
        car=vehicle('API dispute '+role);tx=accept(offer(car));id=tx['publicId']
        api(f'/marketplace/transactions/{id}/disputes',{'note':'Test dispute'},'buyer')
        api(f'/manager/marketplace/trades/{id}/resolve',{'decision':'RESUME','note':'Unauthorized attempt'},'seller',403)
        tx=api(f'/manager/marketplace/trades/{id}/resolve',{'decision':'RESUME','note':'Reviewed test evidence; continue.'},role);assert tx['status']=='DEPOSIT_PENDING'
    ok('Manager and Admin resolve disputes; participants cannot grant themselves staff decisions')
    car=vehicle('API cancellation responses');tx=accept(offer(car));id=tx['publicId']
    api(f'/marketplace/transactions/{id}/cancellation/request',{'note':'Test withdrawal'},'buyer')
    tx=api(f'/marketplace/transactions/{id}/cancellation/withdraw',{},'buyer');assert tx['status']=='DEPOSIT_PENDING'
    api(f'/marketplace/transactions/{id}/cancellation/request',{'note':'Test rejection'},'buyer')
    tx=api(f'/marketplace/transactions/{id}/cancellation/reject',{},'seller');assert tx['status']=='DEPOSIT_PENDING'
    tx=plan(tx);rev=tx['handover']['revision']
    api(f'/marketplace/transactions/{id}/handover/agree',{'revision':rev-1},'buyer',409)
    api(f'/marketplace/transactions/{id}/handover/agree',{'revision':rev},'buyer')
    api(f'/marketplace/transactions/{id}/cancellation/request',{'note':'No funds received'},'seller')
    tx=api(f'/marketplace/transactions/{id}/cancellation/approve',{},'buyer');assert tx['status']=='CANCELLED'
    ok('Cancellation withdrawal/rejection, stale handover revision and no-money cancellation')
    target=vehicle('API equal exchange');swap=vehicle('API equal buyer vehicle','buyer')
    tx=accept(offer(target,deposit=0,exchange=swap,credit=10000000));assert tx['status']=='FULLY_PAID' and not tx['payments']
    tx=finish(tx);assert tx['status']=='COMPLETED'
    assert api(f'/marketplace/listings/{target}')['status']=='EXCHANGED' and api(f'/marketplace/listings/{swap}')['status']=='EXCHANGED'
    ok('Equal exchange requires no payment and marks both vehicles exchanged')
    car=vehicle('API concurrent acceptance');first=offer(car)
    second=api(f'/marketplace/listings/{car}/offers',{'amount':10000000,'depositAmount':0,'exchangeValue':0,'policyAccepted':True},'outsider',201)
    def concurrent_accept(candidate):
        try:accept(candidate);return 'accepted'
        except AssertionError as error:
            if 'got 409' in str(error):return 'conflict'
            raise
    with ThreadPoolExecutor(max_workers=2) as pool:results=list(pool.map(concurrent_accept,[first,second]))
    assert sorted(results)==['accepted','conflict']
    assert sql("select count(*) from marketplace.transactions t join marketplace.vehicle_listings l on l.id=t.listing_id where l.public_id='"+car+"'")=='1'
    ok('Concurrent acceptance creates exactly one transaction for the vehicle')
    car=vehicle('API exchange-only intent')
    sql("update marketplace.vehicle_listings set listing_type='EXCHANGE' where public_id='"+car+"'")
    api(f'/marketplace/listings/{car}/offers',{'amount':10000000,'depositAmount':0,'policyAccepted':True},'buyer',400)
    sql("update marketplace.vehicle_listings set listing_type='SALE',exchange_allowed=false where public_id='"+car+"'")
    swap=vehicle('API exchange permission buyer','buyer')
    api(f'/marketplace/listings/{car}/offers',{'amount':10000000,'depositAmount':0,'exchangeListingId':swap,'exchangeValue':10000000,'policyAccepted':True},'buyer',400)
    ok('Exchange-only and sale-only listing intentions enforced')
    # Fresh fixtures reserved exclusively for browser UI checks.
    state['fixtures']['listing']=vehicle('CHROME - Xe giao dịch hai bên',publish=False)
    for role in ['manager','admin']:
        car=vehicle('CHROME - Tranh chấp '+role);tx=accept(offer(car));tx=payment(tx)
        tx=api(f'/marketplace/transactions/{tx["publicId"]}/disputes',{'note':'CHROME - Yêu cầu kiểm tra và kết luận.'},'buyer')
        state['fixtures']['dispute_'+role]=tx['publicId']
    state['checks']=passed
    STATE.write_text(json.dumps(state,ensure_ascii=False,indent=2),encoding='utf-8')
    (STATE.parent/'handover.png').write_bytes(PNG)
    print(f'SUCCESS {len(passed)} API groups passed. '+('Chrome fixtures ready; credentials only in ignored state file.' if '--prepare' in sys.argv else 'Temporary resources will be removed.'),flush=True)
    if '--prepare' not in sys.argv:cleanup(state)
except BaseException:
    # Never print configuration or tokens. Keep only concise exception lines from container logs.
    logs=docker('logs','--tail','300',state['container'],check=False)
    for line in logs.splitlines():
        if 'ERROR' in line or 'Caused by:' in line:print('DIAGNOSTIC '+line[:350],flush=True)
    cleanup(state);raise
