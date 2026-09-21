-- 邮箱验证码统一登录：首次登录账号可以尚未设置密码。
-- V1 已在本地库执行，本变更只通过新增迁移演进，禁止修改既有基线。
ALTER TABLE ht_user_account
  MODIFY COLUMN password_hash VARCHAR(255) NULL COMMENT '可选的带算法和参数的加盐密码哈希，不存明文';

ALTER TABLE ht_email_challenge
  DROP CHECK ck_challenge_purpose,
  ADD CONSTRAINT ck_challenge_purpose CHECK (purpose IN ('LOGIN', 'RESET_PASSWORD'));
