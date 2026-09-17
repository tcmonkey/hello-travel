# 数据模型

DDL唯一位置为[Flyway V1](../../../../hello-travel-start/src/main/resources/db/migration/V1__initial_schema.sql)，17张MySQL业务表。本次已实际迁移专用hello-travel库，V1冻结，后续变更新增迁移。Milvus向量集合由专用适配器延迟校验创建；本次未连接初始化集合。MySQL/Milvus说明和详细字段见[技术方案](<../../04 技术方案.md>)。
