# THOSC_BOX - Touhou Project VRChat OSC Bridge | 东方Project VRChat OSC 桥接工具 | 東方Project VRChat OSC 送信ツール

[English](#english) | [简体中文](#简体中文) | [日本語](#日本語)

---

## English

**THOSC_BOX** is a high-performance VRChat OSC bridge designed for official mainline Touhou Project games (TH06 through TH20). It reads real-time gameplay statistics directly from the game's memory and transmits them via OSC (Open Sound Control) protocol to VRChat to drive avatar parameters and display your live gameplay status in the VRChat Chatbox.

### 🌟 Key Features
- **Wide Mainline Game Support**: Seamlessly supports 15 mainline Touhou games from **TH06 (Embodiment of Scarlet Devil)** up to **TH20 (Fossilized Wonders)**.
- **Real-Time Data Extraction**:
  - **Score**: Real-time extraction with automatic scaling multiplier recovery.
  - **Miss & Bomb Tracking**: Automatically calculates cumulative session deaths (misses) and bomb usages (resets on restart/new game, optimized for TH10/TH11 power-based bombs).
  - **Stage & Difficulty**: Auto-detects play difficulty (Easy, Normal, Hard, Lunatic, Extra, Phantasm) and active stage number.
- **ASLR Compatibility**: Dynamic memory offset resolution to fully support Steam versions with ASLR enabled.
- **Stable & Lightweight**: Built with highly compatible process detection and JNA-based memory access, ensuring zero crashes or performance impact on the running game.

### 📡 VRChat OSC Parameters
THOSC_BOX broadcasts OSC messages to local port `127.0.0.1:9000` (VRChat's default OSC input port) every 2 seconds:

| Parameter Name | Data Type | Description |
| :--- | :--- | :--- |
| `/avatar/parameters/TouhouGameID` | `Int` | 0-indexed index of the active game in the supported list. |
| `/avatar/parameters/TouhouGameName` | `String` | Complete title of the running game. |
| `/avatar/parameters/TouhouScore` | `Int` | Current in-game score (fully recovered value). |
| `/avatar/parameters/TouhouMiss` | `Int` | Cumulative player deaths (misses) in the current session. |
| `/avatar/parameters/TouhouBomb` | `Int` | Cumulative bomb usages in the current session. |
| `/avatar/parameters/TouhouDifficulty` | `Int` | Numeric value of difficulty: `0` (Easy), `1` (Normal), `2` (Hard), `3` (Lunatic), `4` (Extra), `5` (Phantasm). |
| `/avatar/parameters/TouhouDifficultyName` | `String` | Text name of the difficulty (e.g. `Normal`, `Lunatic`). |
| `/avatar/parameters/TouhouStage` | `Int` | Normalized active stage number: `1`-`6` for main stages, `7` for Extra, `8` for Phantasm. |
| `/chatbox/input` | `String, Bool, Bool` | Sends a formatted status text to VRChat Chatbox. Example: <br>`Game: <GameName> [<Difficulty>] | Stage: <Stage> | Score: <Score> | Miss: <Miss> | Bomb: <Bomb>` |

### 🛠️ Building from Source
- **Requirements**: JDK 17 or higher.
- **Build shadow JAR**:
  ```powershell
  ./gradlew shadowJar
  ```
  Output file: `build/libs/THOSC_BOX-1.0-SNAPSHOT-all.jar`
- **Build Standalone EXE**:
  ```powershell
  ./gradlew jpackage
  ```
  Output path: `build/jpackage/THOSC_BOX/` (includes the standalone EXE and minimal Java runtime).

### 🚀 Running the App
- **Option 1 (Pre-compiled Release - Recommended)**: Download the latest archive from the [GitHub Releases](https://github.com/XZto502/THOSC_BOX/releases) page, extract it, and double-click `THOSC_BOX.exe`.
- **Option 2 (Build locally)**: Go to `build/jpackage/THOSC_BOX/` and run `THOSC_BOX.exe`.
- **Option 3 (Run JAR)**: Run the shadow JAR from the terminal:
  ```powershell
  java -Dfile.encoding=UTF-8 -jar build/libs/THOSC_BOX-1.0-SNAPSHOT-all.jar
  ```

---

## 简体中文

**THOSC_BOX** 是一款专为《东方Project》官方正作（TH06 至 TH20）设计的 VRChat OSC 桥接工具。它通过读取运行中的游戏内存，实时将分数、累计死亡次数（Miss数）、累计炸弹使用次数（Bomb数）、当前关卡（Stage）及游戏难度（Difficulty）等数据，通过 OSC 协议发送至 VRChat，用以动态驱动 Avatar 虚拟形象的参数，并同步推送实时状态至聊天框（Chatbox）。

### 🌟 功能特性
- **支持作品广泛**：原生无缝支持从 **TH06《东方红魔乡》** 到最新作 **TH20《东方锦上京》** 的共 15 部 mainline 官方正作。
- **实时核心数据提取**：
  - **分数 (Score)**：实时读取并自动完成倍率还原。
  - **死亡与炸弹统计**：自动计算单次会话中的累计死亡次数和炸弹使用数（开始新游戏或重开时自动清零，并针对 TH10/TH11 消耗 Power 释放灵击的特色机制进行了优化判定）。
  - **关卡与难度**：自动获取当前游玩难度（Easy, Normal, Hard, Lunatic, Extra, Phantasm）和当前关卡面数。
- **ASLR 动态基址解析**：完美适配 Steam 版本等启用了 ASLR（地址空间配置随机化）的游戏模块基址。
- **稳定与轻量化**：基于 JNA实现进程检测与安全只读式内存访问，对游戏无任何性能影响，绝不引起游戏崩溃。

### 📡 VRChat OSC 参数表
工具默认每 2 秒将以下参数推送到本地的 `127.0.0.1:9000` 端口：

| 参数名称 | 数据类型 | 描述 |
| :--- | :--- | :--- |
| `/avatar/parameters/TouhouGameID` | `Int` | 游戏在支持列表中的索引（从 0 开始）。 |
| `/avatar/parameters/TouhouGameName` | `String` | 当前运行游戏的完整名称。 |
| `/avatar/parameters/TouhouScore` | `Int` | 游戏当前分数（已还原倍率）。 |
| `/avatar/parameters/TouhouMiss` | `Int` | 当前游玩会话中的累计死亡次数（Miss数）。 |
| `/avatar/parameters/TouhouBomb` | `Int` | 当前游玩会话中的累计炸弹使用次数（已适配 TH10/11 灵击）。 |
| `/avatar/parameters/TouhouDifficulty` | `Int` | 难度的数值映射：`0` (Easy), `1` (Normal), `2` (Hard), `3` (Lunatic), `4` (Extra), `5` (Phantasm)。 |
| `/avatar/parameters/TouhouDifficultyName` | `String` | 难度的文本名称（例如 `Normal`, `Lunatic`）。 |
| `/avatar/parameters/TouhouStage` | `Int` | 标准化后的关卡数：`1`-`6` 为主线关卡，`7` 为 Extra，`8` 为 Phantasm。 |
| `/chatbox/input` | `String, Bool, Bool` | 发送格式化后的游玩状态文本至聊天框。例如：<br>`Game: <游戏名> [<难度>] | Stage: <关卡> | Score: <分数> | Miss: <死亡数> | Bomb: <炸弹数>` |

### 🛠️ 手动构建
- **前提条件**：已安装 JDK 17 或更高版本。
- **构建 Fat JAR**：
  ```powershell
  ./gradlew shadowJar
  ```
  输出文件：`build/libs/THOSC_BOX-1.0-SNAPSHOT-all.jar`
- **构建独立免安装 EXE**：
  ```powershell
  ./gradlew jpackage
  ```
  输出路径：`build/jpackage/THOSC_BOX/`（包含生成的 `THOSC_BOX.exe` 及内置的轻量级运行时环境）。

### 🚀 如何运行
- **方式一（下载发行版 - 推荐）**：直接从 [GitHub Releases](https://github.com/XZto502/THOSC_BOX/releases) 页面下载最新编译好的压缩包，解压后双击运行 `THOSC_BOX.exe`。
- **方式二（手动构建版）**：构建完成后，直接双击 `build/jpackage/THOSC_BOX/` 目录下的 `THOSC_BOX.exe`。
- **方式三（命令行 JAR）**：使用命令行终端运行：
  ```powershell
  java -Dfile.encoding=UTF-8 -jar build/libs/THOSC_BOX-1.0-SNAPSHOT-all.jar
  ```

---

## 日本語

**THOSC_BOX** は、東方Projectの公式メインライン原作ゲーム（TH06〜TH20）向けに設計された高精度な VRChat OSC 送信ツールです。ゲームメモリからスコア、被弾数（Miss）、ボム使用数、ステージ数、および難易度をリアルタイムに直接読み込み、OSC プロトコル経由で VRChat に送信することで、アバターパラメータを動的に制御したり、VRChat のチャットボックス（Chatbox）に現在のリアルタイムステータスを表示することができます。

### 🌟 主な機能
- **幅広い公式作品に対応**：**東方紅魔郷 (TH06)** から最新作 **東方錦上京 (TH20)** までの計15作品にネイティブ対応しています。
- **リアルタイムなゲームデータ同期**：
  - **スコア (Score)**：メモリからスコアを取得し、ゲームごとの倍率を自動で復元。
  - **被弾（Miss）とボム（Bomb）の累積カウント**：ゲーム開始・リトライ時に自動リセットされる累計被弾数・ボム使用数を集計（TH10/TH11のパワー消費型ボムも最適化判定）。
  - **ステージと難易度**：現在の難易度（Easy, Normal, Hard, Lunatic, Extra, Phantasm）および現在のステージ数を自動検知。
- **ASLR 対応**：Steam版などの ASLR（アドレス空間配置のランダム化）モジュールベース自動解析。
- **軽量かつ高安定性**：JNAを用いた安全な読み取り専用メモリ書き込み不要のアプローチを採用。ゲームをクラッシュさせたり負荷を与えることはありません。

### 📡 VRChat OSC パラメーター
本ツールはデフォルトで2秒ごとに以下のパラメーターを `127.0.0.1:9000`（VRChatのデフォルト入力ポート）へ送信します：

| パラメーター名 | データ型 | 説明 |
| :--- | :--- | :--- |
| `/avatar/parameters/TouhouGameID` | `Int` | サポート一覧における該当ゲームのインデックス（0開始）。 |
| `/avatar/parameters/TouhouGameName` | `String` | 実行中のゲームのタイトル。 |
| `/avatar/parameters/TouhouScore` | `Int` | 現在のゲームスコア（倍率復元済み）。 |
| `/avatar/parameters/TouhouMiss` | `Int` | 現在のプレイセッションでの累計被弾数（Miss数）。 |
| `/avatar/parameters/TouhouBomb` | `Int` | 現在のプレイセッションでの累計ボム使用回数（TH10/11の霊撃もカウント）。 |
| `/avatar/parameters/TouhouDifficulty` | `Int` | 難易度の数値マップ：`0` (Easy), `1` (Normal), `2` (Hard), `3` (Lunatic), `4` (Extra), `5` (Phantasm)。 |
| `/avatar/parameters/TouhouDifficultyName` | `String` | 難易度の表示名（例：`Normal`, `Lunatic`）。 |
| `/avatar/parameters/TouhouStage` | `Int` | 標準化された現在のステージ数：`1`〜`6`（通常ステージ）、`7`（Extra）、`8`（Phantasm）。 |
| `/chatbox/input` | `String, Bool, Bool` | 現在のゲームステータステキストを Chatbox へ送信します。出力例：<br>`Game: <ゲーム名> [<難易度>] | Stage: <ステージ> | Score: <スコア> | Miss: <被弾数> | Bomb: <ボム数>` |

### 🛠️ ソースコードからのビルド
- **システム要件**: JDK 17 以上。
- **Fat JAR のビルド**:
  ```powershell
  ./gradlew shadowJar
  ```
  生成ファイル: `build/libs/THOSC_BOX-1.0-SNAPSHOT-all.jar`
- **スタンドアロン EXE のビルド**:
  ```powershell
  ./gradlew jpackage
  ```
  生成ディレクトリ: `build/jpackage/THOSC_BOX/` (独立動作可能な EXE と軽量 JRE ランタイムが含まれます)。

### 🚀 実行方法
- **方法 1（リリース版をダウンロード - 推奨）**: [GitHub Releases](https://github.com/XZto502/THOSC_BOX/releases) ページから最新ビルド済みの ZIP をダウンロードし、解凍して `THOSC_BOX.exe` をダブルクリックして起動します。
- **方法 2（自分でビルドした EXE）**: ビルド後、`build/jpackage/THOSC_BOX/` ディレクトリ内の `THOSC_BOX.exe` を起動します。
- **方法 3（JAR ファイルによる起動）**: 端末から以下のように起動します：
  ```powershell
  java -Dfile.encoding=UTF-8 -jar build/libs/THOSC_BOX-1.0-SNAPSHOT-all.jar
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
