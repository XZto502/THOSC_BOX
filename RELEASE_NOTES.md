# THOSC_BOX v1.0.0 Release Notes / 发布说明 / リリースノート

## 🇨🇳 中文 (Chinese)

### 📌 项目简介
**THOSC_BOX** 是一款专为东方 Project (Touhou Project) 玩家设计的开源 VRChat OSC 桥接工具。它可以实时读取运行中的东方 Project 游戏内存数据，并通过 OSC 协议将玩家的实时战绩、关卡状态、难度以及正在交战的符卡名称发送至 VRChat，显示在您角色的 Avatar 参数及上方 Chatbox 聊天框中。

### 🌟 当前已实现功能
1. **多款游戏深度适配**：配置并支持全系列 13 款官方 mainline 作品的内存读取：
   * 支持作品：TH06 (红魔乡)、TH07 (妖妖梦)、TH08 (永夜抄)、TH10 (风神录)、TH11 (地灵殿)、TH12 (星莲船)、TH13 (神灵庙)、TH14 (辉针城)、TH15 (绀珠传)、TH16 (天空璋)、TH17 (鬼形兽)、TH18 (虹龙洞)、TH20 (锦上京)。
2. **实时核心数据同步**：
   * **分数 (Score)**：实时提取并自动进行倍率还原。
   * **死亡数 (Miss) 与炸弹数 (Bomb) 增量计算**：在关卡重启或新游戏时自动清零，支持增量统计（非单纯读取剩余生命值），并针对 TH10/TH11 消耗 Power 释放 Bomb 的机制进行了判定优化。
   * **当前关卡 (Stage)**：精准识别并标准化输出当前关卡（如 `Stage 1-6`、`Extra`、`Phantasm` 等）。
   * **难度 (Difficulty)**：自动检测当前难度（Easy, Normal, Hard, Lunatic, Extra, Phantasm）。
3. **符卡显示功能 (Scheme B)**：
   * 自动监控活跃 de Boss 符卡战，当有符卡激活时显示其对应的**符卡名称**（首批支持 TH14, TH16, TH17, TH18 关底/关中 Boss 符卡名字）；
   * 未触发符卡战时（道中）自动回退显示为当前关卡名称（如 `Stage 1`）。
   * 使用 GameManager 静态区全局符卡 ID 偏移寻址，**彻底解决并修复了 Stage 1（一关 boss）符卡检测失效的问题**。
4. **VRChat 深度联动**：
   * 每 2 秒（2000ms 频率限制防护）向 VRChat Chatbox 自动推送状态文本：`Game: <游戏名> [<难度>] | Stage/Spell: <关卡/符卡> | Score: <分数> | Miss: <死亡数> | Bomb: <炸弹数>`。
   * 广播标准 OSC Avatar 参数，支持玩家在 VRChat 虚拟形象（Animator）中制作对应的实时状态面板。
5. **ASLR 兼容与绿色发布**：
   * 完美适配 Steam 版本的 ASLR 动态模块基址解析。
   * 打包为带有 Windows 系统默认应用图标的绿色免安装 EXE，即开即用。

---

### 🗺️ 下一版本预计实现功能
1. **完善全系列符卡名字映射**：补全 TH06-TH13、TH15 以及第 20 作 TH20 的所有 Boss 符卡中英文映射表。
2. **角色与机体检测**：读取并显示玩家当前选择的角色（如博丽灵梦、雾雨魔理沙）和武器机体类型。
3. **支持更多外传与衍生作品**：研究并适配 TH09 (花映冢)、TH19 (兽王园) 以及其他官方弹幕联机/外传作品。
4. **可视化配置界面 (GUI)**：提供轻量级窗口界面，允许用户自定义 OSC 端口、Chatbox 发送格式、是否开启特定参数广播等。

---

## 🇺🇸 English

### 📌 Project Introduction
**THOSC_BOX** is an open-source VRChat OSC bridge tool tailored for Touhou Project players. It reads running Touhou game memory in real-time and transmits live play data—including scores, death counts, bomb usages, current stages, play difficulties, and active Boss spell card names—directly to VRChat via the OSC protocol, rendering them on your Avatar parameters and Chatbox.

### 🌟 Currently Implemented Features
1. **Wide Game Compatibility**: Out-of-the-box support for 13 mainline Touhou Project titles:
   * Supported Games: TH06, TH07, TH08, TH10, TH11, TH12, TH13, TH14, TH15, TH16, TH17, TH18, and TH20.
2. **Real-time Core Data Sync**:
   * **Score**: Real-time extraction with auto-multiplier recovery.
   * **Incremental Miss & Bomb Counters**: Incremental statistics (resets on new game/restart) rather than remaining lives/bombs. Specially optimized for TH10/TH11's unique power-consuming bomb mechanic.
   * **Current Stage**: Detects and standardizes the stage value (e.g., `Stage 1-6`, `Extra`, `Phantasm`).
   * **Difficulty**: Automatically reads play difficulty (Easy, Normal, Hard, Lunatic, Extra, Phantasm).
3. **Active Spell Card Display (Scheme B)**:
   * Monitors active Boss fights and displays the exact **Spell Card Name** (initially mapped for TH14, TH16, TH17, and TH18).
   * Falls back to stage names (e.g., `Stage 1`) during regular gameplay sections (道中).
   * Resolved the Stage 1 spell card detection failure bug by utilizing static global GameManager offsets.
