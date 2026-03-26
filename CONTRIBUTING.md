# 贡献指南 | Contributing to wx-aibot-java-sdk

欢迎参与 wx-aibot-java-sdk 的开发！我们感谢每一位贡献者。

Welcome to contribute to wx-aibot-java-sdk! We appreciate every contributor.

## 🤝 如何贡献 | How to Contribute

### 1. 报告问题 | Reporting Issues

发现 Bug 或有新功能建议？请通过 [GitHub Issues](https://github.com/clouds3n/wx-aibot-java-sdk/issues) 提交。

Found a bug or have a feature request? Please submit via [GitHub Issues](https://github.com/clouds3n/wx-aibot-java-sdk/issues).

**提交 Issue 时请包含：**

- 问题描述（清晰简洁）
- 复现步骤
- 预期行为 vs 实际行为
- 环境信息（Java 版本、SDK 版本、操作系统）

**When submitting an issue, please include:**

- Clear and concise description
- Steps to reproduce
- Expected vs actual behavior
- Environment info (Java version, SDK version, OS)

### 2. 提交代码 | Submitting Code

#### Fork & Clone

```bash
git clone https://github.com/your-username/wx-aibot-java-sdk.git
cd wx-aibot-java-sdk
```

#### 创建分支 | Create Branch

```bash
git checkout -b feature/your-feature-name
# 或
git checkout -b fix/issue-123
```

**分支命名规范：**

- `feature/xxx` - 新功能
- `fix/xxx` - Bug 修复
- `docs/xxx` - 文档更新
- `refactor/xxx` - 代码重构
- `test/xxx` - 测试相关

**Branch naming conventions:**

- `feature/xxx` - New feature
- `fix/xxx` - Bug fix
- `docs/xxx` - Documentation update
- `refactor/xxx` - Code refactoring
- `test/xxx` - Test related

#### 开发要求 | Development Requirements

- **Java 版本** | **Java Version**: 25+
- **代码风格** | **Code Style**: 遵循项目现有代码风格
- **测试覆盖** | **Test Coverage**: 新功能需包含单元测试
- **文档更新** | **Documentation**: 更新 README 和相关文档

运行测试：

```bash
mvn test
```

#### 提交信息 | Commit Messages

遵循约定式提交规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 类型：**

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例 | Example:**

```
feat(client): add streaming reply with template card support

- Implement replyStreamWithCard method
- Add msg_item support for mixed content
- Update integration tests

Closes #42
```

#### 提交 PR | Submitting PR

1. 确保代码通过所有测试
2. 更新文档（如适用）
3. 推送到你的 Fork
4. 在 GitHub 上创建 Pull Request

**PR 描述模板：**

```markdown
## 变更描述 | Description
<!-- 简要描述此 PR 的目的 -->

## 相关 Issue | Related Issue
<!-- 关联的 Issue 编号 -->

## 测试计划 | Test Plan
<!-- 如何测试这些变更 -->

## 检查清单 | Checklist
- [ ] 代码通过测试
- [ ] 文档已更新
- [ ] 遵循代码规范
```

## 📋 代码审查流程 | Code Review Process

1. **自动化检查** - CI 运行测试和代码检查
2. **维护者审查** - 至少一名维护者审查代码
3. **反馈与修改** - 根据审查意见修改
4. **合并** - 审查通过后合并到主分支

## 💬 沟通 | Communication

- 通过 GitHub Issues 讨论技术问题
- 在 PR 中询问审查意见
- 保持友好和专业的沟通氛围

## 📜 行为准则 | Code of Conduct

- 尊重他人观点和经验
- 建设性地提供反馈
- 欢迎不同背景的贡献者
- 对不适当行为零容忍

## 🙏 致谢 | Acknowledgments

感谢所有为这个项目做出贡献的开发者！

Thank you to all developers who have contributed to this project!

---

**最后更新 | Last Updated**: 2024-03-26
