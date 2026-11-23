"""Exercise a localhost DEMO server. Creates TEST_ accounts/activities; no real data is used."""
import argparse, base64, concurrent.futures, datetime, io, json, re, uuid, urllib.request, urllib.error, urllib.parse, zipfile

parser=argparse.ArgumentParser()
parser.add_argument('--url',default='http://127.0.0.1:8080')
parser.add_argument('--report')
args=parser.parse_args()
assert urllib.parse.urlsplit(args.url).hostname in ('127.0.0.1','localhost'), 'Only local demo servers are supported'
results=[]
def request(path, token=None, body=None, method=None, expected=200, multipart=False, raw=False):
    headers={}
    if token: headers['Authorization']='Bearer '+token
    if multipart:
        boundary='----Campus'+uuid.uuid4().hex
        fields,file=body
        chunks=[]
        for key,value in fields.items():
            chunks.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode())
        if file:
            chunks.append(f'--{boundary}\r\nContent-Disposition: form-data; name="attachment"; filename="proof.pdf"\r\nContent-Type: application/pdf\r\n\r\n'.encode()+file+b'\r\n')
        chunks.append(f'--{boundary}--\r\n'.encode());payload=b''.join(chunks)
        headers['Content-Type']='multipart/form-data; boundary='+boundary
    elif body is not None:
        payload=json.dumps(body).encode();headers['Content-Type']='application/json'
    else: payload=None
    req=urllib.request.Request(args.url+'/api'+path,data=payload,headers=headers,method=method or ('POST' if body is not None else 'GET'))
    try:
        with urllib.request.urlopen(req,timeout=30) as response:status=response.status;data=response.read()
    except urllib.error.HTTPError as e: status=e.code;data=e.read()
    if status!=expected:raise AssertionError(f'{path}: expected {expected}, got {status}: {data[:300]!r}')
    if raw:return data
    result=json.loads(data)
    if expected==200:
        assert result['success'],result
        return result['data']
    assert not result['success'];return result

def captcha():
    c=request('/captcha');svg=base64.b64decode(c['image'].split(',')[1]).decode()
    return {'captchaId':c['captchaId'],'captcha':re.search(r'>(\d{4})</text>',svg).group(1)}

def login(name,password='Demo1234!'):
    data=request('/login',body={'account':name,'password':password,**captcha()})
    assert 'password' not in data['user']
    return data['token'],data['user']

def check(label, action):
    action();results.append({'case':label,'result':'PASS'});print('PASS',label)

assert request('/health')['demo'], 'Refusing to create fixtures: server is not in demo mode'
tokens={};users={}
for name in ('admin1','activity1','teacher1','student1'):tokens[name],users[name]=login(name)
results.append({'case':'four-role-login','result':'PASS'})
check('anonymous-api-rejected',lambda:request('/profile',expected=401))
check('student-admin-forbidden',lambda:request('/admin/users',tokens['student1'],expected=403))
suffix=uuid.uuid4().hex[:8]
student_name='TEST_s_'+suffix
request('/register',body={'userName':student_name,'email':student_name+'@example.test','password':'Testpass123!','confirmPassword':'Testpass123!','role':'admin',**captcha()})
second,second_user=login(student_name,'Testpass123!');assert second_user['role']=='student'
results.append({'case':'registration-forces-student-role','result':'PASS'})
teacher_name='TEST_t_'+suffix
new_teacher=request('/admin/users',tokens['admin1'],body={'userName':teacher_name,'realName':'TEST teacher','email':teacher_name+'@example.test','password':'Testpass123!','role':'teacher','status':'0','phone':''})
outsider,_=login(teacher_name,'Testpass123!')
now=datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8)))
fmt=lambda d:d.strftime('%Y-%m-%d %H:%M:%S')
schema={'version':1,'attachmentRequired':True,'fields':[{'key':'name','label':'姓名','type':'text','required':True,'maxLength':50},{'key':'grade','label':'年级','type':'select','required':True,'options':['大一','大二']}]}
activity=request('/activities',tokens['activity1'],body={'title':'TEST 活动 '+suffix,'content':'TEST full workflow','startTime':fmt(now-datetime.timedelta(hours=1)),'endTime':fmt(now+datetime.timedelta(days=1)),'reviewerId':users['teacher1']['userId'],'formSchema':schema,'opType':'PUBLISH'})
aid=activity['activityId'];assert activity['status']=='1'
results.append({'case':'publish-activity','result':'PASS'})
assert any(a['activityId']==aid for a in request('/published-activities',tokens['student1']))
form={'name':'TEST 同学','grade':'大一'}
request('/registrations/draft',tokens['student1'],body={'activityId':aid,'formData':{'name':'草稿'}})
assert request(f'/activities/{aid}/draft',tokens['student1'])['status']=='0'
results.append({'case':'save-and-read-draft','result':'PASS'})
def submit(token,fields=form,file=b'%PDF-1.4\n% TEST fixture\n%%EOF'):
    return request('/registrations',token,body=({'activityId':str(aid),'formData':json.dumps(fields)},file),multipart=True)
