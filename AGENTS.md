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

Sanity 后续尚待完成：Fractured 的祭坛互动与隐藏实体，以及 Ascended 的契约失效等仍保留玩法。隐藏实体是后续 Sanity 核心；下一阶段必须由后续任务明确选择。不得无任务授权扩展季节、Boss、祭坛或新的理智来源。

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

- 注册单方块 `thecost:grafting_altar`、对应 BlockItem、BlockEntity 与 ScreenHandler。方块使用朝向状态、分层轮廓碰撞箱和持久化两格库存；破坏时通过原版 `ItemScatterer` 安全返还材料。当前无生存合成配方，开发测试使用 `/give @s thecost:grafting_altar`。
- 祭坛只有 `SanityStage.FRACTURED` 玩家能够打开；Lucid/Ascended 在服务端被拒绝。确认按钮再次在服务端校验玩家仍可使用该方块、仍为 Fractured、选中槽仍为 `BROKEN` 且材料仍有效，客户端不能直接修改肢体数据。
- 两个材料入口均为数据包可扩展的物品标签：`thecost:grafting_components` 默认含铁锭，`thecost:grafting_catalysts` 默认含紫水晶碎片。每次成功嫁接各消耗 1 个；BlockEntity 库存写入 NBT，退出界面或区块卸载不会丢失。
- UI 读取现有玩家 DataTracker 肢体镜像显示左臂、右臂、双腿的 `INTACT/BROKEN/GRAFTED` 状态；选中状态由每个 ScreenHandler 独立同步。界面使用骨白、冷蓝、暗金的程序化占位绘制，不依赖外部图片，也未实现已延期的 Runic Circle 绘图 UI。
- 成功时只通过统一 `LimbData.setReplacement` 将目标从 `BROKEN` 写为 `GRAFTED`，replacement ID 集中使用 `thecost:basic_left_arm`、`thecost:basic_right_arm` 或 `thecost:basic_legs`。基础嫁接当前没有额外能力；因为现有 renderer 只隐藏 `BROKEN`，成功后会恢复原版玩家部件，而非显示正式义肢模型。自定义嫁接部件模型与能力是后续工作。
- 静态祭坛占位模型位于标准 blockstates/models 目录，目前以原版骨块、深板岩、金块和浮冰纹理组合验证模型与朝向接线；正式 Blockbench 模型和项目自有贴图后续可直接替换资源，不改变 BlockEntity 或 UI 协议。

统一人工测试追加：在 Fractured 下放置并打开祭坛，验证铁锭/紫水晶标签槽、shift-click、库存持久化和破坏返还；分别对三种 `BROKEN` 槽嫁接，确认各消耗 1 个材料、状态变为 `GRAFTED`、replacement ID 保留至重进/重生/跨维度且断肢惩罚解除。验证 Lucid/Ascended 无法打开，远离或破坏祭坛后旧界面按钮不能执行，以及正常/已嫁接槽不能重复消耗材料。

## 验证命令

```powershell
.\gradlew.bat build --stacktrace
```
