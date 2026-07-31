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
- Fractured（20–59）：解锁矿脉感知、祭坛互动、隐藏实体；禁止睡眠、村民交易
- Ascended（0–19）：开放战斗增益；所有生物敌对、契约失效；离开该阶段时理智回到 20
- HUD 使用满月、半月、月食图标，不使用普通数字条

## 当前阶段

基础工程基线和 Sanity 数据层已通过人工验收：

- 服务端玩家实体持有 0–100 的理智值，默认 100，所有修改统一 clamp。
- `SanityStage` 集中定义 Lucid、Fractured、Ascended 的固定边界。
- 理智值写入玩家 NBT，并在玩家重生时复制，保证退出重进、死亡/重生和跨维度保留。
- 服务端在加入、重生、跨维度和数值改变时向客户端同步。
- 客户端只保存展示用镜像，不提供修改服务端数值的网络入口。
- 开发命令为 `/thecost sanity get|set|add`；`set` 和 `add` 需要权限等级 2。

项目 Mod ID 已统一为 `thecost`：Fabric 元数据、Loom 配置、资源命名空间、Mixin 配置名和网络频道均使用该值。Sanity 同步频道为 `thecost:sanity_sync`，命令保持 `/thecost`，存档 NBT 键保持 `thecost_sanity`。

最小月相理智 HUD 已实现并通过人工验收：

- HUD 只存在于 client source set，只读取 `ClientSanityState`，不改变服务端权威数据。
- 左上角以 8 像素安全边距显示 16×16 月相图标和当前理智数值。
- Lucid 使用满月、Fractured 使用半月、Ascended 使用日蚀/黑月占位像素图标。
- 图标为原创占位资源，位于 `assets/thecost/textures/gui/sanity/`，后续可替换正式美术。
- 客户端尚未收到服务端同步、尚未进入世界、断线或隐藏 HUD 时不渲染。
- HUD 使用缩放后的 GUI 坐标，窗口和 GUI Scale 改变时保持左上角相对位置。

环境光与温暖火源驱动的理智自然增减 v1 已实现：

- 所有环境判定、计时和理智修改只在服务端执行；客户端仍只接收同步并渲染 HUD。
- 每 40 ticks（2 秒）评估一次在线且存活的生存/冒险模式玩家；creative、spectator 和未存活玩家不参与。
- 在玩家眼睛位置读取 Minecraft 光照系统的实际有效光照，包含天空光和方块光的综合结果。
- 有效光照 0–3 时，同一状态连续累计 10 秒后理智 -1；4–7 时连续累计 30 秒后理智 -1；8–11 时停止自然变化。
- 有效光照 12–15 时，6 格球形范围内存在 `thecost:sanity_comfort_sources` 标签中的有效温暖火源，连续累计 20 秒后理智 +1；没有有效温暖火源时只停止下降。
- 标签包含普通篝火、普通火焰、普通火把/墙上火把、普通灯笼、熔炉/高炉/烟熏炉、所有普通蜡烛和对应蜡烛蛋糕；明确不包含灵魂火系列。
- 带 `LIT` 属性的标签方块仅在 `LIT=true` 时有效；无 `LIT` 属性且被明确列入标签的持续发光方块可直接视为有效。
- 快速下降、缓慢下降、安全、恢复四种环境状态的临时计时互不继承；状态切换、重生、跨维度、退出或服务器停止时按需重置/清理，计时不写入 NBT。
- 所有自然变化继续通过 `SanityData` 的统一 clamp 与同步入口；0 不继续下降，100 不继续恢复。
- 管理员调试命令 `/thecost sanity environment` 返回有效光照、温暖火源、环境状态、参与资格和距离下一次变化的时间。

环境理智系统已通过人工验收：生存模式下黑夜露天 `light=4` 会进入 `slow drain`，并按规则降低理智。该行为不得在后续阶段中无故修改。

三阶段玩法 v1 已实现：

- `SanityStage` 仍是 Lucid、Fractured、Ascended 阶段边界的唯一来源；限制逻辑不重复硬编码数值边界。
- 统一理智写入口在实际跨越阶段时，仅向该玩家显示一次最终目标阶段的可翻译提示；命令和环境自然增减均覆盖。
- NBT 读取、加入/同步当前值、重生复制不会触发阶段提示；从一个阶段直接跨过多个边界时不补发中间阶段提示。
- 生存/冒险玩家处于 Fractured 或 Ascended 时不能睡眠；回到 Lucid 后立即解除。creative 和 spectator 不受限制。
- 生存/冒险玩家处于 Fractured 或 Ascended 时不能打开村民或流浪商人的交易界面；攻击和其他非交易交互不受该限制。
- 阶段切换、睡眠拒绝和交易拒绝文本均使用 `message.thecost.sanity.*` 语言键，并提供 `en_us` 与 `zh_cn`。

