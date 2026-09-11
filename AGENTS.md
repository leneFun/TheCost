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

环境理智自然变化已按用户最新决定改为参考《饥荒》原版规则；本规则明确覆盖此前已验收的 10/30 秒掉点及温暖火源恢复规则：

- 所有环境判定、累计和理智修改仍只在服务器执行；每 20 ticks（1 秒）评估存活的生存/冒险玩家，creative、spectator 与未存活玩家不参与。
- 玩家眼睛处有效光照 `0–3` 视为完全黑暗，按《饥荒》原版 `-50 sanity/min` 执行；实现为 24 ticks 累计 -1，并保留余数，因此长期速率准确且不会因 1 秒评估粒度持续漂移。
- 不处于完全黑暗时，主世界黄昏/夜晚（Minecraft `timeOfDay >= 12000`）或眼睛位置不可见天空的洞穴/室内环境，按原版 `-5 sanity/min` 执行，即每 240 ticks（12 秒）-1。白天露天安全，不自然变化。
- 普通火源和高亮度只能避免“完全黑暗”速率；黄昏、夜晚与洞穴的基础 -5/min 仍存在。温暖火源不再自然恢复理智，`thecost:sanity_comfort_sources` 资源标签仅为旧兼容保留，不参与当前理智算法。
- 完全黑暗、黄昏/夜晚/洞穴和安全三种状态的临时累计互不继承；状态切换、重生、跨维度、退出或服务器停止时清理。所有数值修改继续走 `SanityData` 统一 clamp、阶段切换提示和同步入口，0 不继续下降。
- `/thecost sanity environment` 返回有效光照、天空可见性、是否暗夜时段、当前状态、每分钟速率、参与资格及下一次变化时间。

统一人工测试更新：旧的“黑夜露天 light=4 每 30 秒 -1”不再是目标。新目标为夜晚有灯/洞穴有灯每 12 秒 -1，完全黑暗长期每分钟 -50，白天露天不变化，火源不再自然恢复理智。

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

Sanity 后续尚待完成：Fractured 的隐藏实体，以及 Ascended 的契约失效等仍保留玩法；Fractured 祭坛互动已有下文记录的功能基础版。隐藏实体是后续 Sanity 核心；下一阶段必须由后续任务明确选择。不得无任务授权扩展季节、Boss、祭坛或新的理智来源。

The Witness 可召唤实体基础版已实现：

- GeckoLib 固定使用与 Minecraft 1.20.1/Fabric 兼容的 `4.2.4`，依赖坐标为 `software.bernie.geckolib:geckolib-fabric-1.20.1:4.2.4`；公共入口在实体注册前调用 `GeckoLib.initialize()`。源码仍以 Java 17 为目标，未升级 Minecraft、Loom 或 Fabric API。
- 实体 ID 为 `thecost:witness`，属于敌对生物组；基础碰撞箱为 `0.75 × 2.75` 格，生命 30、攻击 5、移动速度 0.28、追踪范围 24。
- v1 AI 包含游泳、近战、游荡、注视玩家、环顾、反击及主动选择玩家目标。仅复用原版稳定 Goal，不加入自然生成、生物群系规则、掉落、Boss、祭坛、维度或 Sanity 生成逻辑。
- 服务端实体实现 GeckoLib `GeoEntity`。持续动画控制器在 idle、walk 和 observe 间切换：移动时 walk；有攻击目标、基本静止且尚未进入近战距离时 observe；其余为 idle。
- 一次性动作控制器使用 GeckoLib triggerable animation：实体首次加入世界后的首个服务端 tick 触发 reveal；真实 `tryAttack` 成功后触发 attack；服务端死亡回调触发 death，并在动画末帧保持姿态。实体自身以 `thecost_reveal_played` 布尔 NBT 记录 reveal 已播放，避免区块卸载、服务器重启后重复；该键不涉及玩家 Sanity 数据。
- 客户端 `GeoModel` 严格引用 `assets/thecost/geo/witness.geo.json`、`assets/thecost/textures/entity/witness.png` 和 `assets/thecost/animations/witness.animation.json`；`GeoEntityRenderer` 阴影半径为 0.35，不添加发光层。现有模型高度与实体碰撞箱相符，本轮不做渲染缩放或原点偏移。
- 普通 `/summon thecost:witness` 创建的是 unbound 调试实体，继续对所有客户端正常渲染，可用于验证注册、模型、纹理、动画与 AI；未添加自然生成或刷怪蛋。Sanity 驱动的 managed 实体采用下文记录的 owner-only 规则。

The Witness 专属音效与死亡渲染修正已实现：

- 注册服务端广播的 `thecost:entity.witness.ambient`、`thecost:entity.witness.hurt`、`thecost:entity.witness.death` 三个 `SoundEvent`；实体通过原版 `getAmbientSound`、`getHurtSound`、`getDeathSound` 返回事件，不在客户端 tick 中手动播放。
- 音效源自 OpenGameArt 的 **80 CC0 creature SFX #2**，作者/上传者为 rubberduck，页面明确标记 CC0。项目根目录 `SOUND_CREDITS.md` 记录原页面、许可证、原始文件到成品文件的映射及播放时处理；原始 ZIP 保留于 `art/audio_sources/witness/` 供审计，不得将这些声音宣称为 The Cost 原创录音。
- 最终声音包含 `ambient_1.ogg`、`ambient_2.ogg`、`ambient_3.ogg`、`hurt_1.ogg`、`hurt_2.ogg` 和 `death.ogg`，均保持源文件的合法 stereo 48 kHz OGG Vorbis 位流；通过 `sounds.json` 设置低音量与降调播放。环境音最小延迟由原版 80 ticks 提高到 240 ticks，新实体也从该负延迟开始计时，并继续使用原版随机 ambient 调度，不循环播放。
- death 使用独立且最后注册的 GeckoLib triggerable controller，`onDeath` 先执行原版死亡逻辑，再由服务端只触发一次 death 动画；正常伤害和 `/kill` 使用同一路径。Witness 自身覆写 `updatePostDeath`，只把原版 20 ticks 的死亡移除阈值延长到 26 ticks，沿用原版死亡粒子状态和 `KILLED` 移除原因，不复活、不重复执行掉落/经验，也不继续攻击。
- Witness 专属 `GeoEntityRenderer#getDeathMaxRotation` 在死亡时返回 0，取消 GeckoLib/原版整体侧翻；`getPackedOverlay` 仅在 `isDead()` 或 `deathTime > 0` 时返回无 overlay，取消死亡红层。活着且 `hurtTime > 0` 时仍调用父类实现，因此普通受击红闪保留。不得用全局 Mixin 改变其他实体。

The Witness 的 Sanity 驱动生成与私人绑定 v1 已实现：

