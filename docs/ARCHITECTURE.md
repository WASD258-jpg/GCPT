# 架构

## 概览

广城通是单 Activity 的 Android 原生应用：

```text
MainActivity
├── 四页原生 UI：首页 / 课表 / 生活 / 设置
├── 子页面：学业与学时 / 学校门户 / 学工
├── WebView：学校门户、教务、学工、图书馆、办事大厅、医务室
├── Android Keystore：学校 / 一卡通凭证
├── 本地缓存：课表 JSON、设置、余额、电子证图片
└── 外部 Intent：微信小程序 scheme、图片选择器
```

没有服务端、账号体系、统计 SDK、推送或远程配置。

## 关键文件

| 文件 | 职责 |
| --- | --- |
| `MainActivity.java` | 页面、导航、WebView、同步、Keystore、Intent |
| `Ui.java` | 设计 token、玻璃卡、按钮、圆角、渐变 |
| `LineIconView.java` | 手绘线性图标，不依赖图标库 |
| `WeekGrid.java` | 周览网格与课程命中 |
| `CourseDetailDialog.java` | 课程详情 |
| `CourseParser.java` / `CourseData.java` | 课表解析与缓存 |
| `TermCalendar.java` | 教学周与日期计算 |
| `HoursData.java` | 学时账本 |
| `PeriodTimes.java` | 11 节课时间 |

## UI

- 四页底部导航，子页面隐藏导航并显示返回；
- 页面切换 280ms 淡入 + 8dp 上浮，旧页 170ms 淡出；
- 侧栏、周选择器、课程详情、电子证弹层有独立的进入动画；
- 设计 token 在 `Ui.java`，玻璃面板是半透明白 + 细边 + 阴影的原生近似；
- 无 AndroidX、无第三方 UI/图标库。

## WebView 与网络

- 只允许 `*.gcp.edu.cn`；图书馆额外允许 `gzlib.org.cn` 与 OPAC 内网 `10.9.20.13`；
- 图书馆仅 HTTPS，`opac.gzlib.org.cn` / `10.9.20.13` 的 HTTP 作为显式例外；
- 未启用 `addJavascriptInterface`；
- SSL 错误直接取消，不忽略；
- 学工 HTTP 必须经过用户确认；
- 预填只对 `https://auth.gcp.edu.cn` 等明确页面生效，验证码手动。

## 数据与凭证

- 学校 / 一卡通密码使用独立的 Android Keystore AES-GCM alias；
- 账号名同样用 Keystore 加密；
- 清除凭证时删除对应 key；
- 课表、设置、余额在应用私有目录；`allowBackup=false`；
- 电子证二维码只存私有目录，导入时校验来源/类型/大小并原子替换。

## 同步与页面

- 课表同步导入后只刷新当前页，不强行切换 tab；
- 后台自动同步在用户浏览门户时跳过；
- 一卡通从生活页发起登录，完成后回生活页；若中途切到其它页则不抢页面。

## 已知风险

- 学工接口仍为学校 HTTP + 服务端 Basic 凭证（F1，已接受风险）；
- 图书馆 OPAC 仍为 HTTP；
- 学校接口变更可能导致解析失败；
- 详见 [SECURITY.md](../SECURITY.md)。
