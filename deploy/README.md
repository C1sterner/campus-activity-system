# 开发部署模板使用说明

这里提供 MySQL 开发模板和 Nginx 配置片段，尚无前后端容器镜像。没有启动数据库，也没有执行迁移。

在仓库根目录先复制 `.env.example` 为 `.env`，为两个密码变量填写不同的随机密码。当前模板仅绑定本机 `127.0.0.1:3307`，避免与现有 3306 服务冲突。Docker 必须先安装并启动。

```powershell
Copy-Item -LiteralPath .env.example -Destination .env
# 编辑 .env 后，在仓库根目录运行
docker compose --env-file .env -f deploy/compose.mysql.yaml up -d
```

只在全新数据卷初始化时，MySQL 官方入口才会执行 `db/mysql` 下的 SQL；已有数据卷不会自动应用新文件。不能把这个机制当作后续版本迁移工具。容器镜像当前仅指定 8.0 系列，首次通过测试后须固定补丁标签或镜像摘要并登记。

停止容器可使用同样参数加 `down`，不要在有数据时随意删除卷。数据库业务账号使用 `MYSQL_USER`，应用不得使用 root。文件备份需和数据库备份保持可恢复的一致时点。

Nginx 示例必须替换域名、证书和根目录，先运行 `nginx -t`。应用对应设置 `spring.servlet.multipart.max-file-size=20MB` 和 `max-request-size=22MB`，服务端业务校验仍以 20971520 字节为准；请求体还需限制表单 JSON 大小。此处没有提供或生成 HTTPS 证书。