- `WitnessSanityPolicy` 是 managed Witness 规则的唯一集中来源。`SanityStage` 继续提供 20/60 边界；Fractured 内部分界只定义一次为 `FRACTURED_GLIMPSE_MINIMUM = 40`。精确映射为：60–100 `NONE`，40–59 `GLIMPSE`，20–39 `HUNT`，0–19 `ASCENDED`。
- 服务器每 20 ticks 评估一次在线、存活、非 creative、非 spectator 玩家。每名玩家最多 1 只 managed Witness；内存索引按 owner UUID 保存当前实体引用与冷却，不扫描全部维度或全部实体。
- `GLIMPSE` 在 16–26 格生成，存续 100–180 ticks（5–9 秒），玩家靠近到 8 格、模式变化或超时即无掉落清理；冷却 600–1200 ticks（30–60 秒）。它不持有攻击目标、不造成伤害，轻微游走，静止时凝视 owner 并播放 observe。
- `HUNT` 在 12–22 格生成，保持 30 生命、5 攻击、0.28 基础移速，持续追踪且只攻击 owner；被杀或丢失后冷却 300–600 ticks（15–30 秒）。
- `ASCENDED` 在 8–16 格生成，冷却 200–400 ticks（10–20 秒），只追踪 owner；使用固定 UUID 的 transient 移速 `MULTIPLY_TOTAL +0.15` 表现更主动，离开该模式时只移除该 modifier，不重复叠加。
- 每次到期生成最多尝试 12 个水平位置；每个候选只在玩家 Y 上下 6 格寻找落脚点。候选区块必须已加载，空间必须无碰撞、有可站立完整上表面、不含液体或火、位于世界边界内；不强制加载区块。失败后等待 100 ticks 再尝试。
- managed 状态、owner UUID、模式和 GLIMPSE 到期世界时间同时写入 Witness 自身 DataTracker 与 NBT；不写玩家 NBT。`thecost_reveal_played` 保持兼容。实体设为 persistent，但超过 owner 64 格、owner 不存在/死亡、不同维度、资格或模式不匹配时由管理器清理，不持有区块加载票。
- 玩家加入、退出、死亡、重生、跨维度、切换 creative/spectator、恢复 Lucid 和服务器停止均清理或重置对应索引。实体加载事件校正重复和孤儿；实体卸载事件只更新对应 owner 索引并安排冷却，不周期扫描世界。普通 `/summon thecost:witness` 创建的 unbound 实体不受管理器清理。
- managed Witness 的目标设置和 `tryAttack` 都只允许 owner；GLIMPSE 永不攻击。伤害入口只接受 owner（含 owner 发射物）和原版 `generic_kill`，其他玩家、宠物、环境或生物伤害被拒绝。managed 实体不掉战利品与经验；阶段/生命周期清理使用 `discard`，不伪造死亡。
- managed Witness 与非 owner 实体的 `collidesWith`/`pushAwayFrom` 返回隔离结果，并拒绝非 owner 交互；owner 仍可碰撞、攻击和正常击杀。实体跟踪包仍可能发给其他客户端，这是 v1 的明确限制，但不为 managed 实体增加全局 Mixin。
- 客户端 renderer 仅在本地玩家 UUID 等于 owner、`ClientSanityState` 已同步且阶段不是 Lucid 时渲染 managed Witness；否则模型、阴影和名牌均不进入渲染。unbound 调试实体继续对所有玩家可见。
- managed 实体覆写 `playSound`，不调用附近广播；服务器使用 `PlaySoundS2CPacket` 按 Witness 世界坐标只发送给同维度 owner，seed 使用实体随机源。环境、受击、死亡及其他实体声音不会传给看不见它的玩家；unbound 实体继续调用原版广播。
- 管理员调试命令 `/thecost sanity witness` 返回期望模式、活动模式、实体 UUID、owner 距离、剩余冷却和参与资格。

统一人工测试追加：59/40 为 GLIMPSE，39/20 为 HUNT，19/0 为 ASCENDED，60+ 为 NONE；反复写入相同 Sanity 不重复生成。多人分别验证 owner-only 模型、名牌、声音、目标、伤害和碰撞；验证 owner 正常击杀与 `/kill` 均播放 death 且无掉落经验；验证靠近 GLIMPSE、超时、恢复 Lucid、死亡、退出、重生、跨维度、模式切换、实体丢失和服务器停止后的清理与冷却。

玩家肢体损伤系统 v1 的数据层、同步与基础惩罚已实现：

- 权威槽位固定为 `LEFT_ARM`、`RIGHT_ARM`、`LEGS`，其中 `LEGS` 代表双腿整体。状态类型固定支持 `INTACT`、`BROKEN`、`GRAFTED`；v1 只正式使用前两者，`GRAFTED` 与可选 replacement `Identifier` 仅作为后续嫁接系统的数据/API 结构，不附带能力。
- `PlayerEntity` DataTracker 使用一个 byte 的紧凑 flags，每槽占 2 bits（左臂 bits 0–1、右臂 bits 2–3、双腿 bits 4–5）。服务端是唯一写入权威；本地及附近客户端自动收到 flags，可直接通过只读 `LimbData.getState(PlayerEntity, LimbSlot)` 查询，为下一轮 PlayerModel 表现提供数据。
- 玩家 NBT 根键为 `thecost_limbs`，保存三槽状态、每槽可选 replacement 字符串和全局 `arm_break_count`。缺失槽、非法状态、非法 replacement 或没有合法 replacement 的 `GRAFTED` 均安全回退 `INTACT`；语法合法但当前未知的 replacement ID 原样保存，不查询不存在的注册表对象。
- 肢体数据在退出重进、死亡/重生和跨维度后保留；断肢绝不计时、自然恢复或因普通伤害恢复。所有修改统一经过 `LimbData.breakLimb`、`restore`、`restoreAll`、`setReplacement`，幂等写入后统一校正惩罚；不得在其他 Mixin 直接改 NBT。
- `LimbBreakService.selectAndBreakNextArm` 集中保存未来 Deerclops 调用的顺序：`armBreakCount=0` 必选左臂；`=1` 时右臂 70%/左臂 30%；`>=2` 必选右臂。只有显式 `next_arm` 选择事件增加计数，普通定向调试 break 和查询不增加；计数不因 restore 回退。
- 只有存活的生存/冒险玩家承受惩罚；creative/spectator 保留状态但临时解除全部限制，切回后立即幂等校正。加入、重生、跨维度、模式切换、旧档读取和每次统一写入都会校正，属性清理只按 The Cost 固定 UUID 操作。
- `BROKEN LEGS` 添加固定 UUID transient modifier：移动速度 `MULTIPLY_TOTAL -0.40`（保留原版 60%）、攻击伤害 `MULTIPLY_TOTAL -0.50`；允许原版跳跃，但继续阻止 climbing，并使用下文的贴地姿态与腿部模型隐藏。校正会按同一 UUID 检查数值并替换旧的 `-0.75` modifier，特殊原版姿态仍优先。只有 `BROKEN` 触发这些效果，`INTACT/GRAFTED` 不受影响。
- 用户最新确认的断臂规则覆盖旧的“绝对空手/绝对禁用”：`BROKEN RIGHT_ARM` 对应主手，`BROKEN LEFT_ARM` 对应副手；断臂玩家可以正常持有、切换、拾取和在 GUI 中放置物品，`thecost:requires_two_hands` 标签保留但当前不参与惩罚。
- 只有主动使用对应手时，由服务端 `LimbGripService` 使用唯一常量 `DROP_CHANCE=0.10` 判定一次：主手覆盖近战攻击、`START_DESTROY_BLOCK`、物品/方块/实体交互，副手覆盖物品/方块/实体交互。空手、单纯持有、滚轮、交换键、GUI、拾取、走路和站立不判定；creative/spectator 不判定；双臂断裂也只按本次实际使用的手判定一次。
- 90% 成功时完全沿用原版动作；10% 失败时先取消动作并 `stopUsingItem`，从准确手槽移除完整 ItemStack、同步物品栏，再通过原版 `dropItem` 路径甩出。掉落保留数量、耐久、附魔和自定义 NBT；若实体未真正进入当前 `ServerWorld`，恢复同一 ItemStack 并重新同步，但该次动作仍取消。提示键为 `message.thecost.limb.grip_failed`，沿用每玩家 40 ticks 节流。
- 断臂不再绝对禁止近战、挖掘、副手交互或双手物品，也没有服务器每 tick 清空手槽的路径。不阻止走路、聊天、开门、背包或未来 Grafting Altar 交互；“拳击伤害固定为 1”仍未实现。
- 被阻止操作使用 `message.thecost.limb.*` actionbar 翻译键，提供 `en_us`、`zh_cn`。普通伤害、摔落与 Witness 不调用断肢 API；未来仅允许 Deerclops 明确招式调用统一选择服务。
- 调试命令：`/thecost limb get [target]`；权限等级 2 的 `/thecost limb break <left_arm|right_arm|legs> [target]`、`restore <left_arm|right_arm|legs|all> [target]`。另提供显式测试命令 `break next_arm [target]`，它会真实执行集中选择并增加一次 `armBreakCount`，不存在“查询导致计数增加”的歧义。输出包含三槽状态、计数和当前实际惩罚资格。
- Grafting Altar 必须依赖本数据层和统一 API，不得先做脱离真实肢体状态的空壳祭坛。Boss 肢体、能力、正式 HUD、自然恢复、血液或断面美术均未实现。

