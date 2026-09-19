# 旅行对话三路编排与规划 Graph 验证

日期：2026-09-19  
来源：SRC-055、SRC-056、SRC-057  
阶段：05 编码开发证据；不替代 06 完整开发自测或独立 CR

## 实现检查

- `TravelApplication`集中完成上下文准备、结构化意图识别和 `DIALOGUE / FACT_QUERY / PLANNING`三路判断。
- 普通对话和事实问答不进入 Graph；事实问答先调用知识与实时事实能力。
- 规划图包含需求校验、目的地证据、实时事实、草稿、校验、最多两次修订、渲染和追问八个节点。
- LangGraph4j 类型限定在 Application graph 包；LangChain4j `@AiService`限定在 OutAdaptor 实现。
- `TravelPlanDomainService`拒绝日期/人数缺失、日程重叠、非法接驳、预算越界和缺少复核提示的未核实事实。
- 旧 `TravelGraph`、`TravelPlanningAgent`、`TravelIntentAgent`、planning workflow/agent 引用扫描为空。

## 实际反例

首轮把完整 `TravelConversationContext`写入图状态，LangGraph4j 克隆状态时出现 `NotSerializableException`。修订后图状态仅含 `contextKey`，执行上下文由 Application 内并发注册表按 UUID 隔离，Graph 在 `finally`无条件清理。`TravelPlanningGraphTest`执行真实条件边，证明缺失要求会在调用外部证据前转入追问；另一用例证明重叠、未核实无提示与预算超限不能绕过领域校验。

## 执行结果

命令：`mvn -q -pl hello-travel-start -am test`

- JUnit：52 项通过，失败 0，错误 0，跳过 0。
- Spring：无供应商凭据的离线 ApplicationContext 装配通过，包含 `TravelPlanDomainService`、`TravelPlanningGraph`和 `TravelApplication`。
- 业务质量：360 个类、916 个方法/构造器、413 个多语句流程块，违规 0。
- 源码检查：`git diff --check`通过；旧抽象和空参数 `@Component()`扫描为空。

## 尚未验证

- 真实百炼结构化意图、计划草稿、usage 和流式中断行为。
- 高德天气/路线准确性以及景区临时闭园、票务库存和政策权威来源。
- Milvus 真实召回质量、SMTP、Redis/MySQL 故障、多设备断流与并发体验。
- JVM 退出后的节点级恢复；当前注册表只服务单机同步图执行，持久恢复以数据库任务状态为准。
- 完整 06、自主代码评审、独立测试及发布阶段。

本轮按用户要求只记录 hello-travel 项目结论，没有写入 solo 或 DDD 公共 AI 架构规范。

## SRC-058补充证据

- `TravelApplicationRoutingTest`逐一验证普通对话、事实问答和规划图只调用对应业务能力，并验证`CONTEXT_LIMIT`同时写入run失败终态和返回结果。
- `TravelPlanningGraphTest`执行真实Graph条件边，覆盖直接渲染、一次修订后渲染、两次修订后追问及日期/交通/费用/来源领域反例。
- `TravelFactOutputConverterTest`证明天气响应裁剪为有界可读事实，零距离或非法时长不会进入模型上下文。
- 旅行意图、模型阶段、Graph节点、图版本和提示版本已有稳定类型或契约常量；旧字符串路由、旧节点拼写和旅行主链`Result.require()`扫描为空。
- 当前总计52项JUnit通过；业务门禁为360类、916方法/构造器、413流程块、0违规。

该证据仍不包含真实百炼、高德、Milvus、SMTP、多设备和进程故障验证。
