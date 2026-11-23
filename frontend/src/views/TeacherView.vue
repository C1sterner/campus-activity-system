<script setup>
import {computed,onMounted,ref} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import {api,download} from '../api'
const list=ref([]),search=ref(''),detail=ref(null),opened=ref(false),opinion=ref(''),busy=ref(false),loading=ref(false)
const filtered=computed(()=>list.value.filter(r=>`${r.activityTitle} ${r.studentName}`.includes(search.value)))
async function load(){loading.value=true;try{list.value=await api('/reviews/pending')}finally{loading.value=false}}
async function open(row){detail.value=await api(`/reviews/${row.regId}`);opinion.value='';opened.value=true}
async function decide(result){
  if(result==='2'&&!opinion.value.trim()){ElMessage.warning('驳回时必须填写审核原因');return}
  try{await ElMessageBox.confirm(result==='1'?'确认通过这条报名申请？':'确认驳回这条报名申请？','提交审核',{confirmButtonText:'确认提交',cancelButtonText:'再检查一下',type:result==='1'?'info':'warning'})}catch{return}
  busy.value=true
  try{const r=detail.value.registration;await api('/reviews',{method:'POST',body:{regId:r.regId,result,opinion:opinion.value,version:r.version}});ElMessage.success('审核已提交');opened.value=false;await load()}
  catch(e){if(e.code==='CONFLICT_409'){opened.value=false;await load()}}finally{busy.value=false}
}
onMounted(load)
</script>
<template>
  <div class="page-heading"><div><div class="eyebrow">REVIEW WORKSPACE</div><h1>审核中心</h1><p class="muted">这里只展示分配给你的活动报名，处理后自动移出待审核列表。</p></div><el-button @click="load">刷新列表</el-button></div>
  <section class="hero"><div><h2>{{list.length}} 条申请，等待你的审核。</h2><p>查看报名信息与佐证材料，给出清楚的审核结果。<br>驳回申请时，请向同学说明原因。</p></div><div class="hero-symbol">◎</div></section>
  <section class="surface"><div class="surface-title"><h3>待审核报名</h3><el-input v-model="search" clearable placeholder="搜索活动或学生" style="width:260px"/></div><el-table v-loading="loading" :data="filtered" empty-text="暂时没有待审核报名"><el-table-column prop="regId" label="编号" width="80"/><el-table-column prop="activityTitle" label="活动名称" min-width="190"/><el-table-column prop="studentName" label="学生" min-width="120"/><el-table-column prop="submitTime" label="提交时间" width="180"/><el-table-column label="状态" width="100"><template #default><el-tag type="warning" size="small">待审核</el-tag></template></el-table-column><el-table-column label="操作" width="110"><template #default="{row}"><el-button type="primary" plain size="small" @click="open(row)">查看并审核</el-button></template></el-table-column></el-table></section>
  <el-dialog v-model="opened" title="报名材料与审核" width="700px" :close-on-click-modal="false"><template v-if="detail"><div class="form-intro"><b>{{detail.activity.title}}</b><small>报名编号 #{{detail.registration.regId}} · 提交于 {{detail.registration.submitTime}}</small></div><dl class="detail-fields"><template v-for="f in detail.activity.formSchema.fields" :key="f.key"><dt>{{f.label}}</dt><dd>{{Array.isArray(detail.registration.formData[f.key])?detail.registration.formData[f.key].join('、'):detail.registration.formData[f.key]??'—'}}</dd></template></dl><el-button v-if="detail.registration.hasAttachment" @click="download(`/reviews/${detail.registration.regId}/attachment`,'佐证材料')">下载佐证材料</el-button><p v-else class="muted">本条报名未附佐证材料。</p><el-divider/><el-form label-position="top"><el-form-item label="审核意见"><el-input v-model="opinion" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="通过时可选填；驳回时请说明原因"/></el-form-item></el-form></template><template #footer><el-button @click="opened=false">取消</el-button><el-button type="danger" plain :loading="busy" @click="decide('2')">驳回申请</el-button><el-button type="primary" :loading="busy" @click="decide('1')">审核通过</el-button></template></el-dialog>
</template>