统一人工测试追加：断腿移动速度只降至原版 60%、攻击伤害减半、允许跳跃但不能攀爬，restore 立即解除其余惩罚；断臂只持物、滚轮、交换键、GUI 和拾取均不触发判定，主动攻击、开始挖掘、使用或交互时按实际手最多判定一次 10%。失败取消动作并完整掉物，成功完全走原版；双断臂不会对同一动作重复判定。验证物品数量、耐久、附魔/NBT 不变且不复制、不删除，并覆盖死亡重生、重进、跨维度和 creative/spectator 往返、附近客户端 flags，以及 `next_arm` 顺序。

玩家肢体模型与姿态表现 v1 已实现：

- common `PlayerEntityLimbMixin` 在 `PlayerEntity.updatePose` 完成原版选择后进行局部校正：只有存活、生存/冒险且 `LEGS=BROKEN` 的玩家，在原版没有选中更高优先级姿态时改为 `EntityPose.SWIMMING`。这同时取得原版低碰撞箱、低视角和陆地游泳/贴地动作，不使用每 tick 无条件 `setPose`，也不修改全局 `LivingEntity`。
- 原版特殊姿态优先：真实 `SWIMMING` 保持原样；`SLEEPING`、`FALL_FLYING`、`SPIN_ATTACK`、`DYING`、骑乘/载具，以及 `isSleeping`、`isFallFlying`、`isUsingRiptide` 均不会被强制覆盖。creative/spectator 不强制贴地；restore 或模式切换后，下一次原版 `updatePose` 自然恢复合适姿态。
- Minecraft 1.20.1 的 `LivingEntity.updateLeaningPitch` 会在 `isInSwimmingPose()` 时每 tick 以 0.09 向 1.0 插值；强制姿态会进入同一原版路径，因此第三人称可得到完整游泳动画，无需额外客户端写死 `leaningPitch`。断腿允许原版跳跃，已有攀爬限制继续生效。
- client `PlayerEntityRendererLimbMixin` 在 `PlayerEntityRenderer.render` 调用原版 `setModelPose` 之后，按该具体玩家 DataTracker 状态隐藏 `leftArm + leftSleeve` 或 `rightArm + rightSleeve`；`LEGS=BROKEN` 同时隐藏 `leftLeg + rightLeg + leftPants + rightPants`。只有 `BROKEN` 隐藏，`INTACT/GRAFTED` 保持原样。Classic/Steve 与 Slim/Alex 共用这些明确字段，因此同时覆盖且不会反向。
- renderer 在每次渲染前保存手臂、袖层、双腿和裤腿共八个 ModelPart 的原始 `visible`，渲染返回后逐项精确恢复；原版 `setModelPose` 也会为每个实体重建皮肤部件可见性。模型实例复用时不会把断肢状态泄漏到下一名健康玩家，帽子与外套不受修改。
- client `ArmorFeatureRendererLimbMixin` 在每件盔甲调用 `setVisible` 后隐藏对应 `BipedEntityModel.leftArm/rightArm`；`LEGS=BROKEN` 隐藏 `leftLeg/rightLeg`，因此护腿和靴子都不会悬空。传入模型是当前装备槽实际选择的 inner 或 outer armor model，两层均覆盖；渲染返回后精确恢复手臂和腿部原值，非玩家实体与 body 不受影响。
- 第一人称手臂本轮明确不处理：1.20.1 的 HeldItemRenderer 将手臂、持有物、双手地图与望远镜组合在不同路径中，宽泛取消会导致物品漂浮或破坏特殊渲染。待后续有独立手臂/物品替代表现设计时再做局部覆盖。
- client `HeldItemFeatureRendererLimbMixin` 仅接管第三人称中由 `BROKEN` 手臂实际渲染的 ItemStack：以当前 `BipedEntityModel.head` 的 yaw/pitch 为基准，将物品放到脸前嘴边；左右手使用相反的 0.10 格横向偏移，双臂断裂且两手持物时不会完全重叠。健康和 `GRAFTED` 手完全走原版，矩阵使用严格 `push/pop`，不影响下一物品、实体或 feature。第一人称仍保持原版，这是为避免破坏地图、望远镜和双手特殊渲染的明确取舍。
- 所有 `net.minecraft.client` 引用只存在 client source set；common 姿态代码只依赖实体 API。restore、重生、跨维度和客户端重新跟踪玩家后均继续使用现有 DataTracker flags，无需重新连接。

统一人工测试追加：第三人称验证左/右 arm、sleeve、内外盔甲对应方向；断腿验证 left/right leg、pants、护腿和靴子全部不可见。连续渲染断肢与健康玩家确认无可见性泄漏，并覆盖 Steve/Alex。断腿仍在陆地进入低碰撞箱、低视角和完整游泳动作；真实游泳、睡眠、鞘翅、旋转攻击、载具、死亡、creative/spectator 不被覆盖，restore 后所有模型与姿态恢复。第一人称仍显示原版手臂是当前已知限制。

Grafting Altar 功能基础版已实现：

- 注册单方块 `thecost:grafting_altar`、对应 BlockItem、BlockEntity 与 ScreenHandler。方块使用朝向状态、分层轮廓碰撞箱和持久化两格库存；破坏时通过原版 `ItemScatterer` 安全返还材料。生存配方为：第一行骨块/紫水晶碎片/骨块，第二行金锭/哭泣的黑曜石/金锭，第三行三个深板岩瓦，输出 1 个祭坛；开发测试仍可使用 `/give @s thecost:grafting_altar`。
- 祭坛只有 `SanityStage.FRACTURED` 玩家能够打开；Lucid/Ascended 在服务端被拒绝。确认按钮再次在服务端校验玩家仍可使用该方块、仍为 Fractured、选中槽仍为 `BROKEN` 且材料仍有效，客户端不能直接修改肢体数据。
- 两个材料入口均为数据包可扩展的物品标签：`thecost:grafting_components` 默认只含 `thecost:frozen_sinew`，`thecost:grafting_catalysts` 默认只含 `thecost:deerclops_eye`，且 `replace=false` 允许数据包扩展。每次成功嫁接消耗 1 个 Frozen Sinew 与 1 个 Deerclops Eye；BlockEntity 库存写入 NBT，退出界面或区块卸载不会丢失。
- UI 读取现有玩家 DataTracker 肢体镜像显示左臂、右臂、双腿的 `INTACT/BROKEN/GRAFTED` 状态；选中状态由每个 ScreenHandler 独立同步。界面使用骨白、冷蓝、暗金的程序化占位绘制，不依赖外部图片，也未实现已延期的 Runic Circle 绘图 UI。
- 成功时只通过统一 `LimbData.setReplacement` 将目标从 `BROKEN` 写为 `GRAFTED`，replacement ID 集中使用 `thecost:basic_left_arm`、`thecost:basic_right_arm` 或 `thecost:basic_legs`。基础嫁接当前没有额外能力；因为现有 renderer 只隐藏 `BROKEN`，成功后会恢复原版玩家部件，而非显示正式义肢模型。自定义嫁接部件模型与能力是后续工作。
- 静态祭坛占位模型位于标准 blockstates/models 目录，目前以原版骨块、深板岩、金块和浮冰纹理组合验证模型与朝向接线；正式 Blockbench 模型和项目自有贴图后续可直接替换资源，不改变 BlockEntity 或 UI 协议。

统一人工测试追加：在 Fractured 下放置并打开祭坛，验证铁锭/紫水晶标签槽、shift-click、库存持久化和破坏返还；分别对三种 `BROKEN` 槽嫁接，确认各消耗 1 个材料、状态变为 `GRAFTED`、replacement ID 保留至重进/重生/跨维度且断肢惩罚解除。验证 Lucid/Ascended 无法打开，远离或破坏祭坛后旧界面按钮不能执行，以及正常/已嫁接槽不能重复消耗材料。

夏/冬全局季节日历 v1 已实现：

