# 旅行 AI 规划 Graph 设计

来源：SRC-055。本文先固定一期规划图的业务节点，再作为代码结构重构的依据。它是 hello-travel 的项目决策，当前不转写为 solo 或 DDD 公共规范。

## 1. 外部产品事实

- [携程 AI 行程助手](https://www.ctrip.com/tripplanner)公开页面要求目的地、游玩天数与旅行风格，并强调核验开放时间、建议游玩时长和交通时间。
- [TripGenie 官方介绍](https://www.trip.com/newsroom/introducing-tripgenie-groundbreaking-ai-travel-assistant/)说明对复杂需求做信息提取，生成结构化行程，推荐景点、购物、地图与预订链接，并支持后续调整。
- [世界旅游联盟公开演讲](https://www.wta-web.org/chn/observation/insights-inspirations-en/news_613)记录飞猪“问一问”以多 Agent 分工处理行程、机票、酒店、攻略、会员和预算，使用实时价格/库存、天气、地图与知识库。
- [携程集团 2025 可持续发展报告摘要](https://investors.trip.com/static-files/0fa048a3-5bcc-46d0-a1f7-66cd2baebad6)强调官方验证的开放时间、游玩时长、交通估算、灵活调整与人工协作。

上述来源只证明产品对外能力，无法证明内部代码的节点划分。本项目根据一期“咨询和规划，不交易”的边界重新建模。

## 2. Application 入口路由

`TravelApplication`先完成任务领取、会话上下文与意图识别，再使用三个明确分支：

1. `DIALOGUE`：普通旅行交流，不进入 Graph，直接调用对话模型。
2. `FACT_QUERY`：需要天气、路线、景区或政策事实的咨询，由 Application 先调用对应工具/知识能力，再调用对话模型。
3. `PLANNING`：用户要求创建、补全或调整多步行程，进入 `TravelPlanningGraph`。

意图识别、记忆装载、上下文压缩/预算与最终消息提交是三个分支的公用流程，不伪装成规划图的业务节点。

## 3. 一期 Graph 节点

| 节点 | 输入 | 主要协作 | 输出/路由 |
|---|---|---|---|
| `validate_request` | 结构化旅行意图 | `TravelPlanDomainService` | 核对目的地、出发地、日期/天数、人数、预算、偏好和特殊约束；缺必要信息转 `clarify_requirements` |
| `collect_destination_evidence` | 用户需求与目的地 | 知识库检索、景区信息端口 | 开放/闭园、预约、门票政策、建议游玩时长及来源；未核实项保留标记 |
| `collect_realtime_facts` | 出发地、目的地、日期 | 天气、地图、路线 OutAdaptor | 预报适用日期、距离、路程时间与可用状态；外部失败进入可降级事实 |
| `generate_draft` | 需求、历史、记忆、事实与来源 | `TravelPlanGenerationAdaptor` | 结构化逐日草稿，不直接形成领域认可的旅行计划 |
| `validate_draft` | 结构化草稿 | `TravelPlanDomainService` | 核对日期覆盖、时间重叠、路线缓冲、预算边界、事实来源与未核实声明 |
| `revise_draft` | 草稿与结构化违规项 | `TravelPlanGenerationAdaptor` | 最多两次修订并回到 `validate_draft`；不允许无界重试 |
| `render_plan` | 已校验草稿 | Application Assembler | 确定性投影路线、时间、优先级、提醒、来源和待核实项 |
| `clarify_requirements` | 缺失字段或不可安全修订的违规项 | Application Assembler | 生成简短、具体、可直接回答的追问，结束当前图 |

## 4. 边界决定

- LangGraph4j 依赖限定在 Application 的 `travel/planning/graph`内，Graph 类型不进入 Domain、Controller、公共 Model 和 Adaptor 契约。
- LangChain4j 仍位于 OutAdaptor；Application Graph Node 只调用按业务语义定义的 Agent/Adaptor 接口。
- Node 只负责步骤编排和状态更新；确定性计算与业务不变量进入 DomainService，内部持久化走 Repository，外部能力走 OutAdaptor。
- 一期不把机票、酒店、门票预订、支付、取消或真实售后建模成 Graph Node；只提供咨询、规划和待核实提示。
- 外部事实不可用时允许生成带降级标记的规划，但不允许模型把未核实价格、库存、开放时间或政策写成事实。

## 5. 暂不沉淀的内容

本设计会先在 hello-travel 实现并经过代码检查、反例与运行验证。在用户审阅通过之前，不把“Application 使用 LangGraph4j”、节点划分或 AI 防腐层命名写入公共 solo/DDD 规范。

## 6. 实现映射与可行性反思

| 设计责任 | 当前代码位置 | 边界说明 |
|---|---|---|
| 三路入口 | `application.chat.travel.service.TravelApplication` | 直接判断 `DIALOGUE / FACT_QUERY / PLANNING`；不建立动态 workflow 门面 |
| 普通/事实对话 | `dialogue.flow.TravelDialogueFlow` | 事实分支先收集工具和知识证据，随后复用同一流式对话端口 |
| 规划编排 | `planning.graph.TravelPlanningGraph`与八个 node | Application 依赖 LangGraph4j core，Node 按业务协作，不持有供应商 SDK |
| 确定性规则 | `domain.travel.service.TravelPlanDomainService` | 校验请求、日期、人数、日程重叠、接驳、预算、事实核实状态 |
| 模型防腐端口 | `intent/dialogue/planning.adaptor` | 稳定命令和中间模型不引用 LangChain4j；实现可替换为其他框架或平台 |
| 框架实现 | `adaptor.chat.output.intent/dialogue/planning` | LangChain4j `@AiService`负责结构化识别、流式对话和结构化草稿 |
| 外部事实 | `fact.adaptor`及 AMap OutAdaptor | 只读天气/路线；景区闭园和票务实时性仍是明确缺口 |

实际图测试发现 `TravelConversationContext`直接放入 LangGraph 状态时，框架克隆会抛出 `NotSerializableException`。将业务对象统一改成 `Serializable`会让框架约束扩散到 Domain/Model，也可能扩大 checkpoint 中的敏感信息范围。因此图状态现在只保存可序列化 `contextKey`，Application 内的 `TravelPlanningContextRegistry`以 `ConcurrentMap`保存本次同步执行上下文，并在 `finally`删除。并发键包含任务标识和随机 UUID，不覆盖同任务的并发/重放实例。

该桥接方式适用于当前单机、领取任务后同步完成一张图的约束，不等于节点级持久恢复。进程退出会丢失注册表，恢复仍以 MySQL 中的 run、消息、记忆、调用证据和 Outbox 为准；已向收费模型发送但结果未知的调用不盲目重放。未来多实例或长时间暂停图需要另行设计最小化、版本化、无敏感数据的 checkpoint，而不是持久化整个应用上下文。

验证覆盖两个关键反例：需求不完整时在任何外部证据调用前进入追问；计划含时间重叠、预算超限或未核实且无复核提示的事实时，领域校验拒绝直接渲染。当前 42 项 JUnit、Spring 离线装配和业务质量门禁通过。真实百炼结构化响应、高德时效、景区闭园权威来源、Milvus 召回、流式断线及进程崩溃恢复未执行，不能由本地通过推导为生产可行。

## 7. SRC-058代码调用链加固

为便于按代码推演，三类业务分支使用`TravelIntentMode`，八个节点与持久化检查点使用`TravelPlanningNode`，模型调用审计使用`ChatModelStage`，图和提示契约版本使用`TravelAiContract`。这些类型位于稳定Model或Application图定义中，不引用LangChain4j、LangGraph4j供应商对象。

完整成功路径、一次修订、两次修订停止、缺失信息提前追问、错误分类、日期/交通/费用/来源反例均已有直接测试。Graph仍按调用构建并编译，以避免未经框架线程安全证明就共享可变编译对象；业务上下文仍通过执行键隔离并在`finally`清理。是否缓存编译图需要性能证据和并发安全验证后再决定。
