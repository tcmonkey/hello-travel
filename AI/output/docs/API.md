# API与运行约定

统一 `/api/v1`；JSON结果为 `success/code/message/data`，业务失败使用真实HTTP状态400/401/404/409/422/429/503。SQL、凭据、验证码和提示正文不进入错误响应。BIGINT主键、序号、版本、游标按字符串传输，前端使用BigInt比较。

## 认证

`POST /auth/{challenge,login,refresh,reset,logout}`，body字段：email/password/challengeId/code/purpose。purpose为LOGIN/RESET_PASSWORD。邮箱验证码登录同时适用于首次和既有用户：首次有效LOGIN码原子创建账号；密码可通过RESET_PASSWORD验证码设置或重置，长度为12～128字符。

所有写入要求精确允许的Origin。鉴权请求携带 `Authorization: Bearer <access>` 与 `X-Session-ID`；刷新另带 `X-CSRF-Token` 和HttpOnly Cookie。同设备Cookie为服务端签名随机设备标识。Cookie替换不会改变旧页面的SID；SID先于CSRF验证，旧页面不会撤销新SID。访问15分钟、登录绝对7天、验证码5分钟/5次。限流依赖Redis，Redis不可用时失败关闭。

## 对话与任务

`POST /chat/{bootstrap,list,create,rename,history,submit,run,cancel,retry,delete,delete-messages,context}`。

body可含conversationId/runId/title/text/requestKey/messageIds/expectedVersion/after/maxSeq/historyEpoch/limit。归属不由body决定。rename/delete/delete-messages须expectedVersion。submit须客户端UUIDrequestKey，同键同输入返回原任务、不同输入409；一轮固定用户与助手两个序号，重试不重复插入用户消息。

bootstrap返回恢复高水位syncSeq和对话主键上界maxSeq；list分页固定该上界，history固定消息maxSeq/historyEpoch并遍历hasMore。全部原消息展示与模型近期窗口分开。删除代次冲突要求重新拉该对话全量，普通草稿版本变化不重启分页。

context返回应用窗口、输入保守估算、输出和安全预留、压缩状态，同时给出最近任务。占比包含预留，不冒充供应商实际用量。供应商提供回答usage时另展示actualInputTokens/actualOutputTokens，缺失时不造数；其他阶段用量以模型调用记录区分。正在运行的一轮可cancel，失败/中断/取消可显式retry。

## 同步

`GET /sync?after=<eventSeq>`补齐连续持久事件，`GET /events?after=<eventSeq>`为fetch读取的SSE。SSE协议例外返回事件流；每账号最多4条、单实例最多100条连接、60秒重连。每轮重新核验原页面登录，约1秒轮询持久同步事件。客户端先REST补齐，再SSE并在断线后重连；保留窗口过期/缺口时409 SYNC_RESET_REQUIRED，全量恢复后接续。

事件只包含类型、公开目标ID、版本、任务引用和有界状态，不携带完整私密正文。客户端按消息版本合并，按事件序号幂等。当前单机推送通过持久事件轮询，不声称Redis广播或跨实例推送已实现。

## 私有知识库

`POST /knowledge/upload`为multipart file及可选HTTPS sourceUrl；`POST /knowledge/{list,read,retry,delete}`为documentId/expectedVersion/after/limit。

list展示状态与最多240字符明文预览，read展示有界提取原文，retry仅FAILED且提高索引代次。Milvus召回后以MySQL当前owner、READY、generation、正文hash和deletedAt二次校验。删除先SQL排除，再清理专用集合和受控文件。迟到旧向量通过定期对账删除；对账只清理小于所读当前代次的向量，避免旧任务误删未来代次；其他集合从不访问或重建。

## 故障约定

模型/嵌入/SMTP等待在数据库事务之外。发件箱和索引任务使用版本CAS与租约栅栏。未知收费结果和未知邮件发送不自动重放，任务中断后等待显式用户重试；向量upsert使用稳定块键。原始提示不持久到工作流状态或日志，模型调用表只记录分类、用量和供应商关联ID。

Actuator仅公开health且不展示详情；Micrometer记录HTTP观察指标，不把账号ID、对话ID、正文或凭据作为指标标签。正式运行前需06～08检查会话踢出、分页重建、SMTP、真实模型、Milvus与故障注入，本次未宣称这些结果已通过。
