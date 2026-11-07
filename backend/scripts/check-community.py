"""Integration checks against fixtures created by check-trades.py --prepare.
Never targets the application's database. Run check-trades.py --cleanup afterwards.
"""
import json,subprocess,time,urllib.request,urllib.error,uuid
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from datetime import datetime,timedelta,timezone
state=json.loads((Path(__file__).resolve().parents[2]/'.local-checks'/'trade-state.json').read_text(encoding='utf-8'))
assert state['database'].startswith('motionx_trade_')
BASE='http://127.0.0.1:18082/api/v1'
passed=0
def sql(query):
 r=subprocess.run(['docker','exec','ebike-postgres','psql','-U','ebike_user','-d',state['database'],'-Atc',query],capture_output=True,text=True,encoding='utf-8');assert r.returncode==0,r.stderr;return r.stdout.strip()
def api(path,body=None,role='buyer',method=None,expected=200):
 headers={}
 if role:headers['Authorization']='Bearer '+state['accounts'][role]['token']
 data=None
 if body is not None:data=json.dumps(body).encode();headers['Content-Type']='application/json'
 req=urllib.request.Request(BASE+path,data=data,headers=headers,method=method or ('POST' if data is not None else 'GET'))
 try:
  with urllib.request.urlopen(req,timeout=20) as response:code,payload=response.status,response.read()
 except urllib.error.HTTPError as error:code,payload=error.code,error.read()
 if code!=expected:raise AssertionError(f'{req.method} {path}: expected {expected}, got {code}: '+payload.decode(errors='replace')[:300])
 return json.loads(payload) if payload else None
def ok(label):
 global passed;passed+=1;print('PASS '+label,flush=True)
