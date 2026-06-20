# THOSC_BOX - Touhou Project VRChat OSC Bridge | 东方Project VRChat OSC 桥接工具 | 東方Project VRChat OSC 送信ツール

[English](#english) | [简体中文](#简体中文) | [日本語](#日本語)

---

## English

**THOSC_BOX** is a VRChat OSC bridge designed for official Touhou Project games (TH06 through TH20). It reads statistics such as Score, cumulative Misses (deaths), and Bomb usage directly from the game's memory and transmits them to VRChat to dynamically control avatar parameters.

### Features
- **Broad Game Support**: Works out of the box with 15 mainline games from **TH06 (Embodiment of Scarlet Devil)** up to **TH20 (Fossilized Wonders)**.
- **Smart Stat Tracking**: Automatically calculates your cumulative deaths and bomb usage during a play session (resets automatically when a new game starts).
- **Game Name Broadcast**: Automatically detects and sends the title of the game you are playing.
- **Stable Memory Reading**: Uses highly compatible methods to locate game processes and retrieve values safely without crashing the game.

### VRChat OSC Parameters

THOSC_BOX sends OSC messages to `127.0.0.1:9000` (VRChat's default OSC port):

| Parameter Name | Data Type | Description |
|---|---|---|
| `/avatar/parameters/TouhouGameID` | `Int` | Index of the running game in the supported list (0-indexed). |
| `/avatar/parameters/TouhouGameName` | `String` | Title of the active game (e.g. `东方风神录 ~ Mountain of Faith`). |
| `/avatar/parameters/TouhouScore` | `Int` | Current in-game score. |
| `/avatar/parameters/TouhouMiss` | `Int` | Cumulative death (miss) count in the current session. |
| `/avatar/parameters/TouhouBomb` | `Int` | Cumulative bomb usage count (optimized for power-based bombs in TH10/11). |
| `/chatbox/input` | `String, Bool, Bool` | Sends the current live game status (Game Name, Score, Miss, Bomb) to the VRChat Chatbox. |

### How to Build
- **Prerequisites**: JDK 17 or higher.
- **Build Fat JAR**: 
  ```powershell
  ./gradlew shadowJar
  ```
  Generates `build/libs/THOSC_BOX-1.0-SNAPSHOT-all.jar`.
- **Build Standalone EXE**: 
  ```powershell
  ./gradlew jpackage
  ```
  Generates a Windows application folder at `build/jpackage/THOSC_BOX`.

### How to Run
- **Option 1 (Download Release - Recommended)**: Download the pre-compiled package from the GitHub Releases page, extract it, and double-click `THOSC_BOX.exe`.
- **Option 2 (Build from Source)**: After building from source, double-click `THOSC_BOX.exe` in the `build/jpackage/THOSC_BOX/` folder.
- **Option 3 (JAR File)**: Run the compiled JAR from your terminal:
  ```powershell
  java -jar build/libs/THOSC_BOX-1.0-SNAPSHOT-all.jar
  ```

---

## 简体中文

**THOSC_BOX** 是一款专为《东方Project》官方正作（TH06 至 TH20）设计的 VRChat OSC 桥接工具。它通过直接读取游戏内存，实时将分数、累计 Miss 数（死亡次数）和 Bomb 使用次数发送给 VRChat，以动态控制虚拟形象的各项参数。

### 功能特性
- **游戏支持广泛**：无缝支持从 **TH06《东方红魔乡》** 到最新作 **TH20《东方锦上京》** 的共 15 部官方正作。
- **智能统计追踪**：自动计算单次游玩会话中累计死亡数和炸弹使用数（开始新游戏时会自动清零并重新开始计数）。
- **游戏名称广播**：自动识别当前运行的游戏标题并实时发送。
- **稳定安全读取**：采用多重高兼容性内存读取方案，保证稳定获取数据的同时绝不引起游戏进程崩溃。

### VRChat OSC 参数表

工具会将以下参数推送到本地的 `127.0.0.1:9000` 端口：

| 参数名称 | 数据类型 | 描述 |
|---|---|---|
| `/avatar/parameters/TouhouGameID` | `Int` | 游戏在支持列表中的索引（从 0 开始）。 |
| `/avatar/parameters/TouhouGameName` | `String` | 当前运行游戏的完整名称（例如 `东方风神录 ~ Mountain of Faith`）。 |
| `/avatar/parameters/TouhouScore` | `Int` | 游戏当前分数。 |
| `/avatar/parameters/TouhouMiss` | `Int` | 当前游玩会话中的累计死亡次数（Miss数）。 |
| `/avatar/parameters/TouhouBomb` | `Int` | 当前游玩会话中的累计炸弹使用次数（已适配 TH10/11 的灵击机制）。 |
| `/chatbox/input` | `String, Bool, Bool` | 将当前实时游戏状态（游戏名称、分数、死亡数、炸弹数）发送至 VRChat 聊天框。 |

### 如何构建
- **前提条件**：已安装 JDK 17 或更高版本。
- **构建 Fat JAR**：
  ```powershell
  ./gradlew shadowJar
  ```
  输出文件：`build/libs/THOSC_BOX-1.0-SNAPSHOT-all.jar`。
- **构建独立 EXE**：
  ```powershell
  ./gradlew jpackage
  ```
  输出路径：`build/jpackage/THOSC_BOX`。

### 如何运行
- **方式一（下载发行版 - 推荐）**：直接从 GitHub Releases 页面下载最新编译好的压缩包，解压后双击运行 `THOSC_BOX.exe`。
- **方式二（手动构建 EXE）**：在自己构建后，直接双击 `build/jpackage/THOSC_BOX/` 目录下的 `THOSC_BOX.exe`。
- **方式三（JAR 包）**：使用命令行运行：
  ```powershell
  java -jar build/libs/THOSC_BOX-1.0-SNAPSHOT-all.jar
  ```

---

## 日本語

**THOSC_BOX** は、東方Projectの原作ゲーム（TH06〜TH20）向けに設計された VRChat OSC 送信ツールです。ゲームメモリからスコア、累計被弾数（Miss）、ボム使用数をリアルタイムに読み取り、OSC経由で VRChat に送信することでアバターのギミックやパラメーターを動的に制御できます。

### 機能特徴
- **幅広い作品に対応**：**東方紅魔郷 (TH06)** から最新作 **東方錦上京 (TH20)** までの計15作品に対応しています。
- **スマートな統計追跡**：プレイ中の被弾回数とボム使用回数を自動的に累積カウントします（新しいゲームを開始すると自動で 0 にリセットされます）。
- **ゲームタイトルの自動送信**：現在プレイしている東方作品の名前を自動で検出して送信します。
- **高い安定性**：ゲームの動作に影響を与えない互換性の高いメモリ読み取り方法を採用しています。

### VRChat OSC パラメーター

本ツールは以下のパラメーターを `127.0.0.1:9000`（VRChatのデフォルトOSCポート）へ送信します：

| パラメーター名 | データ型 | 説明 |
|---|---|---|
| `/avatar/parameters/TouhouGameID` | `Int` | サポート一覧におけるゲームのインデックス（0開始）。 |
| `/avatar/parameters/TouhouGameName` | `String` | 実行中のゲーム名（例：`东方风神录 ~ Mountain of Faith`）。 |
| `/avatar/parameters/TouhouScore` | `Int` | 現在のゲームスコア。 |
| `/avatar/parameters/TouhouMiss` | `Int` | 現在のセッションでの累計被弾数（Miss数）。 |
| `/avatar/parameters/TouhouBomb` | `Int` | 現在のセッションでの累計ボム使用回数（TH10/11の霊撃もカウント）。 |
| `/chatbox/input` | `String, Bool, Bool` | 現在のリアルタイムなゲームステータス（ゲーム名、スコア、被弾数、ボム数）を VRChat チャットボックスへ送信します。 |

### ビルド方法
- **前提条件**: JDK 17 以上がインストールされていること。
- **Fat JAR のビルド**:
  ```powershell
  ./gradlew shadowJar
  ```
  生成ファイル: `build/libs/THOSC_BOX-1.0-SNAPSHOT-all.jar`
- **スタンドアロン EXE のビルド**:
  ```powershell
  ./gradlew jpackage
  ```
  生成ディレクトリ: `build/jpackage/THOSC_BOX`

### 実行方法
- **方法 1（リリース版をダウンロード - 推奨）**: GitHub Releases ページからビルド済みのパッケージをダウンロードし、解凍して `THOSC_BOX.exe` をダブルクリックして実行します。
- **方法 2（自分でビルドした EXE）**: ビルド後、`build/jpackage/THOSC_BOX/` ディレクトリ内の `THOSC_BOX.exe` をダブルクリックして実行します。
- **方法 3（JAR ファイル）**: コマンドラインから起動します：
  ```powershell
  java -jar build/libs/THOSC_BOX-1.0-SNAPSHOT-all.jar
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
13. **TH18**: Unconnected Marketeers | 东方虹龙洞 | 東方虹龍洞 (`th18.exe`)
14. **TH19**: Unfinished Dream of All Living Ghost | 东方兽王园 | 東方獣王園 (`th19.exe`)
15. **TH20**: Fossilized Wonders | 东方锦上京 | 东方锦上京 (`th20.exe`)

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