- 世界只采用 `SUMMER` 与 `WINTER` 两季。长度是集中且暂定的 v1 常量：`SUMMER_LENGTH_DAYS=20`、`WINTER_LENGTH_DAYS=15`，完整循环 35 天；未来调整只修改 `SeasonCalendarConfig`，其他模块不得重复硬编码。
- Overworld 的 `PersistentStateManager` 是唯一权威保存位置，状态 ID 为 `thecost_season_calendar`。NBT 保存 `current_season`、从 1 开始的 `day_in_season`、只增不减的 `total_elapsed_days` 和 `last_observed_day_index`；任何维度的查询都通过服务器回到 Overworld 同一状态。缺失字段、未知季节、越界日数或负累计值会安全回退 `SUMMER day 1`。
- 服务端每 20 ticks 检查一次 Overworld `timeOfDay / 24000` 的日序号，但只在首次建立锚点、向后重设锚点或实际推进时标记 dirty，不会每 tick 写盘。服务器持续运行且没有在线玩家时仍推进，广播列表为空不会改变权威状态。
- 时间向前跨过 N 个日界（包括 `/time add` 跳过多天）会一次推进 N 天；重启使用已保存锚点，不重复处理旧日界。`/time set` 向后时不倒退季节、不减少累计天数、也不把负差值当推进，而是把当前日序号作为新锚点；此后从新时间线再次跨越日界才正常推进。这是 v1 明确的时间回拨语义。
- `SeasonSnapshot` 集中提供季节长度、`daysRemaining`、`isWinter` 和 `isFinalWinterDay`；`SUMMER day 20` 的下一天为 `WINTER day 1`，`WINTER day 15` 的下一天为 `SUMMER day 1`。未来冬季雾雪、温度与 Deerclops 必须使用这些查询；本轮不生成 Deerclops，冬季最后一天只提供可靠判定。
- 同步频道为 `thecost:season_sync`。加入、重生、跨维度、自然日推进和管理员修改时同步；客户端 `ClientSeasonState` 只保存已校验的展示镜像，在连接初始化和断线时重置，没有客户端修改服务端日历的数据包。
- 命令：`/thecost season get`；权限等级 2 的 `/thecost season set <summer|winter> [day]` 与 `/thecost season advance [days]`。省略 set 的 day 时为 1，省略 advance 的 days 时为 1，单次管理员 advance 限制为 1–10000；输出季节、季内日/长度、剩余天数、累计天数及是否冬季最后一天，并提供 `en_us`、`zh_cn`。
- v0.1 当前只为 WINTER 添加玩法效果；SUMMER 目前仅是日历状态。本日历模块本身不改变天气或方块；服务端冬季一层地表积雪、季节 HUD、冬季雾、视觉飘雪、体温系统与 Deerclops 已由下文独立模块提供。结冰、强制天气、野火、Dragonfly 与其他夏季玩法仍未实现。

统一人工测试追加：新世界默认为 Summer 1/20；`season set summer 20` 后 `season advance` 进入 Winter 1/15；`season set winter 15` 后推进进入 Summer 1/20；`advance 35` 回到同一季节日且累计增加 35。验证 `/time add 72000` 一次推进 3 天，重启不重复推进，`/time set 0` 不倒退/负推进且之后跨越新日界只加 1；在下界/末地查询与主世界一致。验证非 OP 只能 get，客户端重连/重生/跨维度获得最新镜像。

冬季氛围 v1 已实现：

- 全部实现位于 client source set，只读取服务器同步后的 `ClientSeasonState`。仅在客户端已有世界和玩家、季节镜像有效、季节为 `WINTER` 且当前维度为 `minecraft:overworld` 时产生效果；未同步、断线、无世界、SUMMER 或其他维度均不修改世界、不发包，也不改变服务端天气。
- `BackgroundRendererWinterMixin` 在 `BackgroundRenderer.render` 返回后，把原版空气雾颜色最多以 35% 权重混合至冷灰蓝目标 RGB `(0.58, 0.68, 0.76)`；不是纯色覆盖。它在 `BackgroundRenderer.applyFog` 返回后读取原版已经决定的 fog start/end，只在原版 end 比冬季目标更远时收紧：目标 end 为 `max(24, viewDistance × 0.55)`，目标 start 为不晚于原版 start 的 `max(4, targetEnd × 0.22)`。因此水下、熔岩、细雪、浓雾、失明、黑暗以及任何原版更严格的可见距离均优先，不会被冬季逻辑放宽。
- 冬季强度按 `System.nanoTime` 的真实帧间时间以 2 秒完成 0↔1 线性趋近，单帧时间最多按 0.25 秒计入，避免切后台后瞬间跳变。连接初始化和断线立即清零；季节切换时平滑过渡，跨维度时非主世界不应用效果且内部强度向零过渡。该逻辑不创建、替换、复用或关闭 `PostEffectProcessor`，与 Ascended 世界后处理独立，HUD/GUI 不受雾处理。
- 视觉雪完全由客户端 `ClientTickEvents.END_CLIENT_TICK` 生成原版 `SNOWFLAKE` 粒子。每 tick 最多尝试 3 个候选、每候选 35% 概率、硬上限 2 粒；只检查玩家 8–24 格环带、头顶 8–14 格、客户端已加载区块。玩家头部和候选点都必须可见天空，候选必须为空气且无流体，并用 `MOTION_BLOCKING` 高度图把生成点放到露天地表上方，室内、地下或屋顶遮挡时不生成。
- 雪花水平漂移上限约 `0.008` 方块/tick，附加很小随机扰动，下降速度为 `-0.015`；creative/spectator 同样可见。该视觉模块本身没有积雪层、结冰、碰撞、温度判定、伤害、季节声音、天气强制或网络同步开销；体温由下文独立的服务端模块处理。

统一人工测试追加：在主世界露天执行 `/thecost season set winter 1`，确认雾色和距离约 2 秒渐入、雪花稀疏分布且不会集中刷在眼前；执行 `/thecost season set summer 1` 确认约 2 秒渐出。分别进入有屋顶室内/地下、水下、熔岩、细雪、失明或黑暗状态，确认雪不穿屋顶且原版特殊雾优先；切换下界/末地、重生、断线重连并覆盖窗口和 GUI Scale 变化，确认无残留、崩溃或 HUD/Ascended 后处理污染。

冬季体温系统 v1 已按用户指定参考《饥荒》原版（Vanilla Don't Starve，不是 DST/RoG）实现：

- 体温范围固定为 `-20～40`，默认/安全体温为 `35`，服务端 `TemperatureData` 是唯一权威。NBT 键为 `thecost_body_temperature`；非法或缺失值回退 35。退出重进保留；死亡重生按原版新生命语义重置 35；跨维度同步当前值。同步频道为 `thecost:temperature_sync`，客户端只保存只读镜像；下文季节＋体温 HUD 只读取该镜像。
- 每 20 ticks（1 秒）评估存活的生存/冒险玩家；creative/spectator 不受温度惩罚且校正到 35。只有主世界 `WINTER` 计算寒冷与冻伤；其他季节或维度以安全环境 35 让体温恢复，不实现夏季过热。
- 冬季环境温度使用集中曲线：季初/季末基础 5，季中以正弦曲线最多下压 25；Minecraft 昼夜再叠加 `±5`，正午最暖、午夜最冷，最终 clamp 到 `-30～10`。典型季中约为正午 -15、午夜 -25，接近原版冬季约 -20 且夜晚更冷的节奏。
- 降温严格使用原版公式 `30 / (30 + totalInsulation)` 度/秒；无保暖为 1°/秒，60/120/240/480 insulation 分别约为每 3/5/9/17 秒下降 1°。升温上限通常为 1°/秒，体温低于 0 时寻找温暖的解冻上限为 5°/秒。
- 保暖采用数据包可扩展的 `thecost:winter_insulation_60|120|240` 物品标签并按已装备部位相加。Minecraft v1 映射为皮革靴/裤各 60、皮革帽 120、皮革胸甲 240，全套合计 480；铁、锁链、金、钻石、下界合金等普通战斗盔甲默认 0，遵循《饥荒》“盔甲不等于保暖衣物”的取舍。重叠标签按最高档而不是重复叠加。
- 6 格球形范围的热源分为 `temperature_heat_sources_40|70|100` 三个可扩展方块标签。普通火把/灯笼/点燃蜡烛为 40，燃烧中的熔炉/高炉/烟熏炉为 70，火焰/点燃篝火/熔岩/岩浆块为 100；带 `LIT` 的方块只有 `true` 生效，灵魂火系列不在默认标签。热源目标温度按距离线性衰减，多个来源取实际目标最高者，升温仍受 1°/秒或解冻 5°/秒上限。
- 体温降至 5 时显示一次可翻译预警；低于 0 时显示冻结提示，并使用原版 freezing damage source 每秒造成 1.25 生命伤害；升回 0 及以上立即停止。消息提供 `en_us` 与 `zh_cn`，不会每 tick 刷屏。
- 命令：`/thecost temperature get`；权限等级 2 的 `/thecost temperature set <-20..40>` 与 `/thecost temperature environment`。环境调试输出体温、环境/目标温度、实际变化率、总保暖、最强热源及距离、冬季资格和冻结状态。
- 当前明确未实现《饥荒》的暖石、热食、衣物耐久/缝纫、胡须保暖、潮湿修正与过热；这些不是当前“范围、变化速率、护甲保暖”基线的一部分，后续如需要必须单独授权。温度 HUD 已由下文 v1 实现。

