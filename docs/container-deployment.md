# 容器部署指南

本文档说明如何使用 Podman/Docker 运行 printer_system 容器。

## 目录

- [镜像信息](#镜像信息)
- [快速开始](#快速开始)
- [环境变量配置](#环境变量配置)
- [端口说明](#端口说明)
- [运行方式](#运行方式)
  - [方式一：使用 Podman Compose（推荐）](#方式一使用-podman-compose推荐)
  - [方式二：使用 Podman Run](#方式二使用-podman-run)
  - [方式三：使用 Docker](#方式三使用-docker)
- [数据持久化](#数据持久化)
- [健康检查](#健康检查)
- [常见问题](#常见问题)

## 镜像信息

- **镜像地址**: `ghcr.io/powercess/printer_system:latest`
- **基础镜像**: Debian Bookworm Slim
- **JDK 版本**: Azul Zulu JDK 25
- **Node.js 版本**: 22.x
- **支持架构**: linux/amd64, linux/arm64

## 快速开始

```bash
# 拉取镜像
podman pull ghcr.io/powercess/printer_system:latest

# 运行容器（最小配置）
podman run -d \
  --name printer-system \
  -p 8080:8080 \
  -p 3000:3000 \
  -e DB_HOST=your-db-host \
  -e DB_PASSWORD=your-password \
  ghcr.io/powercess/printer_system:latest
```

## 环境变量配置

### 必需环境变量

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `DB_HOST` | MySQL 数据库地址 | `mysql` 或 `192.168.1.100` |
| `DB_PORT` | MySQL 端口 | `3306` |
| `DB_NAME` | 数据库名称 | `print_system` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | `your_password` |
| `SA-TOKEN-SECRET-KEY` | Sa-Token 密钥（生产环境请修改） | `your-secret-key` |
| `BASE_URL` | 后端访问地址（用于支付回调） | `https://printer.example.com` |
| `FRONTEND_URL` | 前端访问地址 | `https://printer.example.com` |

### 支付配置

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `PAY_PID` | 支付商户 ID | `1001` |
| `PAY_KEY` | 支付密钥 | `your_pay_key` |
| `PAY_API_URL` | 支付 API 地址 | `https://api.payqixiang.cn/mapi.php` |
| `PAY_SUBMIT_URL` | 支付提交地址 | `https://api.payqixiang.cn/submit.php` |
| `PAY_QUERY_URL` | 支付查询地址 | `https://api.payqixiang.cn/api.php` |

### CUPS 打印服务配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `CUPS_HOST` | CUPS 服务器地址 | `localhost` |
| `CUPS_PORT` | CUPS 端口 | `631` |
| `CUPS_USERNAME` | CUPS 用户名 | - |
| `CUPS_PASSWORD` | CUPS 密码 | - |
| `CUPS_SECURE` | 是否使用 HTTPS | `false` |
| `CUPS_STRATEGY` | 负载均衡策略 | `priority` |
| `CUPS_MOCK_MODE` | 模拟模式（测试用） | `false` |

### 存储配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `STORAGE_TYPE` | 存储类型 (`local` / `s3`) | `local` |
| `UPLOAD_DIR` | 本地上传目录 | `/app/uploads` |

#### S3 对象存储配置（可选）

| 变量名 | 说明 |
|--------|------|
| `S3_ENDPOINT` | S3 端点地址 |
| `S3_REGION` | S3 区域 |
| `S3_BUCKET` | 存储桶名称 |
| `S3_ACCESS_KEY` | 访问密钥 |
| `S3_SECRET_KEY` | 秘密密钥 |
| `S3_PATH_STYLE` | 是否使用路径风格 |

### Gotenberg PDF 服务配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `GOTENBERG_URL` | Gotenberg 服务地址 | `http://localhost:3000` |
| `GOTENBERG_TIMEOUT` | 转换超时时间（毫秒） | `60000` |

### JVM 配置（可选）

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `JAVA_OPTS` | JVM 参数 | `-Xms512m -Xmx1g` |

### 日志配置（可选）

| 变量名 | 说明 | 可选值 |
|--------|------|--------|
| `LOG_MODE` | 日志模式 | `dev`, `prod`, `quiet` |
| `LOG_LEVEL_APP` | 应用日志级别 | `DEBUG`, `INFO`, `WARN`, `ERROR` |
| `LOG_LEVEL_WEB` | Web 请求日志级别 | `DEBUG`, `INFO`, `WARN`, `ERROR` |
| `LOG_LEVEL_DB` | 数据库日志级别 | `DEBUG`, `INFO`, `WARN`, `ERROR` |

## 端口说明

| 端口 | 服务 | 说明 |
|------|------|------|
| `8080` | 后端 API (Spring Boot) | 包含所有 `/api/*` 接口，支付回调 `/api/payment/notify` |
| `3000` | 前端 (Nuxt) | Web 界面 |

## 运行方式

### 方式一：使用 Podman Compose（推荐）

使用 Compose 可以一键启动完整服务栈（应用 + MySQL + Gotenberg）。

1. **创建配置文件**

   复制项目根目录的 `compose.yml` 文件，修改环境变量：

   ```bash
   cp compose.yml compose.prod.yml
   ```

2. **编辑 `compose.prod.yml`**

   修改以下关键配置：

   ```yaml
   services:
     printer-system:
       environment:
         - DB_PASSWORD=your_secure_password
         - SA-TOKEN-SECRET-KEY=your-production-secret-key
         - BASE_URL=https://printer.yourdomain.com
         - FRONTEND_URL=https://printer.yourdomain.com
         - PAY_PID=your_pay_pid
         - PAY_KEY=your_pay_key

     mysql:
       environment:
         - MYSQL_ROOT_PASSWORD=your_secure_password
   ```

3. **启动服务**

   ```bash
   # 启动所有服务
   podman-compose -f compose.prod.yml up -d

   # 查看日志
   podman-compose -f compose.prod.yml logs -f printer-system

   # 停止服务
   podman-compose -f compose.prod.yml down
   ```

4. **服务架构**

   ```
   ┌─────────────────────────────────────────────────────────┐
   │                     用户请求                             │
   └─────────────────────────────────────────────────────────┘
                              │
                              ▼
   ┌─────────────────────────────────────────────────────────┐
   │  printer-system (容器)                                   │
   │  ┌─────────────────┐    ┌─────────────────────────────┐ │
   │  │  前端 (Nuxt)     │    │  后端 (Spring Boot)          │ │
   │  │  端口: 3000      │───▶│  端口: 8080                  │ │
   │  └─────────────────┘    └─────────────────────────────┘ │
   └─────────────────────────────────────────────────────────┘
                              │
           ┌──────────────────┼──────────────────┐
           ▼                  ▼                  ▼
   ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
   │  MySQL        │  │  CUPS         │  │  Gotenberg    │
   │  (数据库)      │  │  (打印服务)    │  │  (PDF转换)    │
   └───────────────┘  └───────────────┘  └───────────────┘
   ```

### 方式二：使用 Podman Run

适用于已有外部数据库的场景。

1. **创建网络（可选，便于服务发现）**

   ```bash
   podman network create printer-network
   ```

2. **运行容器**

   ```bash
   podman run -d \
     --name printer-system \
     --network printer-network \
     -p 8080:8080 \
     -p 3000:3000 \
     -e DB_HOST=your-mysql-host \
     -e DB_PORT=3306 \
     -e DB_NAME=print_system \
     -e DB_USERNAME=root \
     -e DB_PASSWORD=your_password \
     -e SA-TOKEN-SECRET-KEY=your-secret-key \
     -e BASE_URL=https://printer.yourdomain.com \
     -e FRONTEND_URL=https://printer.yourdomain.com \
     -e PAY_PID=your_pay_pid \
     -e PAY_KEY=your_pay_key \
     -e CUPS_HOST=host.containers.internal \
     -e JAVA_OPTS="-Xms512m -Xmx1g" \
     -v printer-uploads:/app/uploads \
     ghcr.io/powercess/printer_system:latest
   ```

3. **常用管理命令**

   ```bash
   # 查看日志
   podman logs -f printer-system

   # 进入容器
   podman exec -it printer-system /bin/bash

   # 重启容器
   podman restart printer-system

   # 停止并删除容器
   podman rm -f printer-system
   ```

### 方式三：使用 Docker

命令与 Podman 基本相同，只需将 `podman` 替换为 `docker`：

```bash
# 使用 Docker Compose
docker-compose up -d

# 或使用 Docker Run
docker run -d \
  --name printer-system \
  -p 8080:8080 \
  -p 3000:3000 \
  -e DB_HOST=your-db-host \
  -e DB_PASSWORD=your-password \
  ghcr.io/powercess/printer_system:latest
```

## 数据持久化

建议持久化的数据：

| 路径 | 说明 | 建议挂载方式 |
|------|------|--------------|
| `/app/uploads` | 上传文件存储 | 命名卷或主机目录 |
| MySQL 数据 | 数据库数据 | 命名卷 |

**示例：**

```bash
# 使用命名卷
podman run -d \
  -v printer-uploads:/app/uploads \
  ...

# 使用主机目录
podman run -d \
  -v /data/printer/uploads:/app/uploads \
  ...
```

## 健康检查

容器内置健康检查，每 30 秒检查一次后端服务：

```bash
# 查看健康状态
podman inspect --format='{{.State.Health.Status}}' printer-system

# 查看健康检查日志
podman inspect --format='{{json .State.Health}}' printer-system | jq
```

## 常见问题

### 1. 容器启动后无法访问

**检查步骤：**

```bash
# 查看容器状态
podman ps -a

# 查看日志
podman logs printer-system

# 检查端口是否被占用
ss -tlnp | grep -E '8080|3000'
```

### 2. 数据库连接失败

**常见原因：**
- 数据库地址配置错误
- 数据库未启动
- 网络不通
- 用户名/密码错误

**排查：**

```bash
# 进入容器测试连接
podman exec -it printer-system /bin/bash
curl -v telnet://$DB_HOST:$DB_PORT
```

### 3. 支付回调无法接收

**检查：**
- `BASE_URL` 配置是否正确（必须是外网可访问的地址）
- 端口 8080 是否对外开放
- 反向代理配置是否正确转发

### 4. 打印功能不工作

**检查：**
- CUPS 服务是否可访问
- `CUPS_HOST` 配置是否正确
- 如果 CUPS 在宿主机，使用 `host.containers.internal` 作为地址

### 5. 文件上传失败

**检查：**
- 上传目录是否有写入权限
- 磁盘空间是否充足
- `UPLOAD_DIR` 配置是否正确

## 版本升级

```bash
# 拉取最新镜像
podman pull ghcr.io/powercess/printer_system:latest

# 停止旧容器
podman stop printer-system
podman rm printer-system

# 使用新镜像启动（保留数据卷）
podman run -d \
  --name printer-system \
  ... \
  -v printer-uploads:/app/uploads \
  ghcr.io/powercess/printer_system:latest
```

## 反向代理配置示例

### Nginx

```nginx
server {
    listen 80;
    server_name printer.yourdomain.com;

    # 前端
    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    # 后端 API
    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 支付回调可能需要较长时间
        proxy_read_timeout 60s;
    }

    # API 文档
    location /api-docs {
        proxy_pass http://127.0.0.1:8080;
    }

    location /swagger-ui {
        proxy_pass http://127.0.0.1:8080;
    }
}
```

### Caddy

```caddyfile
printer.yourdomain.com {
    # 前端
    reverse_proxy localhost:3000

    # 后端 API
    handle /api/* {
        reverse_proxy localhost:8080
    }

    # API 文档
    handle /api-docs/* {
        reverse_proxy localhost:8080
    }

    handle /swagger-ui/* {
        reverse_proxy localhost:8080
    }
}
```