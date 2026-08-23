# Module Map

本文件维护 `.zcode/modules/` 的写作规则和跳转索引。警告：模块文档体量较大；按索引命中后只读取必要模块。

## 读取策略

- 先从 `.zcode/overview.md` 定位运行入口，再用 API、类名、文件名或资源 ID 选择模块。
- 索引无法确定归属时，允许读取候选模块的边界与参考章节进行判定。
- 命中模块后必须沿真实调用、数据、生命周期或契约关系继续读取直接依赖。
- 无上述关系的模块禁止进入任务上下文。

## 模块文档规范

### 文件与命名

- 模块文档必须位于 `.zcode/modules/`。
- 文件名必须使用英文 kebab-case，并以 `.md` 结尾。
- 原版模块必须使用 `vanilla-` 前缀。
- 项目模块必须使用 `mod-` 前缀。
- 硬依赖模块必须使用 `lib-<库名>-` 前缀。
- 可选兼容模块必须使用 `compat-<库名>-` 前缀。
- 文档标题必须使用与索引一致的中文模块名。

### 内容结构

- 每个模块文档包括 `定义`、`参考`、`边界`、`链路`、`规范` 和 `陷阱` 章节。
- `定义` 必须用 1 句话说明模块或 API 的定义与用途，严禁写入实现细节；最多 1 行。
- `参考` 只允许收录支撑正式模型的 API、关键文件、目录或方法，并说明其作用；至少 5 行；每行必须以目录、文件或方法开头，目录优先。
- `边界` 只允许记录长期成立的职责归属、状态归属、读写边界、消费边界和跨模块所有权；至少 5 行。
- `链路` 中的每条真实链路必须使用独立次级章节，只允许按真实调用顺序记录。
- `规范` 只允许记录输入输出、状态持久化、错误语义、保存语义、跨模块调用语义和长期实现规则；至少 5 行。
- `陷阱` 只允许记录会造成状态错归属、输入绕过、输出污染、错误语义错位或持久化污染的具体风险；至少 3 行；严禁与 `规范` 重复。

### 书写规则

- Java 类在 `参考` 中首次出现时必须写完整包名，后续允许使用短类名。
- API 文档与项目实现文档必须分开；项目内部接入方式只允许写入 `mod-` 文档。
- 模块正文严禁引用其它模块文件名。
- 模块正文严禁使用泛指、字段堆砌、凑行数内容和无明确指导作用的空话。
- 模块正文必须直接按完整正式模型组织。
- 链路和其它顺序内容必须按真实顺序排列；其余同级条目必须按字典序排列。

### 更新边界

- 模块文档严禁记录阶段任务完成情况、测试计划、临时实现、候选设计和后续计划。
- 普通新增内容严禁形成逐项实现清单。
- 只有长期规范发生变化时才允许更新对应模块文档。
- `mod-` 文档之外严禁记录项目内部实现。
- `mod-` 文档严禁逐项列举资源 ID；资源注册内容只记录命名模式、注册入口、特例规则和同步要求。
- 新增模块时必须先添加索引，再填写模块文档。

## 原版 Starsector

