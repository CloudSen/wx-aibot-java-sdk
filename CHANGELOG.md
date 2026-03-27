# 变更日志 | Changelog

本项目的所有重要变更都将记录在此文件中。

All notable changes to this project will be documented in this file.

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

This project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [2.0.0] - 2026-03-27

### ⚠️ 不兼容变更 | Breaking Changes

- 企业微信相关类从原先的 `io.github.cloudsen.ai.*` 迁移到 `io.github.cloudsen.ai.wecom.*`
- 微信协议相关类独立放入 `io.github.cloudsen.ai.weixin.*`
- 微信协议类去掉 `OpenClaw` 前缀，例如 `OpenClawWeixinClient` 已改为 `WeixinClient`
- 共享 JSON 支持类迁移到 `io.github.cloudsen.ai.common.JsonSupport`
- 这是一次不兼容升级，旧版本 import 路径不能直接继续使用，接入代码需要跟随包路径调整

### ✨ 新增 | Added

- 新增 `io.github.cloudsen.ai.weixin` 协议层 Java SDK
- 支持微信二维码登录、长轮询拉消息、文本消息发送、输入态发送
- 支持 CDN 媒体上传、下载与 `AES-128-ECB` 加解密
- 新增 `docs/openclaw-weixin-protocol.md` 协议分析文档
- 新增真实扫码启动的 `WeixinClientIntegrationTest`

### 🔧 优化 | Changed

- WeCom 代码整体迁移到 `io.github.cloudsen.ai.wecom` 包下
- README 拆分为独立的 WeCom 和 Weixin 文档
- Maven 依赖示例统一为 `io.github.cloudsen:wx-aibot-java-sdk:2.0.0`

### 🧪 测试 | Testing

- 新增 `WeixinClientTest`
- 新增 `WeixinClientIntegrationTest`
- 保留并迁移 `WeComAiBotClientTest`
- 保留并迁移 `WeComAiBotClientIntegrationTest`

## [1.0.0] - 2024-03-26

### ✨ 新增 | Added

- 基于 WebSocket 长连接的企业微信 AI Bot Java SDK 核心功能
- 自动连接管理与鉴权（`aibot_subscribe`）
- 内置应用层心跳保活机制
- 断线自动重连（指数退避策略）
- 鉴权失败自动重试
- 完整的消息类型支持：
  - 文本消息
  - 图片消息
  - 文件消息
  - 语音消息
  - 视频消息
  - 图文混排消息
- 流式回复支持（`replyStream` 系列方法）
- 模板卡片消息支持：
  - 回复模板卡片
  - 更新模板卡片
  - 欢迎语模板卡片
- 主动消息推送能力
- 媒体文件上传（`init -> chunk -> finish` 三步上传）
- 文件下载与 AES-256-CBC 解密
- 便捷事件监听器注册方法（`onTextMessage`、`onEnterChat` 等）
- 完整的集成测试套件
- 支持通过系统属性或环境变量配置集成测试参数

### 🔧 优化 | Changed

- 优化心跳与重连机制，支持自定义配置
- 优化流式消息串行发送，避免乱序
- 优化 API 设计，提供语义化的便捷方法

### 📖 文档 | Documentation

- 中英双语 README
- 完整的 API 使用示例
- 集成测试执行指南
- 心跳与重连配置说明
- 贡献指南（CONTRIBUTING.md）

### 🧪 测试 | Testing

- 单元测试（`WeComAiBotClientTest`）
- 真实企业微信集成测试（`WeComAiBotClientIntegrationTest`）
- 支持通过 Maven 参数或环境变量启用集成测试

### 📦 技术栈 | Tech Stack

- Java 25
- Jackson 2.17.1（JSON 处理）
- OkHttp 4.12.0（HTTP 客户端）
- JUnit 5.10.2（测试框架）
- MockWebServer（测试辅助）

---

## 版本说明 | Version Notes

### 版本号格式 | Version Number Format

遵循语义化版本规范（Semantic Versioning）：`MAJOR.MINOR.PATCH`

- **MAJOR** - 不兼容的 API 变更
- **MINOR** - 向后兼容的功能新增
- **PATCH** - 向后兼容的问题修复

### 发布流程 | Release Process

1. 更新 `pom.xml` 版本号
2. 更新本变更日志
3. 创建 Git 标签
4. 发布到 Maven Central

---

**[1.0.0]**: 2024-03-26 - Initial Release | 初始发布
**[2.0.0]**: 2026-03-27 - Breaking package split for WeCom and Weixin | WeCom 与 Weixin 拆包的不兼容升级
