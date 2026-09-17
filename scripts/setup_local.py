#!/usr/bin/env python3
"""创建仅本机使用的独立安全密钥，不覆盖已有配置、不输出密钥。"""
import base64
import os
from pathlib import Path

root = Path(__file__).resolve().parents[1]
path = root / '.env.local'
if path.exists():
    raise SystemExit('.env.local已存在，保留原配置；请按.env.example补充所需变量。')
fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(fd, 'w') as stream:
    stream.write('# 本机开发配置，禁止提交或用于生产\n')
    for name in ('OTP_HMAC_KEY', 'OTP_DELIVERY_KEY', 'DEVICE_SIGNING_KEY'):
        stream.write(name + '=' + base64.b64encode(os.urandom(32)).decode('ascii') + '\n')
print('已创建权限0600的.env.local，三个独立密钥已生成；未输出凭据。')
