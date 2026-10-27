# 贡献指南

感谢你愿意改进广城通。

## 适合提交什么

- 修复学校页面结构变化导致的课表/学时解析问题；
- 修复 WebView 白名单、证书、Keystore、二维码导入等安全或稳定性问题；
- 改进原生 UI 的可读性、可访问性和小屏适配；
- 补充构建、架构、隐私说明和测试。

## 开发约束

- **平台原生**：不引入 Gradle、AndroidX、第三方 UI 库或图标库；UI 使用 `src/cn/gcpt/app/Ui.java` 与 `LineIconView.java`。
- **最小权限**：只保留 INTERNET；`allowBackup=false`；不新增后台服务、推送、统计或账号体系。
- **本地优先**：不上传账号、课表、余额、二维码；不新增服务端依赖。
- **安全**：不开启 WebView 远程调试；不添加 JavaScript 原生桥；不忽略 SSL 错误；新增域名必须说明理由。
- **Java 8 语法**：`javac -source 8 -target 8`，不引入新语言特性。

## 构建与测试

见 [docs/BUILDING.md](docs/BUILDING.md)。提交前至少：

1. `pwsh -File .\build-demo.ps1` 构建通过；
2. 在真机验证受影响的页面；
3. 搜索明文/密钥/真实账号没有进入提交：
   ```powershell
   git diff --check
   git grep -nE 'password|passwd|secret|token|1[3-9][0-9]{9}'
   ```

## 不要提交

- 真实学号、密码、验证码、余额、电子证、成绩；
- `build/`、`dist/`、`*.keystore`、`*.jks`、`release-keystore-password.txt`；
- 抓包得到的 Cookie、Authorization、session；
- 任何未脱敏的截图。

## Pull Request

- 一个 PR 只解决一类问题；说明复现步骤、修改点和验证结果；
- 涉及学校页面的改动，请附脱敏后的页面结构或截图；
- 涉及安全问题的，请不要公开 Issue，按 [SECURITY.md](SECURITY.md) 处理。

## 许可证

提交即表示你同意以 [GPL-3.0](LICENSE) 授权你的贡献。请只提交你有权提交的代码或素材。
