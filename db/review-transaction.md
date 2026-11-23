# 事务与并发实现约束

## 审核

在一个数据库事务中执行：

```sql
UPDATE registration
SET status = :new_status, version = version + 1, update_time = CURRENT_TIMESTAMP
WHERE reg_id = :reg_id AND version = :version AND status = '1';
```

影响行数为 0 时回滚并返回 `CONFLICT_409`。影响行数为 1 时插入 `review_record`，插入失败必须回滚报名状态。审核前还要用服务端身份确认 `activity.reviewer_id = current_user_id`。

## 报名

草稿和正式提交共用 `(activity_id, user_id)` 唯一记录。正式提交应在一个事务中检查和更新草稿；唯一索引是并发请求的最终兜底。捕获重复键时返回 `DUPLICATE_409`，不把数据库异常直接暴露给用户。

## 文件孤儿清理

文件写入和数据库事务不是天然原子操作。先写临时随机文件，再提交报名事务；事务失败删除临时文件，提交成功后原子改名。定期任务扫描无引用文件，但不能凭路径让用户访问。草稿是否持久化文件遵循 D08。
