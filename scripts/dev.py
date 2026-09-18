#!/usr/bin/env python3
"""安全读取选定环境变量并启动单个本地服务，不执行配置文件中的Shell代码。"""
import argparse
import os
import re
import shlex
import shutil
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
app_root = root.parent / (root.name + '-app')
parser = argparse.ArgumentParser()
parser.add_argument('service', choices=('backend', 'frontend'))
args = parser.parse_args()
allowed = set('MYSQL_HOST MYSQL_PORT MYSQL_USERNAME MYSQL_PASSWORD REDIS_HOST REDIS_PORT REDIS_PASSWORD MILVUS_URI MILVUS_TOKEN DASHSCOPE_API_KEY AMAP_MAPS_API_KEY CHAT_MODEL TRAVEL_MODEL_CONTEXT_WINDOW TRAVEL_MODEL_OUTPUT_RESERVE TRAVEL_MODEL_SAFETY_RESERVE TRAVEL_MODEL_COMPRESSION_INPUT_LIMIT DASHSCOPE_BASE_URL OTP_HMAC_KEY OTP_DELIVERY_KEY DEVICE_SIGNING_KEY SMTP_HOST SMTP_PORT SMTP_USERNAME SMTP_PASSWORD SMTP_FROM SMTP_STARTTLS COOKIE_SECURE ALLOWED_ORIGINS PORT FILES_DIR SPRING_PROFILES_ACTIVE'.split())
env = os.environ.copy()
for path in ((Path.home() / '.zprofile', root / '.env.local') if args.service == 'backend' else ()):
    if not path.exists():
        continue
    for line in path.read_text().splitlines():
        match = re.match(r'^\s*(?:export\s+)?([A-Z_][A-Z0-9_]*)=(.*)$', line)
        if not match or match[1] not in allowed:
            continue
        values = shlex.split(match[2], comments=True)
        if len(values) > 1:
            raise SystemExit('允许变量的配置含不支持的Shell表达式，请改为单一字面值；未输出变量内容。')
        value = values[0] if values else ''
        if '$(' in value or '${' in value or '`' in value:
            raise SystemExit('禁止执行配置中的动态Shell表达式；请使用环境变量或字面值。')
        env[match[1]] = value
if args.service == 'frontend':
    for name in allowed:
        env.pop(name, None)
    if not (app_root / 'package.json').is_file():
        raise SystemExit('未找到同级前端项目：' + str(app_root))
    if not (app_root / 'node_modules').exists():
        subprocess.run(['npm', 'ci', '--no-fund', '--no-audit'], cwd=app_root, env=env, check=True)
    command = ['npm', 'run', 'dev']
    cwd = app_root
else:
    missing = sorted(name for name in ('MYSQL_USERNAME', 'MYSQL_PASSWORD', 'DASHSCOPE_API_KEY', 'OTP_HMAC_KEY', 'OTP_DELIVERY_KEY', 'DEVICE_SIGNING_KEY') if not env.get(name))
    if missing:
        raise SystemExit('缺少配置变量：' + ', '.join(missing))
    maven = shutil.which('mvn')
    if not maven:
        raise SystemExit('请将Maven与Java17放入PATH。')
    subprocess.run([maven, '-B', '-ntp', '-DskipTests', 'package'], cwd=root, env=env, check=True)
    command = ['java', '-jar', str(root / 'hello-travel-start/target/hello-travel-start-0.0.1-SNAPSHOT.jar')]
    cwd = root
child = subprocess.Popen(command, cwd=cwd, env=env)
try:
    raise SystemExit(child.wait())
except KeyboardInterrupt:
    child.terminate()
    try:
        child.wait(timeout=20)
    except subprocess.TimeoutExpired:
        child.kill()
        child.wait()
