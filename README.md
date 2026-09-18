# Hello Travel

一期旅行对话助手：咨询与路线规划、邮箱账号、持久对话、多设备同步、隔离记忆与私有RAG知识库。本次交付止于代码生成，订单、支付和真实售后属于后续阶段。

## 项目结构

- 同级独立项目[hello-travel-app](../hello-travel-app/README.md)：Node24、React、TypeScript、Ant Design、Vite；浏览器SID绑定、完整分页恢复、SSE与游标补齐。
- `hello-travel-common/client/model/domain/application/infrastructure/adaptor/start`：Java17八模块DDD工程。领域模块无Spring/MyBatis依赖；启动模块装配Spring Boot、MyBatis-Plus、LangChain4j与LangGraph4j。
- `hello-travel-start/src/main/resources/db/migration/V1__initial_schema.sql`：MySQL单库 `hello-travel` 的17张表，Flyway唯一迁移来源。
- Milvus独立collection：`hello_travel_kb_v1_d1024`。向量存Milvus，原文、分块、状态与归属存MySQL。Redis仅用于原子限流，不承担唯一消息/登录事实。
- [技术方案](<AI/output/04 技术方案.md>)、[开发交付记录](<AI/output/05 开发交付记录.md>)、[接口与运行约定](AI/output/docs/API.md)。

## 本地配置与启动

要求Java17、Maven、Node24，以及本机MySQL8.4、Redis、Milvus。项目专用MySQL数据库名包含短横线，SQL使用反引号引用。

```sh
python3 scripts/setup_local.py
# 按.env.example在.env.local补充配置，尤其真实SMTP。
python3 scripts/dev.py backend
# 另一个终端，在同级前端项目启动
cd ../hello-travel-app
npm run dev
```

打开 `http://127.0.0.1:5173`。脚本只安全读取 `.zprofile` 的允许变量和忽略提交的 `.env.local` 字面值，不source配置、不输出密钥；现有百炼、高德和MySQL变量可直接沿用。后台本次已经通过Flyway创建17张业务表；新环境首次启动通过Flyway建表，禁止clean、baseline或重建既有数据。缺少SMTP时注册验证码、邮箱登录和重置密码明确不可用；没有模拟验证码后门。

Cookie在本机HTTP使用 `COOKIE_SECURE=false`，生产HTTPS必须true并配置精确 `ALLOWED_ORIGINS`、真实邮件、独立安全密钥、数据库TLS和凭据权限。生产配置不等于已完成部署验证。

## 构建与范围

```sh
mvn -B -ntp -DskipTests package
cd ../hello-travel-app
npm ci --no-fund --no-audit
npm run build
```

默认Maven执行Checkstyle与Flatten，禁止跳过规范门禁。`-DskipTests`只表示本次未自动推进完整开发自测，并会编译测试源码。具体构建证据见05文档。

同一浏览器重新登录替换旧SID，其他设备继续使用；旧页面无法借共享刷新Cookie绑定新SID。访问令牌只在页面内存，刷新Cookie为HttpOnly。刷新令牌轮换及已消费摘要账本用于重放检测，重放会撤销其原SID。

模型上下文使用保守UTF8预算，完整原始消息始终独立保存；最近完整问答、来源化滚动摘要和明确“请记住”的事实按用户和对话隔离。删除提高记忆代次并分批清除旧派生正文。模型调用未知结果保留任务证据，等待用户显式重试，不承诺供应商恰好调用一次。

资料仅支持TXT、Markdown和非加密PDF；原文件10MB、PDF50页、提取文本2MB、最多256块。超过限制失败并展示状态。文件和数据库不具备跨系统原子性，需要后续联调核实故障恢复。[景德镇样本](AI/output/knowledge/README.md)包含2024年运营方公开退款个案，已标明来源和适用限制；当前完整官网退改政策仍未取得。样本可由本人上传后真实索引，不预装READY假资料。

## 入口异常约束

Controller、Application、DomainService、OutAdaptor主入口分别try-catch并返回安全失败；HTTP失败保留相应状态。应用捕获位于事务完成/回滚外侧，内部失败不能提交部分数据。默认Checkstyle检查完整try-catch、throws及catch重抛，具体语义与回归证据见[开发交付记录](<AI/output/05 开发交付记录.md>)。AI接口/数据说明与证据统一AI/output；正式Flyway迁移仍是start模块运行资源。

## 当前业务代码质量约束（2026-09-18）

Java规范1.8：业务方法含私有辅助、回调按真实职责写中文编号步骤，实体持有初始化/校验/变化规则、聚合提供语义协作；迭代前后检查完整链路，复用职责并清理失效或重复代码。根validate默认执行Checkstyle及scripts/JavaBusinessQuality.java，后者扫描编号、45语句节点阈值与有限领域结构，不能证明注释含义或完整面向对象。独立CR和生产适配仍按流程执行。

```sh
mvn validate
java scripts/JavaBusinessQuality.java . --report AI/output/docs/verification/quality-method-inventory.csv
```

实际范围与证据见[当前开发交付记录](<AI/output/05 开发交付记录.md>)和AI/output/docs/verification/quality-summary.json；旧日志/哈希保留原日期。

## 业务领域与开发模式

领域与应用代码按auth、chat、memory、knowledge、sync组织；各域WriteDomainService只依赖本域仓储，Param/assembler/协作归对应业务目录。普通读、外部读、写、规则计算、纯计算是每个业务可采用的用例模式，不是全项目按模式各建总服务。跨域事务由Application/Flow显式协调；没有统一写入路由或全局仓储集合。详见AI/output/04技术方案第10章和05开发记录第13章。