统一人工测试追加：冬季主世界用 `temperature environment` 核对季初/季中、正午/午夜环境温度；无装备约每秒降 1°，分别穿戴皮革部件核对 60/120/240/480 公式速率。点燃/熄灭篝火和熔炉、普通/灵魂灯笼、火把、蜡烛、熔岩分别核对热源档位；5°出现一次预警，低于 0 每秒 1.25 冻伤，回到 0 停止。确认夏季、非主世界、creative/spectator 无冻伤，重进保留、死亡回 35、跨维度与客户端镜像同步。

季节＋体温 HUD v1 已实现：

- 独立 client-only `SeasonTemperatureHudRenderer` 通过 Fabric `HudRenderCallback` 注册，只读取 `ClientSeasonState` 与 `ClientTemperatureState`，不发送网络包、不修改服务端权威数据，也不改动已人工验收的 `SanityHudRenderer`。
- 左上布局使用缩放后的 GUI 坐标和 8px 安全边距：Sanity 保持 `y=8`；季节固定 `y=30`；体温固定 `y=50`。每行使用 16×16 图标，文字从 `x=28`、图标顶部下移 4px 开始；两个新增槽位始终固定，某个同步包晚到不会使另一行上下跳动，也不使用原始 framebuffer 尺寸。
- 总门控要求 `client.player`、`client.world` 均存在且 `hudHidden=false`。季节与体温随后分别检查各自 `isSynced()`：只同步一种时只显示对应固定行；未同步、连接初始化、断线、切换世界或 F1 时不会显示默认 `Summer 1` 或 `35°C` 假值，也不会因另一镜像无效而阻止已同步行。
- 季节文本使用 `hud.thecost.season`，其中季节名称继续复用 `Season.getTranslationKey()`；英文格式为 `Winter · Day 8/15`，中文为 `冬季 · 第 8/15 天`。体温通过 `Locale.ROOT` 固定格式化一位小数并使用 `hud.thecost.temperature` 追加 `°C`，例如 `-3.5°C`；Java 不硬编码中英文显示文本。
- 季节文字颜色：SUMMER 暗金 `#C7A85B`、WINTER 冷蓝 `#9FC9E8`。体温颜色集中分级：`<=0` 冰蓝高警戒 `#69D4EF`；`0–5` 浅蓝 `#9BC7E5`；`5–15` 冷灰蓝 `#AEBDCA`；`15–37` 骨白 `#F2F0DF`；`>37` 暗金 `#C7A85B`。颜色不闪烁、不触发全屏效果；温度计内部使用同一温区色动态填充。
- 三个原创 16×16 RGBA 透明硬边像素图标位于 `assets/thecost/textures/gui/status/`：暗金太阳 `season_summer.png`、冷蓝雪花 `season_winter.png`、骨白轮廓温度计 `thermometer.png`。标准库确定性生成器为 `art/hud/generate_season_temperature_icons.py`；资源不引用外部素材。静态验证脚本 `art/validation/validate_season_temperature_hud.py` 检查尺寸、RGBA、透明背景、有限调色板、renderer 注册、独立同步/F1 分支、双语语言键及可选 JAR 内容。

统一人工测试追加：用 `/thecost season set summer 1`、`season set winter 8` 和 `/thecost temperature set -3.5|3|10|20|38` 核对季节格式、三张图标、五档温度颜色及一位小数；覆盖只收到季节/只收到体温/两者均未同步、断线重连、死亡重生、跨维度、F1、不同窗口尺寸与 GUI Scale。确认三行固定在 `y=8/30/50`，夏季安全体温仍显示，BossBar、聊天和 Sanity HUD 不被改变或遮挡。

冬季地表积雪 v1 已实现：

- `WinterTerrainManager` 是服务端唯一执行者，只读取 `SeasonCalendarManager` 的全局季节；仅在真实 Overworld、`WINTER`、存活的生存/冒险玩家附近工作。creative、spectator、其他维度与没有合格在线玩家时不铺雪；客户端不能请求或决定世界方块变化。
- 冬季每 40 ticks（2 秒）评估一次。候选半径为玩家周围 4–24 格，每玩家每轮最多 8 次随机候选与 2 次成功铺雪；全服务器单轮硬上限为 64 次候选、8 次成功。多人重叠列使用本轮 packed-position 集合去重，玩家起始索引轮转，避免固定在线顺序长期独占预算；不进行区域全扫描。
- 所有候选先检查区块已加载，再读取 `MOTION_BLOCKING_NO_LEAVES` 高度图；绝不强加载区块。只允许在空气、无流体、可见天空、世界边界内且 `SnowBlock.canPlaceAt` 成立的位置放置恰好一层原版雪。承载面必须属于可由数据包扩展的 `thecost:snow_coverable_surfaces`、无流体、无 BlockEntity，并具有完整坚实的上表面。
- 默认承载标签仅含 grass block、dirt、coarse dirt、podzol，以及完整静态的 stone/granite/diorite/andesite/deepslate/tuff；不含耕地、土径、作物、植物、树苗、红石、容器/工作方块、门、栅栏、楼梯、台阶、光源、火、液体或任何方块实体。既有雪层与任何非空气方块都不会被覆盖；本模块不冻结水、不改变天气、不破坏或替换地表方块。
- Overworld `PersistentState` ID 为 `thecost_winter_terrain`。NBT 用 packed `long[] tracked_positions` 保存精确坐标，并保存固定期望状态 `minecraft:snow`、`expected_layers=1`；内存与持久化均去重，硬上限 16,384 条。只有本模组成功放置后才写入记录，因而能在重启后继续区分模组雪和原版/玩家/其他模组雪。
- 冬季每轮额外轮转检查最多 32 条追踪记录；夏季每轮最多处理 16 条，形成渐进融雪。未加载区块始终保留记录且不读取方块；当前位置仍恰好是一层雪时，夏季才以无掉落的方块更新移除并取消追踪。若玩家或外部机制已将其改为空气、加厚为多层雪或替换为任意其他方块，只删除追踪记录，绝不修改/还原该位置；从未追踪的雪永远不会被本模块融化。
- `/thecost season terrain status` 显示季节、合格玩家、追踪数/容量以及上一轮候选、铺雪、融雪、失效记录和未加载跳过统计。权限等级 2 的 `/thecost season terrain cleanup` 只清理当前已加载区块内仍匹配“一层雪”的追踪位置；外部修改只解除追踪，未加载记录安全保留，未追踪雪不受影响。
- `art/validation/validate_winter_terrain.py` 提供静态与单元风格安全矩阵，覆盖追踪雪专属融化、外部加厚/替换不触碰、未加载跳过、非主世界守卫、夏季渐融、容量/多人候选去重、表面过滤、命令权限和双语资源。当前模块不修改客户端视觉飘雪、冬季雾、体温算法、HUD、Boss 或季节长度。

