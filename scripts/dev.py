#!/usr/bin/env python3
"""构建并启动单个本地服务；后端配置由Spring Boot读取本地YAML。"""
import argparse
import os
import shutil
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
app_root = root.parent / (root.name + '-app')
parser = argparse.ArgumentParser()
parser.add_argument('service', choices=('backend', 'frontend'))
args = parser.parse_args()
env = os.environ.copy()
if args.service == 'frontend':
    if not (app_root / 'package.json').is_file():
        raise SystemExit('未找到同级前端项目：' + str(app_root))
    if not (app_root / 'node_modules').exists():
        subprocess.run(['npm', 'ci', '--no-fund', '--no-audit'], cwd=app_root, env=env, check=True)
    command = ['npm', 'run', 'dev']
    cwd = app_root
else:
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
