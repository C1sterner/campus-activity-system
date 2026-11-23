<script setup>
import {reactive,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {api,roleNames} from '../api'
const props=defineProps({user:Object}),emit=defineEmits(['saved']),busy=ref(false)
const form=reactive({realName:props.user.realName||'',email:props.user.email||'',phone:props.user.phone||'',oldPassword:'',newPassword:''})
async function save(){busy.value=true;try{const changed=!!form.newPassword;const updated=await api('/profile',{method:'PUT',body:form});ElMessage.success('资料已保存');form.oldPassword='';form.newPassword='';emit('saved',updated,changed)}catch{}finally{busy.value=false}}
</script>
<template><div class="page-heading"><div><div class="eyebrow">PERSONAL PROFILE</div><h1>个人中心</h1><p class="muted">维护基本资料，或修改你的登录密码。</p></div></div><section class="surface"><div class="surface-title"><h3>{{user.userName}}</h3><el-tag type="success" effect="plain">{{roleNames[user.role]}}</el-tag></div><el-form class="profile-form" label-position="top" @submit.prevent="save"><el-form-item label="姓名" required><el-input v-model="form.realName" maxlength="50"/></el-form-item><el-form-item label="邮箱" required><el-input v-model="form.email" maxlength="100"/></el-form-item><el-form-item label="手机号"><el-input v-model="form.phone" maxlength="20"/></el-form-item><el-divider content-position="left">修改密码（可选）</el-divider><el-form-item label="原密码"><el-input v-model="form.oldPassword" type="password" show-password/></el-form-item><el-form-item label="新密码"><el-input v-model="form.newPassword" type="password" show-password placeholder="8至64位，包含字母和数字"/></el-form-item><el-button type="primary" native-type="submit" :loading="busy">保存修改</el-button></el-form><p class="muted" style="margin:22px 0 0">最后登录：{{user.loginDate||'—'}} · 修改密码后需要重新登录。</p></section></template>
