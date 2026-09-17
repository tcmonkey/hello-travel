-- hello-travel 一期MySQL迁移基线 0.3，17张业务表
-- SRC-017用户明确授权一期实施；仅初始化项目专用数据库，不重建现有服务数据。
-- 本文件是Flyway V1唯一来源，执行后不得修改，后续变更须新增迁移。
-- 只为新项目空schema初始化，不含DROP/TRUNCATE/其他数据库写入。
-- 每项带归属的外键只防跨归属写入；读取权限仍必须在服务端校验。
-- 所有时间为UTC；MEDIUMTEXT/JSON字段由应用另行限制长度。
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- 账号及用户同步序号
CREATE TABLE ht_user_account (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不直接作为前端数值ID',
  public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对外ULID字符串标识',
  email_normalized VARCHAR(254) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '应用规范化后的唯一邮箱',
  password_hash VARCHAR(255) NOT NULL COMMENT '带算法和参数的加盐密码哈希，不存明文',
  email_verified_at DATETIME(6) NOT NULL COMMENT '邮箱验证成功时间',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态',
  auth_epoch BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '全设备撤销版本，重置密码时递增',
  sync_seq BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '按用户分配并提交的持久同步序号',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
  version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，更新时递增',
PRIMARY KEY (id),
  UNIQUE KEY uk_account_public (public_id),
  UNIQUE KEY uk_account_email (email_normalized),
  CONSTRAINT ck_account_status CHECK (status IN ('ACTIVE','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号及用户同步序号';

-- 账号下的浏览器设备实例
CREATE TABLE ht_device (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不直接作为前端数值ID',
  public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对外ULID字符串标识',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '账号归属',
  device_key_hash BINARY(32) NOT NULL COMMENT '服务端设备Cookie随机标识的哈希，不是物理指纹',
  device_label VARCHAR(120) NOT NULL COMMENT '可读设备名称，安全文本',
  last_seen_at DATETIME(6) NOT NULL COMMENT '最近活跃时间',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
  version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，更新时递增',
PRIMARY KEY (id),
  UNIQUE KEY uk_device_public (public_id),
  UNIQUE KEY uk_device_identity (user_id,device_key_hash),
  UNIQUE KEY uk_device_owner (user_id,id),
  CONSTRAINT fk_device_user FOREIGN KEY (user_id) REFERENCES ht_user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号下的浏览器设备实例';

-- 可撤销的页面登录会话
CREATE TABLE ht_login_session (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不直接作为前端数值ID',
  public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对外ULID字符串标识',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '账号归属',
  device_id BIGINT UNSIGNED NOT NULL COMMENT '浏览器设备实例',
  access_token_hash BINARY(32) NULL COMMENT '随机访问令牌哈希，不存原始令牌',
  refresh_token_hash BINARY(32) NULL COMMENT '轮换刷新令牌哈希，不存原始令牌',
  csrf_token_hash BINARY(32) NULL COMMENT 'CSRF校验随机值哈希',
  auth_epoch BIGINT UNSIGNED NOT NULL COMMENT '签发时账号全局撤销版本',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态',
  revoke_reason VARCHAR(40) NULL COMMENT '被踢、退出、重置或重放等撤销原因',
  access_expires_at DATETIME(6) NOT NULL COMMENT '访问令牌到期时间',
  refresh_expires_at DATETIME(6) NOT NULL COMMENT '刷新令牌到期时间',
  last_seen_at DATETIME(6) NOT NULL COMMENT '最近活跃时间',
  revoked_at DATETIME(6) NULL COMMENT '撤销时间',
  active_device_id BIGINT UNSIGNED GENERATED ALWAYS AS
    (CASE WHEN status = 'ACTIVE' THEN device_id ELSE NULL END) STORED COMMENT '设备内唯一活动会话约束槽',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
  version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，更新时递增',
PRIMARY KEY (id),
  UNIQUE KEY uk_session_public (public_id),
  UNIQUE KEY uk_session_access (access_token_hash),
  UNIQUE KEY uk_session_refresh (refresh_token_hash),
  UNIQUE KEY uk_session_active_device (active_device_id),
  UNIQUE KEY uk_session_owner (user_id,id),
  KEY ix_session_user_state (user_id,status,refresh_expires_at),
  CONSTRAINT fk_session_device FOREIGN KEY (user_id,device_id) REFERENCES ht_device(user_id,id),
  CONSTRAINT ck_session_status CHECK (status IN ('ACTIVE','REVOKED','EXPIRED')),
  CONSTRAINT ck_session_expiry CHECK (access_expires_at <= refresh_expires_at),
  CONSTRAINT ck_session_credentials CHECK (status <> 'ACTIVE' OR
    (access_token_hash IS NOT NULL AND refresh_token_hash IS NOT NULL AND csrf_token_hash IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='可撤销的页面登录会话';

-- 用途隔离的一次性邮箱验证码
CREATE TABLE ht_email_challenge (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不直接作为前端数值ID',
  public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对外ULID字符串标识',
  email_normalized VARCHAR(254) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '目标邮箱，注册前可无账号',
  purpose VARCHAR(16) NOT NULL COMMENT '注册、登录或重置用途',
  code_hmac BINARY(32) NOT NULL COMMENT '含挑战ID、邮箱和用途的验证码HMAC',
  code_key_version VARCHAR(32) NOT NULL COMMENT '验证码校验密钥版本标识，无密钥值',
  delivery_ciphertext VARBINARY(512) NULL COMMENT '待发送验证码AEAD密文封装，发送后清除',
  delivery_key_version VARCHAR(32) NULL COMMENT '发送密钥版本标识，独立于校验密钥',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING_SEND' COMMENT '投递及消费状态',
  attempts SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '失败验证次数',
  max_attempts SMALLINT UNSIGNED NOT NULL DEFAULT 5 COMMENT '允许的失败次数上限',
  expires_at DATETIME(6) NOT NULL COMMENT '验证码到期时间',
  consumed_at DATETIME(6) NULL COMMENT '成功消费时间',
  active_slot TINYINT GENERATED ALWAYS AS
    (CASE WHEN status IN ('PENDING_SEND','ISSUED') THEN 1 ELSE NULL END) STORED COMMENT '同邮箱同用途唯一有效槽',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
  version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，更新时递增',
PRIMARY KEY (id),
  UNIQUE KEY uk_challenge_public (public_id),
  UNIQUE KEY uk_challenge_active (email_normalized,purpose,active_slot),
  KEY ix_challenge_expiry (status,expires_at),
  CONSTRAINT ck_challenge_purpose CHECK (purpose IN ('REGISTER','LOGIN','RESET_PASSWORD')),
  CONSTRAINT ck_challenge_status CHECK
    (status IN ('PENDING_SEND','ISSUED','CONSUMED','EXPIRED','FAILED','REVOKED')),
  CONSTRAINT ck_challenge_attempts CHECK (max_attempts > 0 AND attempts <= max_attempts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用途隔离的一次性邮箱验证码';

-- 用户拥有的对话tab及序号/记忆版本
CREATE TABLE ht_conversation (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不直接作为前端数值ID',
  public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对外ULID字符串标识',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '账号归属，所有访问必须校验',
  title VARCHAR(120) NOT NULL COMMENT '对话标题，作为纯文本展示',
  last_message_seq BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已分配的最大消息序号',
  history_epoch BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '消息删除代次；流式内容更新不递增',
  memory_epoch BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除或重建时递增，隔离过期派生记忆',
  last_activity_at DATETIME(6) NOT NULL COMMENT '最近活动时间，侧栏排序依据',
  deleted_at DATETIME(6) NULL COMMENT '逻辑删除时间，删除后禁止推理及查询',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
  version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，更新时递增',
PRIMARY KEY (id),
  UNIQUE KEY uk_conversation_public (public_id),
  UNIQUE KEY uk_conversation_owner (user_id,id),
  KEY ix_conversation_list (user_id,deleted_at,last_activity_at,id),
  CONSTRAINT fk_conversation_user FOREIGN KEY (user_id) REFERENCES ht_user_account(id),
  CONSTRAINT ck_conversation_title CHECK (CHAR_LENGTH(TRIM(title)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户拥有的对话tab及序号/记忆版本';

-- 完整可持久化的用户及助手消息
CREATE TABLE ht_message (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不直接作为前端数值ID',
  public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对外ULID字符串标识',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '账号归属',
  conversation_id BIGINT UNSIGNED NOT NULL COMMENT '对话归属',
  message_seq BIGINT UNSIGNED NOT NULL COMMENT '对话内稳定顺序，删除不复用',
  role VARCHAR(16) NOT NULL COMMENT '用户或助手；系统规则不混作用户消息',
  status VARCHAR(16) NOT NULL COMMENT '提交、生成、完成或失败等状态',
  content MEDIUMTEXT NOT NULL COMMENT '消息正文；流式草稿可定期保存，受应用大小限制',
  citations_json JSON NULL COMMENT '已核验的引用片段ID、来源、日期和版本',
  deleted_at DATETIME(6) NULL COMMENT '逻辑删除时间，后续不得参与记忆或检索',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
  version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，更新时递增',
PRIMARY KEY (id),
  UNIQUE KEY uk_message_public (public_id),
  UNIQUE KEY uk_message_sequence (user_id,conversation_id,message_seq),
  UNIQUE KEY uk_message_owner (user_id,conversation_id,id),
  KEY ix_message_history (user_id,conversation_id,deleted_at,message_seq),
  CONSTRAINT fk_message_conversation FOREIGN KEY (user_id,conversation_id)
    REFERENCES ht_conversation(user_id,id),
  CONSTRAINT ck_message_role CHECK (role IN ('USER','ASSISTANT')),
  CONSTRAINT ck_message_status CHECK
    (status IN ('ACCEPTED','STREAMING','COMPLETED','FAILED','CANCELLED','INTERRUPTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='完整可持久化的用户及助手消息';

-- 幂等生成任务及LangGraph4j步骤状态
CREATE TABLE ht_chat_run (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不直接作为前端数值ID',
  public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对外ULID字符串标识',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '账号归属',
  conversation_id BIGINT UNSIGNED NOT NULL COMMENT '对话归属',
  initiating_session_id BIGINT UNSIGNED NOT NULL COMMENT '发起时登录会话，仅作审计归属',
  request_key CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '客户端幂等请求UUID',
  request_digest BINARY(32) NOT NULL COMMENT '请求规范化正文摘要，禁止同键换内容',
  user_message_id BIGINT UNSIGNED NOT NULL COMMENT '已持久化的输入消息',
  assistant_message_id BIGINT UNSIGNED NOT NULL COMMENT '已预分配的回答消息',
  status VARCHAR(16) NOT NULL DEFAULT 'ACCEPTED' COMMENT '生成状态；终态不自动重复调用模型',
  attempt_count SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '显式重试代次，不重复创建用户消息',
  graph_node VARCHAR(64) NULL COMMENT '最近完成或正在执行的图节点',
  graph_revision VARCHAR(32) NOT NULL COMMENT '工作流代码版本，恢复时检查兼容',
  memory_epoch_at_start BIGINT UNSIGNED NOT NULL COMMENT '推理输入使用的记忆代次',
  state_json JSON NULL COMMENT '受限图状态及已脱敏工具结果，不保存密钥',
  context_snapshot_json JSON NULL COMMENT '本轮预算组成、估算方法及使用进度',
  lease_owner VARCHAR(64) NULL COMMENT '当前执行者标识',
  lease_fence BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租约代次，旧执行者不可提交',
  lease_until DATETIME(6) NULL COMMENT '执行租约到期时间',
  error_code VARCHAR(64) NULL COMMENT '内部稳定错误码，不保存原始供应商错误',
  started_at DATETIME(6) NULL COMMENT '生成开始时间',
  completed_at DATETIME(6) NULL COMMENT '终态时间',
  active_conversation_id BIGINT UNSIGNED GENERATED ALWAYS AS
    (CASE WHEN status IN ('ACCEPTED','RUNNING') THEN conversation_id ELSE NULL END)
    STORED COMMENT '对话内唯一进行中任务约束槽',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
  version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，更新时递增',
PRIMARY KEY (id),
  UNIQUE KEY uk_run_public (public_id),
  UNIQUE KEY uk_run_request (user_id,conversation_id,request_key),
  UNIQUE KEY uk_run_active_conversation (active_conversation_id),
  UNIQUE KEY uk_run_user_message (user_message_id),
  UNIQUE KEY uk_run_assistant_message (assistant_message_id),
  UNIQUE KEY uk_run_owner (user_id,conversation_id,id),
  KEY ix_run_recovery (status,lease_until,id),
  CONSTRAINT fk_run_conversation FOREIGN KEY (user_id,conversation_id)
    REFERENCES ht_conversation(user_id,id),
  CONSTRAINT fk_run_session FOREIGN KEY (user_id,initiating_session_id)
    REFERENCES ht_login_session(user_id,id),
  CONSTRAINT fk_run_user_message FOREIGN KEY (user_id,conversation_id,user_message_id)
    REFERENCES ht_message(user_id,conversation_id,id),
  CONSTRAINT fk_run_assistant_message FOREIGN KEY (user_id,conversation_id,assistant_message_id)
    REFERENCES ht_message(user_id,conversation_id,id),
  CONSTRAINT ck_run_message_pair CHECK (user_message_id <> assistant_message_id),
  CONSTRAINT ck_run_status CHECK
    (status IN ('ACCEPTED','RUNNING','COMPLETED','FAILED','CANCELLED','INTERRUPTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='幂等生成任务及LangGraph4j步骤状态';

-- 按对话代次生成的短期结构化摘要
CREATE TABLE ht_memory_summary (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不直接作为前端数值ID',
  public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对外ULID字符串标识',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '账号归属',
  conversation_id BIGINT UNSIGNED NOT NULL COMMENT '对话归属',
  memory_epoch BIGINT UNSIGNED NOT NULL COMMENT '与对话代次匹配才可使用',
  covered_from_seq BIGINT UNSIGNED NOT NULL COMMENT '摘要来源最小消息序号',
  covered_through_seq BIGINT UNSIGNED NOT NULL COMMENT '摘要来源最大消息序号',
  structured_content JSON NOT NULL COMMENT '有界摘要，包含约束、事实、问题、引用及来源序号',
  estimated_tokens INT UNSIGNED NOT NULL COMMENT '摘要token估算，非供应商实际值',
  estimator_version VARCHAR(40) NOT NULL COMMENT '估算方法版本',
  model_name VARCHAR(120) NOT NULL COMMENT '摘要使用的模型名称',
  prompt_revision VARCHAR(32) NOT NULL COMMENT '压缩提示模板版本',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '摘要是否仍有效',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
  version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，更新时递增',
PRIMARY KEY (id),
  UNIQUE KEY uk_summary_public (public_id),
  UNIQUE KEY uk_summary_boundary (user_id,conversation_id,memory_epoch,covered_through_seq),
  KEY ix_summary_current (user_id,conversation_id,memory_epoch,status,covered_through_seq),
  CONSTRAINT fk_summary_conversation FOREIGN KEY (user_id,conversation_id)
    REFERENCES ht_conversation(user_id,id),
  CONSTRAINT ck_summary_range CHECK (covered_from_seq > 0 AND covered_from_seq <= covered_through_seq),
  CONSTRAINT ck_summary_status CHECK (status IN ('ACTIVE','INVALIDATED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='按对话代次生成的短期结构化摘要';

-- 有来源且会话隔离的长期旅行记忆
CREATE TABLE ht_memory_fact (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不直接作为前端数值ID',
  public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对外ULID字符串标识',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '账号归属',
  conversation_id BIGINT UNSIGNED NOT NULL COMMENT '一期禁止跨对话共享长期事实',
  memory_epoch BIGINT UNSIGNED NOT NULL COMMENT '当前有效记忆代次',
  fact_key VARCHAR(120) NOT NULL COMMENT '稳定语义键，如出行预算，不由前端决定归属',
  category VARCHAR(32) NOT NULL COMMENT '偏好、限制或已确认计划',
  content TEXT NOT NULL COMMENT '有界事实文本，禁止模型推测冒充用户确认',
  evidence_type VARCHAR(32) NOT NULL DEFAULT 'USER_EXPLICIT' COMMENT '一期只采纳用户明确陈述',
  source_count SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '来源数，使用前必须验证至少一个有效来源',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '有效、失效或到期',
  expires_at DATETIME(6) NULL COMMENT '有时间适用性的事实到期时间',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
  version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，更新时递增',
PRIMARY KEY (id),
  UNIQUE KEY uk_fact_public (public_id),
  UNIQUE KEY uk_fact_owner (user_id,conversation_id,id),
  UNIQUE KEY uk_fact_semantic (user_id,conversation_id,memory_epoch,fact_key),
  KEY ix_fact_current (user_id,conversation_id,memory_epoch,status,expires_at),
  CONSTRAINT fk_fact_conversation FOREIGN KEY (user_id,conversation_id)
    REFERENCES ht_conversation(user_id,id),
  CONSTRAINT ck_fact_category CHECK (category IN ('PREFERENCE','TRAVEL_CONSTRAINT','CONFIRMED_PLAN')),
  CONSTRAINT ck_fact_evidence CHECK (evidence_type = 'USER_EXPLICIT'),
  CONSTRAINT ck_fact_status CHECK (status IN ('ACTIVE','INVALIDATED','EXPIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='有来源且会话隔离的长期旅行记忆';

-- 长期事实到原始消息的可撤销来源关系
CREATE TABLE ht_memory_fact_source (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '账号归属',
  conversation_id BIGINT UNSIGNED NOT NULL COMMENT '对话归属',
  fact_id BIGINT UNSIGNED NOT NULL COMMENT '长期事实标识',
  message_id BIGINT UNSIGNED NOT NULL COMMENT '来源原始用户消息标识',
  evidence_excerpt VARCHAR(500) NOT NULL COMMENT '可核对的有界证据摘录，来源删除后同步清除',
  message_version BIGINT UNSIGNED NOT NULL COMMENT '引用时原消息版本',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
PRIMARY KEY (id),
  UNIQUE KEY uk_fact_source (fact_id,message_id),
  KEY ix_fact_source_message (user_id,conversation_id,message_id),
  CONSTRAINT fk_source_fact FOREIGN KEY (user_id,conversation_id,fact_id)
    REFERENCES ht_memory_fact(user_id,conversation_id,id),
  CONSTRAINT fk_source_message FOREIGN KEY (user_id,conversation_id,message_id)
    REFERENCES ht_message(user_id,conversation_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='长期事实到原始消息的可撤销来源关系';

-- 用户私有知识原文及处理状态
CREATE TABLE ht_knowledge_document (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不直接作为前端数值ID',
  public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对外ULID字符串标识',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '账号归属，一期无隐式全站共享',
  title VARCHAR(200) NOT NULL COMMENT '资料标题',
  original_filename VARCHAR(255) NOT NULL COMMENT '原文件名仅用于展示，不作磁盘路径',
  mime_type VARCHAR(100) NOT NULL COMMENT '服务端检测的内容类型',
  storage_key VARCHAR(255) NOT NULL COMMENT '服务端生成的相对存储键，禁止任意路径',
  byte_size BIGINT UNSIGNED NOT NULL COMMENT '原文件大小，应用配置上限',
  content_sha256 BINARY(32) NOT NULL COMMENT '原始文件SHA256校验及重复导入识别',
  extracted_text MEDIUMTEXT NULL COMMENT '提取的明文，页面分段加载；应用有提取上限',
  source_url VARCHAR(2048) NULL COMMENT '官方来源链接或用户提供的来源，仅作引用',
  source_accessed_at DATETIME(6) NULL COMMENT '资料访问/核验时间',
  policy_effective_at DATETIME(6) NULL COMMENT '来源明确提供时记录政策生效时间',
  status VARCHAR(16) NOT NULL DEFAULT 'RECEIVED' COMMENT '上传、解析、索引及删除状态',
  index_generation BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '重试/重建的索引代次，防旧任务覆盖',
  embedding_model VARCHAR(120) NOT NULL COMMENT '嵌入模型名称',
  embedding_dimension SMALLINT UNSIGNED NOT NULL COMMENT '固定向量维度，变化需新集合',
  collection_name VARCHAR(128) NOT NULL COMMENT 'hello_travel专用Milvus集合名',
  error_code VARCHAR(64) NULL COMMENT '可展示内部错误码',
  deleted_at DATETIME(6) NULL COMMENT '逻辑删除时间，后续RAG立即排除',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
  version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，更新时递增',
PRIMARY KEY (id),
  UNIQUE KEY uk_document_public (public_id),
  UNIQUE KEY uk_document_owner (user_id,id),
  KEY ix_document_list (user_id,deleted_at,created_at,id),
  KEY ix_document_hash (user_id,content_sha256),
  CONSTRAINT fk_document_user FOREIGN KEY (user_id) REFERENCES ht_user_account(id),
  CONSTRAINT ck_document_state CHECK
    (status IN ('RECEIVED','PARSING','INDEXING','READY','FAILED','DELETING','DELETED')),
  CONSTRAINT ck_document_dimension CHECK (embedding_dimension > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户私有知识原文及处理状态';

-- 带来源和代次的RAG分块
CREATE TABLE ht_knowledge_chunk (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不直接作为前端数值ID',
  public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对外ULID字符串标识',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '账号归属',
  document_id BIGINT UNSIGNED NOT NULL COMMENT '知识文档归属',
  index_generation BIGINT UNSIGNED NOT NULL COMMENT '匹配文档当前索引代次',
  chunk_no INT UNSIGNED NOT NULL COMMENT '文档内分块顺序',
  content TEXT NOT NULL COMMENT '块正文，检索后在MySQL二次校验',
  content_sha256 BINARY(32) NOT NULL COMMENT '块内容摘要，防过期向量对应错误正文',
  estimated_tokens INT UNSIGNED NOT NULL COMMENT '分块token估算',
  vector_key CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Milvus主键，对应块public_id',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '分块索引状态',
  deleted_at DATETIME(6) NULL COMMENT '块删除时间',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
  version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，更新时递增',
PRIMARY KEY (id),
  UNIQUE KEY uk_chunk_public (public_id),
  UNIQUE KEY uk_chunk_order (user_id,document_id,index_generation,chunk_no),
  UNIQUE KEY uk_chunk_vector (vector_key),
  KEY ix_chunk_current (user_id,document_id,index_generation,status,deleted_at),
  CONSTRAINT fk_chunk_document FOREIGN KEY (user_id,document_id)
    REFERENCES ht_knowledge_document(user_id,id),
  CONSTRAINT ck_chunk_status CHECK (status IN ('PENDING','READY','FAILED','DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='带来源和代次的RAG分块';

-- 可重试的知识解析/索引/清理任务
CREATE TABLE ht_index_job (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不直接作为前端数值ID',
  public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对外ULID字符串标识',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '账号归属',
  document_id BIGINT UNSIGNED NOT NULL COMMENT '目标文档',
  index_generation BIGINT UNSIGNED NOT NULL COMMENT '任务绑定的索引代次',
  job_type VARCHAR(16) NOT NULL COMMENT '导入或删除索引任务',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
  attempt_count SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前尝试次数',
  max_attempts SMALLINT UNSIGNED NOT NULL DEFAULT 3 COMMENT '重试次数上限',
  next_attempt_at DATETIME(6) NOT NULL COMMENT '下次可执行时间',
  lease_owner VARCHAR(64) NULL COMMENT '任务执行者',
  lease_fence BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '执行租约代次',
  lease_until DATETIME(6) NULL COMMENT '租约截止时间',
  error_code VARCHAR(64) NULL COMMENT '稳定错误码',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
  version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，更新时递增',
PRIMARY KEY (id),
  UNIQUE KEY uk_job_public (public_id),
  UNIQUE KEY uk_job_generation (user_id,document_id,index_generation,job_type),
  KEY ix_job_pending (status,next_attempt_at,lease_until,id),
  CONSTRAINT fk_job_document FOREIGN KEY (user_id,document_id)
    REFERENCES ht_knowledge_document(user_id,id),
  CONSTRAINT ck_job_type CHECK (job_type IN ('INGEST','DELETE')),
  CONSTRAINT ck_job_status CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','FAILED','CANCELLED')),
  CONSTRAINT ck_job_attempts CHECK (max_attempts > 0 AND attempt_count <= max_attempts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='可重试的知识解析/索引/清理任务';

-- 按用户有序且可补齐的跨设备同步事件
CREATE TABLE ht_sync_event (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不作为客户端补齐游标',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '目标账号，其他账号不得订阅',
  event_seq BIGINT UNSIGNED NOT NULL COMMENT '该用户的连续提交序号；来自账号sync_seq',
  event_type VARCHAR(80) NOT NULL COMMENT '消息、会话、用量、资料或会话撤销事件类型',
  aggregate_public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '事件目标对外ID',
  aggregate_version BIGINT UNSIGNED NOT NULL COMMENT '目标实体版本，避免重放覆盖新状态',
  target_session_public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '定向踢登录会话事件的目标',
  payload_json JSON NOT NULL COMMENT '有界ID/状态/版本载荷，不含令牌、验证码或完整私密正文',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '与业务事务一起提交的UTC时间',
  expires_at DATETIME(6) NOT NULL COMMENT '补齐事件保留截止；过期需要重新拉完整快照',
PRIMARY KEY (id),
  UNIQUE KEY uk_sync_sequence (user_id,event_seq),
  KEY ix_sync_retention (expires_at,id),
  CONSTRAINT fk_sync_user FOREIGN KEY (user_id) REFERENCES ht_user_account(id),
  CONSTRAINT ck_sync_sequence CHECK (event_seq > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='按用户有序且可补齐的跨设备同步事件';

-- 业务事务后可靠派发的发件箱
CREATE TABLE ht_outbox_event (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不直接作为前端数值ID',
  public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对外ULID字符串标识',
  user_id BIGINT UNSIGNED NULL COMMENT '可选账号归属；注册验证码可尚无账号',
  event_type VARCHAR(80) NOT NULL COMMENT '推送、开始生成、提取记忆或验证码投递等事件',
  dedupe_key VARCHAR(200) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '事件投递幂等键',
  payload_json JSON NOT NULL COMMENT '只存受限任务引用，验证码密文在challenge表',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '派发状态',
  attempt_count SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '派发尝试次数',
  max_attempts SMALLINT UNSIGNED NOT NULL DEFAULT 8 COMMENT '派发次数上限，死信可诊断',
  next_attempt_at DATETIME(6) NOT NULL COMMENT '下次派发时间',
  lease_owner VARCHAR(64) NULL COMMENT '派发执行者',
  lease_fence BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '派发租约代次',
  lease_until DATETIME(6) NULL COMMENT '租约到期时间',
  delivered_at DATETIME(6) NULL COMMENT '实际完成/幂等交付时间',
  error_code VARCHAR(64) NULL COMMENT '稳定内部错误码',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
  version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，更新时递增',
PRIMARY KEY (id),
  UNIQUE KEY uk_outbox_public (public_id),
  UNIQUE KEY uk_outbox_dedupe (dedupe_key),
  KEY ix_outbox_pending (status,next_attempt_at,lease_until,id),
  CONSTRAINT fk_outbox_user FOREIGN KEY (user_id) REFERENCES ht_user_account(id),
  CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING','DELIVERING','DELIVERED','DEAD')),
  CONSTRAINT ck_outbox_attempts CHECK (max_attempts > 0 AND attempt_count <= max_attempts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务事务后可靠派发的发件箱';

-- 模型调用用量与故障证据，不持久化完整提示
CREATE TABLE ht_model_invocation (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键，不直接作为前端数值ID',
  public_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对外ULID字符串标识',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '账号归属',
  conversation_id BIGINT UNSIGNED NOT NULL COMMENT '对话归属',
  run_id BIGINT UNSIGNED NOT NULL COMMENT '生成任务标识',
  stage VARCHAR(32) NOT NULL COMMENT '意图、压缩、回答或记忆提取阶段',
  run_attempt_no SMALLINT UNSIGNED NOT NULL COMMENT '所属生成尝试代次；对应run的attempt_count',
  attempt_no SMALLINT UNSIGNED NOT NULL COMMENT '本生成尝试内该阶段调用编号',
  model_name VARCHAR(120) NOT NULL COMMENT '实际配置的模型名称',
  prompt_revision VARCHAR(32) NOT NULL COMMENT '提示模板版本',
  estimated_input_tokens INT UNSIGNED NOT NULL COMMENT '发送前保守输入估算',
  estimator_version VARCHAR(40) NOT NULL COMMENT '估算方法和版本',
  actual_input_tokens INT UNSIGNED NULL COMMENT '供应商确实返回时记录实际输入用量',
  actual_output_tokens INT UNSIGNED NULL COMMENT '供应商确实返回时记录实际输出用量',
  latency_ms INT UNSIGNED NULL COMMENT '调用耗时',
  provider_request_id VARCHAR(120) NULL COMMENT '供应商提供的关联标识，禁止写认证头',
  status VARCHAR(16) NOT NULL DEFAULT 'STARTED' COMMENT '调用状态',
  error_code VARCHAR(64) NULL COMMENT '内部稳定故障分类',
  completed_at DATETIME(6) NULL COMMENT '调用结束时间',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
  version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，更新时递增',
PRIMARY KEY (id),
  UNIQUE KEY uk_invocation_public (public_id),
  UNIQUE KEY uk_invocation_stage (run_id,run_attempt_no,stage,attempt_no),
  KEY ix_invocation_usage (user_id,conversation_id,created_at,id),
  CONSTRAINT fk_invocation_run FOREIGN KEY (user_id,conversation_id,run_id)
    REFERENCES ht_chat_run(user_id,conversation_id,id),
  CONSTRAINT ck_invocation_attempt CHECK (run_attempt_no > 0 AND attempt_no > 0),
  CONSTRAINT ck_invocation_stage CHECK (stage IN ('INTENT','COMPRESSION','ANSWER','MEMORY_EXTRACTION')),
  CONSTRAINT ck_invocation_status CHECK (status IN ('STARTED','SUCCEEDED','FAILED','INTERRUPTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模型调用用量与故障证据，不持久化完整提示';


CREATE TABLE ht_refresh_receipt (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '账号归属',
  session_id BIGINT UNSIGNED NOT NULL COMMENT '已消费刷新令牌所属登录',
  token_hash BINARY(32) NOT NULL COMMENT '仅保留已消费随机令牌摘要，用于重放检测',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '消费时间，UTC',
  expires_at DATETIME(6) NOT NULL COMMENT '原登录绝对到期时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_refresh_consumed (token_hash),
  KEY ix_refresh_receipt_expiry (expires_at,id),
  CONSTRAINT fk_receipt_session FOREIGN KEY (user_id,session_id) REFERENCES ht_login_session(user_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='已消费刷新令牌重放证据，不保存令牌明文';
