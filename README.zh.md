# wx-aibot-java-sdk

[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![Maven Central](https://img.shields.io/badge/Maven-2.0.0-orange.svg)](https://central.sonatype.com/artifact/io.github.cloudsen/wx-aibot-java-sdk)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

[English](README.md) | 中文

当前仓库在一个 Maven artifact 中同时提供两套彼此独立的微信接入能力。

## 模块

| 模块 | 包名 | 说明 | 文档 |
|------|------|------|------|
| WeCom | `io.github.cloudsen.ai.wecom` | 基于 WebSocket 长连接协议的企业微信 AI Bot Java SDK | [English](README.wecom.md) / [中文](README.wecom.zh.md) |
| Weixin | `io.github.cloudsen.ai.weixin` | 基于 `@tencent-weixin/openclaw-weixin` 底层 HTTP、二维码登录、长轮询、CDN 协议封装的 Java SDK | [English](README.weixin.md) / [中文](README.weixin.zh.md) |

## Maven 依赖

两个模块都在同一个 artifact 中：

```xml
<dependency>
    <groupId>io.github.cloudsen</groupId>
    <artifactId>wx-aibot-java-sdk</artifactId>
    <version>2.0.0</version>
</dependency>
```

## 快速导航

- WeCom 用法和集成测试: [README.wecom.zh.md](README.wecom.zh.md)
- Weixin 用法和扫码集成测试: [README.weixin.zh.md](README.weixin.zh.md)
- Weixin 协议分析: [docs/openclaw-weixin-protocol.md](docs/openclaw-weixin-protocol.md)

## License

基于 Apache License 2.0 分发。更多信息请查看 [LICENSE](LICENSE)。