用户选择暂不逐阶段人工验收；三阶段玩法 v1 已构建成功但尚未人工验收，需保留实现，待 Sanity 系统连续完成后统一测试。

Ascended 视觉效果 v1 已实现：

- 效果只在客户端已进入世界、已收到有效理智同步且 `SanityStage` 为 Ascended 时启用；服务端权威和同步包结构不变。
- 在 `GameRenderer.render` 中原版世界渲染及其后处理完成、主 framebuffer 重新绑定之后，GUI 投影和 HUD 渲染之前运行独立 `PostEffectProcessor`，因此 HUD、聊天、菜单、字幕、调试界面和月相图标不被去饱和；F1 只隐藏 HUD，不关闭世界效果。
- Ascended 深度使用阶段自身的最小/最大值计算，不重复硬编码阶段边界。深度 `d=(Ascended.max-sanity)/(Ascended.max-Ascended.min)`，先经 `d²(3-2d)` 平滑，再得到强度 `0.28+(0.88-0.28)×smoothstep(d)`；对应饱和度从 sanity 19 的 0.72 连续降至 sanity 0 的 0.12。
- 强度按真实帧间时间以 1.5 秒全量变化速度线性趋近目标，进入/离开 Ascended 及数值变化均平滑，帧率变化时速度基本稳定。
- 使用项目自管的后处理实例，不占用、替换或关闭 `GameRenderer` 的原版后处理器；效果串接在原版后处理之后。
- 后处理配置位于 `assets/thecost/shaders/post/ascended_desaturate.json`。Minecraft 1.20.1 的 effect program 加载器会将 program/GLSL 阶段硬编码到 `assets/minecraft/shaders/program`，因此本实现安全复用原版 `color_convolve` 与 `blit` 程序，只在 `thecost` 命名空间保存自有后处理链，避免污染 `minecraft` 命名空间。
- 窗口或 framebuffer 尺寸变化时重新配置独立后处理目标；主 framebuffer 实例被替换时重建处理器；资源重载后关闭并按需重建。
- 连接初始化、断线、未同步、未进入世界以及死亡/跨维度的 respawn 包会立即清空插值状态；新同步到达前不读取旧理智或渲染效果。

用户确认 Ascended“全生物敌意”的语义为“世界排斥”：不是让所有动物获得攻击能力，而是攻击型生物主动敌视、普通动物和商人远离、宠物拒绝 Ascended 主人。

Ascended 世界排斥 v1 已实现：

- 只对存活、非 creative、非 spectator 且 `SanityStage.ASCENDED` 的服务端玩家生效；所有 Goal 和交互最终判定均在服务器完成。
- 使用 `ServerEntityEvents.ENTITY_LOAD` 在实体加载时一次性挂载条件 Goal，不按 tick 扫描全世界；临时“已挂载”标记仅为实体内存字段，不写 NBT。
- `HostileEntity` 及 `thecost:ascended_forced_attackers` 实体类型标签中的攻击型生物获得优先级 1 的 `AscendedPlayerTargetGoal`。目标搜索平均每 10 ticks 尝试一次，使用生物原生 `GENERIC_FOLLOW_RANGE`、具体玩家谓词和可见性检查；Goal 只设置目标，伤害和追击继续由生物原有攻击 AI 完成。
- 强制攻击标签 v1 包含狼、蜜蜂、猪灵、僵尸猪灵、北极熊、铁傀儡、史莱姆、岩浆怪和恶魂；便于未来安全扩展。
- 普通 `AnimalEntity` 获得优先级 1、12 格范围、速度 1.1/1.4 的条件逃离 Goal，不添加攻击伤害或近战 Goal。位于强制攻击标签中的动物不使用普通逃离行为。
- 村民和流浪商人使用同样的 12 格逃离 Goal，不写入或清理 Brain 记忆、职业、日程和交易数据；既有低理智交易限制保持不变。
- 驯服实体不会攻击自己的 Ascended 主人，并获得仅对主人的条件逃离 Goal；未驯服的非攻击型可驯服动物按普通动物逃离。Goal 不解除驯服、不改 owner UUID、名字、持久 NBT 或坐下状态；坐下或导航受限时只保证“尽量”远离。
- Ascended 玩家右键普通 `AnimalEntity` 时，服务端拒绝喂食、繁殖、挤奶、剪毛、乘骑/命令等交互；攻击使用独立路径，不受此回调影响。提示键为 `message.thecost.sanity.animal_avoids`，每名玩家最多每 40 ticks 显示一次，并提供 `en_us`、`zh_cn`。
- 多人谓词逐个检查候选玩家；正常玩家不会因附近另一名 Ascended 玩家而被本模组选择为目标或被拒绝动物交互。
- 玩家恢复至 Fractured/Lucid、死亡、退出或跨维度后，条件 Goal 会自然停止；只在当前目标仍是该 Goal 选中的玩家时执行标准清理，不写 anger NBT，也不粗暴清除其他 Goal/模组的仇恨数据。
- v1 不强行处理没有安全通用攻击 Goal 的 Boss 或纯 Brain 驱动生物；这些实体即使可被设置目标，也可能不会执行攻击。非 `AnimalEntity` 的部分被动水生/环境生物也不在普通动物逃离覆盖内，后续按具体生物安全扩展。

