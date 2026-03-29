# SuperWirelessKit 功能实现规范 (Spec)

## 1. 项目概述
`SuperWirelessKit` 是一个高级 AE2 工具，旨在彻底简化 AE2 网络的布线逻辑。它允许玩家直接将任何需要频道的 AE2 设备（如 ME Interface, Bus, Pattern Provider 等）逻辑连接到指定的 ME 控制器，而无需物理线缆、`BlockWireless` 或 `TileWireless` 中转方块。

---

## 2. 核心需求
- **绑定机制**：玩家手持工具右键点击 ME 控制器记录“主站”，右键点击目标设备建立“逻辑链路”。
- **无感连接**：连接后，目标设备应立即获得频道并接入网络，如同直接贴在控制器上。
- **稳定性**：连接必须跨越区块加载、服务器重启和网络重构。
- **性能优化**：不应使用高频全局 `WorldTick` 轮询，应集成至 AE2 原生的寻路和更新机制中。
- **能耗惩罚**：根据欧几里得距离计算额外的 AE 能量消耗。

---

## 3. 实现机理对比

| 特性 | 现有 AE2Stuff 实现 | SuperWirelessKit 提议实现 |
| :--- | :--- | :--- |
| **连接对象** | `TileWireless` <-> `TileWireless` | `ControllerNode` <-> `DeviceNode` |
| **连接性质** | 模拟物理邻近（方块实体对） | 纯逻辑映射（虚拟邻居） |
| **邻居发现** | 靠 `FindConnections` 扫描 6 面 | **Mixin 注入** `FindConnections` 返回虚拟邻居 |
| **生命周期** | 随方块加载/卸载自动管理 | 靠全局注册表 + Mixin 强制维持 |
| **频道上限** | 32 (密集电缆模拟) | 受限于控制器的面/总频道限制 |

---

## 4. 技术实现方案 (推荐)

### A. 全局注册表 (Data Layer)
实现一个 `WorldSavedData` 类，用于存储 `Map<DimPos, DimPos>`（设备位置 -> 控制器位置）。
- **参考位置**：`appeng.core.worlddata.WorldData`

### B. Mixin 注入 (Logic Layer)
注入 `appeng.me.GridNode.FindConnections()` 方法：
1. 在原有的 6 方向物理扫描结束后，检查当前 `GridNode` 的坐标是否在 `SuperWirelessKit` 注册表中。
2. 如果存在，获取远程控制器的 `IGridNode`。
3. 手动调用 `new GridConnection(controllerNode, thisNode, ForgeDirection.UNKNOWN)`。
4. **优势**：利用 AE2 原生的 `GridConnection` 管理电力、频道和重连，无需额外写 Tick 监听。

### C. 能量消耗 (Energy Layer)
模仿 `ae2stuff` 的能耗计算公式，在 `GridNode.getIdlePowerUsage` 中注入额外消耗。

---

## 5. 绝对路径源码参考指南

### AE2 核心源码 (核心逻辑与接口)
- **邻居扫描逻辑**：`D:\CODE\Sources\Applied-Energistics-2-Unofficial-rv3-beta-695-GTNH\src\main\java\appeng\me\GridNode.java` 中的 `FindConnections()`。这是实现虚拟连接的最佳切入点。
- **连接生命周期**：`D:\CODE\Sources\Applied-Energistics-2-Unofficial-rv3-beta-695-GTNH\src\main\java\appeng\me\GridConnection.java`。了解 `mergeGrids` 和 `repath` 的触发条件。
- **P2P 远程连接参考**：`D:\CODE\Sources\Applied-Energistics-2-Unofficial-rv3-beta-695-GTNH\src\main\java\appeng\parts\p2p\PartP2PTunnelME.java`。观察它是如何维护两个 `outerProxy` 之间的手动连接的。
- **网络代理工具**：`D:\CODE\Sources\Applied-Energistics-2-Unofficial-rv3-beta-695-GTNH\src\main\java\appeng\me\helpers\AENetworkProxy.java`。
- **网格接口**：`D:\CODE\Sources\Applied-Energistics-2-Unofficial-rv3-beta-695-GTNH\src\main\java\appeng\api\networking\IGridNode.java`。

### AE2Stuff 源码 (交互逻辑与能耗公式)
- **无线连接器实体**：`D:\CODE\Sources\ae2stuff-0.10.0-GTNH\src\main\scala\net\bdew\ae2stuff\machines\wireless\TileWireless.scala`。
    - *重点 1*: `setupConnection` (L109-151) 调用 AEApi 建立连接。
    - *重点 2*: `getIdlePowerUsage` 计算公式 (L121-125)。
- **高级连接工具**：`D:\CODE\Sources\ae2stuff-0.10.0-GTNH\src\main\scala\net\bdew\ae2stuff\items\AdvWirelessKit.scala`。
    - *重点 1*: 排队机制 (L145-192)。
    - *重点 2*: 绑定与权限检查 (L193-300)。
- **坐标 NBT 存储基类**：`D:\CODE\Sources\ae2stuff-0.10.0-GTNH\src\main\scala\net\bdew\ae2stuff\misc\AdvItemLocationStore.scala`。用于学习如何高效在物品 NBT 中存储多个坐标对。
- **Waila 数据支持**：`D:\CODE\Sources\ae2stuff-0.10.0-GTNH\src\main\scala\net\bdew\ae2stuff\waila\WailaWirelessDataProvider.scala`。可参考如何显示连接状态。

---

## 6. 注意事项
- **跨维度支持**：AE2 默认不支持跨维度的 `IGridConnection`。如果需要支持跨维度，需要更复杂的虚拟代理（类似量子链接桥）。
- **控制器面限制**：直接连到控制器节点可能会占用该面的 32 频道配额，需在 UI 或提示中告知玩家。
- **安全性**：连接前必须校验玩家对两端网络的 `BUILD` 权限。
