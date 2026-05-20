# Online Player Tag Editor

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-62b47a?logo=minecraft)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-d4c5a9?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAA7AAAAOwBeShxvQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAUpSURBVFiFzVdbbBxVFP3OzM7aWTt27MTxI34kjZuqERQJJCQe4qNCVKhS+UB8IFQQUhF/ICTggw9A/ABSK4FUIfHBQwIVCIkPCVXio4oSEj/ED8lDTeLEduzYsXdnZ3d2uHfW3t3Z8cYrFQjY0j7mnHvuOffcO4J/qRHufIdNmIRHk7j7zho36kCDaG0FGagGGpoNN28T0OjUwAVXJwQab2AwO24ay6/x3/XPoIPX8AR68CF7qxhooEXzOATqdPQHdmkEGi1XkSDIfE5Fv09Bla7O1GX3WjgVB2l0GBosX1sZDqQEFcyAElaAiguAv1G2VouE1TMDI+o+H2zbs0ZJ0SJ2UXUK0wCuIvAcKvNOw4AcKlRA0AQZ6AAQNJclAmw/J01RqxFUFSSv6r0IIM4BRQbG0KVFxJEENYVCFnMGAJoGAMlpEpyQYY02Ak45CTEcEuPjMYcnLB+AJ96jf4ZbP0hMl0QxVVH0aRBU2Cavk0pXVLiq0I0egUHdAiBQCecUAFdQyYIxGJ2jIfdP0Mlc3Li2WdcS9VKCRwO8VZkr1eiMJbMhyB2GAb42IXjWRiVpqcjcSjknLIpMC56YnnE75A7FrwzINoHcaMrlQoCgGeOXCoqsikHh+QQETJYvS4o4u+N5E/Ta5ZVlJaHZgQ5mMHHD/IYnL5n3diBQm3+6kmDFTEEZcYE2FIA5mAcbK+g0HYKQJmhQiYCZGso10xYBUbXsLVxIkj1WBAhQbFfbmC6QCSbB7EJGXMiQ1SZHCRJFPRaFPSnlMnBPEByF2ToEhR4VFh1TIsYNwQAvMYKQpwyxUrKoyBu0cYkFqnLMNOwHCYgsQtWkk1Lhjj8H9Lt+6i0uG82owE0VKKeNElGRBVQ3ZJll8HCVyAtUIRuFjxXbO6q2aMWFFaDoZg9aR1FjiPjdyyfO4PmDAFxFJK3WixXrgXxRm43gxY+NYoUBUJBjjQ6qGkKNGLYdVMK4EKAZUJ6u5vGHcfnF2sE5LFfCRKMCkE4Nza1KdSYPiC1/1qoDSAT2l++2X/xXAPY3AhoHVEAzVAQ8yTFC7vXU1Gxm9nIBhA30eK5cDBOACtLrYUMlQy6QtBf1DcJKBXGxImKn0dXyRIXPs4v9igCiFGnH4dE3oAAu0MVFBYB+LB79NBZq3W+YOX+26Wun7WmySgVzqhshwA0q6LUhkCpoP8yOzHW3yA09La+E/OrAPyPABgWkYoPSUJI2IDKy+MnI6Y8Sqzc1F08WDOAHJCBJCZrr5y0VmBBDe48DDj67+PChhG8tPdk32v3wDIA+goXxHZ2BmtAuEGCnkEq7G39ZChO+fLDHQPOjG56ZP37jc7djtiUhDJRyU4IZFEWWb19TkfNV2bu1PDscTThva8MguGLvfq3//hDNvqqkZVkkx9kgoQFQJJGgmfM+Z7YUD/ZPvnD3N11t8QwAio7NuMqExjhU0JQLDlp0a2NLMEqD0Y2nO1qIeT9Pov4MpuMYq1G4qFe5ys9PpXK29LX8VdRY+2A2qvZgHr9gGqvQr5cKX4eIdaHl/DMJ1ayysaEN4bGDZSW8ppRS4lqDp3eGCMsIor7x+vu/fd2gSq4GcA3msaYI9BHy9FA4FVyq2B3fw1G9zRYV+PpSucSHU7tQNS2mFGsI6TqGaAV/Q7u1KBb//8aXoIdOIsQ6cJuAFIJ4Ex2Z4GtZXVYm1+9+tLV/13BAiuXNAAAAAElFTkSuQmCC)](https://fabricmc.net)
[![Java](https://img.shields.io/badge/Java-21-ed8215?logo=openjdk)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Build](https://img.shields.io/badge/build-passing-brightgreen)]()

在线玩家 Tag 管理工具 —— 通过 GUI 快速配置 Minecraft 原版 Entity Tags。

**这不是**聊天前缀 mod、LuckPerms 权限组、scoreboard team、头顶称号、Tab 列表前缀 mod。  
**本 mod 的核心目标**是管理 Minecraft 原版 `/tag` 系统对应的 Entity Tags。

## 支持版本

- Minecraft: **1.21.8**
- Loader: **Fabric**
- Java: **21**
- Mappings: **Yarn (1.21.8+build.1)**

## 安装方式

### 服务端（必须）

1. 将 `online_player_tag_editor-1.0.0.jar` 放入服务端 `mods/` 目录
2. 启动服务端，首次运行会自动生成配置文件 `config/online-player-tag-editor.json`
3. 服务端安装后，管理员可使用 `/playertags` 命令和 GUI

### 客户端（可选）

- **只用 `/playertags` 命令和 GUI**：客户端不需要安装 mod
- **想要 ESC 菜单中显示"Tag 管理"按钮**：客户端也需要安装 mod，放入 `mods/` 目录

### 权限说明

- **普通玩家即使客户端装了 mod，没有权限也无法打开管理 GUI**
- 权限由服务端判断，客户端不做 OP 权限校验
- 无权限时由服务端返回提示信息

## 命令列表

| 命令 | 说明 | 可用方 |
|------|------|--------|
| `/playertags` | 打开在线玩家列表 GUI | 仅玩家 |
| `/playertags <player>` | 直接打开指定玩家的 tag 编辑 GUI | 仅玩家 |
| `/playertags reload` | 重载配置文件 | 玩家和控制台 |
| `/playertags list <player>` | 在聊天栏输出指定玩家的所有 tags | 玩家和控制台 |
| `/playertags add <player> <tag>` | 给指定玩家添加 tag | 玩家和控制台 |
| `/playertags remove <player> <tag>` | 移除指定玩家的 tag | 玩家和控制台 |
| `/playertags toggle <player> <tag>` | 切换指定玩家的 tag（有则移除，无则添加） | 玩家和控制台 |

命令支持 Tab 补全：
- `<player>` 补全在线玩家名
- `<tag>` 补全预设 tags 列表

## 权限

默认需要 **OP 等级 2** 或以上（可在配置文件中修改）。

权限判断使用 `source.hasPermissionLevel(config.permissionLevel)`。

预留权限节点（代码中已预留扩展点，默认不启用节点检查）：
- `online_player_tag_editor.open` — 打开 GUI
- `online_player_tag_editor.edit` — 编辑 tags
- `online_player_tag_editor.reload` — 重载配置

权限规则：
1. 打开 GUI 需要 open 权限或 OP 等级
2. 添加/删除/切换 tag 需要 edit 权限或 OP 等级
3. reload 需要 reload 权限或 OP 等级
4. GUI 中每次点击操作都会重新检查权限

## 配置文件

自动生成于 `config/online-player-tag-editor.json`：

```json
{
  "permissionLevel": 2,
  "guiTitle": "在线玩家 Tag 管理",
  "enableEscButton": true,
  "escButtonText": "Tag 管理",
  "presetTags": [
    "ema", "cero", "nnk", "mago", "milya",
    "sherry", "yalisa", "noa", "anan", "yuki",
    "mll", "coco", "hanna",
    "WitchSlayer", "Reversal", "Floating", "Power", "BrainWash",
    "Imitation", "Heal", "VisionControl", "Clairvoyance", "FireControl",
    "LiquidControl", "Swap", "Vision", "Sandevistan", "Perception",
    "Intervention"
  ],
  "tagDisplayNames": {
    "ema": "樱羽艾玛",
    "cero": "二阶堂希罗",
    "nnk": "黑部奈叶香",
    "mago": "宝生玛格",
    "milya": "佐伯米莉亚",
    "sherry": "橘雪莉",
    "yalisa": "紫藤亚里沙",
    "noa": "城崎诺亚",
    "anan": "夏目安安",
    "yuki": "月代雪",
    "mll": "冰上梅露露",
    "coco": "泽渡可可",
    "hanna": "远野汉娜",
    "WitchSlayer": "魔杀",
    "Reversal": "回溯",
    "Floating": "漂浮",
    "Power": "怪力",
    "BrainWash": "洗脑",
    "Imitation": "模仿",
    "Heal": "治愈",
    "VisionControl": "视线诱导",
    "Clairvoyance": "千里眼",
    "FireControl": "点火",
    "LiquidControl": "液体操纵",
    "Swap": "互换",
    "Vision": "幻视",
    "Sandevistan": "过载",
    "Perception": "感知",
    "Intervention": "介入过去"
  },
  "dangerousConfirm": true
}
```

配置说明：
- `permissionLevel`: 所需的最低 OP 等级，默认 2
- `guiTitle`: GUI 标题文字
- `enableEscButton`: 是否在客户端 ESC 菜单显示按钮（客户端读取本地配置）
- `escButtonText`: ESC 菜单按钮文字
- `presetTags`: GUI 中显示的预设 tags 列表
- `tagDisplayNames`: tag 的显示名映射，只影响 GUI 显示，**真实写入的 tag 仍是原始 key**
- `dangerousConfirm`: 危险操作确认开关（预留，当前版本未使用）

## 使用示例

1. 管理员输入 `/playertags`
2. 看到在线玩家列表（玩家头颅 + 当前 tags 数量）
3. 左键点击某个玩家头像
4. 进入该玩家的 tag 编辑界面
5. 预设 tags 以染色物品显示：
   - **绿色染料** = 玩家已有此 tag，点击移除
   - **灰色染料** = 玩家没有此 tag，点击添加
6. 点击 `script_1` → 玩家获得原版 tag `script_1`
7. 在游戏内执行 `/tag <玩家名> list` 可验证

## 验证方式

在游戏内执行：
```
/tag <玩家名> list
```
应该能看到通过本 mod 添加的 tags。

也可通过命令验证：
```
/playertags list <玩家名>
```

## GUI 功能

### 主界面（在线玩家列表）
- 显示所有在线玩家（按名称排序）
- 每个玩家使用玩家头颅显示
- Lore 显示当前 tag 数量和前 5 个 tags
- 支持分页（上一页/下一页）
- 刷新按钮：重新列出在线玩家
- 关闭按钮

### 玩家 Tag 编辑界面
- 标题显示正在编辑的玩家名
- 显示配置文件中的预设 tags
- 已拥有 tag：绿色染料，点击移除
- 未拥有 tag：灰色染料，点击添加
- 点击后即时生效并刷新界面
- 返回玩家列表按钮
- 查看全部 tags 按钮（在聊天栏输出）
- 清除配置内 tags 按钮（仅移除预设列表中的 tags，不删除玩家其他 tags）
- 支持分页（tags 多于一页时）

## 技术实现

- 使用 Yarn mappings 直接调用原版 Entity Tags API：
  - `player.getCommandTags()` — 获取所有 tags
  - `player.addCommandTag(tag)` — 添加 tag
  - `player.removeCommandTag(tag)` — 移除 tag
- 不通过执行 `/tag` 字符串命令来实现 tag 操作
- GUI 使用原版 `ScreenHandler` + 箱子 GUI（`ScreenHandlerType.GENERIC_9X4` / `GENERIC_9X6`）
- 客户端无需安装 mod 即可使用 GUI
- ESC 菜单按钮使用 Mixin + `@Shadow` 注入 `GameMenuScreen.init()`

## Tag 合法性校验

所有写入入口均进行校验：
- 不允许空 tag
- 不允许包含空格
- 不允许包含非法字符（只允许 `[a-zA-Z0-9_:\\-.]`）
- 长度限制：1-64 字符
- 验证不通过会给出中文提示

## 已知限制

1. **配置重载后需手动重开 GUI**：执行 `/playertags reload` 后，已打开的 GUI 不会自动刷新。需手动关闭并重新打开 GUI 才能看到新配置。

2. **ESC 按钮的 enableEscButton 配置**：客户端读取本地配置文件判断是否显示按钮。如果客户端和服务端配置文件不同步，可能出现不一致。

3. **玩家头颅皮肤**：当前版本玩家列表使用预览类型的 PLAYER_HEAD，不加载实际皮肤纹理。

4. **不依赖第三方 GUI 库**：完全使用 Minecraft 原版 ScreenHandler API 实现。

## 编译

```bash
./gradlew build
```

编译输出：
- `build/libs/online_player_tag_editor-1.0.0.jar` — 可安装的 mod 文件
- `build/libs/online_player_tag_editor-1.0.0-sources.jar` — 源码 jar

## 项目结构

```
online-player-tag-editor/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── src/
│   ├── main/
│   │   ├── java/com/lantern/onlineplayertageditor/
│   │   │   ├── OnlinePlayerTagEditor.java          # 主入口 (ModInitializer)
│   │   │   ├── command/
│   │   │   │   └── PlayerTagsCommand.java          # 命令系统 (Brigadier)
│   │   │   ├── config/
│   │   │   │   ├── ModConfig.java                  # 配置数据类
│   │   │   │   └── ConfigManager.java              # 配置读写
│   │   │   ├── gui/
│   │   │   │   ├── PlayerListScreenHandler.java    # 在线玩家列表 GUI
│   │   │   │   └── PlayerTagEditorScreenHandler.java # Tag 编辑 GUI
│   │   │   ├── tag/
│   │   │   │   └── TagService.java                 # Tag 操作 + 校验
│   │   │   └── util/
│   │   │       ├── PermissionUtil.java             # 权限工具
│   │   │       └── TextUtil.java                   # 文本工具
│   │   └── resources/
│   │       ├── fabric.mod.json
│   │       └── online_player_tag_editor.mixins.json
│   └── client/
│       ├── java/com/lantern/onlineplayertageditor/client/
│       │   ├── OnlinePlayerTagEditorClient.java    # 客户端入口
│       │   └── mixin/
│       │       └── GameMenuScreenMixin.java        # ESC 菜单按钮 Mixin
│       └── resources/
│           └── online_player_tag_editor.client.mixins.json
└── README.md
```

## 注意事项

1. 本 mod 不修改聊天名、头顶名、Tab 名
2. 不影响 LuckPerms 或其他权限插件
3. 不依赖任何第三方权限 API
4. 服务端必须安装，客户端可选安装
5. 管理对象仅为当前在线玩家
6. Tag 数据存储在 Minecraft 原版 Entity Tags 中，不会丢失
7. 如果客户端未安装 mod 但服务端安装了，ESC 按钮不会出现，但 `/playertags` 命令和 GUI 正常可用

## License

MIT
