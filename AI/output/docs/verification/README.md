# 编码阶段有限检查证据

这些记录只证明所列检查，不等同06完整开发自测、07独立CR或08产品验收。未调用真实模型、嵌入、高德或SMTP，启动检查禁用后台调度。

- backend-install.log：九项目聚合构建/安装、默认Checkstyle/Flatten、7条离线检查。
- revision-override.log：覆盖revision的实际源码构建，测试明确跳过。
- independent-consumer.log：源码聚合工程之外消费安装后的application及传递依赖。
- frontend-build.log/frontend-format.log：TypeScript、生产打包、Prettier检查。
- startup-excerpt.log：最终JAR迁移校验及启动，仅摘取不含配置值的证明行。
- http-error-paths.json：健康、缺SMTP、未登录路径；没有模拟正常登录或真实生成。
- mysql-schema.txt：专用库表名，其后0是账号数，最后1/1是Flyway版本1及成功标志。
- summary.json：记录时刻、范围和迁移/lockfile/JAR校验和。

install隔离项目坐标使用临时本地仓库，复用第三方依赖缓存；不是完全无缓存构建，也未远程发布。正式库V1最初执行时使用Boot默认Flyway，最终版本升11.20.3后再次校验同一V1通过，无repair或改写迁移。

## SRC-021当前整改证据

boundary-*及entry-audit.csv是当前10条离线检查/50入口审计/HTTP分类/独立消费者证据；app-*来自独立同级hello-travel-app。原文件为SRC-020历史证据，原summary.json保留历史哈希，当前哈希见boundary-summary.json。

## 当前业务质量整改证据（2026-09-18）

quality-*为当前规范1.8/快照1.6的源码业务质量扫描、全方法清单、入口异常审计、针对性回归、独立安装消费者与构建证据；solo-quality-*为门禁/流程/启动/安装回归。历史summary/boundary-*保留原日期与哈希，不拿旧摘要证明当前源码。质量清单的自动分类有明确限界：编号、规模和有限领域结构不证明注释正确、真实对象职责或完整业务/生产验收。当前权威结果见quality-summary.json与05交付记录。

## 2026-09-18当前映射与异常归属证据

mapping-summary.json为本轮范围与统计；mapping-*.log为实际构建/回归；mapping-junit保留全部模块JUnit原始结果；mapping-sha256.json核对日志/清单。hello-travel的mapping-class/constructor/method-inventory.csv及mapping-entry-audit.csv提供全量位置导航；DDD mapping-entry-audit.csv提供14主入口结构检查。旧quality-/boundary-文件保留历史证据，不将旧执行重复计入本轮。

## 2026-09-18业务垂直领域整改

vertical-summary.json是当前范围，vertical-*.log/CSV与vertical-junit为原始证据，vertical-sha256.json校验文件；hello-travel的vertical-java-before.zip保留整改前Java来源，仅作归档。当前39项后端与36主入口统计不混算旧轮35/50；DDD根与临时独立快照各7项。跨域事务使用mock管理器验证commit/rollback调用，不声称真实MySQL故障数据回滚。

## 领域服务命名校正

[domain-service-naming.md](domain-service-naming.md)记录SRC-050的业务职责命名、构建、边界审计、DDD快照和solo发行检查。此前vertical-*清单是当时的历史位置快照，不冒充本轮的类型名称清单。