check('required-field-server-validation',lambda:request('/registrations',tokens['student1'],body=({'activityId':str(aid),'formData':'{}'},b'%PDF-1.4'),multipart=True,expected=400))
r=submit(tokens['student1']);rid=r['regId'];assert r['status']=='1' and r['version']==1 and 'attachmentPath' not in r
results.append({'case':'draft-to-pending-with-file','result':'PASS'})
check('duplicate-submit-rejected',lambda:request('/registrations',tokens['student1'],body=({'activityId':str(aid),'formData':json.dumps(form)},b'%PDF-1.4'),multipart=True,expected=409))
check('formal-record-not-overwritten-by-draft',lambda:request('/registrations/draft',tokens['student1'],body={'activityId':aid,'formData':form},expected=409))
check('other-student-data-forbidden',lambda:request(f'/my-registrations/{rid}',second,expected=403))
check('unassigned-teacher-data-forbidden',lambda:request(f'/reviews/{rid}',outsider,expected=403))
detail=request(f'/reviews/{rid}',tokens['teacher1']);assert detail['registration']['hasAttachment']
file=request(f'/reviews/{rid}/attachment',tokens['teacher1'],raw=True);assert file.startswith(b'%PDF-')
results.append({'case':'authorized-teacher-material-download','result':'PASS'})
check('rejection-requires-reason',lambda:request('/reviews',tokens['teacher1'],body={'regId':rid,'result':'2','opinion':'  ','version':r['version']},expected=400))
out=request('/reviews',tokens['teacher1'],body={'regId':rid,'result':'1','opinion':'审核通过','version':r['version']});assert out['status']=='2'
check('stale-review-rejected',lambda:request('/reviews',tokens['teacher1'],body={'regId':rid,'result':'2','opinion':'重复审核','version':r['version']},expected=409))
assert request(f'/my-registrations/{rid}',tokens['student1'])['registration']['status']=='2'
results.append({'case':'approve-and-student-query','result':'PASS'})
r2=submit(second);request('/reviews',tokens['teacher1'],body={'regId':r2['regId'],'result':'2','opinion':'材料不完整','version':r2['version']})
mine=request('/my-registrations',second);assert next(x for x in mine if x['regId']==r2['regId'])['latestOpinion']=='材料不完整'
results.append({'case':'reject-and-student-query','result':'PASS'})
stats=request(f'/reg-stat?activityId={aid}',tokens['activity1']);assert int(stats['total'])==2 and int(stats['approved'])==1 and int(stats['rejected'])==1
results.append({'case':'statistics-match-review-results','result':'PASS'})
xlsx=request(f'/reg-export?activityId={aid}',tokens['activity1'],raw=True)
with zipfile.ZipFile(io.BytesIO(xlsx)) as z: assert 'xl/workbook.xml' in z.namelist()
results.append({'case':'xlsx-export','result':'PASS'})
assert request('/admin/logs',tokens['admin1'])
results.append({'case':'audit-log-created','result':'PASS'})
request('/admin/users',tokens['admin1'],body={**new_teacher,'status':'1','password':''})
check('disabled-account-existing-token-rejected',lambda:request('/profile',outsider,expected=401))
request('/logout',tokens['student1'],method='POST')
check('logout-revokes-token',lambda:request('/profile',tokens['student1'],expected=401))
report={'server':args.url,'time':now.isoformat(),'passed':len(results),'cases':results}
if args.report:
    from pathlib import Path
    Path(args.report).write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(f'ALL {len(results)} SMOKE CHECKS PASSED')
