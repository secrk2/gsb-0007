-- 特安监管平台 数据库结构（MySQL 8，utf8mb4）
-- 首次启动自动执行；业务数据由后端 DataSeeder 在空库时写入（日期相对当天计算，保证演示效果）

USE tean;

CREATE TABLE IF NOT EXISTS org_unit (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    code    VARCHAR(32)  NOT NULL UNIQUE,
    name    VARCHAR(128) NOT NULL,
    region  VARCHAR(64)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    username  VARCHAR(64)  NOT NULL UNIQUE,
    password  VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    real_name VARCHAR(64),
    role      VARCHAR(32)  NOT NULL COMMENT 'DEVICE_ADMIN/MAINTAINER/INSPECTOR/SUPERVISOR',
    org_id    BIGINT,
    enabled   TINYINT(1)   NOT NULL DEFAULT 1
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS device (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_code          VARCHAR(64)  NOT NULL UNIQUE COMMENT '设备注册代码',
    name                 VARCHAR(128) NOT NULL,
    type                 VARCHAR(16)  NOT NULL COMMENT 'ELEVATOR/CRANE',
    model                VARCHAR(128),
    org_id               BIGINT       NOT NULL,
    status               VARCHAR(16)  NOT NULL COMMENT 'REGISTERED/ACCEPTED/IN_USE/SUSPENDED/SCRAPPED',
    region               VARCHAR(64)  COMMENT '脱敏位置：区域',
    mask_code            VARCHAR(32)  COMMENT '脱敏位置：编号',
    exact_address        VARCHAR(255) COMMENT '精确地址，仅监察员留痕后可见',
    install_date         DATE,
    next_inspection_date DATE,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           DATETIME,
    updated_at           DATETIME,
    INDEX idx_device_org (org_id),
    INDEX idx_device_status (status),
    INDEX idx_device_next_insp (next_inspection_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS device_status_log (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id     BIGINT      NOT NULL,
    from_status   VARCHAR(16),
    to_status     VARCHAR(16) NOT NULL,
    operator_id   BIGINT,
    operator_name VARCHAR(64),
    reason        VARCHAR(255),
    created_at    DATETIME,
    INDEX idx_status_log_device (device_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS address_reveal_log (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id  BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    username   VARCHAR(64),
    real_name  VARCHAR(64),
    reason     VARCHAR(255) NOT NULL,
    ip         VARCHAR(64),
    created_at DATETIME,
    INDEX idx_reveal_device (device_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS maintenance_record (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_no         VARCHAR(64)  NOT NULL COMMENT '客户端幂等键（UUID）',
    device_id         BIGINT       NOT NULL,
    maintainer_id     BIGINT       NOT NULL,
    maintainer_name   VARCHAR(64),
    content           VARCHAR(500) NOT NULL,
    client_created_at DATETIME COMMENT '离线端原始记录时间',
    offline           TINYINT(1)   NOT NULL DEFAULT 0,
    created_at        DATETIME,
    UNIQUE KEY uk_maintenance_record_no (record_no),
    INDEX idx_maint_device (device_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS hazard (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id   BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL,
    level       VARCHAR(16)  NOT NULL COMMENT 'HIGH/MEDIUM/LOW',
    status      VARCHAR(16)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/RESOLVED',
    deadline    DATE COMMENT '整改期限',
    created_at  DATETIME,
    resolved_at DATETIME,
    INDEX idx_hazard_device (device_id),
    INDEX idx_hazard_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