统一人工测试追加：用 `/thecost season set winter 1` 后在主世界生存模式露天移动，观察雪层逐步出现在标签允许的完整地表；确认室内、地下、未加载区块、农田、土径、作物、箱子/熔炉、楼梯/台阶、围栏、液体和现有雪不被覆盖。用 `season terrain status` 核对每轮预算及追踪增长；手工移除一处追踪雪、把另一处加厚后切到 Summer，确认外部修改只解除记录且多层雪不被删，其他追踪的一层雪以每 2 秒最多 16 处渐融。重启世界确认剩余追踪继续有效；在下界/末地及 creative/spectator 验证不铺雪；用管理员 `season terrain cleanup` 验证只删除已加载的追踪一层雪。

冬季可逆水面结冰 v1 已实现：

- 结冰与积雪共享 `WinterTerrainManager` 的服务器端 40-tick 调度和合格玩家列表，但使用独立候选集合、预算和 `WinterIceState`；只在全局季节为 `WINTER`、真实 Overworld、存活生存/冒险玩家附近的已加载区块运行。creative、spectator、其他维度和无合格玩家时不冻结水，也不强加载区块或扫描整个区块/世界。
- 冰候选半径沿用 4–24 格。每玩家每轮最多 6 次随机候选、2 次成功冻结；全服每轮最多 48 次候选、6 次成功。多人重叠水柱用本轮 packed column 集合去重，在线玩家起始索引轮转。冬季每轮最多惰性检查 24 条冰记录，夏季每轮最多解冻 12 处；这些预算与既有积雪 8/64、融雪 16 的预算完全独立，未降低或扩大积雪处理量。
- 水面定位使用已加载区块的 `MOTION_BLOCKING` 高度图。候选方块必须严格是 `minecraft:water`，block `level=0`，FluidState 为 still water；因此流水、waterlogged 方块、bubble column、kelp、seagrass、植物、其他流体及任何方块实体均不会被冻结。水面上方还必须为空气、无流体且可见天空，地下或有屋顶水池不会冻结；不检查寒冷生物群系，The Cost 冬季对主世界露天水面普遍生效。
- 成功转换目标固定为 `minecraft:frosted_ice`，不使用普通冰；冻结前以略扩展的单方块 Box 拒绝所有相交实体，避免直接冻结玩家、生物、船或漂浮实体所在水面。转换只使用普通 block update flags，不播放额外声音/粒子、不掉落物品、不改变天气。`frosted_ice` 明确不加入 `thecost:snow_coverable_surfaces`，冰面不会被本模块快速覆雪。
- 独立 Overworld `PersistentState` ID 为 `thecost_winter_ice`，用 packed `long[] tracked_positions` 保存坐标，同时写入期望状态 `minecraft:frosted_ice`、恢复状态 `minecraft:water` 和恢复 level 0；去重且硬上限 8,192。只有 `setBlockState` 成功后才追踪；追踪失败时仅在当前位置仍为 frosted ice 的前提下回滚为纯 source water，避免丢失原水源或覆盖并发外部改动。
- 夏季/非冬季仅在记录已加载且当前位置仍是 `frosted_ice` 时恢复为纯 source water 并解除追踪。若原版 FrostedIceBlock 已自然融为水，或玩家/其他模组将其打掉、替换为其他方块，只解除追踪，绝不再次写水覆盖外部结果。冬季同样惰性清理这些失效记录；未加载区块保留记录并跳过，从未追踪的 Frost Walker 冰永远不会被解冻或 cleanup 修改。
- `/thecost season terrain status` 现同时显示 tracked snow/ice、各自容量、最近一轮雪候选/铺设/融化、冰候选/冻结/解冻，以及合并的失效记录和未加载跳过数。权限等级 2 的 `/thecost season terrain cleanup` 同时删除已加载且仍匹配的一层追踪雪，并把已加载且仍匹配的追踪 frosted ice 恢复为 source water；失效记录只解除追踪，未加载记录保留，未追踪雪/冰均不触碰。
- `art/validation/validate_winter_ice.py` 提供可重复纯策略和源码契约检查，覆盖 source water 唯一准入、flowing/waterlogged/bubble/地下/遮顶/实体占位拒绝、追踪去重与容量、原版自然融化及外部替换只 untrack、夏季只恢复追踪 frosted ice、未追踪 Frost Walker 冰不进入处理、未加载跳过、非主世界守卫、独立性能预算、命令与双语资源。

统一人工测试追加：冬季主世界在露天静止水池/河海附近生存移动，确认水面以最多每 2 秒 6 格的全服速度缓慢出现 frosted ice；流水、含水方块、气泡柱、海草/海带、室内与地下水池不冻，玩家、生物或船占据的水格不直接冻结。切到 Summer 确认每轮最多 12 处追踪冰恢复为静止水源；在切季前分别打掉、替换和等待一处 frosted ice 自然融化，确认它们只解除追踪而不被覆盖。放置未追踪的 Frost Walker 冰后执行 `season terrain cleanup`，确认其保持不变；重启、卸载区块、多人重叠与下界/末地分别验证持久化、跳过、去重及维度隔离，并用 `season terrain status` 核对 snow/ice 独立统计。

## 验证命令

```powershell
.\gradlew.bat build --stacktrace
```

## Deerclops v1 战斗闭环

Deerclops v1 已完成代码与资源接线，等待统一人工验收：

