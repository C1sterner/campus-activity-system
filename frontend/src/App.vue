<script setup>
import {onMounted,onUnmounted,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {api,roleNames,clearDrafts} from './api'
import LoginView from './views/LoginView.vue'
import StudentView from './views/StudentView.vue'
import TeacherView from './views/TeacherView.vue'
import ActivityAdminView from './views/ActivityAdminView.vue'
import SystemAdminView from './views/SystemAdminView.vue'
import ProfileView from './views/ProfileView.vue'
const user=ref(null),page=ref('home'),ready=ref(false)
const homeNames={student:'活动广场',teacher:'审核中心',activity_admin:'活动与报名',admin:'系统管理'}
function loggedIn(data){sessionStorage.setItem('campus-token',data.token);user.value=data.user;page.value='home'}
function expired(){user.value=null;page.value='home'}
async function logout(){try{await api('/logout',{method:'POST'})}finally{if(user.value)clearDrafts(user.value.userId);sessionStorage.removeItem('campus-token');expired()}}
function profileSaved(updated,passwordChanged){user.value=updated;if(passwordChanged){clearDrafts(updated.userId);sessionStorage.removeItem('campus-token');expired();ElMessage.success('密码已修改，请重新登录')}}
onMounted(async()=>{window.addEventListener('session-expired',expired);try{if(sessionStorage.getItem('campus-token'))user.value=await api('/profile')}catch{}finally{ready.value=true}})
onUnmounted(()=>window.removeEventListener('session-expired',expired))
</script>
<template>
  <div v-if="!ready" class="loading-page">正在连接校园活动服务…</div>
  <LoginView v-else-if="!user" @login="loggedIn"/>
  <div v-else class="app-layout">
    <aside class="sidebar">
      <div class="brand"><span class="brand-mark">活</span><div>校园活动<small>CAMPUS ACTIVITIES</small></div></div>
      <p class="nav-label">工作空间</p>
      <button :class="['nav-item',{active:page==='home'}]" @click="page='home'"><span>▦</span>{{homeNames[user.role]}}</button>
      <button :class="['nav-item',{active:page==='profile'}]" @click="page='profile'"><span>◎</span>个人中心</button>
      <div class="sidebar-bottom"><span class="online-dot"></span>课程演示版 v0.1<small>第23组 · 软件工程课程设计</small></div>
    </aside>
    <div class="workspace">
      <header class="topbar"><span class="breadcrumb">校园活动 / <b>{{page==='profile'?'个人中心':homeNames[user.role]}}</b></span><div class="user-menu"><el-tag effect="plain" type="success">{{roleNames[user.role]}}</el-tag><span class="avatar">{{(user.realName||user.userName).slice(0,1)}}</span><span>{{user.realName||user.userName}}</span><el-button text @click="logout">退出</el-button></div></header>
      <main>
        <ProfileView v-if="page==='profile'" :user="user" @saved="profileSaved"/>
        <StudentView v-else-if="user.role==='student'" :user="user"/>
        <TeacherView v-else-if="user.role==='teacher'"/>
        <ActivityAdminView v-else-if="user.role==='activity_admin'"/>
        <SystemAdminView v-else/>
      </main>
      <footer>校园活动报名审核系统 · 让报名与审核更清楚</footer>
    </div>
  </div>
</template>
