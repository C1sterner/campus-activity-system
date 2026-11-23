# 前端实现入口

已实现Vue3、Element Plus、Vite页面。Node22环境执行`npm ci`、`npm run dev`；默认代理后端`http://127.0.0.1:8080`，可通过`BACKEND_URL`覆盖。`npm run build`输出dist，整合后端请运行根目录`scripts/build.ps1`。学生、教师、活动管理员、系统管理员各有页面，登录token只存在当前标签页sessionStorage。
