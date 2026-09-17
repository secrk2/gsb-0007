# 特安 · 特种设备检验与维保全生命周期监管平台

市特检机构自研系统：覆盖**电梯 / 起重机械**的检验与维保全生命周期管理。当前已落地「设备作战台」与「设备档案」两大模块。

## 一键启动

```bash
docker compose up -d --build
```

| 入口 | 地址 | 说明 |
|---|---|---|
| 前端 | http://localhost:8107 | Nginx 托管 SPA，并反代 `/api` 到后端 |
| 后端 | http://localhost:7107 | Spring Boot，健康检查 `/actuator/health` |

MySQL / Redis 仅在编排网络内暴露。首次启动自动建表（`deploy/mysql/init/01-schema.sql`）并由后端 `DataSeeder` 写入预置数据（仅空库执行；**重置数据**：`docker compose down -v && docker compose up -d --build`）。

## 预置账号（密码均为 `Tean@2026`）

| 账号 | 姓名 | 角色 | 数据范围 |
|---|---|---|---|
| `eq_admin` / `eq_admin2` / `eq_admin3` | 王建国 / 李慧 / 张伟 | 设备管理员 | 各自使用单位 |
| `maint01` / `maint02` | 赵铁柱 / 钱进 | 维保人员 | 各自使用单位 |
| `inspector01` | 陈明 | 检验员 | 全市 |
| `superv01` | 周正 | 监察员 | 全市 + 精确地址查看 |

预置数据：3 家使用单位（蓝天物业 / 云帆商业 / 惠民安居）、12 台设备（电梯 + 起重机械，覆盖**注册告知 / 验收 / 在用 / 停用 / 报废**全状态）、未闭环与逾期隐患、维保记录。检验日期按当天相对计算，**临期 / 逾期 / 红点开箱即现**。

## 核心能力

### 设备作战台（`/`，适配 1440 / 1024 / 390）
- 总览指标：设备总数、在用、检验临期（30 天）、检验逾期、未闭环隐患
- 各使用单位**在用漏斗**（注册告知→验收→在用→停用→报废）
- 检验临期 / 逾期清单、未闭环隐患墙；**逾期或隐患逾期 → 单位卡片红点**
- 结果缓存 Redis（60s），设备 / 隐患变更主动失效

### 设备档案（`/devices`）
- **状态机**：注册告知→验收→在用→停用→报废；停用可恢复在用，报废为终态。非法回退 / 跳变返回 `409` 并说明原因（详情页有「非法流转演示」入口）
- **位置脱敏**：默认仅展示「区域 · 编号」；精确地址仅**监察员**填写理由 + 二次确认后可见，查看人 / 理由 / 时间 / IP 全部留痕可追溯
- **多单位隔离**：使用单位账号仅见本单位设备；越权访问返回 `403`，前端渲染错误态页面（非空白）
- 维保记录登记、隐患闭环、流转留痕时间线

### 离线巡检（维保人员外勤）
- 顶栏「模拟断网」开关可演示：断网后数据为**缓存快照并显著标注**，状态变更禁用（不拿旧状态糊弄）
- 离线登记维保 → 进入待同步队列（客户端生成幂等键 `recordNo`）
- 恢复联网自动同步：服务端按 `recordNo` 唯一约束**幂等合并，重复提交不产生重复记录**

## 快速验证（curl）

```bash
# 登录获取 token
TOKEN=$(curl -s -X POST http://localhost:7107/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"eq_admin","password":"Tean@2026"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["token"])')

# 1) 作战台汇总（本单位）
curl -s http://localhost:7107/api/dashboard/summary -H "Authorization: Bearer $TOKEN"

# 2) 越权访问他单位设备 → 403 明确错误（eq_admin 属单位 1，设备 6 属单位 2）
curl -s http://localhost:7107/api/devices/6 -H "Authorization: Bearer $TOKEN"

# 3) 非法状态回退 → 409 说明原因（设备 1 在用 → 注册告知）
curl -s -X POST http://localhost:7107/api/devices/1/transition \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"toStatus":"REGISTERED"}'

# 4) 非监察员查看精确地址 → 403；监察员留痕查看：
SUP=$(curl -s -X POST http://localhost:7107/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"superv01","password":"Tean@2026"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["token"])')
curl -s -X POST http://localhost:7107/api/devices/1/address/reveal \
  -H "Authorization: Bearer $SUP" -H 'Content-Type: application/json' \
  -d '{"reason":"专项监察抽查","confirm":true}'
curl -s http://localhost:7107/api/devices/1/address/reveals -H "Authorization: Bearer $SUP"   # 留痕

# 5) 离线同步幂等：同一 recordNo 提交两次 → 第二次 DUPLICATE，不产生重复记录
curl -s -X POST http://localhost:7107/api/maintenance/sync \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"items":[{"recordNo":"demo-uuid-0001","deviceId":1,"content":"幂等验证","offline":true}]}'
curl -s -X POST http://localhost:7107/api/maintenance/sync \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"items":[{"recordNo":"demo-uuid-0001","deviceId":1,"content":"幂等验证","offline":true}]}'
```

## 目录结构

```
├── docker-compose.yml          # 前端/后端/MySQL/Redis/Nginx 全编排
├── backend/                    # Spring Boot 3（Java 17），端口 7107
│   └── src/main/java/com/tean/
│       ├── auth/               # 登录、当前用户
│       ├── config/             # 安全配置、CORS、种子数据
│       ├── dashboard/          # 作战台聚合 + Redis 缓存
│       ├── device/             # 档案、状态机、地址脱敏与留痕
│       ├── hazard/             # 隐患
│       ├── maintenance/        # 维保记录与离线幂等同步
│       ├── security/           # JWT 鉴权
│       └── ...
├── frontend/                   # Vue 3 + Vite，Nginx 托管，端口 8107
│   └── src/{views,components,stores,api}
└── deploy/mysql/init/          # 建库建表脚本
```

## 本地开发

```bash
# 后端（需本地 MySQL/Redis，或用 compose 只起依赖）
cd backend && mvn spring-boot:run
# 前端（代理 /api → 7107）
cd frontend && npm install && npm run dev
```

### 免依赖冒烟模式

本机没有 MySQL/Redis 时，可用 H2 内存库（MySQL 模式）直接启动后端验证全流程（Redis 缺席时看板自动降级为直查数据库）：

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=smoke
```
