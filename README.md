# 自助打印系统

一个基于 Spring Boot 和 Nuxt.js 的自助打印管理系统，适用于办公室、学校等多人共享打印场景。

## 功能特性

- **多打印机管理**：支持管理多台打印机，自动负载均衡
- **用户系统**：用户注册、登录、钱包充值
- **文件管理**：支持上传 PDF、Word、Excel 等格式文件，自动去重存储
- **在线支付**：支持微信支付、支付宝支付，以及钱包余额支付
- **打印配置**：支持设置打印份数、彩色/黑白、单双面、纸张大小
- **社区分享**：用户可以分享文件到社区

## 技术栈

### 后端
- Java 25
- Spring Boot 4.0.3
- MyBatis-Plus
- Sa-Token
- MySQL

### 前端
- Nuxt 4
- Vue 3
- TypeScript
- Tailwind CSS
- Nuxt UI

### 基础设施
- CUPS（通用 Unix 打印系统）
- 对象存储（支持 S3 协议）

## 快速开始

### 环境要求

- JDK 25
- Node.js 18+
- MySQL 8.0+
- CUPS 打印服务

### 后端配置

1. 创建数据库：
```sql
CREATE DATABASE print_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 配置环境变量（或创建 `.env` 文件）：
```bash
DB_HOST=localhost
DB_PORT=3306
DB_NAME=print_system
DB_USERNAME=root
DB_PASSWORD=your_password

# CUPS 配置
CUPS_HOST=localhost
CUPS_PORT=631

# 支付配置
PAY_PID=your_pid
PAY_KEY=your_key

# 存储配置（可选 S3）
STORAGE_TYPE=local
UPLOAD_DIR=./uploads
```

3. 运行后端：
```bash
./gradlew bootRun
```

### 前端配置

1. 安装依赖：
```bash
cd web
bun install
```

2. 运行开发服务器：
```bash
bun run dev
```

## 项目结构

```
printer_system/
├── src/main/java/          # 后端源码
│   ├── controller/         # REST API 控制器
│   ├── service/            # 业务逻辑层
│   ├── mapper/             # 数据访问层
│   ├── entity/             # 实体类
│   ├── dto/                # 数据传输对象
│   └── config/             # 配置类
├── web/                    # 前端项目
│   ├── app/                # Nuxt 应用
│   │   ├── pages/          # 页面
│   │   ├── components/     # 组件
│   │   ├── middleware/     # 中间件
│   │   └── stores/         # Pinia 状态管理
│   ├── api/                # API 客户端
│   └── types/              # TypeScript 类型定义
└── else/                   # 旧版 Python 原型（已废弃）
```

## API 文档

启动后端后访问 `/swagger-ui.html` 查看完整的 API 文档。

## 未来规划

- [ ] Web 管理后台完善
- [ ] Android 客户端
- [ ] iOS 客户端
- [ ] 微信小程序

## 许可证

本项目采用 **CC BY-NC 4.0** 许可证。

- **允许**：查看、学习、修改源代码，用于个人学习和非商业项目
- **禁止**：未经授权将本项目用于商业用途，包括但不限于销售、商业化部署
- **要求**：使用时需保留原作者署名

如需商业授权，请联系作者。