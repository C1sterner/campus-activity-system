# 校园活动报名审核系统

本科软件工程课程演示项目。Java 17 + Spring Boot 3.5.7 + MyBatis，Vue 3 + Element Plus。支持学生、活动管理员、审核教师、系统管理员四类角色，完成活动发布、报名材料提交、教师审核与状态查询。

## 直接运行

下载 Release 运行包，解压后执行 `start-demo.cmd`（Windows）或运行：

```sh
java -jar campus-activity-0.1.0.jar
```

打开 [http://127.0.0.1:8080](http://127.0.0.1:8080)。只需要 JDK 17+，JAR 已包含前端和内置 H2 文件数据库；首次运行自动创建演示数据。

| 角色 | 账号 | 演示密码 |
| --- | --- | --- |
| 学生 | student1 | Demo1234! |
| 活动管理员 | activity1 | Demo1234! |
| 审核教师 | teacher1 | Demo1234! |
| 系统管理员 | admin1 | Demo1234! |

演示账号只适合本机课程展示，默认服务绑定 127.0.0.1。验证码需自行输入。系统数据保存在运行目录的 `data/`，重启后报名记录保留，登录会话需要重新建立。

## 基本功能

- 学生注册、四角色登录、个人资料与改密、后台新增和停用账号。
- 活动草稿、字段配置、指定审核教师、发布、下线。
- 动态报名表、jpg/png/pdf 材料、草稿与自动暂存、防重复报名。
- 指定教师审核、驳回原因校验、版本冲突检查、事务保存审核记录。
- 本人报名和意见查询、统计筛选、Excel 导出、基础配置与操作日志。

课程演示版不实现签到、多级审核、短信、名额抢占、复杂表单设计器、批量用户导入导出等扩展。未宣称达到100请求每秒或99.9%可用性。完整实现边界与 MySQL 启动方式见[使用说明](docs/11-演示版使用说明与实现范围.md)。

## 从源码构建

需要 JDK 17+、Node 22.x，首次下载依赖需要网络。后端自带 Maven Wrapper。

Windows PowerShell：

```powershell
.\scripts\build.ps1
java -jar backend/target/campus-activity-0.1.0.jar
```

macOS/Linux：

```sh
sh scripts/build.sh
java -jar backend/target/campus-activity-0.1.0.jar
```

前端单独开发：在 frontend 目录运行 `npm ci`、`npm run dev`；默认代理后端8080。后端开发可在 backend 目录运行 `mvnw.cmd spring-boot:run`。

## 测试与文档

- [测试说明书 DOCX 第1至6章](docs/校园活动报名审核系统测试说明书_第1至6章.docx)：保留样本39个标题，含源码、控制流图、基本路径、黑盒、边界和执行流程。
- [演示版使用说明与范围](docs/11-演示版使用说明与实现范围.md)
- [开发验证记录](docs/12-开发验证记录.md)
- [四人分工](docs/08-四人分工与课程交付范围.md)
- [本周计划](docs/09-本周项目计划书.md)

在 backend 目录运行 `mvnw.cmd test` 或 `./mvnw test`。测试使用隔离H2库；MySQL验证另用专用测试库，不能使用有真实数据的库。自动化脚本 `scripts/smoke-test.py` 仅允许本地演示服务，会创建 TEST_ 数据。

## 目录

```text
backend/     Java服务、MyBatis、数据库初始化与测试
frontend/    Vue页面与锁文件
db/          原设计六表SQL和并发设计参考
docs/        测试DOCX、使用说明、分工和课程计划
scripts/     构建、启动和本地接口验证
release/     运行包说明（大文件通过Release分发）
```

前期准备文档01至10保留项目过程记录；当前软件实际行为以代码与第11号使用说明为准。第1至6章DOCX是测试计划和设计，不含虚构的组员签署记录或最终验收结论。