统一验收追加：Ascended 与正常玩家同场时分别验证敌对目标隔离；狼、蜜蜂、猪灵系、北极熊、铁傀儡主动攻击；普通动物和商人逃离；宠物不攻击主人且拒绝命令；动物交互提示节流；恢复到 20、死亡、退出和跨维度后行为停止且原关系保留。

Ascended 战斗增益 v1 已实现：

- 只对存活、非 creative、非 spectator 且 `SanityStage.ASCENDED` 的服务端玩家生效；客户端只接收原版属性同步。
- 使用四个固定 UUID 的 transient attribute modifier，不使用药水效果、不显示粒子或状态栏图标，也不写入玩家 NBT。
- 攻击伤害 `GENERIC_ATTACK_DAMAGE` 使用 `MULTIPLY_TOTAL +0.25`；攻击速度 `GENERIC_ATTACK_SPEED` 使用 `MULTIPLY_TOTAL +0.15`；移动速度 `GENERIC_MOVEMENT_SPEED` 使用 `MULTIPLY_TOTAL +0.10`；击退抗性 `GENERIC_KNOCKBACK_RESISTANCE` 使用 `ADDITION +0.20`。
- 统一理智写入口在每次写入时幂等校正 modifier；即使重复写入相同理智也只保留每个固定 UUID 的单个 modifier。
- 玩家加入、死亡重生和跨维度时强制移除同 UUID 的旧 modifier 后按当前阶段重建，以校正旧存档中的缺失或残留；游戏模式改变后立即重新校正。
- 离开 Ascended 或切换到 creative/spectator 时只移除 The Cost 自己的四个固定 UUID，不修改基础属性，也不覆盖装备、药水、附魔或其他模组的 modifier。

Fractured 矿脉感知 v1 已实现：

- 只在客户端进入世界、收到有效理智同步且阶段严格为 `SanityStage.FRACTURED` 时启用；Lucid 和 Ascended 均不扫描、不显示。
- 每 20 client ticks 更新一次缓存，以玩家为中心扫描半径 12 格的球形区域；理论最多检查约 7,153 个已加载位置，不读取未加载区块，不在服务端扫描。
- 使用有界最大堆只保留距离玩家最近的 32 个命中方块；渲染每帧只遍历这份最多 32 项的缓存，不扫描世界。
- 可扩展标签为 `thecost:fractured_detectable_ores`，默认包含主世界煤、铜、铁、金、红石、绿宝石、青金石、钻石及对应深板岩矿石，并包含下界金矿、下界石英矿和远古残骸。
- 使用 `WorldRenderEvents.LAST` 渲染低亮度、冷蓝/月银色、轻微脉动的方块线框；按实际世界坐标减去当前摄像机位置，支持第三人称和摄像机偏移。
- 线框禁用深度测试以实现克制的隔墙感知，不修改世界、不发送挖掘或交互包、不显示文字标签。F1 隐藏 HUD 时仍继续显示。
- 渲染后恢复矩阵、line width、depth mask、深度测试、混合和先前 shader，避免污染原版渲染与 Ascended 后处理。
- 离开 Fractured、未同步、断线、退出世界、死亡重生或跨维度时立即停止渲染并清空缓存；新世界/新同步到达后才重新扫描。

统一验收追加：sanity 19 必须有四项战斗增益且无矿脉轮廓；20 必须无战斗增益且有轮廓；59 有轮廓；60 两者均无。重复执行 `set 19` 不得叠加；还需核对加入、重生、跨维度、创造/旁观切换后的属性清理，以及矿石数量上限、隔墙显示、第三人称、F1 和断线缓存清理。

Sanity 范围已由用户明确收敛：复杂配方限制从计划中删除；异界入口、自定义维度和 Boss 裂隙从计划中删除；隐藏实体保留为后续 Sanity 核心内容。不得在后续任务中沿用被删除的早期规划，除非用户再次明确改变决定。

Sanity 后续尚待完成：Fractured 的祭坛互动与隐藏实体，以及 Ascended 的契约失效等仍保留玩法。隐藏实体是后续 Sanity 核心；下一阶段必须由后续任务明确选择。本轮不实现隐藏实体、季节、肢体、Boss、祭坛或新的理智来源。

## 验证命令

```powershell
.\gradlew.bat build --stacktrace
```