4. **VRChat Chatbox & Parameter Integration**:
   * Sends formatted text to VRChat Chatbox every 2 seconds (with rate-limit protection): `Game: <Name> [<Difficulty>] | Stage/Spell: <Stage/Spell> | Score: <Score> | Miss: <Miss> | Bomb: <Bomb>`.
   * Broadcasts standard OSC Avatar parameters to drive real-time dashboard UI in your Avatar's Animator.
5. **ASLR Compatibility & Native EXE Release**:
   * Dynamic base address resolution to fully support Steam versions with ASLR enabled.
   * Packaged as a portable standalone EXE utilizing the Windows native application icon.

---

### 🗺️ Planned Features for Next Release
1. **Complete Spell Card Mappings**: Complete spell card mapping tables for TH06-TH13, TH15, and TH20.
2. **Character & Shot Type Detection**: Read and display the active character (e.g., Reimu, Marisa) and selected weapon type.
3. **Expand Spin-off & Versus Game Support**: Research and adapt memory layouts for TH09 (Phantasmagoria of Flower View), TH19 (Unfinished Dream of All Living Ghost), and other versus titles.
4. **Graphical Configuration GUI**: Provide a lightweight user interface to easily customize OSC ports, Chatbox formatting, and toggles for parameter broadcasts.

---

## 🇯🇵 日本語 (Japanese)

### 📌 プロジェクト紹介
**THOSC_BOX** は、東方Projectのプレイヤー向けに設計されたオープンソースの VRChat OSC ブリッジツールです。起動中の東方Projectゲームのメモリデータをリアルタイムに読み込み、スコア、ミス（被弾数）、ボム使用数、現在のステージ、難易度、および戦闘中のスペルカード名を OSC プロトコル経由で VRChat に送信し、アバターパラメータや Chatbox にリアルタイム表示します。

### 🌟 現在実装されている機能
1. **多数の東方作品に対応**：計13の公式メインライン作品のメモリ読み込みに対応：
   * 対応作品：東方紅魔郷 (TH06)、東方妖々夢 (TH07)、東方永夜抄 (TH08)、東方風神録 (TH10)、東方地霊殿 (TH11)、東方星蓮船 (TH12)、東方神霊廟 (TH13)、東方輝針城 (TH14)、東方紺珠伝 (TH15)、東方天空璋 (TH16)、東方鬼形獣 (TH17)、東方虹龍洞 (TH18)、東方錦上京 (TH20)。
2. **リアルタイムなコアデータ同期**：
   * **スコア (Score)**：メモリからスコアを取得し、ゲームごとの倍率を自動で復元。
   * **ミス (Miss) とボム (Bomb) のインクリメンタル集計**：残機・残ボムの単純取得ではなく、ゲーム開始・リトライ時に自動リセットされる累計被弾数・ボム使用数を集計。TH10/TH11 のパワー消費型ボムも最適化判定。
   * **現在のステージ (Stage)**：進行度を判定し、`Stage 1-6` や `Extra`、`Phantasm` などに標準化して表示。
   * **難易度 (Difficulty)**：現在の難易度（Easy, Normal, Hard, Lunatic, Extra, Phantasm）を自動取得。
3. **アクティブスペルカード表示機能 (Scheme B)**：
   * ボス戦でのスペルカード発動を監視し、現在の**スペルカード名**を表示（初期対応：TH14, TH16, TH17, TH18 の中ボス・大ボスのスペカ名）。
   * 非スペルカード戦時（道中）は自動的に現在のステージ名（例：`Stage 1`）を表示。
   * GameManager 静的グローバルメモリを参照することで、**ステージ1（1面ボス）のスペカ検出失敗バグを完全に修正**。
4. **VRChat チャットボックス＆アバターパラメータ連携**：
   * 2秒ごと（レートリミット保護）にステータステキストを送信：`Game: <ゲーム名> [<難易度>] | Stage/Spell: <ステージ/スペカ名> | Score: <スコア> | Miss: <被弾数> | Bomb: <ボム数>`。
   * 標準的な OSC アバターパラメータをブロードキャストし、アバター内のステータス表示ギミックと連動可能。
5. **ASLR 対応とポータブル EXE リリース**：
   * Steam版の ASLR（アドレス空間配置のランダム化）モジュールベース自動解析。
   * Windows 標準のアプリケーションアイコンが埋め込まれた、インストール不要のポータブル EXE バージョン。

---

### 🗺️ 次期バージョンで実装予定の機能
1. **全シリーズのスペルカード名マッピング完了**：TH06-TH13、TH15、および最新の TH20 におけるボススペルカード名の日英中マッピングテーブルを補完。
2. **自機キャラクター＆装備タイプの検出**：プレイヤーが選択している自機（霊夢、魔理沙など）および装備タイプ（アタックタイプなど）を検出して表示。
3. **対戦・外伝作品のサポート拡充**：東方花映塚 (TH09) や東方獣王園 (TH19) などの対戦作品のメモリ構造を解析・対応。
4. **GUI（グラフィカル設定画面）の提供**：OSC ポートの設定、Chatbox の送信フォーマットのカスタマイズ、特定パラメータの放送オン・オフなどを簡単に行える設定ウィンドウを実装。
