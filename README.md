# THOSC_BOX - Touhou Project & osu! VRChat OSC Bridge | 东方Project & osu! VRChat OSC 桥接工具 | 東方Project & osu! VRChat OSC 送信ツール

[English](#english) | [简体中文](#简体中文) | [日本語](#日本語)

---

## English

**THOSC_BOX** is a high-performance VRChat OSC bridge designed for official mainline Touhou Project games (TH06 through TH20) and **osu!** gameplay. It reads real-time gameplay statistics directly from game memory or HTTP telemetry APIs and transmits them via OSC (Open Sound Control) protocol to VRChat to drive avatar parameters and display your live gameplay status in the VRChat Chatbox.

### 🌟 Key Features
- **Multi-Mode Support**: Toggle between **Touhou Mode** and **osu! Mode** dynamically from the UI.
- **Wide Mainline Game Support**: Seamlessly supports 15 mainline Touhou games from **TH06 (Embodiment of Scarlet Devil)** up to **TH20 (Fossilized Wonders)**.
- **osu! Live Integration**:
  - Automatically fetches stats from local HTTP APIs (`gosumemory` / `tosu`).
  - **Auto-Launch & Stop**: Automatically scans and launches helper executables (e.g. `gosumemory.exe` or `tosu.exe`) when entering osu! mode and cleans them up upon exit.
  - Telemetry features: Real-time PP, accuracy, combos, hit counts (300/100/50), stars, BPM, HP, active mods (num/str), and grades.
- **Extended Touhou Memory Statistics**:
  - Tracks score (scaled), cumulative misses/bombs, active stage & difficulty, character/subshot selection, graze, power, point items (PIV), cherry max, and active spell card ID & names.
- **Dynamic MD3 UI Theme Customizer**:
  - Customize the UI accent color at runtime using an MD3 preset grid palette and custom hex inputs.
  - Contrast-aware color generation and runtime UI look-and-feel updates.
  - Automatically persists preferences in `settings.json`.
- **ASLR & High Stability**: Dynamic memory base offset resolving to fully support Steam versions with ASLR. Lightweight read-only JNA memory access ensures zero crashes or performance impact.
- **Modular Codebase**: Split into clean component modules (`UiComponents.kt`, `Settings.kt`, `Scanner.kt`, `OsuTelemetry.kt`, `MainWindow.kt`) for easy maintainability.

### 📡 VRChat OSC Parameters

#### 1. Touhou Mode Parameters
Broadcasts to `127.0.0.1:9000`:

| Parameter Name | Data Type | Description |
| :--- | :--- | :--- |
| `/avatar/parameters/TouhouGameID` | `Int` | 0-indexed index of the active game in the supported list. |
| `/avatar/parameters/TouhouGameName` | `String` | Complete title of the running game. |
| `/avatar/parameters/TouhouScore` | `Int` | Current in-game score (fully recovered value). |
| `/avatar/parameters/TouhouMiss` | `Int` | Cumulative player deaths (misses) in the current session. |
| `/avatar/parameters/TouhouBomb` | `Int` | Cumulative bomb usages in the current session. |
| `/avatar/parameters/TouhouDifficulty` | `Int` | Difficulty index: `0` (Easy) to `5` (Phantasm). |
| `/avatar/parameters/TouhouDifficultyName` | `String` | Text name of the difficulty (e.g. `Lunatic`). |
| `/avatar/parameters/TouhouCharacter` | `Int` | Character ID index. |
| `/avatar/parameters/TouhouSubshot` | `Int` | Subshot/shottype ID index. |
| `/avatar/parameters/TouhouCharacterName` | `String` | Text name of character & shottype (e.g. `Reimu A (Homing)`). |
| `/avatar/parameters/TouhouStage` | `Int` | Normalized active stage: `1`-`6` (Main), `7` (Extra), `8` (Phantasm). |
| `/avatar/parameters/TouhouGraze` | `Int` | Current graze count. |
| `/avatar/parameters/TouhouPower` | `Float` | Current power (normalized to `0.0` - `4.0` / `8.0`). |
| `/avatar/parameters/TouhouPowerRaw` | `Int` | Current raw power value. |
| `/avatar/parameters/TouhouPoint` | `Int` | Current point item value (PIV). |
| `/avatar/parameters/TouhouCherryMax` | `Int` | Current maximum cherry points (TH07). |
| `/avatar/parameters/TouhouSpellID` | `Int` | Active spell card ID (or `-1` if inactive). |
| `/avatar/parameters/TouhouSpellActive` | `Bool` | `true` if currently in a spellcard duel. |
| `/avatar/parameters/TouhouSpellName` | `String` | Text name of the active spellcard. |
| `/chatbox/input` | `String, Bool, Bool` | Sends: `Playing: <GameName> [<Diff>] | Chara: <Chara> | Stage: <Stage> | Score: <Score> | Miss: <Miss> | Bomb: <Bomb>` |

#### 2. osu! Mode Parameters
Broadcasts to `127.0.0.1:9000`:

| Parameter Name | Data Type | Description |
| :--- | :--- | :--- |
| `/avatar/parameters/OsuStatus` | `Int` | Gameplay status: `2` (Menu), `4` (Playing), etc. |
| `/avatar/parameters/OsuScore` | `Int` | Active map score. |
| `/avatar/parameters/OsuCombo` | `Int` | Current combo count. |
| `/avatar/parameters/OsuMaxCombo` | `Int` | Highest combo achieved in current map. |
| `/avatar/parameters/OsuAccuracy` | `Float` | Normalised accuracy (`0.0f` to `1.0f`). |
| `/avatar/parameters/OsuMiss` | `Int` | Active map miss count. |
| `/avatar/parameters/OsuGrade` | `String` | Current hit grade (e.g. `SS`, `SH`, `A`). |
| `/avatar/parameters/OsuBPM` | `Float` | Song tempo beats per minute. |
| `/avatar/parameters/OsuStars` | `Float` | Mod-adjusted star rating (SR) of the beatmap. |
| `/avatar/parameters/OsuHP` | `Float` | Normalised health bar value (`0.0f` - `1.0f`). |
| `/avatar/parameters/OsuPPCurrent` | `Float` | Live performance points (PP) generated. |
| `/avatar/parameters/OsuPPFC` | `Float` | Maximum performance points (PP) for a Full Combo. |
| `/avatar/parameters/OsuModsNum` | `Int` | Raw integer representation of active mods. |
| `/avatar/parameters/OsuModsStr` | `String` | Text representation of active mods (e.g. `HDDT`). |
| `/avatar/parameters/OsuHit300` | `Int` | Count of 300 hits. |
| `/avatar/parameters/OsuHit100` | `Int` | Count of 100 hits. |
| `/avatar/parameters/OsuHit50` | `Int` | Count of 50 hits. |
| `/chatbox/input` | `String, Bool, Bool` | Sends: `[osu!] Playing: <Artist> - <Title> [<Diff>] (<Stars>*) [<Mods>] | Combo: <Combo>x | PP: <PP>/<PP_FC> | Acc: <Acc>% | Miss: <Miss>` |

### 🛠️ Building from Source
- **Requirements**: JDK 17 or higher.
- **Build shadow JAR**:
  ```powershell
  ./gradlew shadowJar
  ```
- **Build Standalone EXE**:
  ```powershell
  ./gradlew jpackage
  ```

---

## 简体中文

**THOSC_BOX** 是一款专为《东方Project》官方正作（TH06 至 TH20）及 **osu!** 游戏设计的 VRChat OSC 桥接工具。它通过读取运行中的游戏内存或 HTTP 遥测接口，实时将游玩数据（分数、死亡次数、炸弹使用数、当前关卡、PP值等）通过 OSC 协议发送至 VRChat，用以动态驱动 Avatar 虚拟形象的参数，并同步推送实时状态至聊天框（Chatbox）。

### 🌟 功能特性
- **多模式动态切换**：在 UI 顶部一键切换 **东方模式** 与 **osu! 模式**。
- **支持作品广泛**：无缝支持从 **TH06《东方红魔乡》** 到最新作 **TH20《东方锦上京》** 的共 15 部官方正作。
- **osu! 实时遥测集成**：
  - 自动通过本地 HTTP API 抓取游戏数据（支持 `gosumemory` / `tosu`）。
  - **自动拉起与清理**：进入 osu! 模式时若检测到接口未运行，自动查找并后台拉起 `gosumemory.exe` 或 `tosu.exe`，退出模式时自动结束进程。
  - 支持数据：实时 PP、准确率、Combo、最大 Combo、击中判定（300/100/50）、星数、BPM、HP 槽、模组（mods）及评级。
- **东方丰富内存数据解析**：
  - 支持读取分数（自动还原倍率）、单次死亡与炸弹统计（灵击优化）、机体与子弹类型、擦弹、火力值、得分道具（PIV）、最大樱点（TH07）及实时符卡名/符卡 ID。
- **Material Design 3 风格与动态主题色**：
  - 采用 MD3 扁平化面板设计与调色盘自定义颜色选取。
  - 支持根据明暗度自适应前景色计算，实时重绘并应用主题，在 `settings.json` 中保存配置。
- **基址解析与高稳定性**：完美适配 Steam 版本等启用了 ASLR 的游戏模块。基于 JNA 的只读型安全内存访问，杜绝游戏崩溃。
- **模块化结构**：将大型 `Main.kt` 拆分为 `UiComponents.kt`、`Settings.kt`、`Scanner.kt`、`OsuTelemetry.kt` 及 `MainWindow.kt`，大幅提升二次开发与维护效率。

### 📡 VRChat OSC 参数表

#### 1. 东方模式参数表
推送到本地的 `127.0.0.1:9000` 端口：

| 参数名称 | 数据类型 | 描述 |
| :--- | :--- | :--- |
| `/avatar/parameters/TouhouGameID` | `Int` | 运行游戏在支持列表中的索引（从 0 开始）。 |
| `/avatar/parameters/TouhouGameName` | `String` | 当前运行游戏的完整名称。 |
| `/avatar/parameters/TouhouScore` | `Int` | 游戏当前分数（已还原倍率）。 |
| `/avatar/parameters/TouhouMiss` | `Int` | 当前游玩会话中的累计死亡次数（Miss数）。 |
| `/avatar/parameters/TouhouBomb` | `Int` | 当前游玩会话中的累计炸弹使用次数。 |
| `/avatar/parameters/TouhouDifficulty` | `Int` | 难度的数值映射：`0` (Easy) 到 `5` (Phantasm)。 |
| `/avatar/parameters/TouhouDifficultyName` | `String` | 难度的文本名称（例如 `Lunatic`）。 |
| `/avatar/parameters/TouhouCharacter` | `Int` | 人物角色 ID 索引。 |
| `/avatar/parameters/TouhouSubshot` | `Int` | 子弹类型 ID 索引。 |
| `/avatar/parameters/TouhouCharacterName` | `String` | 角色与子弹类型文本名称（例如 `Reimu A (Homing)`）。 |
| `/avatar/parameters/TouhouStage` | `Int` | 标准化后的关卡数：`1`-`6` 为主线关卡，`7` 为 Extra，`8` 为 Phantasm。 |
| `/avatar/parameters/TouhouGraze` | `Int` | 当前擦弹数（Graze）。 |
| `/avatar/parameters/TouhouPower` | `Float` | 当前火力值（标准化后映射至 `0.0` - `4.0` / `8.0`）。 |
| `/avatar/parameters/TouhouPowerRaw` | `Int` | 内存中原始的火力值整数。 |
| `/avatar/parameters/TouhouPoint` | `Int` | 当前得分道具价值（PIV）。 |
| `/avatar/parameters/TouhouCherryMax` | `Int` | 当前最大樱点（TH07）。 |
| `/avatar/parameters/TouhouSpellID` | `Int` | 当前正在交战的符卡 ID（未处于符卡战时为 `-1`）。 |
| `/avatar/parameters/TouhouSpellActive` | `Bool` | 当前是否处于符卡战中。 |
| `/avatar/parameters/TouhouSpellName` | `String` | 正在交战的符卡名称。 |
| `/chatbox/input` | `String, Bool, Bool` | 发送格式化文本：`正在玩: <游戏名> [<难度>] | 机体: <机体> | 关卡: <关卡> | 分数: <分数> | Miss: <死亡数> | Bomb: <炸弹数>` |

#### 2. osu! 模式参数表
推送到本地的 `127.0.0.1:9000` 端口：

| 参数名称 | 数据类型 | 描述 |
| :--- | :--- | :--- |
| `/avatar/parameters/OsuStatus` | `Int` | 游玩状态：`2` (Menu), `4` (Playing) 等。 |
| `/avatar/parameters/OsuScore` | `Int` | 谱面当前得分。 |
| `/avatar/parameters/OsuCombo` | `Int` | 当前连击数。 |
| `/avatar/parameters/OsuMaxCombo` | `Int` | 当前谱面达到的最大连击数。 |
| `/avatar/parameters/OsuAccuracy` | `Float` | 游玩准确率（已归一化，范围为 `0.0f` 至 `1.0f`）。 |
| `/avatar/parameters/OsuMiss` | `Int` | 谱面当前 Miss 次数。 |
| `/avatar/parameters/OsuGrade` | `String` | 当前评级字母（例如 `SS`, `SH`, `A`）。 |
| `/avatar/parameters/OsuBPM` | `Float` | 谱面的每分钟节拍数（BPM）。 |
| `/avatar/parameters/OsuStars` | `Float` | 模组折算后的谱面星数（SR）。 |
| `/avatar/parameters/OsuHP` | `Float` | 归一化后的血条数值（范围为 `0.0f` - `1.0f`）。 |
| `/avatar/parameters/OsuPPCurrent` | `Float` | 实时计算已获得的 Performance Points (PP)。 |
| `/avatar/parameters/OsuPPFC` | `Float` | Full Combo（无失误）状态下的理论最大 PP。 |
| `/avatar/parameters/OsuModsNum` | `Int` | 活动模组的原始二进制位移表示整数。 |
| `/avatar/parameters/OsuModsStr` | `String` | 活动模组文本表现（例如 `HDDT`）。 |
| `/avatar/parameters/OsuHit300` | `Int` | 300 击中次数。 |
| `/avatar/parameters/OsuHit100` | `Int` | 100 击中次数. |
| `/avatar/parameters/OsuHit50` | `Int` | 50 击中次数。 |
| `/chatbox/input` | `String, Bool, Bool` | 发送格式化文本：`[osu!] 正在玩: <Artist> - <Title> [<Diff>] (<Stars>*) [<Mods>] | Combo: <Combo>x | PP: <PP>/<PP_FC> | Acc: <Acc>% | Miss: <Miss>` |

### 🛠️ 手动构建
- **前提条件**：已安装 JDK 17 或更高版本。
- **构建 Fat JAR**：
  ```powershell
  ./gradlew shadowJar
  ```
- **构建独立免安装 EXE**：
  ```powershell
  ./gradlew jpackage
  ```

---

## 日本語

**THOSC_BOX** は、東方Projectの公式メインライン原作ゲーム（TH06〜TH20）および **osu!** 向けに設計された高精度な VRChat OSC 送信ツールです。ゲームメモリやローカル HTTP 遥測 API からスコア、被弾数（Miss）、ボム使用数、ステージ数、難易度、およびリアルタイム PP などを直接読み込み、OSC プロトコル経由で VRChat に送信することで、アバターパラメータを動的に制御したり、VRChat のチャットボックス（Chatbox）に現在のリアルタイムステータスを表示することができます。

### 🌟 主な機能
- **複数モードの動的切り替え**：UI ヘッダーのボタンにより、**東方モード** と **osu! モード** をワンクリックで切り替えます。
- **幅広い公式作品に対応**：**東方紅魔郷 (TH06)** から最新作 **東方錦上京 (TH20)** までの計 15 作品に対応。
- **osu! ライブデータの同期**：
  - ローカル HTTP API (`gosumemory` / `tosu`) からゲーム情報を自動で取得します。
  - **自動起動と終了**：osu! モード開始時にツールがバックグラウンドで `gosumemory.exe` や `tosu.exe` を自動検索・起動し、終了時にそのプロセスを自動でクリーンアップします。
  - 統計機能：リアルタイム PP、精度、コンボ、判定数 (300/100/50)、星数、BPM、HP ゲージ、アクティブ Mod (文字列/整数)、および現在のランク。
- **東方メモリ拡張データ解析**：
  - スコア、累計被弾数、ボム使用数、ステージ数、難易度、キャラクター/装備、グレイズ（Graze）、パワー（Power）、得点アイテム（PIV）、最大桜点（TH07）、および交戦中のスペルカード ID・名前をリアルタイム取得。
- **MD3 UI テーマカスタマイザー**：
  - 設定パネルからテーマカラーを選択できます。カスタム Hex コードと MD3 パレットグリッドに対応。明るさに応じて前景色を自動計算し、UI コンポーネントの色調を瞬時に反映、`settings.json` に設定を保存します。
- **ASLR 対応と高安定性**：Steam 版などの ASLR 基址の自動解析。JNA を用いた安全な読み取り専用メモリ参照により、ゲームをクラッシュさせたり負荷を与えることはありません。
- **モジュール化されたソースコード**：巨大な `Main.kt` は `UiComponents.kt`、`Settings.kt`、`Scanner.kt`、`OsuTelemetry.kt`、および `MainWindow.kt` に綺麗に分割され、拡張性と保守性を向上しました。

### 📡 VRChat OSC パラメーター

#### 1. 東方モード パラメーター
`127.0.0.1:9000` 宛てに送信されます：

| パラメーター名 | データ型 | 説明 |
| :--- | :--- | :--- |
| `/avatar/parameters/TouhouGameID` | `Int` | サポート一覧における該当ゲームのインデックス（0開始）。 |
| `/avatar/parameters/TouhouGameName` | `String` | 実行中のゲームのタイトル。 |
| `/avatar/parameters/TouhouScore` | `Int` | 現在のゲームスコア（倍率復元済み）。 |
| `/avatar/parameters/TouhouMiss` | `Int` | 現在のプレイセッションでの累計被弾数（Miss数）。 |
| `/avatar/parameters/TouhouBomb` | `Int` | 現在のプレイセッションでの累計ボム使用回数。 |
| `/avatar/parameters/TouhouDifficulty` | `Int` | 難易度の数値マップ：`0` (Easy) から `5` (Phantasm)。 |
| `/avatar/parameters/TouhouDifficultyName` | `String` | 難易度の表示名（例：`Lunatic`）。 |
| `/avatar/parameters/TouhouCharacter` | `Int` | キャラクター ID インデックス。 |
| `/avatar/parameters/TouhouSubshot` | `Int` | サブショット ID インデックス。 |
| `/avatar/parameters/TouhouCharacterName` | `String` | キャラクターおよび装備の表示名（例：`Reimu A (Homing)`）。 |
| `/avatar/parameters/TouhouStage` | `Int` | 標準化されたステージ数：`1`〜`6`（主線）、`7`（Extra）、`8`（Phantasm）。 |
| `/avatar/parameters/TouhouGraze` | `Int` | 現在のグレイズ数（Graze）。 |
| `/avatar/parameters/TouhouPower` | `Float` | 現在のパワー（`0.0` - `4.0` / `8.0` に正規化）。 |
| `/avatar/parameters/TouhouPowerRaw` | `Int` | メモリ内の生のパワー値整数。 |
| `/avatar/parameters/TouhouPoint` | `Int` | 得点アイテム値（PIV）。 |
| `/avatar/parameters/TouhouCherryMax` | `Int` | 現在の最大桜点（TH07）。 |
| `/avatar/parameters/TouhouSpellID` | `Int` | 対戦中のスペルカード ID（通常時は `-1`）。 |
| `/avatar/parameters/TouhouSpellActive` | `Bool` | スペルカード戦中かどうかの真偽値。 |
| `/avatar/parameters/TouhouSpellName` | `String` | 対戦中のスペルカードの名前。 |
| `/chatbox/input` | `String, Bool, Bool` | ステータステキスト送信例：`プレイ中: <ゲーム名> [<難易度>] | 自機: <自機> | ステージ: <ステージ> | スコア: <スコア> | 被弾: <被弾数> | ボム: <ボム数>` |

#### 2. osu! モード パラメーター
`127.0.0.1:9000` 宛てに送信されます：

| パラメーター名 | データ型 | 説明 |
| :--- | :--- | :--- |
| `/avatar/parameters/OsuStatus` | `Int` | プレイステータス：`2` (Menu), `4` (Playing) など。 |
| `/avatar/parameters/OsuScore` | `Int` | 譜面の現在スコア。 |
| `/avatar/parameters/OsuCombo` | `Int` | 現在のコンボ数。 |
| `/avatar/parameters/OsuMaxCombo` | `Int` | 現在の譜面で達成した最大コンボ数。 |
| `/avatar/parameters/OsuAccuracy` | `Float` | プレイ精度（`0.0f` 〜 `1.0f` に正規化）。 |
| `/avatar/parameters/OsuMiss` | `Int` | 譜面の現在ミス回数。 |
| `/avatar/parameters/OsuGrade` | `String` | 現在の評価（例：`SS`, `SH`, `A`）。 |
| `/avatar/parameters/OsuBPM` | `Float` | 譜面の Beats Per Minute (BPM)。 |
| `/avatar/parameters/OsuStars` | `Float` | Mod 適用後の難易度（星数 SR）。 |
| `/avatar/parameters/OsuHP` | `Float` | 正規化された残り HP ゲージ（`0.0f` - `1.0f`）。 |
| `/avatar/parameters/OsuPPCurrent` | `Float` | リアルタイムで獲得したパフォーマンスポイント (PP)。 |
| `/avatar/parameters/OsuPPFC` | `Float` | フルコンボ（ノーミス）達成時の理論最大 PP。 |
| `/avatar/parameters/OsuModsNum` | `Int` | アグティブな Mod のビットマスク整数。 |
| `/avatar/parameters/OsuModsStr` | `String` | アグティブな Mod の文字列（例：`HDDT`）。 |
| `/avatar/parameters/OsuHit300` | `Int` | 300 ヒット数。 |
| `/avatar/parameters/OsuHit100` | `Int` | 100 ヒット数。 |
| `/avatar/parameters/OsuHit50` | `Int` | 50 ヒット数。 |
| `/chatbox/input` | `String, Bool, Bool` | ステータステキスト送信例：`[osu!] プレイ中: <Artist> - <Title> [<Diff>] (<Stars>*) [<Mods>] | Combo: <Combo>x | PP: <PP>/<PP_FC> | Acc: <Acc>% | Miss: <Miss>` |

### 🛠️ ソースコードからのビルド
- **システム要件**: JDK 17 以上。
- **Fat JAR のビルド**:
  ```powershell
  ./gradlew shadowJar
  ```
- **スタンドアロン EXE のビルド**:
  ```powershell
  ./gradlew jpackage
  ```

---

## Supported Games List | 支持的游戏列表 | 対応ゲーム一覧

1. **TH06**: Embodiment of Scarlet Devil | 东方红魔乡 | 東方紅魔郷 (`th06.exe` / `th06e.exe`)
2. **TH07**: Perfect Cherry Blossom | 东方妖妖梦 | 東方妖々夢 (`th07.exe`)
3. **TH08**: Imperishable Night | 东方永夜抄 | 東方永夜抄 (`th08.exe`)
4. **TH09**: Phantasmagoria of Flower View | 东方花映冢 | 東方花映塚 (`th09.exe`)
5. **TH10**: Mountain of Faith | 东方风神录 | 東方風神録 (`th10.exe`)
6. **TH11**: Subterranean Animism | 东方地灵殿 | 東方地霊殿 (`th11.exe`)
7. **TH12**: Undefined Fantastic Object | 东方星莲船 | 東方星蓮船 (`th12.exe`)
8. **TH13**: Ten Desires | 东方神灵庙 | 東方神霊廟 (`th13.exe`)
9. **TH14**: Double Dealing Character | 东方辉针城 | 東方輝針城 (`th14.exe`)
10. **TH15**: Legacy of Lunatic Kingdom | 东方绀珠传 | 東方紺珠伝 (`th15.exe`)
11. **TH16**: Hidden Star in Four Seasons | 东方天空璋 | 東方天空璋 (`th16.exe`)
12. **TH17**: Wily Beast and Weakest Creature | 东方鬼形兽 | 東方鬼形獣 (`th17.exe`)
18. **TH18**: Unconnected Marketeers | 东方虹龙洞 | 東方虹龍洞 (`th18.exe`)
14. **TH19**: Unfinished Dream of All Living Ghost | 东方兽王园 | 東方獣王園 (`th19.exe`)
15. **TH20**: Fossilized Wonders | 东方锦上京 | 東方錦上京 (`th20.exe`)

---

## Future Outlook | 后期期望 | 将来の展望

- **English**: In the future, we hope to extend compatibility and adapt memory reading offsets to support popular Touhou fangames and secondary creations.
- **简体中文**：后期期望在未来支持并适配热门的东方二次创作同人游戏的数据读取。
- **日本語**：将来の展望として、今後は人気の高い東方二次創作・同人ゲームのデータ読み込みへの対応も計画しています。

---

## License | 许可证 | ライセンス

Copyright (C) 2026 Sanae-Koishi

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

本项目采用 GNU General Public License v3.0 (GPLv3) 许可证开源 - 详情请参阅 [LICENSE](LICENSE) 文件。

このプロジェクトは GNU General Public License v3.0 (GPLv3) の下でライセンスされています。詳細は [LICENSE](LICENSE) ファイルをご覧ください。