def later(hours):return (datetime.now(timezone.utc)+timedelta(hours=hours)).isoformat()
car=sql("select public_id from marketplace.vehicle_listings where description='Isolated scenario: API cancellation refund'")
buyer=state['accounts']['buyer']['id'];seller=state['accounts']['seller']['id']
api('/marketplace/community/comments/'+car,{'content':'API community buyer question'})
comments=api('/marketplace/community/comments/'+car,role=None);root=comments['items'][0]['publicId']
api('/marketplace/community/comments/'+car,{'content':'API seller reply','parentId':root},'seller')
api('/marketplace/community/comment/'+root+'/edit',{'content':'Forbidden'},'outsider',expected=403)
api('/marketplace/community/comment/'+root+'/edit',{'content':'API edited question'})
api('/marketplace/community/comment/'+root+'/hide',{},'seller')
assert not any(x['content']=='API edited question' for x in api('/marketplace/community/comments/'+car,role=None)['items'])
api('/marketplace/community/comment/'+root+'/edit',{'content':'Cannot bypass moderation'},expected=403)
api('/marketplace/community/comment/'+root+'/restore',{},'admin')
api('/marketplace/community/comments/'+car,{'content':'API reportable seller comment'},'seller')
seller_comment=next(x for x in api('/marketplace/community/comments/'+car)['items'] if x['content']=='API reportable seller comment')
ok('Public comments/replies, edit ownership, seller hiding, hidden content redaction and Admin restore')
api('/marketplace/community/reports',{'type':'COMMENT','target':seller_comment['publicId'],'reason':'SPAM','description':'API review report'})
report=api('/marketplace/community/reports')[0]
api('/marketplace/community/reports?staff=true',expected=403)
api('/marketplace/community/reports/'+str(report['id'])+'/resolve',{'action':'HIDE','note':'API hide reviewed comment'},'manager')
assert api('/marketplace/community/reports')[0]['status']=='RESOLVED'
assert any(x['content']=='Bình luận đã được ẩn hoặc xóa.' for x in api('/marketplace/community/comments/'+car,role=None)['items'])
ok('Report ownership, Manager resolution, reporter result and hidden evidence')
conversation=api('/marketplace/listings/'+car+'/conversation',{})
cid=conversation['publicId']
api('/marketplace/conversations/'+cid+'/messages',{'content':'API private message to report'},'seller')
message=next(x for x in api('/marketplace/conversations/'+cid+'/messages') if x['content']=='API private message to report')
api('/marketplace/community/reports',{'type':'MESSAGE','target':message['publicId'],'reason':'SPAM','description':'Cannot report another conversation'},'outsider',expected=404)
api('/marketplace/community/reports',{'type':'MESSAGE','target':message['publicId'],'reason':'SPAM','description':'API report message'})
report=api('/marketplace/community/reports')[0]
api('/marketplace/community/reports/'+str(report['id'])+'/resolve',{'action':'DELETE','note':'API delete private message after review'},'admin')
assert all(x['publicId']!=message['publicId'] for x in api('/marketplace/conversations/'+cid+'/messages'))
api('/marketplace/community/blocks/'+str(seller),{})
api('/marketplace/conversations/'+cid+'/messages',{'content':'Blocked'},'seller',expected=403)
api('/marketplace/listings/'+car+'/conversation',{},expected=403)
api('/marketplace/community/blocks/'+str(seller)+'?enabled=false',{})
api('/marketplace/conversations/'+cid+'/messages',{'content':'API unblocked message'},'seller')
ok('Private message reporting, Admin deletion and bilateral block/unblock enforcement')
tx=sql("select t.public_id from marketplace.transactions t join marketplace.vehicle_listings l on l.id=t.listing_id where l.description='Isolated scenario: API negotiation vehicle'")
for role in ['buyer','seller']:api('/marketplace/transactions/'+tx+'/seller-review',{'rating':5,'comment':'API verified '+role+' review'},role,expected=201)
api('/marketplace/transactions/'+tx+'/seller-review',{'rating':4},expected=409)
reviews=api('/marketplace/community/reviews-mine');assert len(reviews)==2
received=next(x for x in reviews if x['targetId']==buyer)
api('/marketplace/community/reviews/'+received['publicId']+'/reply',{'content':'API reply to received review'})
api('/marketplace/community/reviews/'+received['publicId']+'/reply',{'content':'Not target'},'seller',expected=403)
assert any(x.get('reply')=='API reply to received review' for x in api('/marketplace/sellers/'+str(buyer)+'/reputation',role=None)['reviews'])
api('/marketplace/community/reports',{'type':'REVIEW','target':received['publicId'],'reason':'OTHER','description':'API moderate review'})
report=api('/marketplace/community/reports')[0]
api('/marketplace/community/reports/'+str(report['id'])+'/resolve',{'action':'HIDE','note':'API review moderation'},'admin')
assert not api('/marketplace/sellers/'+str(buyer)+'/reputation',role=None)['reviews']
ok('Bilateral completed-transaction reviews, one review per participant, target reply and moderation')
appointment=api('/marketplace/listings/'+car+'/appointments',{'scheduledAt':later(5),'location':'API viewing location','note':'API appointment'},expected=201)
aid=appointment['publicId'];rev=appointment['revision']
api('/marketplace/appointments/'+aid,{'status':'RESCHEDULE','scheduledAt':later(6),'location':'API proposed place','revision':rev},'seller',method='PATCH')
api('/marketplace/appointments/'+aid,{'status':'CONFIRMED','revision':rev+1},'seller',method='PATCH',expected=400)
api('/marketplace/appointments/'+aid,{'status':'CONFIRMED','revision':rev},method='PATCH',expected=409)
appointment=api('/marketplace/appointments/'+aid,{'status':'CONFIRMED','revision':rev+1},method='PATCH')
api('/marketplace/appointments/'+aid,{'status':'COMPLETED','revision':appointment['revision']},method='PATCH',expected=400)
api('/marketplace/listings/'+car+'/appointments',{'scheduledAt':later(6),'location':'Conflicting appointment'},expected=409)
sql("update marketplace.appointments set scheduled_at=now()+interval '20 minutes' where public_id='"+aid+"'")
time.sleep(12)
assert sql("select count(*) from marketplace.reminders where event_key like 'APPOINTMENT:%'")!='0'
ok('Reschedule recipient confirmation, stale revision guard, time conflict and scheduled reminder')
ticket=api('/support/tickets',{'requesterName':'API Buyer','requesterEmail':'api@example.test','category':'MARKETPLACE','subject':'API community support','message':'API original support request'},expected=201)
tid=ticket['id'];path='/support/tickets/'+str(tid)+'/messages'
api(path,role='outsider',expected=403)
api(path,{'content':'API buyer follow-up'})
api(path,{'content':'API Manager reply'},'manager')
api(path,{'content':'API buyer additional details'})
api(path,{'content':'API Admin reply'},'admin')
assert len(api(path))==4
api('/admin/support-tickets/'+str(tid),{'staffNote':'API legacy staff note retained','status':'IN_PROGRESS'},'admin',method='PATCH')
assert len(api(path))==5
api('/admin/support-tickets/'+str(tid),{'status':'CLOSED'},'admin',method='PATCH')
api(path,{'content':'Cannot reply closed'},expected=409)
ok('Ticket owner/staff isolation, append-only conversation, legacy note history and closed-ticket guard')
result=api('/marketplace/discovery/search?q=API&sort=priceAsc',role=None);prices=[x['price'] for x in result['items']];assert prices==sorted(prices)
api('/marketplace/discovery/search?sort=unsafe',role=None,expected=400)
api('/marketplace/discovery/search?minPrice=-1',role=None,expected=400)
api('/marketplace/discovery/saved',{'name':'API saved search','filters':{'q':'Honda Civic 2022 bản thể thao'}})
saved=api('/marketplace/discovery/saved')[0]
api('/marketplace/discovery/saved/'+str(saved['id']),'',role='outsider',method='DELETE',expected=403)
api('/marketplace/discovery/comparison',{'ids':[car]});assert api('/marketplace/discovery/comparison')==[car]
assert len(api('/marketplace/discovery/compare?ids='+car,role=None))==1
api('/marketplace/discovery/recent/'+car,{});assert api('/marketplace/discovery/recent')[0]['publicId']==car
api('/marketplace/discovery/compare?ids='+state['fixtures']['listing'],role=None,expected=404)
ok('Search sorting/input validation, saved ownership, comparison persistence and recent views without draft leaks')
before=int(sql("select count(*) from marketplace.search_alerts where search_id="+str(saved['id'])))
sql("update marketplace.vehicle_listings set title='Honda Civic 2022 bản thể thao',published_at=now() where public_id='"+car+"'")
time.sleep(12)
assert int(sql("select count(*) from marketplace.search_alerts where search_id="+str(saved['id'])))>before
notifications=api('/notifications');assert any(x['type']=='MARKETPLACE' for x in notifications)
assert all(x['createdAt'].endswith('Z') or '+' in x['createdAt'][10:] for x in notifications)
assert any(x['title']=='Xe phù hợp tìm kiếm đã lưu' for x in notifications)
api('/notifications/'+str(notifications[0]['id'])+'/read',role='outsider',method='PATCH',expected=404)
ok('Saved-search alert, durable Marketplace notifications and notification ownership')
streams=[]
try:
 def subscribe(_):
  request=urllib.request.Request(BASE+'/notifications/stream',headers={'Authorization':'Bearer '+state['accounts']['buyer']['token']})
  connection=urllib.request.urlopen(request,timeout=10)
  line=connection.readline();assert b'unread-count' in line
  return connection
 with ThreadPoolExecutor(max_workers=25) as pool:
  for stream in pool.map(subscribe,range(25)):streams.append(stream)
 start=time.monotonic();api('/auth/profile');assert time.monotonic()-start<5
 ok('25 simultaneous SSE subscriptions leave the database pool available for authenticated requests')
finally:
 for stream in streams:stream.close()
print(f'SUCCESS {passed} community integration groups passed.',flush=True)
