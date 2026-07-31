# The Cost 项目约束

每次开始工作时先阅读本文件，并以本文件和项目实际文件为准。

## 工程基线

- 项目：The Cost
- Minecraft Java Edition：1.20.1
- Java：17
- 模组加载器：Fabric
- API：Fabric API
- 构建：Gradle Groovy DSL，使用项目自带 Wrapper
- mappings：Yarn
- Mod ID：`thecost`
- Java 包名：`com.lenefun.thecost`
- source sets：common/client 已拆分
- Data Generation：已启用
- 不使用 Kotlin
- GeckoLib 计划使用 4.2.x；在基础工程编译成功前不得添加、升级或调整该依赖
- 不升级 Minecraft，不做与当前任务无关的依赖升级或重构
- 保护用户已有修改；改动前先检查现状，只做当前目标所需的最小修改

## 项目定位

黑暗奇幻生存模组，核心主题是“力量具有不可逆代价”。完整设计包括夏/冬两季、温度、三阶段理智、肢体损伤、Grafting Altar、Deerclops、Dragonfly、契约与牺牲系统。

美术采用骨白、冷蓝、暗金、月银的 Souls-like 风格，不使用血浆或内脏视觉。

## v0.1 范围

仅实现：

- 冬季
- Deerclops
- 三阶段理智
- 月相 HUD
- 肢体损伤及玩家模型表现
- Grafting Altar
- 冬季雾雪
- Ascended 去饱和视觉

暂缓到后续阶段：

- Dragonfly
- 夏季野火
- 完整契约系统
- Runic Circle 完整绘图 UI

## 三阶段理智

- Lucid（60–100）：正常状态
- Fractured（20–59）：解锁矿物透视、祭坛互动、隐藏实体；禁止复杂配方、睡眠、村民交易
- Ascended（0–19）：开放异界入口与战斗增益；所有生物敌对、契约失效；离开该阶段时理智回到 20
- HUD 使用满月、半月、月食图标，不使用普通数字条

## 当前阶段

基础工程基线和 Sanity 数据层已通过人工验收：

- 服务端玩家实体持有 0–100 的理智值，默认 100，所有修改统一 clamp。
- `SanityStage` 集中定义 Lucid、Fractured、Ascended 的固定边界。
- 理智值写入玩家 NBT，并在玩家重生时复制，保证退出重进、死亡/重生和跨维度保留。
- 服务端在加入、重生、跨维度和数值改变时向客户端同步。
- 客户端只保存展示用镜像，不提供修改服务端数值的网络入口。
- 开发命令为 `/thecost sanity get|set|add`；`set` 和 `add` 需要权限等级 2。

当前项目元数据和资源仍沿用历史 Mod ID `the-cost`，与目标 Mod ID `thecost` 不一致。Sanity 同步频道暂时使用实际已加载的 `the-cost` 命名空间，命令保持 `/thecost`；不要在无专项迁移与验证时批量重命名资源。

最小月相理智 HUD 已实现：

- HUD 只存在于 client source set，只读取 `ClientSanityState`，不改变服务端权威数据。
- 左上角以 8 像素安全边距显示 16×16 月相图标和当前理智数值。
- Lucid 使用满月、Fractured 使用半月、Ascended 使用日蚀/黑月占位像素图标。
- 图标为原创占位资源，位于 `assets/the-cost/textures/gui/sanity/`，后续可替换正式美术。
- 客户端尚未收到服务端同步、尚未进入世界、断线或隐藏 HUD 时不渲染。
- HUD 使用缩放后的 GUI 坐标，窗口和 GUI Scale 改变时保持左上角相对位置。

下一步是在客户端人工验证三个阶段图标、数值更新、GUI Scale、窗口尺寸、F1 隐藏和重新连接行为。当前不得提前实现屏幕滤镜、音效、能力、自然增减、动画、配置界面、GeckoLib 或其他玩法。

## 验证命令

```powershell
.\gradlew.bat build --stacktrace
```
