# 特安 · 特种设备检验与维保全生命周期管理平台

市特检机构自研系统骨架：覆盖电梯、起重机械的**设备档案**（注册告知 → 验收 → 在用 ⇄ 停用 → 报废状态机）与**设备作战台**（在用漏斗 / 检验临期 / 隐患与逾期红点），含使用单位数据隔离、位置脱敏、维保离线作业与幂等合并。

## 一键启动

```bash
docker compose up -d --build
```

| 入口 | 地址 |
|---|---|
| 前端（Nginx，反代后端） | http://localhost:8107 |
| 后端直连 | http://localhost:7107/api/health |

首次启动后端自动建表并写入预置数据（空库时执行，幂等）。

## 演示账号（密码均为 `123456`）

| 账号 | 姓名 | 角色 | 所属 |
|---|---|---|---|
| `wuye_admin` | 王建国 | 设备管理员 | 云栖物业（城东片区） |
| `yiyuan_admin` | 李秀兰 | 设备管理员 | 仁济医院（城西片区） |
| `mall_admin` | 张伟 | 设备管理员 | 万象商业广场（高新片区） |
| `weibao01` / `weibao02` | 赵铁柱 / 孙丽 | 维保人员 | 迅捷维保 |
| `jianyan01` | 陈明 | 检验员 | 市特检院 |
| `jiancha01` | 周正 | 监察员 | 市特检院 |

预置 12 台设备（电梯 + 起重机械），覆盖注册告知 / 验收 / 在用 / 停用 / 报废全部状态，含检验临期、检验逾期、未闭环与逾期隐患。

## 关键设计

### 设备档案状态机
合法流转：`注册告知→验收→在用⇄停用→报废`（注册/验收/停用亦可直接报废）。
非法回退由后端 `DeviceStateMachine` 拦截并返回具体原因，如：
- 报废 → 任意：「设备已报废，档案已封存，不允许任何状态变更」
- 在用 → 注册告知：「设备已在用，不允许回退到注册告知；如需退出使用，请先停用再报废」
- 注册告知 → 在用：「设备尚未验收，不允许直接投入使用，请先完成验收」

每次流转写入 `device_status_history` 留痕。

### 位置脱敏与监察留痕
- 所有列表/详情默认只返回「区域 · 编号」（如 `城东片区 · A-101`），精确建筑与地址不下发。
- 仅**监察员**可查看精确地址：需二次确认 + 填写理由（≥4 字），服务端校验后返回并写入 `address_reveal_log`（操作人、理由、IP、时间）。

### 使用单位隔离
- 设备管理员：仅本使用单位设备；维保人员：仅本维保单位承维设备；检验员/监察员（特检机构）：全部。
- 越权访问他人设备返回 `403 无权查看其他单位的设备档案`，前端渲染明确错误态而非空白页。

### 离线维保（断网不糊弄）
- 断网时：顶部离线横幅，页面展示**带时间戳的缓存数据**并明确标注；状态变更、地址查看等操作禁用，避免基于过期状态误操作。
- 维保登记离线暂存 `localStorage` 队列；恢复在线自动批量合并到 `/api/maintenance/sync`。
- 幂等：每条记录携带客户端生成的 `clientId`（UUID），服务端唯一约束兜底——重复提交、离线重放、并发双击均合并为同一记录，返回 `DUPLICATE` 不产生重复数据。

### 设备作战台
- 各使用单位：五态漏斗、检验临期（30 天内）、检验逾期、未闭环隐患、隐患逾期（红点）。
- 机构账号只看本单位卡片；特检机构看全部使用单位。
- 响应式：1440 三列 / 1024 两列 / 390 单列，表格在窄屏自动转卡片。

## 技术栈与结构

```
backend/    Spring Boot 3 (Java 17) · Spring Data JPA · Redis 令牌 · BCrypt
frontend/   Vue 3 · Vite · Pinia · Vue Router（无重型 UI 依赖，纯手写响应式样式）
docker-compose.yml   MySQL 8.4 · Redis 7 · 后端(7107) · Nginx 前端(8107)
```

本地开发：
```bash
# 后端（需本地 MySQL/Redis，或先 docker compose up -d mysql redis）
cd backend && mvn spring-boot:run
# 前端（Vite 代理 /api → localhost:7107）
cd frontend && npm install && npm run dev
```

## API 一览

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 登录，签发 Redis 令牌（12h 滑动续期） |
| GET | `/api/dashboard/battle` | 作战台聚合 |
| GET | `/api/devices` | 设备列表（按角色隔离） |
| GET | `/api/devices/{id}` | 设备详情（脱敏；越权 403） |
| POST | `/api/devices/{id}/transition` | 状态机流转 `{to, reason}` |
| GET | `/api/devices/{id}/history` | 流转留痕 |
| POST | `/api/devices/{id}/address/reveal` | 监察员查看精确地址 `{reason, confirm}`，留痕 |
| GET/POST | `/api/hazards` · `/api/hazards/{id}/rectify` | 隐患 |
| GET/POST | `/api/inspections` | 检验记录（回写设备检验日期） |
| GET/POST | `/api/maintenance` · `/api/maintenance/sync` | 维保登记 / 离线批量合并（幂等） |
