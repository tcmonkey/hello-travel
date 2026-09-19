# 业务包与模型能力重构验证

日期：2026-09-19

## 范围

- 一级业务目录按认证、对话、知识库组织；跨业务 Web 支撑单独保留。
- 通用模型 action 端口拆为旅行回答、旅行意图、会话记忆和知识嵌入端口。
- LangChain4j `@AiService` 与命名 `ChatModel`、`StreamingChatModel`、`EmbeddingModel` 由 start 显式装配。
- RAG 保留 MySQL 归属、状态、删除代次、索引代次和正文哈希复核。

## 执行证据

| 检查 | 结果 |
|---|---|
| `mvn -q -s /tmp/ht-settings.xml -f pom.xml clean test` | 成功；40项JUnit失败、错误、跳过均为0 |
| Java业务质量门禁 | 322类、819方法/构造器、376流程块，0违规 |
| Spring离线装配 | 成功；无凭据禁用模型未触发外部调用 |
| 流式Agent回归 | 两个增量片段累积为完整草稿，进度回调收到全文 |
| 旧技术总包扫描 | `application/adaptor` 下无一级 `http/model/file/mail/security/travel` Java包 |
| solo流程回归 | workflow 10项、startup 14项、Java DDD门禁11项、installation 16项通过 |

## 边界

本轮未调用百炼、嵌入、高德、SMTP或Milvus真实服务，未写业务数据库。`@AiService`高阶接口、流式回调与离线装配已由编译和本地测试验证，供应商权限、真实usage、断流和向量召回质量仍属于后续完整自测与集成验证。
