<script setup>
import {onMounted,reactive,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {api} from '../api'
const emit=defineEmits(['login']),mode=ref('login'),busy=ref(false),captcha=ref({}),demo=ref(false)
const form=reactive({account:'',password:'',userName:'',email:'',confirmPassword:'',captcha:''})
async function refresh(){captcha.value=await api('/captcha');form.captcha=''}
async function submit(){busy.value=true;try{
  if(mode.value==='login'){const data=await api('/login',{method:'POST',body:{account:form.account,password:form.password,captcha:form.captcha,captchaId:captcha.value.captchaId}});emit('login',data)}
  else{await api('/register',{method:'POST',body:{...form,captchaId:captcha.value.captchaId}});ElMessage.success('注册成功，请登录');form.account=form.userName;mode.value='login'}
}catch{}finally{busy.value=false;await refresh()}}
async function toggle(){mode.value=mode.value==='login'?'register':'login';await refresh()}
function demoUser(account){form.account=account;form.password='Demo1234!'}
onMounted(async()=>{await refresh();demo.value=(await api('/health')).demo})
</script>
<template>
<div class="login-page">
  <section class="login-story"><div class="brand"><span class="brand-mark">活</span><div>校园活动<small>CAMPUS ACTIVITIES</small></div></div><div class="eyebrow">DISCOVER · PARTICIPATE · CONNECT</div><h1>在校园里，<br>找到你的下一次参与。</h1><p>发现值得参加的活动，在线填写报名材料，清楚查看每一次审核进展。</p><div class="story-foot">第23组 / 软件工程课程设计</div></section>
  <section class="login-panel"><div class="login-form"><div class="eyebrow">WELCOME TO CAMPUS</div><h2>{{mode==='login'?'欢迎回来':'创建学生账号'}}</h2><p class="muted">{{mode==='login'?'登录后进入你的活动工作空间':'使用邮箱注册，教师与管理员由后台创建账号'}}</p>
  <el-form label-position="top" @submit.prevent="submit">
    <template v-if="mode==='login'"><el-form-item label="账号"><el-input v-model="form.account" placeholder="请输入账号" autocomplete="username" size="large"/></el-form-item></template>
    <template v-else><el-form-item label="用户名"><el-input v-model="form.userName" placeholder="3至50位字母、数字或下划线" maxlength="50"/></el-form-item><el-form-item label="邮箱"><el-input v-model="form.email" type="email" placeholder="请输入邮箱" maxlength="100"/></el-form-item></template>
    <el-form-item label="密码"><el-input v-model="form.password" placeholder="至少8位，包含字母和数字" type="password" show-password autocomplete="current-password" size="large"/></el-form-item>
    <el-form-item v-if="mode==='register'" label="确认密码"><el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入密码"/></el-form-item>
    <el-form-item label="图片验证码"><div class="captcha-row"><el-input v-model="form.captcha" placeholder="输入右侧数字" maxlength="4" size="large" @keyup.enter="submit"/><img :src="captcha.image" alt="图片验证码，点击刷新" title="点击刷新" @click="refresh"/></div></el-form-item>
    <el-button class="login-submit" type="primary" native-type="submit" :loading="busy">{{mode==='login'?'登 录':'注 册'}}</el-button>
  </el-form><div style="text-align:center;margin-top:16px"><el-button link type="primary" @click="toggle">{{mode==='login'?'还没有账号？立即注册':'已有账号？返回登录'}}</el-button></div>
  <div v-if="demo&&mode==='login'" class="demo-note">课程演示账号 · 密码均为 Demo1234!<div class="demo-buttons"><button @click="demoUser('student1')">学生</button><button @click="demoUser('activity1')">活动管理员</button><button @click="demoUser('teacher1')">审核教师</button><button @click="demoUser('admin1')">系统管理员</button></div></div>
  </div></section>
</div>
</template>
