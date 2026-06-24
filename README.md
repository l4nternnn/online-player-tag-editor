# Online Player Tag Editor

![Version](https://img.shields.io/badge/version-2.2.0-2f855a)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-62b47a)
![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.19.2-f0b429)
![Fabric API](https://img.shields.io/badge/Fabric%20API-0.136.1%2B1.21.8-5a67d8)
![Modern UI](https://img.shields.io/badge/Modern%20UI-3.12.0.4-805ad5)
![License](https://img.shields.io/badge/license-MIT-blue)

Minecraft 1.21.8 Fabric 在线玩家 Tag 管理工具。它通过 Modern UI 管理台管理玩家的原版 Entity Tags，并提供 `monvhua` 计分板分数编辑入口。

本 mod 不提供聊天前缀、称号、LuckPerms 权限组、scoreboard team 或 Tab 列表前缀功能。它只负责写入原版 Entity Tags 和 `monvhua` 分数。

## 版本

- Mod: `2.2.0`
- Minecraft: `1.21.8`
- Fabric Loader: `0.19.2`
- Fabric API: `0.136.1+1.21.8`
- Java: `21`
- Modern UI: `3.12.0.4`
- Forge Config API Port: `21.8.2`，或任意 `21.6.0+` 版本

## 安装

### 服务端

1. 将 `online_player_tag_editor-2.2.0.jar` 放入服务端 `mods/`。
2. 启动服务端，首次运行会生成 `config/online-player-tag-editor.json`。
3. 服务端负责权限校验、Tag 写入和 `monvhua` 计分板写入。

服务端不需要安装 Modern UI；Modern UI 只用于管理员客户端显示图形界面。

### 管理员客户端

管理员客户端必须安装：

- `online_player_tag_editor-2.2.0.jar`
- Modern UI `3.12.0.4`
- Forge Config API Port `21.6.0+`

安装后，管理员可以使用 `/tageditor` 打开 Modern UI 管理台，也可以按默认快捷键 `=` 打开。快捷键可在 Minecraft 按键设置中修改。

普通玩家如果不使用管理台，不需要安装客户端 mod。没有权限的玩家即使安装了客户端 mod，也无法打开或操作管理台。

## 命令

| 命令 | 说明 | 可用方 |
|---|---|---|
| `/tageditor` | 打开 Modern UI 管理台 | 玩家 |
| `/tageditor <player>` | 打开管理台并预选指定玩家 | 玩家 |
| `/tageditor reload` | 重载配置文件 | 玩家和控制台 |
| `/tageditor list <player>` | 在聊天栏输出指定玩家所有 tags | 玩家和控制台 |
| `/tageditor add <player> <tag>` | 给指定玩家添加 tag | 玩家和控制台 |
| `/tageditor remove <player> <tag>` | 移除指定玩家 tag | 玩家和控制台 |
| `/tageditor toggle <player> <tag>` | 切换指定玩家 tag | 玩家和控制台 |

`<player>` 支持在线玩家补全，`<tag>` 支持预设 tags 补全。

## Modern UI 管理台

新版 GUI 不再使用原版箱子 `ScreenHandler`。`/tageditor` 会通过 Fabric custom payload 从服务端获取快照，并在客户端打开 Modern UI 界面。

管理台采用单屏三栏布局：

- 左侧：在线玩家列表、搜索框、tag 数量、刷新按钮。
- 中间：目标玩家名称、UUID 摘要、在线状态、当前 tags、当前 `monvhua` 分数和阶段。
- 右侧：`角色 Tags`、`魔法 Tags`、`身份 Tags`、`monvhua` 四个编辑页。

Tag 项以开关状态展示。点击已拥有的 tag 会移除，点击未拥有的 tag 会添加。每次操作都回到服务端重新检查权限，并继续通过 `TagService.validate()` 做统一合法性校验。

## 预设 Tags

默认配置按三类显示。已有配置会在启动或 `/tageditor reload` 时自动合并缺失的新增默认 tag，不会覆盖你已有的自定义配置。

### 角色 Tags

`ema`、`cero`、`nnk`、`mago`、`milya`、`sherry`、`yalisa`、`noa`、`anan`、`yuki`、`leiya`、`mll`、`coco`、`hanna`

### 魔法 Tags

`WitchSlayer` 魔杀、`Reversal` 回溯、`Floating` 漂浮、`Power` 怪力、`BrainWash` 洗脑、`Imitation` 模仿、`Heal` 治愈、`VisionControl` 视线诱导、`Clairvoyance` 千里眼、`FireControl` 点火、`LiquidControl` 液体操纵、`Swap` 互换、`Vision` 幻视、`Sandevistan` 过载、`Perception` 感知、`Intervention` 介入过去、`Through` 穿墙、`MindReading` 窃密

### 身份 Tags

`GrandWitch`、`muhou`、`player`、`master`、`guard`、`MonvhuaFull`

## monvhua

本 mod 不会自动创建 `monvhua` objective。需要由服务端或数据包预先创建：

```mcfunction
/scoreboard objectives add monvhua dummy
```

固定阈值：

| 分数 | 阶段 |
|---:|---|
| 0 | 神志清醒 |
| 10 | 略染污浊 |
| 25 | 轻度魔女化 |
| 45 | 中度魔女化 |
| 60 | 高度魔女化 |
| 70 | 重度魔女化 |
| 80 | 准魔女 |
| 90 | 魔女 |

## 权限

默认需要 OP 等级 `2`，可通过配置文件的 `permissionLevel` 修改。

预留权限节点：

- `online_player_tag_editor.open`
- `online_player_tag_editor.edit`
- `online_player_tag_editor.reload`

默认不启用第三方权限 API。GUI 中每次操作都会重新由服务端检查权限。

## 配置

配置文件：`config/online-player-tag-editor.json`

主要字段：

- `permissionLevel`: 使用命令和 GUI 所需的最低 OP 等级，默认 `2`。
- `guiTitle`: Modern UI 管理台标题。
- `presetTags`: 管理台显示的预设 tags。
- `tagDisplayNames`: tag 显示名映射，只影响界面显示，真实写入的仍是原始 tag。
- `enableEscButton` / `escButtonText`: 保留字段。
- `dangerousConfirm`: 保留字段。

`ConfigManager.load()` 会把缺失的默认 `presetTags` 和 `tagDisplayNames` 合并到已有配置。

## Tag 合法性

所有写入入口继续调用 `TagService.validate()`：

- tag 不能为空。
- tag 不能包含空格。
- tag 长度不能超过 64。
- tag 只能包含 `[a-zA-Z0-9_:\-.]`。

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/`。

## 验证建议

```mcfunction
/tag <玩家名> list
/tageditor list <玩家名>
/scoreboard players get <玩家名> monvhua
```

建议手动验证：

- `/tageditor` 和 `/tageditor <player>` 能打开 Modern UI 管理台。
- 玩家搜索、刷新、分类切换正常。
- `Through` 和 `MindReading` 出现在魔法 Tags 页。
- 点击魔法 tag 后原版 Entity Tags 正确添加或移除。
- ESC 关闭后再次打开不会停留在暗色高斯模糊遮罩。
- `monvhua` objective 不存在时显示警告，存在时可以设置分数。

## 项目结构

```text
src/main/java/com/lantern/onlineplayertageditor/
├── OnlinePlayerTagEditor.java
├── command/PlayerTagsCommand.java
├── config/ConfigManager.java
├── config/ModConfig.java
├── network/EditorNetworking.java
├── network/EditorPayloads.java
├── network/EditorSnapshot.java
├── network/EditorSnapshotService.java
├── network/TagCategory.java
├── scoreboard/ScoreboardService.java
├── tag/TagService.java
└── util/

src/client/java/com/lantern/onlineplayertageditor/client/
├── OnlinePlayerTagEditorClient.java
├── EditorClientNetworking.java
├── ModernTagEditorLauncher.java
└── ModernTagEditorScreen.java
```

## License

MIT