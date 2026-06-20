# THOSC_BOX v1.0.0 Release Notes / 发布说明 / リリースノート

## 🇨🇳 中文 (Chinese)

### 🌟 当前已实现功能
1. **多款游戏深度适配**：配置并支持全系列 15 款官方 mainline 作品的内存读取：
   * 支持作品：TH06 (红魔乡)、TH07 (妖妖梦)、TH08 (永夜抄)、TH09 (花映冢)、TH10 (风神录)、TH11 (地灵殿)、TH12 (星莲船)、TH13 (神灵庙)、TH14 (辉针城)、TH15 (绀珠传)、TH16 (天空璋)、TH17 (鬼形兽)、TH18 (虹龙洞)、TH19 (兽王园)、TH20 (锦上京)。
2. **实时核心数据同步**：
   * **分数 (Score)**：实时提取并自动进行倍率还原。
   * **死亡数 (Miss) 与炸弹数 (Bomb) 增量计算**：在关卡重启或新游戏时自动清零，支持增量统计（非单纯读取剩余生命值），并针对 TH10/TH11 消耗 Power 释放 Bomb 的机制进行了判定优化。
   * **当前关卡 (Stage)**：精准识别并标准化输出当前关卡（如 `Stage 1-6`、`Extra`、`Phantasm` 等）。
   * **难度 (Difficulty)**：自动检测当前难度（Easy, Normal, Hard, Lunatic, Extra, Phantasm）。
3. **VRChat 深度联动**：
   * 每 2 秒（2000ms 频率限制防护）向 VRChat Chatbox 自动推送状态文本：`Game: <游戏名> [<难度>] | Stage: <关卡> | Score: <分数> | Miss: <死亡数> | Bomb: <炸弹数>`。
   * 广播标准 OSC Avatar 参数，支持玩家在 VRChat 虚拟形象（Animator）中制作对应的实时状态面板。
4. **ASLR 兼容与绿色发布**：
   * 完美适配 Steam 版本的 ASLR 动态模块基址解析。
   * 打包为绿色免安装 EXE，即开即用。

---

### 🗺️ 下一版本预计实现功能
1. **角色与机体检测**：读取并显示玩家当前选择的角色（如博丽灵梦、雾雨魔理沙）和武器机体类型。
2. **支持更多外传与衍生作品**：研究并适配官方弹幕联机、双人对战及外传作品（如《弹幕天邪鬼》、《秘封噩梦日记》、《刚欲异闻》等）。

---

## 🇺🇸 English

### 🌟 Currently Implemented Features
1. **Wide Game Compatibility**: Out-of-the-box support for 15 mainline Touhou Project titles:
   * Supported Games: TH06, TH07, TH08, TH09, TH10, TH11, TH12, TH13, TH14, TH15, TH16, TH17, TH18, TH19, and TH20.
2. **Real-time Core Data Sync**:
   * **Score**: Real-time extraction with auto-multiplier recovery.
   * **Incremental Miss & Bomb Counters**: Incremental statistics (resets on new game/restart) rather than remaining lives/bombs. Specially optimized for TH10/TH11's unique power-consuming bomb mechanic.
   * **Current Stage**: Detects and standardizes the stage value (e.g., `Stage 1-6`, `Extra`, `Phantasm`).
   * **Difficulty**: Automatically reads play difficulty (Easy, Normal, Hard, Lunatic, Extra, Phantasm).
3. **VRChat Chatbox & Parameter Integration**:
   * Sends formatted text to VRChat Chatbox every 2 seconds (with rate-limit protection): `Game: <Name> [<Difficulty>] | Stage: <Stage> | Score: <Score> | Miss: <Miss> | Bomb: <Bomb>`.
   * Broadcasts standard OSC Avatar parameters to drive real-time dashboard UI in your Avatar's Animator.
4. **ASLR Compatibility & Native EXE Release**:
   * Dynamic base address resolution to fully support Steam versions with ASLR enabled.
   * Packaged as a portable standalone EXE.

---

### 🗺️ Planned Features for Next Release
1. **Character & Shot Type Detection**: Read and display the active character (e.g., Reimu, Marisa) and selected weapon type.
2. **Expand Spin-off & Versus Game Support**: Research and adapt memory layouts for other versus and spin-off titles (such as TH14.3, TH16.5, TH17.5).

---

## 🇯🇵 日本語 (Japanese)

### 🌟 現在実装されている機能
1. **多数の東方作品に対応**：計15の公式メインライン作品のメモリ読み込みに対応：
   * 対応作品：東方紅魔郷 (TH06)、東方妖々夢 (TH07)、東方永夜抄 (TH08)、東方花映塚 (TH09)、東方風神録 (TH10)、東方地霊殿 (TH11)、東方星蓮船 (TH12)、東方神霊廟 (TH13)、東方輝針城 (TH14)、東方紺珠伝 (TH15)、東方天空璋 (TH16)、東方鬼形獣 (TH17)、東方虹龍洞 (TH18)、東方獣王園 (TH19)、東方錦上京 (TH20)。
2. **リアルタイムなコアデータ同期**：
   * **スコア (Score)**：メモリからスコアを取得し、ゲームごとの倍率を自動で復元。
   * **ミス (Miss) とボム (Bomb) のインクリメンタル集計**：残機・残ボムの単純取得ではなく、ゲーム開始・リトライ時に自动リセットされる累計被弾数・ボム使用数を集計。TH10/TH11 のパワー消費型ボムも最適化判定。
   * **現在のステージ (Stage)**：進行度を判定し、`Stage 1-6` や `Extra`、`Phantasm` などに標準化して表示。
   * **難易度 (Difficulty)**：現在の難易度（Easy, Normal, Hard, Lunatic, Extra, Phantasm）を自動取得。
3. **VRChat チャットボックス＆アバターパラメータ連携**：
   * 2秒ごと（レートリミット保護）にステータステキストを送信：`Game: <ゲーム名> [<難易度>] | Stage: <ステージ> | Score: <スコア> | Miss: <被弾数> | Bomb: <ボム数>`。
   * 标准的な OSC アバターパラメータをブロードキャストし、アバター内のステータス表示ギミックと連動可能。
4. **ASLR 対応とポータブル EXE リリース**：
   * Steam版の ASLR（アドレス空間配置のランダム化）モジュールベース自動解析。
   * インストール不要のポータブル EXE バージョン。

---

### 🗺️ 次期バージョンで実装予定の機能
1. **自機キャラクター＆装備タイプの検出**：プレイヤーが選択している自機（霊梦、魔理沙など）および装備タイプ（アタックタイプなど）を検出して表示。
2. **対戦・外伝作品のサポート拡充**：公式弾幕対戦、外伝作品（弾幕アマノジャク、秘封ナイトメアダイアリー、剛欲異聞など）のメモリ構造を解析・対応。
