-- ChatModelStage 已按普通对话、旅行规划和记忆职责细分；既有库同步扩展审计阶段约束。
ALTER TABLE ht_model_invocation
  DROP CHECK ck_invocation_stage,
  ADD CONSTRAINT ck_invocation_stage CHECK (
    stage IN (
      'INTENT',
      'DIALOGUE',
      'PLAN_DRAFT',
      'PLAN_REVISION',
      'MEMORY_SUMMARY',
      'MEMORY_EXTRACTION'
    )
  );