- 实体 ID 为 `thecost:deerclops`，基础属性集中在 `DeerclopsConfig`：250 生命、14 攻击基准、0.23 移速、48 追踪范围、8 护甲、0.80 击退抗性；压迫感 v3 的实体碰撞箱为 `2.4 × 5.8`，主动眼高为 `3.25`。碰撞与眼高只为匹配新视觉体型，未改攻击范围或战斗数值。
- 服务端显式状态机固定包含 `SPAWN`、`ROAR`、`CHASE`、`SWIPE_WINDUP`、`SWIPE_ATTACK`、`SLAM_WINDUP`、`SLAM_ATTACK`、`ICE_SPIKE_WINDUP`、`ICE_SPIKE_ATTACK`、`RECOVER`、`DEATH`。伤害只在服务端进入攻击窗口时结算；动画和客户端不能决定伤害。同一攻击用玩家 UUID 集合防止重复结算。
- Swipe 前摇 24 ticks、攻击窗口 8 ticks、恢复 30 ticks：5.5 格、前方 65° 半角扇形、14 点伤害。旧的 35% 随机断臂已删除；有效命中改为累计 1 层手臂创伤，累计 2 层时才通过 `LimbBreakService.selectAndBreakNextIntactArm` 打断手臂并清零。实际顺序继续使用全局 `armBreakCount` 规则；首选槽已 `BROKEN/GRAFTED` 时只回退到另一只 `INTACT` 手臂，绝不覆盖已有状态。没有可打断手臂时不累计。
- Slam 前摇 40 ticks、攻击窗口 10 ticks、恢复 40 ticks：7.5 格范围内始终正常结算 18 点生命伤害和击退，并播放原版爆裂声音、冷雾与雪花粒子。旧的 25% 随机断腿已删除；只有水平内圈 4 格且结算帧 `isOnGround()` 的玩家累计 1 层腿部创伤，累计 2 层才通过 `LimbData.breakLimb` 将仍为 `INTACT` 的 `LEGS` 打断并清零。跳起或正在跌落只避免腿部创伤，不能避免普通伤害与击退；`BROKEN/GRAFTED` 双腿不累计。
- 第三个独立技能 Ice Spike Wave 已实现。目标在 Swipe 5.5 格之外、16 格以内且独立冷却就绪时触发；前摇 32 ticks、逐排攻击阶段 24 ticks、恢复 30 ticks，冷却 140 ticks。开始施法时锁定 Boss 位置与水平方向，前摇和释放阶段停止导航且不重新追踪玩家；状态、状态 ticks、冷却、锁定原点/方向和本次已命中玩家 UUID 均随实体 NBT 保存，区块重载不会刷新方向、冷却或让已命中玩家再次结算。
- Ice Spike Wave 使用集中计划器生成完整 30° 扇形：`-15°/0°/+15°` 三条射线，距离 `3/4.5/6/7.5/9` 五排，共 15 点；每排间隔 4 ticks 从近到远爆发。前摇每 4 ticks 只在可用地表点显示少量雪花预警；每个点只在已加载区块内、锁定原点上下 `+3/-6` 格有限搜索有完整上表面、三格无碰撞且无流体/火的安全地面，失败即跳过，不强加载区块、不放置或破坏方块。
- 临时视觉实体 ID 为 `thecost:deerclops_ice_spike`：`SpawnGroup.MISC`、无碰撞、无推动、无重力、不保存、30 ticks 后删除。GeckoLib 资源为独立 6 骨骼/7 cube 原创低多边形冰晶，包含一根三段主锥和四枚不对称侧晶，64×64 RGBA 低色数冷蓝纹理；`animation.deerclops_ice_spike.erupt` 完成快速破土、震颤和下沉。可编辑工程与确定性生成器分别为 `art/blockbench/deerclops_ice_spike.bbmodel` 和 `build_deerclops_ice_spike_art.py`。
- 每次完整 Ice Spike Wave 对每名玩家最多成功结算一次。玩家必须位于某个实际成功生成冰刺的 1.10 格水平半径、脚部与该地表高度差不超过 0.65 格且结算帧 `isOnGround()`；侧移或离地可规避。成功命中造成 10 点生命伤害、0.80 水平击退与 0.35 向上速度、30 ticks Slowness I，并通过现有 `TemperatureData.applyColdHit`/环境温度下限入口降低 5°C。死亡、creative、spectator 玩家不参与。
- Ice Spike Wave 命中调用 `DeerclopsTraumaManager.recordLegTrauma`，与 Slam 共享同一个腿部创伤池：任意组合累计 2 层才打断仍为 `INTACT` 的 `LEGS`；继续遵守 160 ticks 全局创伤保护、1200 ticks 衰减、消息节流及 `BROKEN/GRAFTED` 不覆盖。跳离冰刺只显示可翻译节流提示，不增加创伤。第一排进入 `ICE_SPIKE_ATTACK` 时只播放一次原创 Slam 声层，最后一排只播放一次原创 Ice Impact，绝不按 15 根冰刺叠加声音。
- 正面有效举盾且面向 Deerclops 来源时，Swipe 仍调用原版 `damage`，因此盾牌伤害、耐久和禁用规则保持原版，但该次不增加手臂创伤；背对 Boss 举盾无效。服务端使用玩家朝向与指向 Boss 的水平向量进行 75° 半角判断，不由客户端声明反制成功。
- 任意 Deerclops 成功打断一处肢体后，该玩家获得 160 ticks 的创伤保护；保护期内仍正常受到 Deerclops 生命伤害与击退，但不累计手臂或腿部创伤。创伤和保护仅保存在服务器内存，不写玩家 NBT、不增加客户端写入口，并可安全共享管理员召唤的多只 Deerclops。
- 玩家死亡、断线、重生、跨维度、服务器停止或最后一只已加载 Deerclops 消失时清除临时创伤；连续 1200 ticks 没有新增 Deerclops 创伤也会自动衰减清除。举盾反制和跳跃规避提示不延长衰减计时。
- 血量阶段只改变招式选择，不改变前摇、攻击帧、恢复期或创伤层数：生命高于 60% 时只使用 Swipe；30%–60% 时每第 3 次近身攻击使用 Slam；低于 30% 时每第 2 次近身攻击使用 Slam。进入新的血量阶段会重置该阶段的攻击计数，使节奏可预测。
- 创伤第 1 层、真实断肢、正面盾牌反制和跳起规避均使用 `message.thecost.deerclops.*` 可翻译 actionbar，并提供 `en_us/zh_cn`；同一玩家消息有 40 ticks 节流。管理员诊断命令为 `/thecost deerclops trauma get` 与 `/thecost deerclops trauma clear`，沿用 Deerclops 命令树的权限等级 2。
- Deerclops 寒冰命中只处理存活的生存/冒险玩家，并通过现有 `TemperatureData.applyColdHit` 的 clamp/sync 写入口修改体温：Swipe 成功造成生命伤害后降低 3°C，Slam 成功造成生命伤害后降低 5°C。`TemperatureManager.getAmbientTemperature` 是唯一新增的环境温度只读入口；技能结果为 `min(当前体温, max(当前环境温度, 当前体温-降幅))`，不会加热玩家，也不会由本次命中把体温从环境温度以上直接打穿环境下限。
- Slam 水平 4 格内圈的成功命中额外给予 60 ticks 的 Slowness II（amplifier 1）；这不改变原有跳起规避腿部创伤的规则，玩家即使离地规避创伤，只要仍被 Slam 命中就会降温，内圈仍会缓慢。寒冷命中反馈使用雪花粒子和 `message.thecost.deerclops.cold_hit` actionbar，每玩家 40 ticks 节流；不新增 HUD 或后处理。
- Slam 结算保留原有 POOF/SNOWFLAKE 扩散，并增加 3 圈 × 8 点、共 24 个由内向外带水平速度的原版雪花粒子。每次 Slam 只额外产生 24 个受控服务器粒子事件，不生成持续粒子实体或每 tick 粒子。
- Deerclops 理智压迫由独立 `DeerclopsAuraManager` 在服务端每 20 ticks 评估。存活且未进入 `DEATH` 的 Deerclops 在 8 格内产生 `STRONG=-20 sanity/min`，8–16 格产生 `WEAK=-8 sanity/min`；多只 Boss 按玩家取最强档且绝不叠加。使用整数分子/1200 tick 分母累计，强档长期每 60 ticks -1，弱档长期平均每 150 ticks -1，最终只调用 `SanityData.add`，不读取或重置环境理智 tracker。
- 光环档位改变或离开 16 格会清空本光环自己的余数；玩家死亡、断线、重生、跨维度、creative/spectator、服务器停止或最后一只已加载 Deerclops 消失时清理。`/thecost deerclops aura get` 返回 `none/weak/strong`、距离下次掉点 ticks 和当前体温；`/thecost deerclops status` 同时返回 64 格内最近 Boss 的 UUID、状态、生命、教学/混合/绝境血量阶段、距离、执行者光环、下一次光环掉点和体温。
- 已注册可替换的独立声音 ID：`thecost:entity.deerclops.warning|roar|ambient|step|swipe|slam|ice_impact|hurt|death|body_fall`。10 类事件现已直接引用 20 个项目内程序合成的原创单声道 48 kHz OGG Vorbis（warning 2、roar 2、ambient 3、step 2、swipe 2、slam 2、ice impact 2、hurt 2、death 1、body fall 2），不再使用 Minecraft 原版 `type=event` 占位。确定性生成器位于 `art/audio/generate_deerclops_sounds.py`，固定随机种子并在生成后回读验证 OGG/VORBIS、声道、采样率、时长、峰值、DC 偏移和 `sounds.json` 引用；没有下载、复制、转换或近似复刻 Klei/Don't Starve 音频。`DeerclopsSoundController` 仍在服务器集中负责播放时的分层、随机音高和节流；ambient 间隔 180–320 ticks，移动 step 间隔 12 ticks，状态/伤害时序与声音逻辑不变，声音不决定伤害。
- 自然生成保持同一冬季最多一次、安全地面、失败退避和不强加载区块的约束，但成功候选流程前新增仅目标玩家可听的两阶段 warning：开始时一次、60 ticks 后增强一次、120 ticks（6 秒）后才实际寻找并生成。预警期间若玩家、季节、夜间、露天或持久状态失效则取消并按 200 ticks 退避；管理员 `/thecost deerclops summon` 不经过长预警，继续直接进入 SPAWN/ROAR。
- BossBar 使用蓝色十段样式，按实体追踪玩家增删并同步生命值。死亡状态保留 60 ticks，使 `animation.deerclops.death` 完整播放后才按原版 `KILLED` 原因移除。
- 自然生成管理器每 100 ticks 低频检查一次，只查询 `SeasonCalendarManager/SeasonSnapshot`。条件为主世界、冬季第 15 天、13000–23000 夜间、存在存活的生存/冒险露天玩家；在玩家视线后方 28–40 格、已加载区块内最多尝试 12 个露天安全地面，失败退避 200 ticks，不强加载区块。
- Overworld `PersistentState` ID 为 `thecost_deerclops_spawn`，保存自然 Boss UUID 和已处理冬季的起始累计日。每个冬季成功自然生成最多一次；死亡后清除活动 UUID 但保留“本冬季已处理”，因此死亡、重启或区块卸载不会在同一冬季重复生成。普通管理员调试召唤不占用自然生成名额。
- 管理员命令为 `/thecost deerclops summon`、`status`、`remove`。`remove` 清理当前已加载实例并清除活动 UUID；已卸载的旧自然实例再次加载时会因 UUID 不匹配而安全丢弃。
- Deerclops“压迫感 v3”正式模型已通过项目内确定性文件生成路径完成；当前上下文没有可调用的 Blockbench MCP，因此不依赖或声称实时 Blockbench 连接。`art/blockbench/build_deerclops_art.ps1` 是几何、低色数贴图与 bbmodel 的生成源；每次改造必须运行该脚本同步 `art/blockbench/deerclops.bbmodel` 与 GeckoLib 运行资源，禁止只手改导出 JSON。`art/validation/validate_deerclops_v3.py` 固化 32 骨骼/父子关系、9 动画、关键帧、调色板、包围盒、运行尺寸及可选 JAR 内容检查。
- 压迫感 v3 使用 67 个有目的的 cubes 和原有 32 个稳定骨骼；未新增、删除或改名 bone，parent 映射和 locator=0 均保持。未旋转几何 AABB 为约 `6.00 × 6.05 × 1.64` 格（横向含鹿角、高度含蹄到角尖），geo 可见边界为 `7 × 7`、Y 偏移 3.1；实体不使用 renderer scale 冒充变大。阴影半径为 `1.52`。
- v3 的胸腹真实扩宽、加厚并前倾，外扩抬高的肩背与较大毛簇形成倒三角上身，但下腹仍保持圆滚钟形毛团。头颈前压；独眼使用独立 18×12 正面区域，嘴与加深下颌使用另一独立色区和八枚分离牙齿。鹿角扩至左右约 48 模型单位并保持三向分叉负空间。主体毛团高度约 `H=49` 模型单位；上臂+前臂+手爪设计链长约 37（`0.755H`），可见腿长约 25.8（`0.527H`），四肢在保留修长比例的同时加粗 15–25%，站距和分蹄扩大。
- 原创 128×128 RGBA 硬边贴图限制为 10 个不透明颜色，继续使用骨白、冷灰、月银、近黑、冷青与哑金/枯褐；眼与口腔明暗区分增强，不含渐变噪点、血浆，也不下载、复制、描摹或嵌入 Klei 的贴图、TEX/build、sprite、模型、动画或音频。Wiki 只作辨识特征与节奏参考。
- 动画 key 包含原有 `idle` 4 秒循环、`walk` 1.2 秒不对称重步循环、`roar` 3 秒、`swipe` 1.6 秒、`slam` 2.5 秒、`recover` 2 秒、`hurt` 0.45 秒、`death` 3 秒，以及新增 `ice_spike` 2.8 秒。Swipe 最大横扫在 1.25 秒对齐服务端 24-tick 前摇后的伤害帧；Slam 砸地在 2.05 秒对齐 40-tick 前摇后的伤害帧；Ice Spike 双臂砸地在 1.60 秒对齐 32-tick 前摇后的第一排；Death 对齐 60-tick 保留并保持最终倒地姿态。动画仍只负责表现，不能决定伤害。hurt triggerable controller 只在服务端确认实际受伤且尚未进入 DEATH 时触发，不改变生命、BossBar、声音或状态机。
- v3 动画保持全部 9 个 key 与原长度；idle 增加前倾驼背、压低凝视与左右肩高差，walk 提高到约 23° 重步并增强躯干下沉，roar/swipe/slam/ice_spike 的长臂路径向身体外侧增加安全间隙。Swipe 1.25 秒、Slam 2.05 秒、Ice Spike 1.60 秒的表现关键帧不变，分别继续对应服务端 24/40/32 ticks；动画仍不能决定伤害。
- v3 生成与发布静态验收已通过：生成器连续两次得到完全相同的 geo/PNG/bbmodel SHA-256；验证结果为 32 bones、67 cubes、9 animations、10 个不透明贴图颜色、bbmodel 32 groups/67 elements/1 embedded texture。`gradlew.bat build --stacktrace` 已同时成功编译主 Boss 与此前待最终构建的 Ice Spike Wave；成品 JAR 的主模型、冰刺实体类及两套 geo/animation/texture 均已检查存在。
- 当前仍需游戏内人工检查：`2.4 × 5.8` 碰撞箱的门洞/寻路手感、远近距离独眼和分齿可读性、低屋顶鹿角穿模、长臂/蹄接地、第三人称相机尺度、9 动画在转向和斜坡上的残余自交，以及 Ice Spike 首排 32 ticks 对齐。Deerclops 10 类原创 OGG 已完成，仍需结合真实距离衰减和控制器二次音高做听感验收。
- 正式战利品已经注册为 `thecost:deerclops_eye`（最大堆叠 1、Rare）和 `thecost:frozen_sinew`（最大堆叠 64、Uncommon）。独立实体掉落表 `data/thecost/loot_tables/entities/deerclops.json` 固定掉落 1 个 Deerclops Eye，且不受 Looting 增加；Frozen Sinew 基础掉落 3–6 个，Looting 每级额外增加 0–1 个。原蓝冰与骨头占位掉落已移除。
- 两个物品的 16×16 透明像素图标是本项目原创的 AI 辅助参考结果，经压缩与像素整理后作为运行资源使用；不来自 Klei 或《Don't Starve》的贴图、sprite 或其他资产。

