# NHAEUtilities

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.7.10-62b47a?style=flat-square&logo=mojang" alt="MC">
  <img src="https://img.shields.io/badge/Forge-10.13.4.1614-f16436?style=flat-square" alt="Forge">
  <img src="https://img.shields.io/badge/Version-0.3-blue?style=flat-square" alt="Version">
  <img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/Java-21-e76f00?style=flat-square&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/GTNH-Compatible-ffaa00?style=flat-square" alt="GTNH">
</p>

<p align="center"><strong>GregTech: New Horizons 实用辅助模组</strong> — 样板生成 · 样板路由 · 无线 ME</p>

---

## 功能一览

| 模块 | 物品 | 核心能力 |
|------|------|----------|
| **样板生成器** | Pattern Generator | 批量将 GT 机器配方编码为 AE2 处理样板，支持过滤、矿辞替换、冲突解决 |
| **样板路由** | Recipe Map Analyzer | 自动将 AE2 样板路由至 GT 多方块输入仓，分析配方映射，计算最少样板数 |
| **超级无线设置工具** | Super Wireless Kit | 无需线缆或量子桥，将任意 AE2 频道设备无线连接至 ME 控制器 |

[English](README_EN.md)

---

## 目录

- [快速开始](#快速开始)
- [样板生成器](#样板生成器)
- [样板路由](#样板路由)
- [超级无线设置工具](#超级无线设置工具)
- [构建](#构建)
- [配置](#配置)
- [贡献](#贡献)
- [许可证](#许可证)

---

## 快速开始

**依赖**

| 模组 | 最低版本 |
|:-----|:---------|
| GregTech | 5.09.51.482 |
| Applied Energistics 2 | rv3-beta-690 |
| Not Enough Items | 2.8.44 |

**安装**

1. 从 [Releases](https://github.com/Ch4oooooooLL/NHAEUtilities/releases) 下载最新 JAR
2. 放入 Minecraft 实例的 `mods/` 目录
3. 启动游戏 — 各模块可在配置文件中独立开关

> **提示** — 样板生成器首次使用或模组变动后，请先在主界面点击 **Cache** 构建缓存。

---

## 样板生成器

Pattern Generator 将 GregTech 机器配方批量编码为 AE2 处理样板。

### 特性

- **批量编码** — 构建缓存后一键导出 GT 机器配方为处理样板
- **智能过滤** — 支持正则过滤、矿辞替换、电压等级筛选
- **显式语法** — 过滤字段使用 `[ID]` / `(矿辞正则)` / `{显示名正则}`；`*` 表示不过滤
- **冲突解决** — 多配方输出同一产物时通过 GUI 手动选择
- **虚拟存储** — 生成的样板存入内部存储，不占背包空间
- **等量消耗** — 自动从绑定的 ME 网络或背包中消耗空白样板

### 操作

| 交互 | 效果 |
|:-----|:-----|
| 右键空气 | 打开**主配置终端** |
| Shift + 右键空气 | 打开**存储管理器** |
| Shift + 右键方块 | 检测 GT 机器配方 / 导出样板到容器 |
| Shift + 右键安全终端 | 绑定 ME 网络用于消耗空白样板 |

<details>
<summary>命令参考</summary>

```
/patterngen list                        # 列出所有配方映射
/patterngen count <id> [filters...]     # 预览匹配配方数
/patterngen generate <id> [filters...]  # 生成样板
```
</details>

---

## 样板路由

Recipe Map Analyzer 将 AE2 样板自动路由至匹配的 GT 多方块输入仓，提供配方分析功能。

### 特性

- **自动路由** — 样板生成时自动分发至对应多方块控制器的输入仓
- **仓位刷新** — 多方块结构成型时自动重新分配仓位配方
- **空仓自动配置** — 空输入仓收到首个样板时自动设定配方
- **手动物品提取** — 配置空仓时自动从 AE 网络提取所需编程电路等物品
- **配方分析** — 分析配方映射，展示重复/单一出现的输入类型，计算最少样板组装数
- **过滤规则** — 支持黑名单与手动匹配规则，精确控制路由行为

### 操作

| 交互 | 效果 |
|:-----|:-----|
| 右键空气 | 打开**配方分析 GUI** |
| Shift + 右键 GT 机器 | 保存该机器的配方映射供分析与路由 |

<details>
<summary>命令参考</summary>

```
/nau repairrouting    # 修复多方块控制器中的空白路由元数据
```
</details>

---

## 超级无线设置工具

Super Wireless Kit 将 AE2 频道设备无线连接至 ME 控制器。

### 特性

- **双模式** — QUEUE（记录目标）/ BIND（连接目标到控制器）
- **批量收集** — 潜行 + 左键设备可递归收集相邻的频道设备
- **虚拟网格连接** — 在控制器节点与目标设备间建立虚拟 GridConnection
- **持久化绑定** — 绑定保存至世界数据，区块重载 / 节点刷新时自动重连
- **权限检查** — 遵守 AE2 SecurityPermissions.BUILD

### 操作

| 模式 | 交互 | 效果 |
|:-----|:-----|:-----|
| QUEUE | Shift + 右键设备 | 将设备加入连接队列 |
| QUEUE | 潜行 + 左键设备 | 批量递归收集相邻频道设备 |
| BIND | Shift + 右键控制器 | 将队列中所有目标无线连接至该控制器 |
| 任意 | 右键空气 | 切换 QUEUE / BIND 模式 |

---

## 构建

```bash
JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk ./gradlew build          # 格式检查 + 编译 + 测试 + 打包
JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk ./gradlew spotlessApply  # 修复格式问题
```

> **注意** — 需要 Java 21。Java 25 与 Gradle 8.12 不兼容。

---

## 配置

模块开关、调试选项等通过 Forge 配置文件管理：

- 游戏内：`Mods → NHAEUtilities → Config`
- 文件：`<实例>/config/nhaeutilities.cfg`

---

## 贡献

欢迎提交 Issue 和 Pull Request。请遵循仓库的提交风格：

```
feat: 简短描述
fix: 简短描述
```

---

## 许可证

[MIT](LICENSE)
