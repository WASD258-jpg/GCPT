# 构建与签名

本项目不使用 Gradle，`build-demo.ps1` 手动完成：

```text
javac → jar → d8 → aapt → zipalign → apksigner
```

## 环境

- JDK 17+（`JAVA_HOME`）
- Android SDK 34（`ANDROID_HOME`）
- build-tools 34.0.0（`d8`、`aapt`、`zipalign`、`apksigner`）
- Windows PowerShell 7（`pwsh`）

## 调试构建

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot'
$env:ANDROID_HOME = 'D:\Android\sdk'
$env:RELEASE_KS_PASS = (Get-Content .\build\release-keystore-password.txt -Raw).Trim()
pwsh -File .\build-demo.ps1
```

输出：`dist/gcpt-1.0.2.apk`。

> `build-demo.ps1` 现在默认使用 `build/release.keystore` 正式签名；如果本地没有该文件，脚本会用 `RELEASE_KS_PASS` 生成一个 4096-bit RSA 的 release key。请自行备份 keystore 和口令，**不要提交**。

## 手动验证

```powershell
$tools = Join-Path $env:ANDROID_HOME 'build-tools\34.0.0'
& "$tools\apksigner.bat" verify --print-certs dist\gcpt-1.0.2.apk
& "$tools\aapt.exe" dump badging dist\gcpt-1.0.2.apk |
  Select-String 'package:|application-icon|launchable-activity'
Get-FileHash -Algorithm SHA256 dist\gcpt-1.0.2.apk
```

## 源码清单

新增 Java 文件时，必须同时加入 `build-demo.ps1` 的 `$sources` 列表；当前包含：

```text
Ui.java
LineIconView.java
TermCalendar.java
CourseData.java
HoursData.java
PeriodTimes.java
CourseParser.java
WeekGrid.java
CourseDetailDialog.java
MainActivity.java
```

## 发布

1. 更新 `MainActivity.VERSION`、`AndroidManifest.xml` 的 `versionCode/versionName`；
2. 更新 `README.md` 的版本、下载链接和 SHA-256；
3. 构建并验证签名；
4. 打 tag，例如 `v1.0.2`；
5. 在 GitHub Releases 上传 `dist/guangcheng-<version>.apk`，附上 SHA-256 和数据迁移说明。

release keystore、口令、`build/`、`dist/` 都不进仓库。