统一人工测试追加：用 `/thecost deerclops summon`、`/thecost deerclops trauma get` 和 `aura get` 验证第一次有效 Swipe 为手臂 1/2、第二次确定断一只完好手臂并获得 160 ticks 保护；正面举盾不加创伤，背对举盾仍加。核对 Swipe/Slam 成功命中分别降温 3/5°C 且不穿过 `temperature environment` 的 ambient，下限或伤害免疫导致未成功命中时不降温。Slam 外圈只受伤/击退/降温，4 格内脚着地两次确定断腿并获得 60 ticks Slowness II；结算帧跳起只规避腿创伤，仍受普通伤害、降温和内圈缓慢。把目标保持在 6–16 格核对 Ice Spike：32 ticks 预警锁向、五排每 4 ticks 由近至远、总计最多 15 根且地形失败点跳过；侧移或跳起可以躲过该点，命中只结算一次 10 点伤害、-5°C、30 ticks Slowness I。分别用一次 Slam + 一次 Ice Spike 和两次 Ice Spike 验证共享腿创伤达到 2/2 才断腿，并确认 160 ticks 保护；听感上只应在第一排与最后一排各有一次集中声层。站在 0–8、8–16、16 格外分别核对 strong 每 60 ticks -1、weak 长期每 150 ticks -1、none 不变；召唤多只确认不叠加且环境理智仍独立工作。用生命值高于 60%、30%–60%、低于 30% 分别核对“仅 Swipe / 每 3 次 Slam / 每 2 次 Slam”，同时确认中距离 Ice Spike 独立于这套近身节奏。自然生成时确认目标独享两次预警、约 6 秒后生成，其他玩家不听到预警；回归正式模型的独眼/鹿角/驼背/长臂/分蹄轮廓，检查 4 秒 idle、1.2 秒 walk、roar、1.25 秒 Swipe、2.05 秒 Slam、1.60 秒 Ice Spike 砸地、recover、hurt、3 秒 death、BossBar、60 ticks 移除、UUID 去重和同冬季一次约束；同时听辨 10 类原创声音，确认 warning 远距两阶段、ambient 稀疏、step 节流、swipe 与 slam/ice impact 分层、hurt/death 可区分、body fall 对齐死亡第 38 tick，且控制器二次降调后不过低、不爆音。还需验证击杀固定获得 1 个 Deerclops Eye、Frozen Sinew 数量及 Looting 增量，并以两者各 1 个完成一次祭坛嫁接。
