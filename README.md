# wx-aibot-java-sdk

[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![Maven Central](https://img.shields.io/badge/Maven-2.0.0-orange.svg)](https://central.sonatype.com/artifact/io.github.cloudsen/wx-aibot-java-sdk)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

English | [中文](README.zh.md)

Java SDK collection for WeCom AI Bot and Weixin protocol integration, published in one Maven artifact.

## Modules

| Module | Package | Description | Docs |
|------|------|------|------|
| WeCom | `io.github.cloudsen.ai.wecom` | Enterprise WeCom AI Bot SDK based on WebSocket long-lived connection | [English](README.wecom.md) / [中文](README.wecom.zh.md) |
| Weixin | `io.github.cloudsen.ai.weixin` | Java wrapper around the HTTP, QR login, long-polling, and CDN protocol exposed by `@tencent-weixin/openclaw-weixin` | [English](README.weixin.md) / [中文](README.weixin.zh.md) |

## Maven

Both modules are included in the same artifact:

```xml
<dependency>
    <groupId>io.github.cloudsen</groupId>
    <artifactId>wx-aibot-java-sdk</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Quick Links

- WeCom usage and integration test: [README.wecom.md](README.wecom.md)
- Weixin usage and QR login integration test: [README.weixin.md](README.weixin.md)
- Weixin protocol notes: [docs/openclaw-weixin-protocol.md](docs/openclaw-weixin-protocol.md)

## License

Distributed under the Apache License 2.0. See [LICENSE](LICENSE) for more information.
