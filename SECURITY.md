# 广城通安全审计（1.0.2）

## 范围

Android 原生壳、WebView、学校页面登录态、Keystore 凭证、图书馆/一卡通外链、二维码图片、Intent 跳转。

## 已修复

### 高：图书馆 WebView 域名后缀绕过

原逻辑用 `host.endsWith("gcp.edu.cn")` / `endsWith("gzlib.org.cn")` / `endsWith("dataesb.com")`，`evilgcp.edu.cn`、`evilgzlib.org.cn`、`evildataesb.com` 都会被放行，可打开钓鱼页。

修复：`isAllowedLibraryHost()` 改为 `equals` 或 `endsWith(".gcp.edu.cn")` 这类带点边界的判断。

### 高：凭证预填作用域过大

图书馆 WebView 原来只要 URL 包含 `auth.gcp.edu.cn` 或 `sso/login` 就填学校账号密码，第三方 `dataesb.com` / `gzlib.org.cn` 页面带同路径也能拿到密码。

修复：只在主机精确等于 `auth.gcp.edu.cn` 时预填；主 WebView 只对 `auth.gcp.edu.cn` 和 `jwcjw.gcp.edu.cn` 预填。

### 中：WebView 加固缺失

主 WebView 和图书馆 WebView 补齐：

- 禁止文件访问、Content 访问、file:// 跨源访问；
- 禁止保存表单数据；
- 禁止地理位置；
- 禁止 JavaScript 自动开窗；
- 关闭第三方 Cookie；
- 关闭 WebView 远程调试；
- 保留 SSL 证书错误直接取消，不忽略。

### 中：二维码图片 OOM

截图选择复制上限 12 MB；解码按 2048px 上限采样，避免超大图片拖垮进程。

### 中：热水 Intent 劫持

`weixin://` 跳转加 `setPackage("com.tencent.mm")`，避免被第三方 App 注册同名 scheme 截获。

## 0.9.17 本地红测处置（0.99rc9）

- **F1 已接受风险**：`assets/extract-hours.js` 的 `lamp_web:lamp_web_secret` 是学工 API 服务端 Basic 凭证，随 APK/源码公开；用户决定只在校内使用并保留。必须由学校轮换该凭证或改 HTTPS/用户级 token；校园网不是防护边界。
- **F2 已收窄**：图书馆 WebView 改 `MIXED_CONTENT_NEVER_ALLOW`；导航只允许 HTTPS（`opac.gzlib.org.cn` 的 HTTP 作为唯一例外）；预填只对 `https://auth.gcp.edu.cn`。
- **F3 已修**：`fetchHours`、`loadStudentAffairs`、工时页阶段都先检查 `xgxt` 明文审批；未确认会弹出同一提示，不能静默明文加载。
- **F4 已修**：一卡通预填用完即复位，路径改为 `/cashier-mobile`、`/wechat` 前缀且仅 HTTPS。
- **F5 已修**：二维码只接受 `content`/`file` + `image/*`，先写临时文件并原子替换，超限/失败删除半写文件。
- **F6 部分收窄**：`dataesb.com` 已移除（当前 App 不再使用服务大厅/电子证 H5）；`10.9.20.13` 作为图书馆 OPAC 内网地址保留，接受明文风险。
- **F7 已修**：一卡通与学校凭证拆成两个 Keystore alias；账号名也改为 Keystore 加密；清除凭证时删除对应 key。未启用 `setUserAuthenticationRequired`（会强制锁屏验证，接受该缺口）。
- **F8/F9/F10/F13 已修**：子框架外链拦截、`onCreateWindow` 子 WebView 加固、地址栏显示完整 URL、凭证预填 JS 转义 U+2028/U+2029。
- **F11/F12 接受**：UA 伪装用于学校 H5 兼容；剪贴板只写小程序名称/入口，无密码。
- **F14**：1.0 已换独立 release keystore；keystore 与口令只在本地 `build/`，不提交、不公开，需要自行备份。
- **F15**：死代码不可达，不影响运行。

## 仍存在/正式版需处理

- 图书馆 OPAC 仍使用 HTTP 明文；应用已把 OPAC 作为图书馆 WebView 的唯一 HTTP 例外，并在预填上只允许 https auth。正式版建议学校提供 HTTPS。
- 1.0 使用 `build/release.keystore` 正式签名；口令只在本机 `build/release-keystore-password.txt`，未提交。丢失后无法覆盖安装，务必自行备份。
- 未做证书固定（pinning）；正式版可按学校域名做 pin 或至少做 CA 监控。
- `dataesb.com` 已从白名单移除；如后续需要场馆 H5，再单独评估。
- 学工 HTTP 确认已持久化到本机；清除应用数据可重置。

## 未发现

- 没有 `addJavascriptInterface`，没有 JS 原生桥。
- 没有导出 Service/Receiver/Provider，没有 deep link。
- `allowBackup=false`。
- 密码使用 Android Keystore AES-GCM 加密；明文只存在内存和 WebView 登录页。
- SSL 错误直接取消，不忽略。
