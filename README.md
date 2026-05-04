# NHAEUtilities

GTNH (GregTech: New Horizons) 辅助工具模组，为 Minecraft 1.7.10 提供三个功能模块。

An utility mod for GTNH (Minecraft 1.7.10) providing three functional modules.

## 依赖 / Dependencies

- **GregTech 5** (5.09.51.482+)
- **Applied Energistics 2** (rv3-beta-690+)
- **NotEnoughItems** (2.8.44+)

## 模块 / Modules

模块可在配置文件中独立开关（需重启生效）。

Each module can be toggled independently in the config (requires restart).

---

### 1. 样板生成器 / Pattern Generator

**物品：** 样板生成器 (Pattern Generator)

批量将 GregTech 机器配方编码为 AE2 处理样板，支持高级过滤、矿辞替换和冲突解决。

Batch-encode GregTech machine recipes into AE2 processing patterns with filtering, ore dict replacement, and conflict resolution.

#### 功能 / Features

| 功能 | 说明 |
|------|------|
| 批量编码 | 构建/刷新缓存后，批量导出 GT 机器配方为处理样板 |
| 智能过滤 | 基于缓存的正则过滤、矿辞替换规则和电压等级过滤 |
| 显式语法 | 过滤字段使用 `[ID]` / `(矿辞正则)` / `{显示名正则}`；`*` 为不过滤 |
| 冲突解决 | 多配方输出同一产物时，通过 GUI 手动选择 |
| 虚拟存储 | 生成的样板存在内部存储中，不直接进入背包 |
| 等量消耗 | 自动从绑定 ME 网络或背包中消耗空白样板 |

#### 用法 / Usage

| 操作 | 效果 |
|------|------|
| 右键空气 | 打开生成器 GUI |
| Shift + 右键空气 | 打开虚拟存储 GUI |
| Shift + 右键方块 | 检测方块/导出样板到容器 |
| 安全终端绑定 | 绑定到 ME 网络以自动消耗空白样板 |

#### 命令 / Commands

```
/patterngen list                          # 列出所有配方映射
/patterngen count <id> [filters...]       # 预览匹配配方数量
/patterngen generate <id> [filters...]    # 生成样板
```

---

### 2. 样板路由 / Pattern Routing

**物品：** 配方映射分析仪 (Recipe Map Analyzer)

将 AE2 编码样板自动路由到匹配的 GregTech 多方块输入仓；同时提供配方分析功能。

Automatically route AE2 encoded patterns into matching GregTech multiblock crafting input hatches.

#### 功能 / Features

- **样板自动路由** -- 编码样板生成时自动路由到对应多方块控制器的输入仓
- **仓位分配刷新** -- 多方块结构成型时自动分配配方到双输入仓
- **空白仓自动配置** -- 空仓收到第一个样板时自动设定配方
- **手动物品提取** -- 配置空白仓时自动从 AE 网络提取所需的手动/电路物品
- **配方映射分析** -- 分析配方映射，显示重复与单次出现的输入类型，计算最少样板组装数

#### 用法 / Usage

| 操作 | 效果 |
|------|------|
| 右键空气 | 打开配方分析 GUI |
| Shift + 右键 GT 机器 | 检测并保存配方映射 |

#### 命令 / Commands

```
/nau repairrouting    # 修复已加载多方块控制器中的空白路由元数据
```

---

### 3. 超级无线设置工具 / Super Wireless Kit

**物品：** 超级无线设置工具 (Super Wireless Kit)

将任意 AE2 频道设备无线连接到 ME 控制器，无需线缆或量子桥。

Wirelessly connect arbitrary AE2 channel devices to an ME controller without cables or quantum bridges.

#### 功能 / Features

- **双模式切换** -- QUEUE（记录目标）和 BIND（连接目标到控制器）
- **批量收集** -- 潜行+左键设备可递归加入相邻的频道设备
- **虚拟网格连接** -- 在控制器节点与目标设备间创建虚拟 GridConnection
- **持久化绑定** -- 绑定保存到世界数据，区块重载/节点刷新时自动重连
- **权限检查** -- 遵守 AE2 SecurityPermissions.BUILD

#### 用法 / Usage

| 模式 | 操作 | 效果 |
|------|------|------|
| QUEUE | Shift + 右键设备 | 将目标加入队列 |
| QUEUE | 潜行 + 左键设备 | 批量递归收集相邻频道设备 |
| BIND | Shift + 右键控制器 | 执行绑定，连接所有队列中的目标 |
| 任意 | 右键空气 | 切换 QUEUE / BIND 模式 |

---

## 构建 / Build

```bash
./gradlew build          # spotless → 编译 → 测试 → JAR
./gradlew spotlessApply  # 修复格式问题
```

## 部署 / Deploy

```bash
cp build/libs/nhaeutilities-*-dirty.jar "<你的GTNH实例>/minecraft/mods/"
```

## 配置 / Configuration

模块开关、调试模式等配置通过 Forge 配置文件管理（游戏内 Mods → NHAEUtilities → Config）。

Module toggles and debug modes are managed via Forge configuration (in-game: Mods → NHAEUtilities → Config).