- [核心生命周期](modules/vanilla-core-lifecycle.md)：mod plugin、全局入口、加载阶段和 picker。
- [战斗 API](modules/vanilla-combat-api.md)：战斗实体、插件、渲染层和监听器。
- [战斗伤害 API](modules/vanilla-combat-damage.md)：伤害、EMP、modifier、listener 和统计。
- [战斗光束 API](modules/vanilla-combat-beam.md)：光束实体、端点、来源、武器和效果回调。
- [战斗弹丸 API](modules/vanilla-combat-projectile.md)：弹丸、导弹、spec、生成、命中和 AI。
- [战斗舰船 API](modules/vanilla-combat-ship.md)：舰船、通量、护盾、装甲、AI 和系统状态。
- [战斗舰队 API](modules/vanilla-combat-fleet.md)：部署、预备队、成员、指挥官和部署点。
- [战斗点位 API](modules/vanilla-combat-objective.md)：点位实体、效果、状态与目标语义。
- [战斗任务 API](modules/vanilla-combat-task.md)：task manager、指派、waypoint 和命令点。
- [战斗武器 API](modules/vanilla-combat-weapon.md)：武器数据、spec、slot、弹药、回调和自动开火。
- [战斗战术系统](modules/vanilla-combat-shipsystem.md)：系统数据、Stats、AI 与状态机。
- [船体插件](modules/vanilla-hullmod.md)：hullmod 生命周期、S-mod、tooltip 和监听器。
- [人物](modules/vanilla-person.md)：身份、关系、stats、officer、captain 和 important people。
- [战役 Mission API](modules/vanilla-mission.md)：独立 mission 的注册、舰队、地图和战斗初始化。
- [装配 Variant API](modules/vanilla-variant.md)：variant 来源、武器组、插件、模块与运行态交接。
- [技能](modules/vanilla-skill.md)：skill、aptitude、effect、scope 和 fleet total。
- [生涯能力](modules/vanilla-campaign-ability.md)：ability 注册、生命周期、AI、slot、rulecmd 和互斥。
- [生涯 API](modules/vanilla-campaign-api.md)：Sector、时钟、脚本、UI、插件和 listener。
- [生涯战斗与遭遇](modules/vanilla-campaign-battle.md)：Battle、encounter、结算、战后上报和战利品。
- [生涯势力](modules/vanilla-campaign-faction.md)：关系、声望、doctrine、production 和 ship picker。
- [生涯舰队](modules/vanilla-campaign-fleet.md)：舰队、成员集合、AI、assignment、inflater 和生成。
- [生涯舰队成员](modules/vanilla-campaign-fleet-member.md)：成员状态、类型、视图、创建和集合入口。
- [生涯货物容器](modules/vanilla-campaign-cargo.md)：cargo、stack、credits、人员、补给与舰船。
- [生涯商品](modules/vanilla-campaign-commodity.md)：commodity spec、cargo stack 和商品 UI。
- [生涯特殊物品](modules/vanilla-campaign-special-item.md)：special item、stack、tooltip、render 和右键动作。
- [生涯交互对话](modules/vanilla-campaign-interaction.md)：dialog、选项、视觉、picker、UI 和战斗入口。
- [生涯任务](modules/vanilla-campaign-mission.md)：mission board、hub、阶段、trigger、搜索和 intel。
- [生涯实体](modules/vanilla-campaign-entity.md)：entity、星体、跳跃点、terrain、轨道和视觉。
- [生涯位置](modules/vanilla-campaign-location.md)：location、star system、实体容器、背景和访问状态。
- [生涯市场](modules/vanilla-campaign-market.md)：market、economy、商品、人物、人口和开放状态。
- [生涯子市场](modules/vanilla-campaign-submarket.md)：submarket、cargo、交易规则、经济影响和 UI。
- [生涯产业](modules/vanilla-campaign-industry.md)：industry、供需、升级、AI core、改良和 disruption。
- [生涯市场条件](modules/vanilla-campaign-condition.md)：condition、token、tooltip、survey、suppression 和重算。
- [生涯酒吧](modules/vanilla-campaign-bar.md)：bar event、manager、creator、mission wrapper 和临时规则。
- [生涯 Intel](modules/vanilla-campaign-intel.md)：intel 可见性、消息、渲染、地图和移除。
- [rules.csv 规则系统](modules/vanilla-campaign-rules.md)：规则匹配、条件、脚本、文本、选项和 rulecmd。
- [生涯内存](modules/vanilla-campaign-memory.md)：Memory、MemKeys、memoryMap 与 PersistentData。
- [数值运算](modules/vanilla-mutable-stat.md)：MutableStat、StatBonus、DynamicStats 和舰船 stats。
- [数据资产 API](modules/vanilla-data-assets.md)：CSV、JSON、专用资产文件、资源和文本加载。

## 硬依赖

- [LazyLib API](modules/lib-lazy-api.md)：数学、几何、战斗查询、cargo、JSON 和工具。
- [MagicLib API](modules/lib-magic-api.md)：MagicTrail、FakeBeam、生涯工具、HUD、设置和赏金。
- [GraphicsLib API](modules/lib-graphic-api.md)：ShaderLib、光源、纹理和图形数据。
- [BoxUtil API](modules/lib-box-api.md)：OpenGL、图形、字体、shader 和绘制工具。

## 可选兼容

- [UNGP 兼容](modules/compat-ungp-rules.md)：规则注册、难度、战斗与战役回调。
- [Nexerelin 兼容](modules/compat-nexerelin.md)：生成分支、势力配置和特遣队。
- [LunaLib 设置兼容](modules/compat-luna-settings.md)：设置定义、加载和运行时读取。
- [Console Commands 兼容](modules/compat-console-commands.md)：命令注册、上下文、结果和监听器。
- [微型兼容](modules/compat-minor-integrations.md)：不足以独立成模块的兼容协议。

## 内部模块

- 暂无
