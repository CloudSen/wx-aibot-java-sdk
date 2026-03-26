# Maven Central 发布指南 | Publishing to Maven Central

## 前置准备 | Prerequisites

### 1. Sonatype 账号 | Sonatype Account

访问 [Central Portal](https://central.sonatype.com/) 注册账号。

### 2. 配置 GPG 密钥 | Configure GPG Key

#### macOS

```bash
brew install gpg
gpg --full-generate-key
gpg --list-keys
gpg --keyserver keyserver.ubuntu.com --send-keys <YOUR_KEY_ID>
```

#### 将公钥同步到更多密钥服务器

```bash
gpg --keyserver keys.openpgp.org --send-keys <YOUR_KEY_ID>
gpg --keyserver pgp.mit.edu --send-keys <YOUR_KEY_ID>
```

### 3. 配置 Maven settings.xml

编辑 `~/.m2/settings.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>central</id>
            <username>YOUR_SONATYPE_USERNAME</username>
            <password>YOUR_SONATYPE_TOKEN</password>
        </server>
    </servers>
</settings>
```

> 💡 **提示** | **Tip**: 在 [Central Portal](https://central.sonatype.com/account) 生成 Token

## 发布流程 | Publishing Process

### 1. 验证项目 | Validate Project

```bash
mvn clean verify
```

### 2. 发布到 Maven Central | Publish to Maven Central

```bash
mvn clean deploy -Pskip-gpg
```

> ⚠️ **注意** | **Note**: 
> - 首次发布建议使用 `-Pskip-gpg` 跳过 GPG 签名测试
> - 正式生产发布需要配置 GPG 并移除 `-Pskip-gpg`

### 3. 正式发布（带 GPG 签名）| Production Release (with GPG)

```bash
# 方式 1: 使用 skipGPG 属性控制
mvn clean deploy -DskipGPG=false

# 方式 2: 使用 release profile
mvn clean deploy -P release -DperformRelease=true
```

## 验证发布 | Verify Release

1. 访问 [Maven Central](https://central.sonatype.com/)
2. 搜索 `wx-aibot-java-sdk`
3. 确认版本 `1.0.0` 已发布

## 常见问题 | Troubleshooting

### GPG 签名失败 | GPG Signing Failed

```bash
# 检查 GPG 密钥
gpg --list-secret-keys

# 重新生成密钥（如需要）
gpg --full-generate-key
```

### 权限错误 | Permission Denied

确保 `settings.xml` 中配置的 Sonatype 账号有发布权限。

### 版本冲突 | Version Conflict

确保 `pom.xml` 中的版本号未重复发布。

## 依赖使用 | Usage

发布成功后，用户可通过以下依赖引入：

```xml
<dependency>
    <groupId>com.github.clouds3n.ai</groupId>
    <artifactId>wx-aibot-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 参考资料 | References

- [Sonatype Central Portal](https://central.sonatype.com/)
- [Maven GPG Plugin](https://maven.apache.org/plugins/maven-gpg-plugin/)
- [Central Publishing Maven Plugin](https://github.com/sonatype/central-publishing-maven-plugin)
- [Apache Maven Settings Reference](https://maven.apache.org/settings.html)

---

**最后更新 | Last Updated**: 2024-03-26
