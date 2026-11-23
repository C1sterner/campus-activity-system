import { ElMessage } from 'element-plus'
export const roleNames={student:'学生',teacher:'审核教师',activity_admin:'活动管理员',admin:'系统管理员'}
export const registrationNames={'0':'草稿','1':'待审核','2':'审核通过','3':'已驳回'}
export const activityNames={'0':'草稿','1':'已发布','2':'已下线'}
export const statusType=(status)=>({'0':'info','1':'warning','2':'success','3':'danger'}[status]||'info')
export async function api(path,{method='GET',body,raw=false}={}){
  const headers={};const token=sessionStorage.getItem('campus-token');if(token)headers.Authorization=`Bearer ${token}`
  if(body && !(body instanceof FormData)){headers['Content-Type']='application/json';body=JSON.stringify(body)}
  let response
  try{response=await fetch('/api'+path,{method,headers,body})}catch(e){const err=new Error('网络不可用，请稍后重试');err.network=true;ElMessage.error(err.message);throw err}
  if(raw && response.ok)return response
  let result;try{result=await response.json()}catch{result={msg:'服务响应异常，请稍后重试'}}
  if(!response.ok||!result.success){
    if(response.status===401){sessionStorage.removeItem('campus-token');window.dispatchEvent(new Event('session-expired'))}
    const err=new Error(result.msg||'操作失败');err.code=result.code;ElMessage.error(err.message);throw err
  }
  return result.data
}
export async function download(path,name){const res=await api(path,{raw:true});const url=URL.createObjectURL(await res.blob());const a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}
export function clearDrafts(userId){for(const key of Object.keys(localStorage))if(key.startsWith(`campus-draft:${userId}:`))localStorage.removeItem(key)}
