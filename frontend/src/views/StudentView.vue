<script setup>
import {computed,onMounted,onUnmounted,reactive,ref,watch} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import {api,download,registrationNames,statusType} from '../api'
import DynamicForm from '../components/DynamicForm.vue'
const props=defineProps({user:Object}),tab=ref('activities'),activities=ref([]),mine=ref([]),keyword=ref(''),loading=ref(false),dialog=ref(false),selected=ref(null),form=reactive({}),attachment=ref(null),busy=ref(false),draftStatus=ref(''),detail=ref(null),detailOpen=ref(false),config=ref({draftIntervalSeconds:30,uploadMaxBytes:20971520})
let timer=null,inflight=false,dirty=false,restored=false,uploadInput=null
const filtered=computed(()=>activities.value.filter(a=>a.title.toLowerCase().includes(keyword.value.toLowerCase())))
const formal=computed(()=>mine.value.filter(r=>r.status!=='0'))
const key=()=>`campus-draft:${props.user.userId}:${selected.value?.activityId}`
async function load(){loading.value=true;try{[activities.value,mine.value]=await Promise.all([api('/published-activities'),api('/my-registrations')])}finally{loading.value=false}}
async function open(a){
  if(mine.value.some(r=>r.activityId===a.activityId&&r.status!=='0')){ElMessage.info('你已经报名该活动，可在“我的报名”查看进度');tab.value='mine';return}
  selected.value=a;for(const k of Object.keys(form))delete form[k];for(const f of a.formSchema.fields||[])form[f.key]=f.type==='checkbox'?[]:f.type==='number'?null:''
  const draft=await api(`/activities/${a.activityId}/draft`);if(draft)Object.assign(form,draft.formData)
  const cached=localStorage.getItem(key());restored=false
  if(cached){try{await ElMessageBox.confirm('发现本地未提交的草稿，是否恢复？','恢复草稿',{confirmButtonText:'恢复',cancelButtonText:'使用服务器草稿'});Object.assign(form,JSON.parse(cached));restored=true}catch{}}
  attachment.value=null;draftStatus.value=restored?'已恢复本地草稿；附件需重新选择':draft?'已回显服务器草稿':'';dirty=true;dialog.value=true
  clearInterval(timer);timer=setInterval(()=>{if(dialog.value&&dirty)saveDraft(true)},config.value.draftIntervalSeconds*1000)
}
async function saveDraft(auto=false){
  if(!selected.value||inflight)return;inflight=true
  const payload=JSON.parse(JSON.stringify(form));localStorage.setItem(key(),JSON.stringify(payload))
  try{await api('/registrations/draft',{method:'POST',body:{activityId:selected.value.activityId,formData:payload}});draftStatus.value='草稿已保存 · '+new Date().toLocaleTimeString();dirty=JSON.stringify(form)!==JSON.stringify(payload);if(!auto)ElMessage.success('草稿已保存')}
  catch(e){draftStatus.value=e.network?'网络不可用，已保存至本地':'草稿保存未完成：'+e.message;if(e.code==='DUPLICATE_409'){dirty=false;clearInterval(timer);localStorage.removeItem(key())}}
  finally{inflight=false}
}
async function submit(){
  if(inflight){ElMessage.info('正在保存草稿，请稍候再提交');return}busy.value=true;clearInterval(timer)
  try{const body=new FormData();body.append('activityId',selected.value.activityId);body.append('formData',JSON.stringify(form));if(attachment.value)body.append('attachment',attachment.value);await api('/registrations',{method:'POST',body});localStorage.removeItem(key());dirty=false;dialog.value=false;ElMessage.success('报名已提交，等待教师审核');tab.value='mine';await load()}
  catch{localStorage.setItem(key(),JSON.stringify(form));timer=setInterval(()=>{if(dialog.value&&dirty)saveDraft(true)},config.value.draftIntervalSeconds*1000)}finally{busy.value=false}
}
function changed(){dirty=true;if(selected.value)localStorage.setItem(key(),JSON.stringify(form))}
function fileSelected(event){const file=event.target.files[0];if(file&&file.size>config.value.uploadMaxBytes){ElMessage.error('文件超过允许大小');event.target.value='';return}attachment.value=file||null}
function closed(){clearInterval(timer);if(dirty&&selected.value)localStorage.setItem(key(),JSON.stringify(form));load()}
function inTime(a){const now=Date.now();return now>=new Date(a.startTime.replace(' ','T')+'+08:00').getTime()&&now<new Date(a.endTime.replace(' ','T')+'+08:00').getTime()}
async function show(r){if(r.status==='0'){const a=activities.value.find(x=>x.activityId===r.activityId);if(a)await open(a);else ElMessage.info('活动已下线，草稿暂不能修改');return}detail.value=await api(`/my-registrations/${r.regId}`);detailOpen.value=true}
async function online(){if(dialog.value&&dirty)await saveDraft(true)}
watch(form,()=>{if(dialog.value)changed()},{deep:true})
onMounted(async()=>{config.value=await api('/client-config');await load();window.addEventListener('online',online)})
onUnmounted(()=>{clearInterval(timer);window.removeEventListener('online',online)})
</script>
<template>
  <div class="page-heading"><div><div class="eyebrow">STUDENT SPACE</div><h1>你好，{{user.realName||user.userName}}</h1><p class="muted">把想参加的活动，变成你的校园经历。</p></div><el-button @click="load">刷新</el-button></div>
  <div class="stats"><div class="stat"><small>可浏览活动</small><strong>{{activities.length}}</strong></div><div class="stat"><small>等待审核</small><strong>{{formal.filter(r=>r.status==='1').length}}</strong></div><div class="stat"><small>审核通过</small><strong>{{formal.filter(r=>r.status==='2').length}}</strong></div><div class="stat"><small>已提交报名</small><strong>{{formal.length}}</strong></div></div>
  <el-tabs v-model="tab" class="section-tabs"><el-tab-pane label="活动广场" name="activities"/><el-tab-pane label="我的报名" name="mine"/></el-tabs>
  <template v-if="tab==='activities'"><section class="hero"><div><h2>下一场精彩，从参与开始。</h2><p>查看活动介绍，填写报名信息。<br>提交后，你可以随时在“我的报名”查看审核进度。</p></div><div class="hero-symbol">✳</div></section>
  <div class="toolbar"><el-input v-model="keyword" placeholder="搜索活动名称" clearable style="max-width:300px"/><span class="count-text">{{filtered.length}} 个活动</span></div>
  <div v-loading="loading" class="activity-grid"><article v-for="a in filtered" :key="a.activityId" class="activity-card"><div class="activity-banner"><span class="badge">CAMPUS / {{String(a.activityId).padStart(3,'0')}}</span><span class="letter">✳</span></div><div class="activity-content"><h3>{{a.title}}</h3><p>{{a.content}}</p><div class="activity-meta">报名截止 {{a.endTime}}<br>审核教师 {{a.reviewerName||'已分配'}}</div><div class="activity-actions"><small>{{inTime(a)?'正在接受报名':'当前未开放报名'}}</small><el-button type="primary" plain :disabled="!inTime(a)" @click="open(a)">查看并报名</el-button></div></div></article></div><el-empty v-if="!loading&&!filtered.length" description="暂时没有符合条件的活动"/>
  </template>
  <section v-else class="surface"><div class="surface-title"><h3>我的报名记录</h3><span class="count-text">含未提交草稿</span></div><el-table :data="mine" empty-text="还没有报名记录，去活动广场看看吧"><el-table-column prop="activityTitle" label="活动名称" min-width="180"/><el-table-column label="状态" width="110"><template #default="{row}"><el-tag :type="statusType(row.status)" effect="light">{{registrationNames[row.status]}}</el-tag></template></el-table-column><el-table-column prop="submitTime" label="提交时间" width="175"/><el-table-column prop="latestOpinion" label="审核意见" min-width="160" show-overflow-tooltip/><el-table-column label="操作" width="100"><template #default="{row}"><el-button link type="primary" @click="show(row)">{{row.status==='0'?'继续填写':'查看详情'}}</el-button></template></el-table-column></el-table></section>
  <el-dialog v-model="dialog" :title="selected?.title" width="650px" class="form-dialog" @closed="closed" :close-on-click-modal="false"><template v-if="selected"><div class="form-intro">{{selected.content}}<small>报名截止 {{selected.endTime}}</small></div><el-form label-position="top" @input="changed" @change="changed"><DynamicForm :fields="selected.formSchema.fields" :model-value="form"/><el-form-item label="佐证材料" :required="selected.formSchema.attachmentRequired"><input type="file" accept=".jpg,.png,.pdf" @change="fileSelected"/><div class="muted" style="width:100%;margin-top:7px">支持 jpg、png、pdf，单文件不超过 {{Math.round(config.uploadMaxBytes/1024/1024)}}MiB。草稿仅保存字段，文件需提交前选择。</div></el-form-item></el-form><p class="muted">{{draftStatus||'填写内容将定时保存为草稿'}}</p></template><template #footer><el-button @click="saveDraft(false)">暂存草稿</el-button><el-button type="primary" :loading="busy" @click="submit">提交报名</el-button></template></el-dialog>
  <el-dialog v-model="detailOpen" title="报名详情" width="620px"><template v-if="detail"><h3>{{detail.activity.title}}</h3><el-tag :type="statusType(detail.registration.status)">{{registrationNames[detail.registration.status]}}</el-tag><dl class="detail-fields"><template v-for="f in detail.activity.formSchema.fields" :key="f.key"><dt>{{f.label}}</dt><dd>{{Array.isArray(detail.registration.formData[f.key])?detail.registration.formData[f.key].join('、'):detail.registration.formData[f.key]||'—'}}</dd></template></dl><el-button v-if="detail.registration.hasAttachment" @click="download(`/my-registrations/${detail.registration.regId}/attachment`,'佐证材料')">下载佐证材料</el-button><div v-for="r in detail.reviews" :key="r.reviewId" class="form-intro" style="margin-top:18px">审核意见：{{r.opinion||'通过'}}<small>{{r.reviewTime}}</small></div></template></el-dialog>
</template>
