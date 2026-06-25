import java.awt.Color
import java.awt.Dimension
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class Settings(
    val language: String,
    val oscPort: Int = 9000,
    val enableChatbox: Boolean = true,
    val appMode: String = "touhou",
    val themeColorHex: String = "#D0BCFF",
    val chatboxShowGame: Boolean = true,
    val chatboxShowChara: Boolean = true,
    val chatboxShowStage: Boolean = true,
    val chatboxShowScore: Boolean = true,
    val chatboxShowMiss: Boolean = true,
    val chatboxShowBomb: Boolean = true,
    val chatboxShowAcc: Boolean = true
)

@Volatile
var activeLang: String = "zh"

@Volatile
var activeOscPort: Int = 9000

@Volatile
var activeEnableChatbox: Boolean = true

@Volatile
var activeMode: String = "touhou"

@Volatile
var activeThemeColorHex: String = "#D0BCFF"

@Volatile
var activeChatboxShowGame: Boolean = true

@Volatile
var activeChatboxShowChara: Boolean = true

@Volatile
var activeChatboxShowStage: Boolean = true

@Volatile
var activeChatboxShowScore: Boolean = true

@Volatile
var activeChatboxShowMiss: Boolean = true

@Volatile
var activeChatboxShowBomb: Boolean = true

@Volatile
var activeChatboxShowAcc: Boolean = true

fun parseHexColor(hex: String): Color? {
    return try {
        val cleanHex = if (hex.startsWith("#")) hex.substring(1) else hex
        Color(cleanHex.toInt(16))
    } catch (e: Exception) {
        null
    }
}

fun Color.toHex(): String {
    return String.format("#%02X%02X%02X", this.red, this.green, this.blue)
}

fun selectLanguage(): String {
    val configFile = java.io.File("settings.json")
    if (configFile.exists()) {
        try {
            val content = configFile.readText()
            val settings = Json.decodeFromString<Settings>(content)
            if (settings.language in listOf("zh", "en", "ja")) {
                activeLang = settings.language
                activeOscPort = settings.oscPort
                activeEnableChatbox = settings.enableChatbox
                activeMode = settings.appMode
                activeThemeColorHex = settings.themeColorHex
                activeChatboxShowGame = settings.chatboxShowGame
                activeChatboxShowChara = settings.chatboxShowChara
                activeChatboxShowStage = settings.chatboxShowStage
                activeChatboxShowScore = settings.chatboxShowScore
                activeChatboxShowMiss = settings.chatboxShowMiss
                activeChatboxShowBomb = settings.chatboxShowBomb
                activeChatboxShowAcc = settings.chatboxShowAcc
                parseHexColor(settings.themeColorHex)?.let { MD3Color.updateThemeColor(it) }
                return settings.language
            }
        } catch (e: Exception) {
            // Ignore and prompt
        }
    }

    var choice = ""
    if (!java.awt.GraphicsEnvironment.isHeadless()) {
        val dialog = javax.swing.JDialog(null as java.awt.Frame?, "Language Selection", true)
        dialog.isUndecorated = true
        dialog.background = Color(0, 0, 0, 0)
        dialog.size = Dimension(400, 250)
        dialog.setLocationRelativeTo(null)
        dialog.defaultCloseOperation = javax.swing.JDialog.DISPOSE_ON_CLOSE

        val mainPanel = object : javax.swing.JPanel() {
            override fun paintComponent(g: java.awt.Graphics) {
                val g2d = g.create() as java.awt.Graphics2D
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
                g2d.color = MD3Color.Surface
                g2d.fillRoundRect(1, 1, width - 2, height - 2, 24, 24)
                g2d.color = MD3Color.Outline
                g2d.stroke = java.awt.BasicStroke(1.5f)
                g2d.drawRoundRect(1, 1, width - 3, height - 3, 24, 24)
                g2d.dispose()
            }
        }
        mainPanel.layout = javax.swing.BoxLayout(mainPanel, javax.swing.BoxLayout.Y_AXIS)
        mainPanel.isOpaque = false
        mainPanel.border = javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20)

        val titleLabel = javax.swing.JLabel("Select Language / 选择语言 / 言語選択")
        titleLabel.foreground = MD3Color.TextPrimary
        titleLabel.alignmentX = java.awt.Component.CENTER_ALIGNMENT
        titleLabel.font = getAppFont(java.awt.Font.BOLD, 18)
        
        mainPanel.add(titleLabel)
        mainPanel.add(javax.swing.Box.createRigidArea(java.awt.Dimension(0, 25)))

        var dragStart: java.awt.Point? = null
        mainPanel.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent) {
                dragStart = e.point
            }
        })
        mainPanel.addMouseMotionListener(object : java.awt.event.MouseMotionAdapter() {
            override fun mouseDragged(e: java.awt.event.MouseEvent) {
                val curr = e.locationOnScreen
                if (dragStart != null) {
                    dialog.setLocation(curr.x - dragStart!!.x, curr.y - dragStart!!.y)
                }
            }
        })

        fun createLangBtn(text: String, lang: String): javax.swing.JButton {
            val btn = object : javax.swing.JButton(text) {
                private var isHovered = false
                init {
                    isContentAreaFilled = false
                    isBorderPainted = false
                    isFocusable = false
                    foreground = MD3Color.TextPrimary
                    font = getAppFont(java.awt.Font.PLAIN, 15)
                    addMouseListener(object : java.awt.event.MouseAdapter() {
                        override fun mouseEntered(e: java.awt.event.MouseEvent?) {
                            isHovered = true
                            repaint()
                        }
                        override fun mouseExited(e: java.awt.event.MouseEvent?) {
                            isHovered = false
                            repaint()
                        }
                    })
                }
                override fun paintComponent(g: java.awt.Graphics) {
                    val g2d = g.create() as java.awt.Graphics2D
                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
                    g2d.color = if (isHovered) MD3Color.SecondaryContainer else MD3Color.Surface.brighter()
                    g2d.fillRoundRect(1, 1, width - 2, height - 2, 16, 16)
                    g2d.color = MD3Color.Outline
                    g2d.drawRoundRect(1, 1, width - 3, height - 3, 16, 16)
                    super.paintComponent(g)
                    g2d.dispose()
                }
            }
            btn.alignmentX = java.awt.Component.CENTER_ALIGNMENT
            btn.maximumSize = java.awt.Dimension(300, 40)
            btn.preferredSize = java.awt.Dimension(300, 40)
            btn.addActionListener {
                choice = lang
                dialog.dispose()
            }
            return btn
        }

        mainPanel.add(createLangBtn("简体中文 (Chinese)", "zh"))
        mainPanel.add(javax.swing.Box.createRigidArea(java.awt.Dimension(0, 10)))
        mainPanel.add(createLangBtn("English (English)", "en"))
        mainPanel.add(javax.swing.Box.createRigidArea(java.awt.Dimension(0, 10)))
        mainPanel.add(createLangBtn("日本語 (Japanese)", "ja"))

        dialog.contentPane = mainPanel
        dialog.isVisible = true // Blocks because it's modal

        if (choice.isEmpty()) {
            choice = "zh" // fallback if closed somehow
        }
    } else {
        println("=========================================")
        println("Please select your language / 请选择语言 / 言語を選択してください:")
        println("1. 简体中文 (Chinese)")
        println("2. English (English)")
        println("3. 日本語 (Japanese)")
        println("=========================================")
        print("Enter number (1-3) / 输入数字 (1-3) / 数字を入力してください (1-3): ")
        System.out.flush()

        while (true) {
            val input = readLine()?.trim()
            if (input == "1" || input == "2" || input == "3") {
                choice = when (input) {
                    "1" -> "zh"
                    "2" -> "en"
                    "3" -> "ja"
                    else -> "zh"
                }
                break
            }
            print("Invalid input. Enter 1-3: ")
            System.out.flush()
        }
    }

    try {
        val settings = Settings(
            choice, activeOscPort, activeEnableChatbox, activeMode, activeThemeColorHex,
            activeChatboxShowGame, activeChatboxShowChara, activeChatboxShowStage, activeChatboxShowScore,
            activeChatboxShowMiss, activeChatboxShowBomb, activeChatboxShowAcc
        )
        configFile.writeText(Json.encodeToString(settings))
        val successMsg = when (choice) {
            "zh" -> "语言已设置为：简体中文。配置文件已保存至 settings.json。"
            "en" -> "Language set to English. Configuration saved to settings.json."
            "ja" -> "言語が日本語に設定されました。設定は settings.json に保存されました。"
            else -> ""
        }
        println(successMsg)
    } catch (e: Exception) {
        println("Warning: Failed to save settings.json: ${e.message}")
    }

    activeLang = choice
    return choice
}

fun changeThemeColor(color: Color) {
    MD3Color.updateThemeColor(color)
    activeThemeColorHex = color.toHex()
    try {
        val settings = Settings(
            activeLang, activeOscPort, activeEnableChatbox, activeMode, activeThemeColorHex,
            activeChatboxShowGame, activeChatboxShowChara, activeChatboxShowStage, activeChatboxShowScore,
            activeChatboxShowMiss, activeChatboxShowBomb, activeChatboxShowAcc
        )
        java.io.File("settings.json").writeText(Json.encodeToString(settings))
    } catch (e: Exception) {
        println("Warning: Failed to save settings.json: ${e.message}")
    }
    java.awt.EventQueue.invokeLater {
        mainWindow?.repaint()
    }
}

fun changeLanguage(lang: String) {
    activeLang = lang
    try {
        val settings = Settings(
            lang, activeOscPort, activeEnableChatbox, activeMode, activeThemeColorHex,
            activeChatboxShowGame, activeChatboxShowChara, activeChatboxShowStage, activeChatboxShowScore,
            activeChatboxShowMiss, activeChatboxShowBomb, activeChatboxShowAcc
        )
        java.io.File("settings.json").writeText(Json.encodeToString(settings))
    } catch (e: Exception) {
        // Ignore
    }
    java.awt.EventQueue.invokeLater {
        updateTrayLabels()
        mainWindow?.refreshUILabels()
    }
}

fun changeSettings(
    lang: String,
    port: Int,
    enableChatbox: Boolean,
    showGame: Boolean,
    showChara: Boolean,
    showStage: Boolean,
    showScore: Boolean,
    showMiss: Boolean,
    showBomb: Boolean,
    showAcc: Boolean
) {
    activeLang = lang
    activeEnableChatbox = enableChatbox
    activeChatboxShowGame = showGame
    activeChatboxShowChara = showChara
    activeChatboxShowStage = showStage
    activeChatboxShowScore = showScore
    activeChatboxShowMiss = showMiss
    activeChatboxShowBomb = showBomb
    activeChatboxShowAcc = showAcc
    
    if (activeOscPort != port) {
        activeOscPort = port
        try {
            activeOscSender?.close()
        } catch (e: Exception) {
            // Ignore
        }
        try {
            activeOscSender = com.illposed.osc.transport.OSCPortOut(java.net.InetSocketAddress("127.0.0.1", port))
        } catch (e: Exception) {
            println("Error updating OSC port: ${e.message}")
        }
    }

    try {
        val settings = Settings(
            lang, port, enableChatbox, activeMode, activeThemeColorHex,
            showGame, showChara, showStage, showScore, showMiss, showBomb, showAcc
        )
        java.io.File("settings.json").writeText(Json.encodeToString(settings))
    } catch (e: Exception) {
        println("Warning: Failed to save settings.json: ${e.message}")
    }
    
    java.awt.EventQueue.invokeLater {
        updateTrayLabels()
        mainWindow?.refreshUILabels()
    }
}

fun changeMode(mode: String) {
    activeMode = mode
    try {
        val settings = Settings(
            activeLang, activeOscPort, activeEnableChatbox, mode, activeThemeColorHex,
            activeChatboxShowGame, activeChatboxShowChara, activeChatboxShowStage, activeChatboxShowScore,
            activeChatboxShowMiss, activeChatboxShowBomb, activeChatboxShowAcc
        )
        java.io.File("settings.json").writeText(Json.encodeToString(settings))
    } catch (e: Exception) {
        println("Warning: Failed to save settings.json: ${e.message}")
    }

    if (mode == "osu") {
        checkAndStartOsuHelper()
    } else {
        stopOsuHelper()
    }
}

fun getLocalizedString(key: String, lang: String): String {
    return when (lang) {
        "zh" -> when (key) {
            "tab_dashboard" -> "仪表盘"
            "tab_settings" -> "设置"
            "tab_logs" -> "调试"
            "stat_game" -> "当前游戏"
            "stat_chara" -> "角色/机体"
            "stat_stage" -> "关卡"
            "stat_score" -> "得分"
            "stat_lives" -> "残机/Miss"
            "stat_bombs" -> "炸弹/Bomb"
            "stat_graze" -> "擦弹/Graze"
            "stat_power" -> "火力/Power"
            "stat_point" -> "得分道具/Point"
            "stat_cherry" -> "最大樱点/Cherry"
            "stat_spell" -> "符卡"
            "settings_lang" -> "界面语言:"
            "settings_port" -> "VRChat OSC 端口:"
            "settings_chatbox" -> "启用 VRChat Chatbox 状态发送"
            "btn_save" -> "保存设置"
            "save_success" -> "设置已成功保存！"
            "status_scanning" -> "正在扫描游戏..."
            "status_playing" -> "正在玩"
            "title_app" -> "Touhou VRChat OSC 桥接器"
            "btn_clear" -> "清除日志"
            "btn_copy" -> "复制日志"
            "settings_chatbox_options" -> "聊天框内容定制"
            "settings_chatbox_game" -> "显示 游戏/歌名"
            "settings_chatbox_chara" -> "显示 机体/模组"
            "settings_chatbox_stage" -> "显示 关卡/难度"
            "settings_chatbox_score" -> "显示 分数/PP"
            "settings_chatbox_miss" -> "显示 死亡/失误"
            "settings_chatbox_bomb" -> "显示 炸弹/连击"
            "settings_chatbox_acc" -> "显示 准确率"
            else -> key
        }
        "ja" -> when (key) {
            "tab_dashboard" -> "ダッシュボード"
            "tab_settings" -> "設定"
            "tab_logs" -> "デバッグ"
            "stat_game" -> "現在のゲーム"
            "stat_chara" -> "キャラクター"
            "stat_stage" -> "ステージ"
            "stat_score" -> "スコア"
            "stat_lives" -> "残機/被弾"
            "stat_bombs" -> "ボム"
            "stat_graze" -> "グレイズ"
            "stat_power" -> "パワー"
            "stat_point" -> "得点アイテム"
            "stat_cherry" -> "最大桜点"
            "stat_spell" -> "スペルカード"
            "settings_lang" -> "言語:"
            "settings_port" -> "VRChat OSC ポート:"
            "settings_chatbox" -> "VRChat Chatbox 送信を有効にする"
            "btn_save" -> "設定を保存"
            "save_success" -> "設定が保存されました！"
            "status_scanning" -> "ゲームをスキャン中..."
            "status_playing" -> "プレイ中"
            "title_app" -> "東方 VRChat OSC ブリッジ"
            "btn_clear" -> "ログ消去"
            "btn_copy" -> "ログコピー"
            "settings_chatbox_options" -> "チャットボックス詳細設定"
            "settings_chatbox_game" -> "ゲーム・曲名を表示"
            "settings_chatbox_chara" -> "自機・Modを表示"
            "settings_chatbox_stage" -> "ステージ・難易度を表示"
            "settings_chatbox_score" -> "スコア・PPを表示"
            "settings_chatbox_miss" -> "被弾・ミス数を表示"
            "settings_chatbox_bomb" -> "ボム・コンボを表示"
            "settings_chatbox_acc" -> "精度を表示"
            else -> key
        }
        else -> when (key) {
            "tab_dashboard" -> "Dashboard"
            "tab_settings" -> "Settings"
            "tab_logs" -> "Debug"
            "stat_game" -> "Active Game"
            "stat_chara" -> "Character"
            "stat_stage" -> "Stage"
            "stat_score" -> "Score"
            "stat_lives" -> "Lives/Miss"
            "stat_bombs" -> "Bombs"
            "stat_graze" -> "Graze"
            "stat_power" -> "Power"
            "stat_point" -> "Point/PIV"
            "stat_cherry" -> "Cherry Max"
            "stat_spell" -> "Spell Card"
            "settings_lang" -> "Language:"
            "settings_port" -> "VRChat OSC Port:"
            "settings_chatbox" -> "Enable VRChat Chatbox Output"
            "btn_save" -> "Save Settings"
            "save_success" -> "Settings saved successfully!"
            "status_scanning" -> "Scanning for games..."
            "status_playing" -> "Playing"
            "title_app" -> "Touhou VRChat OSC Bridge"
            "btn_clear" -> "Clear Logs"
            "btn_copy" -> "Copy Logs"
            "settings_chatbox_options" -> "Chatbox Detail Settings"
            "settings_chatbox_game" -> "Show Game/Song"
            "settings_chatbox_chara" -> "Show Chara/Mods"
            "settings_chatbox_stage" -> "Show Stage/Difficulty"
            "settings_chatbox_score" -> "Show Score/PP"
            "settings_chatbox_miss" -> "Show Misses"
            "settings_chatbox_bomb" -> "Show Bomb/Combo"
            "settings_chatbox_acc" -> "Show Accuracy"
            else -> key
        }
    }
}

fun getLocalizedGameName(gameId: String, lang: String): String {
    return when (gameId) {
        "th06" -> when (lang) {
            "zh" -> "东方红魔乡 ~ Embodiment of Scarlet Devil"
            "ja" -> "東方紅魔郷 ~ Embodiment of Scarlet Devil"
            else -> "Embodiment of Scarlet Devil"
        }
        "th07" -> when (lang) {
            "zh" -> "东方妖妖梦 ~ Perfect Cherry Blossom"
            "ja" -> "東方妖々夢 ~ Perfect Cherry Blossom"
            else -> "Perfect Cherry Blossom"
        }
        "th08" -> when (lang) {
            "zh" -> "东方永夜抄 ~ Imperishable Night"
            "ja" -> "東方永夜抄 ~ Imperishable Night"
            else -> "Imperishable Night"
        }
        "th09" -> when (lang) {
            "zh" -> "东方花映冢 ~ Phantasmagoria of Flower View"
            "ja" -> "東方花映塚 ~ Phantasmagoria of Flower View"
            else -> "Phantasmagoria of Flower View"
        }
        "th10" -> when (lang) {
            "zh" -> "东方风神录 ~ Mountain of Faith"
            "ja" -> "東方風神録 ~ Mountain of Faith"
            else -> "Mountain of Faith"
        }
        "th11" -> when (lang) {
            "zh" -> "东方地灵殿 ~ Subterranean Animism"
            "ja" -> "東方地霊殿 ~ Subterranean Animism"
            else -> "Subterranean Animism"
        }
        "th12" -> when (lang) {
            "zh" -> "东方星莲船 ~ Undefined Fantastic Object"
            "ja" -> "東方星蓮船 ~ Undefined Fantastic Object"
            else -> "Undefined Fantastic Object"
        }
        "th13" -> when (lang) {
            "zh" -> "东方神灵庙 ~ Ten Desires"
            "ja" -> "東方神霊廟 ~ Ten Desires"
            else -> "Ten Desires"
        }
        "th14" -> when (lang) {
            "zh" -> "东方辉针城 ~ Double Dealing Character"
            "ja" -> "東方輝針城 ~ Double Dealing Character"
            else -> "Double Dealing Character"
        }
        "th15" -> when (lang) {
            "zh" -> "东方绀珠传 ~ Legacy of Lunatic Kingdom"
            "ja" -> "東方紺珠伝 ~ Legacy of Lunatic Kingdom"
            else -> "Legacy of Lunatic Kingdom"
        }
        "th16" -> when (lang) {
            "zh" -> "东方天空璋 ~ Hidden Star in Four Seasons"
            "ja" -> "東方天空璋 ~ Hidden Star in Four Seasons"
            else -> "Hidden Star in Four Seasons"
        }
        "th17" -> when (lang) {
            "zh" -> "东方鬼形兽 ~ Wily Beast and Weakest Creature"
            "ja" -> "東方鬼形獣 ~ Wily Beast and Weakest Creature"
            else -> "Wily Beast and Weakest Creature"
        }
        "th18" -> when (lang) {
            "zh" -> "东方虹龙洞 ~ Unconnected Marketeers"
            "ja" -> "東方虹龍洞 ~ Unconnected Marketeers"
            else -> "Unconnected Marketeers"
        }
        "th19" -> when (lang) {
            "zh" -> "东方兽王园 ~ Unfinished Dream of All Living Ghost"
            "ja" -> "東方獣王園 ~ Unfinished Dream of All Living Ghost"
            else -> "Unfinished Dream of All Living Ghost"
        }
        "th20" -> when (lang) {
            "zh" -> "东方锦上京 ~ Fossilized Wonders."
            "ja" -> "東方錦上京 ~ Fossilized Wonders."
            else -> "Fossilized Wonders."
        }
        else -> gameId
    }
}

fun extractLocalizedText(rawText: String, lang: String): String {
    val regex = """(.+?)\s*\((.+?)\)""".toRegex()
    val matchResult = regex.matchEntire(rawText)
    if (matchResult != null) {
        val zh = matchResult.groups[1]?.value?.trim() ?: rawText
        val en = matchResult.groups[2]?.value?.trim() ?: rawText
        return when (lang) {
            "zh" -> zh
            "en" -> en
            else -> rawText
        }
    }
    return rawText
}

fun getSpellName(gameId: String, spellId: Int, lang: String): String {
    val th14Map = mapOf(
        100 to "辉针「鬼之杰作」 (Shining Needle \"Oni's Masterpiece\")",
        101 to "辉针「鬼之杰作」 (Shining Needle \"Oni's Masterpiece\")",
        102 to "辉针「鬼之杰作之山」 (Shining Needle \"Oni's Masterpiece of Needle Mountain\")",
        103 to "辉针「鬼之杰作之山」 (Shining Needle \"Oni's Masterpiece of Needle Mountain\")",
        104 to "小槌「变得大吧」 (Grow Bigger, Little Mallet)",
        105 to "小槌「变得大吧」 (Grow Bigger, Little Mallet)",
        106 to "小槌「变得更大吧」 (Grow Even Bigger, Little Mallet)",
        107 to "小槌「变得更大吧」 (Grow Even Bigger, Little Mallet)",
        108 to "「打出小槌的袭击」 (Attack of the Shinmyoumaru)",
        109 to "「打出小槌的袭击」 (Attack of the Shinmyoumaru)",
        110 to "「逆袭的打出小槌」 (Counterattack of the Shinmyoumaru)",
        111 to "「逆袭的打出小槌」 (Counterattack of the Shinmyoumaru)",
        112 to "「一寸之子的巨大反击」 (Giant Counterattack of the One-Inch Boy)",
        113 to "「一寸之子的巨大反击」 (Giant Counterattack of the One-Inch Boy)",
        114 to "「小人国之乱」 (Rebellion of the Kobito)",
        115 to "「小人国之乱」 (Rebellion of the Kobito)",
        116 to "「七个小人」 (Seven Kobitos)",
        117 to "「七个小人」 (Seven Kobitos)",
        118 to "「壁橱中的小人七个」 (Seven Kobitos in the Closet)",
        119 to "「壁橱中的小人七个」 (Seven Kobitos in the Closet)"
    )
    val th16Map = mapOf(
        106 to "「背后的秘仪」 (Backside Ceremony)",
        107 to "「背后的秘仪」 (Backside Ceremony)",
        108 to "秘仪「后门之魂」 (Secret Ceremony \"Behind-Door Souls\")",
        109 to "秘仪「后门之魂」 (Secret Ceremony \"Behind-Door Souls\")",
        110 to "秘仪「背叛的樱吹雪」 (Secret Ceremony \"Betrayal Cherry Blossom Blizzard\")",
        111 to "秘仪「背叛的樱吹雪」 (Secret Ceremony \"Betrayal Cherry Blossom Blizzard\")",
        112 to "秘仪「里七星」 (Secret Ceremony \"Reverse Seven Stars\")",
        113 to "秘仪「里七星」 (Secret Ceremony \"Reverse Seven Stars\")"
    )
    val th17Map = mapOf(
        84 to "线形「线性雕刻物」 (Linear \"Linear Sculpture\")",
        85 to "线形「线性雕刻物」 (Linear \"Linear Sculpture\")",
        86 to "埴轮「偶像防卫队」 (Haniwa \"Idol Defense Force\")",
        87 to "埴轮「偶像防卫队」 (Haniwa \"Idol Defense Force\")"
    )
    val th18Map = mapOf(
        84 to "「无主之物的买卖」 (Trading of Ownerless Goods)",
        85 to "「无主之物的买卖」 (Trading of Ownerless Goods)",
        86 to "「弹幕无产阶级化」 (Danmaku Proletariat)",
        87 to "「弹幕无产阶级化」 (Danmaku Proletariat)"
    )
    val rawText = when (gameId) {
        "th14" -> th14Map[spellId] ?: "Spell ID $spellId"
        "th16" -> th16Map[spellId] ?: "Spell ID $spellId"
        "th17" -> th17Map[spellId] ?: "Spell ID $spellId"
        "th18" -> th18Map[spellId] ?: "Spell ID $spellId"
        else -> "Spell ID $spellId"
    }

    return when (lang) {
        "ja" -> {
            val jaMap = mapOf(
                "th14_100" to "輝針「鬼の傑作」",
                "th14_101" to "輝針「鬼の傑作」",
                "th14_102" to "輝針「針の山なぞる鬼の傑作」",
                "th14_103" to "輝針「針の山なぞる鬼の傑作」",
                "th14_104" to "小槌「大きくなあれ」",
                "th14_105" to "小槌「大きくなあれ」",
                "th14_106" to "小槌「もっと大きくなあれ」",
                "th14_107" to "小槌「もっと大きくなあれ」",
                "th14_108" to "「打ち出の小槌の襲撃」",
                "th14_109" to "「打ち出の小槌の襲撃」",
                "th14_110" to "「逆襲の打ち出の小槌」",
                "th14_111" to "「逆襲の打ち出の小槌」",
                "th14_112" to "「小人の巨大な逆襲」",
                "th14_113" to "「小人の巨大な逆襲」",
                "th14_114" to "「小人国の反乱」",
                "th14_115" to "「小人国の反乱」",
                "th14_116" to "「七人の小人」",
                "th14_117" to "「七人の小人」",
                "th14_118" to "「押し入れの七人の小人」",
                "th14_119" to "「押し入れの七人の小人」",
                "th16_106" to "「裏転生の秘儀」",
                "th16_107" to "「裏転生の秘儀」",
                "th16_108" to "秘儀「後戸の生命体」",
                "th16_109" to "秘儀「後戸の生命体」",
                "th16_110" to "秘儀「背面の暗黒桜吹雪」",
                "th16_111" to "秘儀「背面の暗黒桜吹雪」",
                "th16_112" to "秘儀「裏七星」",
                "th16_113" to "秘儀「裏七星」",
                "th17_84" to "線形「線形彫刻」",
                "th17_85" to "線形「線形彫刻」",
                "th17_86" to "埴輪「アイドル防衛隊」",
                "th17_87" to "埴輪「アイドル防衛隊」",
                "th18_84" to "「無主の取引」",
                "th18_85" to "「無主の取引」",
                "th18_86" to "「弾幕のプロレタリア化」",
                "th18_87" to "「弾幕のプロレタリア化」"
            )
            jaMap["${gameId}_$spellId"] ?: extractLocalizedText(rawText, "ja")
        }
        "zh" -> extractLocalizedText(rawText, "zh")
        "en" -> extractLocalizedText(rawText, "en")
        else -> rawText
    }
}

fun getDifficultyName(gameId: String, difficulty: Int, lang: String): String {
    val basic = when (difficulty) {
        0 -> "Easy"
        1 -> "Normal"
        2 -> "Hard"
        3 -> "Lunatic"
        4 -> "Extra"
        5 -> if (gameId == "th07") "Phantasm" else "Difficulty $difficulty"
        else -> "Difficulty $difficulty"
    }
    return when (lang) {
        "zh" -> when (difficulty) {
            0 -> "简单 (Easy)"
            1 -> "普通 (Normal)"
            2 -> "困难 (Hard)"
            3 -> "疯狂 (Lunatic)"
            4 -> "番外 (Extra)"
            5 -> if (gameId == "th07") "幻想 (Phantasm)" else "难度 $difficulty"
            else -> "难度 $difficulty"
        }
        "ja" -> when (difficulty) {
            0 -> "イージー (Easy)"
            1 -> "ノーマル (Normal)"
            2 -> "ハード (Hard)"
            3 -> "ルナティック (Lunatic)"
            4 -> "エキストラ (Extra)"
            5 -> if (gameId == "th07") "ファンタズム (Phantasm)" else "難易度 $difficulty"
            else -> "難易度 $difficulty"
        }
        else -> basic
    }
}

fun getCharaAndShottypeName(gameId: String, characterId: Int, subshotId: Int, lang: String): String {
    return when (gameId) {
        "th06" -> {
            val charStr = when (lang) {
                "zh" -> if (characterId == 0) "博丽灵梦" else "雾雨魔理沙"
                "ja" -> if (characterId == 0) "博麗霊夢" else "霧雨魔理沙"
                else -> if (characterId == 0) "Reimu" else "Marisa"
            }
            val shotStr = when (lang) {
                "zh" -> if (characterId == 0) (if (subshotId == 0) "灵符 (Reimu A)" else "梦符 (Reimu B)") else (if (subshotId == 0) "魔符 (Marisa A)" else "恋符 (Marisa B)")
                "ja" -> if (characterId == 0) (if (subshotId == 0) "霊符 (Reimu A)" else "夢符 (Reimu B)") else (if (subshotId == 0) "魔符 (Marisa A)" else "恋符 (Marisa B)")
                else -> if (characterId == 0) (if (subshotId == 0) "Reimu A" else "Reimu B") else (if (subshotId == 0) "Marisa A" else "Marisa B")
            }
            "$charStr ($shotStr)"
        }
        "th07" -> {
            val charStr = when (lang) {
                "zh" -> when (characterId) { 0 -> "博丽灵梦"; 1 -> "雾雨魔理沙"; else -> "十六夜咲夜" }
                "ja" -> when (characterId) { 0 -> "博麗霊夢"; 1 -> "霧雨魔理沙"; else -> "十六夜咲夜" }
                else -> when (characterId) { 0 -> "Reimu"; 1 -> "Marisa"; else -> "Sakuya" }
            }
            val shotStr = when (lang) {
                "zh" -> when (characterId) {
                    0 -> if (subshotId == 0) "灵符 (Reimu A)" else "梦符 (Reimu B)"
                    1 -> if (subshotId == 0) "魔符 (Marisa A)" else "恋符 (Marisa B)"
                    else -> if (subshotId == 0) "幻符 (Sakuya A)" else "时符 (Sakuya B)"
                }
                "ja" -> when (characterId) {
                    0 -> if (subshotId == 0) "霊符 (Reimu A)" else "夢符 (Reimu B)"
                    1 -> if (subshotId == 0) "魔符 (Marisa A)" else "恋符 (Marisa B)"
                    else -> if (subshotId == 0) "幻符 (Sakuya A)" else "時符 (Sakuya B)"
                }
                else -> when (characterId) {
                    0 -> if (subshotId == 0) "Reimu A" else "Reimu B"
                    1 -> if (subshotId == 0) "Marisa A" else "Marisa B"
                    else -> if (subshotId == 0) "Sakuya A" else "Sakuya B"
                }
            }
            "$charStr ($shotStr)"
        }
        "th08" -> {
            when (lang) {
                "zh" -> when (subshotId) {
                    0 -> "结界组 (Reimu & Yukari)"; 1 -> "咏唱组 (Marisa & Alice)"; 2 -> "红魔组 (Sakuya & Remilia)"; 3 -> "幽冥组 (Youmu & Yuyuko)"
                    4 -> "博丽灵梦"; 5 -> "八云紫"; 6 -> "雾雨魔理沙"; 7 -> "爱丽丝"; 8 -> "十六夜咲夜"; 9 -> "蕾米莉亚"; 10 -> "魂魄妖梦"; 11 -> "西行寺幽幽子"
                    else -> "未知机体 ($subshotId)"
                }
                "ja" -> when (subshotId) {
                    0 -> "結界組 (Reimu & Yukari)"; 1 -> "詠唱組 (Marisa & Alice)"; 2 -> "紅魔組 (Sakuya & Remilia)"; 3 -> "幽冥組 (Youmu & Yuyuko)"
                    4 -> "博麗霊夢"; 5 -> "八雲紫"; 6 -> "霧雨魔理沙"; 7 -> "アリス"; 8 -> "十六夜咲夜"; 9 -> "レミリア"; 10 -> "魂魄妖夢"; 11 -> "西行寺幽々子"
                    else -> "未知機体 ($subshotId)"
                }
                else -> when (subshotId) {
                    0 -> "Border Team (Reimu & Yukari)"; 1 -> "Magic Team (Marisa & Alice)"; 2 -> "Scarlet Team (Sakuya & Remilia)"; 3 -> "Netherworld Team (Youmu & Yuyuko)"
                    4 -> "Reimu Hakurei"; 5 -> "Yukari Yakumo"; 6 -> "Marisa Kirisame"; 7 -> "Alice Margatroid"; 8 -> "Sakuya Izayoi"; 9 -> "Remilia Scarlet"; 10 -> "Youmu Konpaku"; 11 -> "Yuyuko Saigyouji"
                    else -> "Unknown Shottype ($subshotId)"
                }
            }
        }
        "th10" -> {
            val charStr = when (lang) {
                "zh" -> if (characterId == 0) "博丽灵梦" else "雾雨魔理沙"
                "ja" -> if (characterId == 0) "博麗霊夢" else "霧雨魔理沙"
                else -> if (characterId == 0) "Reimu" else "Marisa"
            }
            val shotStr = when (lang) {
                "zh" -> if (characterId == 0) {
                    when (subshotId) { 0 -> "诱导装备 (Reimu A)"; 1 -> "前方集中攻击装备 (Reimu B)"; else -> "封魔针装备 (Reimu C)" }
                } else {
                    when (subshotId) { 0 -> "高威力追踪装备 (Marisa A)"; 1 -> "前方集中高威能装备 (Marisa B)"; else -> "超范围扫荡装备 (Marisa C)" }
                }
                "ja" -> if (characterId == 0) {
                    when (subshotId) { 0 -> "誘導装備 (Reimu A)"; 1 -> "前方集中装備 (Reimu B)"; else -> "封魔針装備 (Reimu C)" }
                } else {
                    when (subshotId) { 0 -> "高威力誘導装備 (Marisa A)"; 1 -> "前方集中装備 (Marisa B)"; else -> "超範囲装備 (Marisa C)" }
                }
                else -> if (characterId == 0) {
                    when (subshotId) { 0 -> "Reimu A (Homing)"; 1 -> "Reimu B (Forward)"; else -> "Reimu C (Seal)" }
                } else {
                    when (subshotId) { 0 -> "Marisa A (Homing)"; 1 -> "Marisa B (Forward)"; else -> "Marisa C (Spread)" }
                }
            }
            "$charStr ($shotStr)"
        }
        "th11" -> {
            val charStr = when (lang) {
                "zh" -> if (characterId == 0) "博丽灵梦" else "雾雨魔理沙"
                "ja" -> if (characterId == 0) "博麗霊夢" else "霧雨魔理沙"
                else -> if (characterId == 0) "Reimu" else "Marisa"
            }
            val shotStr = when (lang) {
                "zh" -> if (characterId == 0) {
                    when (subshotId) { 0 -> "八云紫支援 (Reimu & Yukari)"; 1 -> "伊吹萃香支援 (Reimu & Suika)"; else -> "射命丸文支援 (Reimu & Aya)" }
                } else {
                    when (subshotId) { 0 -> "爱丽丝支援 (Marisa & Alice)"; 1 -> "帕秋莉支援 (Marisa & Patchouli)"; else -> "河城荷取支援 (Marisa & Nitori)" }
                }
                "ja" -> if (characterId == 0) {
                    when (subshotId) { 0 -> "八雲紫支援 (Reimu & Yukari)"; 1 -> "伊吹萃香支援 (Reimu & Suika)"; else -> "射命丸文支援 (Reimu & Aya)" }
                } else {
                    when (subshotId) { 0 -> "アリス支援 (Marisa & Alice)"; 1 -> "パチュリー支援 (Marisa & Patchouli)"; else -> "河城にとり支援 (Marisa & Nitori)" }
                }
                else -> if (characterId == 0) {
                    when (subshotId) { 0 -> "Reimu & Yukari"; 1 -> "Reimu & Suika"; else -> "Reimu & Aya" }
                } else {
                    when (subshotId) { 0 -> "Marisa & Alice"; 1 -> "Marisa & Patchouli"; else -> "Marisa & Nitori" }
                }
            }
            "$charStr ($shotStr)"
        }
        "th12" -> {
            val charStr = when (lang) {
                "zh" -> when (characterId) { 0 -> "博丽灵梦"; 1 -> "雾雨魔理沙"; else -> "东风谷早苗" }
                "ja" -> when (characterId) { 0 -> "博麗霊夢"; 1 -> "霧雨魔理沙"; else -> "東風谷早苗" }
                else -> when (characterId) { 0 -> "Reimu"; 1 -> "Marisa"; else -> "Sanae" }
            }
            val shotStr = when (lang) {
                "zh" -> when (characterId) {
                    0 -> if (subshotId == 0) "巫女 (Reimu A)" else "宝船 (Reimu B)"
                    1 -> if (subshotId == 0) "激光 (Marisa A)" else "星莲船 (Marisa B)"
                    else -> if (subshotId == 0) "蛇 (Sanae A)" else "蛙 (Sanae B)"
                }
                "ja" -> when (characterId) {
                    0 -> if (subshotId == 0) "巫女 (Reimu A)" else "宝船 (Reimu B)"
                    1 -> if (subshotId == 0) "レーザー (Marisa A)" else "星蓮船 (Marisa B)"
                    else -> if (subshotId == 0) "蛇 (Sanae A)" else "蛙 (Sanae B)"
                }
                else -> when (characterId) {
                    0 -> if (subshotId == 0) "Reimu A" else "Reimu B"
                    1 -> if (subshotId == 0) "Marisa A" else "Marisa B"
                    else -> if (subshotId == 0) "Sanae A" else "Sanae B"
                }
            }
            "$charStr ($shotStr)"
        }
        "th13" -> {
            when (lang) {
                "zh" -> when (characterId) { 0 -> "博丽灵梦"; 1 -> "雾雨魔理沙"; 2 -> "东风谷早苗"; else -> "魂魄妖梦" }
                "ja" -> when (characterId) { 0 -> "博麗霊夢"; 1 -> "霧雨魔理沙"; 2 -> "東風谷早苗"; else -> "魂魄妖夢" }
                else -> when (characterId) { 0 -> "Reimu Hakurei"; 1 -> "Marisa Kirisame"; 2 -> "Sanae Kochiya"; else -> "Youmu Konpaku" }
            }
        }
        "th14" -> {
            val charStr = when (lang) {
                "zh" -> when (characterId) { 0 -> "博丽灵梦"; 1 -> "雾雨魔理沙"; else -> "十六夜咲夜" }
                "ja" -> when (characterId) { 0 -> "博麗霊夢"; 1 -> "霧雨魔理沙"; else -> "十六夜咲夜" }
                else -> when (characterId) { 0 -> "Reimu"; 1 -> "Marisa"; else -> "Sakuya" }
            }
            val shotStr = when (lang) {
                "zh" -> when (characterId) {
                    0 -> if (subshotId == 0) "妖器使用 (Reimu A)" else "妖器不使用 (Reimu B)"
                    1 -> if (subshotId == 0) "妖器使用 (Marisa A)" else "妖器不使用 (Marisa B)"
                    else -> if (subshotId == 0) "妖器使用 (Sakuya A)" else "妖器不使用 (Sakuya B)"
                }
                "ja" -> when (characterId) {
                    0 -> if (subshotId == 0) "妖器使用 (Reimu A)" else "妖器不使用 (Reimu B)"
                    1 -> if (subshotId == 0) "妖器使用 (Marisa A)" else "妖器不使用 (Marisa B)"
                    else -> if (subshotId == 0) "妖器使用 (Sakuya A)" else "妖器不使用 (Sakuya B)"
                }
                else -> when (characterId) {
                    0 -> if (subshotId == 0) "Reimu A (Weapon)" else "Reimu B (No Weapon)"
                    1 -> if (subshotId == 0) "Marisa A (Weapon)" else "Marisa B (No Weapon)"
                    else -> if (subshotId == 0) "Sakuya A (Weapon)" else "Sakuya B (No Weapon)"
                }
            }
            "$charStr ($shotStr)"
        }
        "th15" -> {
            when (lang) {
                "zh" -> when (characterId) { 0 -> "博丽灵梦"; 1 -> "雾雨魔理沙"; 2 -> "东风谷早苗"; else -> "铃仙·优昙华院·因幡" }
                "ja" -> when (characterId) { 0 -> "博麗霊夢"; 1 -> "霧雨魔理沙"; 2 -> "東風谷早苗"; else -> "鈴仙・優曇華院・イナバ" }
                else -> when (characterId) { 0 -> "Reimu Hakurei"; 1 -> "Marisa Kirisame"; 2 -> "Sanae Kochiya"; else -> "Reisen Udongein Inaba" }
            }
        }
        "th16" -> {
            val charStr = when (lang) {
                "zh" -> when (characterId) { 0 -> "博丽灵梦"; 1 -> "琪露诺"; 2 -> "射命丸文"; else -> "雾雨魔理沙" }
                "ja" -> when (characterId) { 0 -> "博麗霊夢"; 1 -> "チルノ"; 2 -> "射命丸文"; else -> "霧雨魔理沙" }
                else -> when (characterId) { 0 -> "Reimu"; 1 -> "Cirno"; 2 -> "Aya"; else -> "Marisa" }
            }
            val shotStr = when (lang) {
                "zh" -> when (subshotId) { 0 -> "春 (Spring)"; 1 -> "夏 (Summer)"; 2 -> "秋 (Autumn)"; 3 -> "冬 (Winter)"; else -> "土用 (Dog Days)" }
                "ja" -> when (subshotId) { 0 -> "春 (Spring)"; 1 -> "夏 (Summer)"; 2 -> "秋 (Autumn)"; 3 -> "冬 (Winter)"; else -> "土用 (Dog Days)" }
                else -> when (subshotId) { 0 -> "Spring"; 1 -> "Summer"; 2 -> "Autumn"; 3 -> "Winter"; else -> "Dog Days" }
            }
            "$charStr [$shotStr]"
        }
        "th17" -> {
            val charStr = when (lang) {
                "zh" -> when (characterId) { 0 -> "博丽灵梦"; 1 -> "雾雨魔理沙"; else -> "魂魄妖梦" }
                "ja" -> when (characterId) { 0 -> "博麗霊夢"; 1 -> "霧雨魔理沙"; else -> "魂魄妖夢" }
                else -> when (characterId) { 0 -> "Reimu"; 1 -> "Marisa"; else -> "Youmu" }
            }
            val shotStr = when (lang) {
                "zh" -> when (subshotId) { 0 -> "狼灵 (Wolf)"; 1 -> "獭灵 (Otter)"; else -> "鹰灵 (Eagle)" }
                "ja" -> when (subshotId) { 0 -> "狼 (Wolf)"; 1 -> "カワウソ (Otter)"; else -> "大鷲 (Eagle)" }
                else -> when (subshotId) { 0 -> "Wolf"; 1 -> "Otter"; else -> "Eagle" }
            }
            "$charStr ($shotStr)"
        }
        "th18" -> {
            when (lang) {
                "zh" -> when (characterId) { 0 -> "博丽灵梦"; 1 -> "雾雨魔理沙"; 2 -> "十六夜咲夜"; else -> "东风谷早苗" }
                "ja" -> when (characterId) { 0 -> "博麗霊夢"; 1 -> "霧雨魔理沙"; 2 -> "十六夜咲夜"; else -> "東風谷早苗" }
                else -> when (characterId) { 0 -> "Reimu Hakurei"; 1 -> "Marisa Kirisame"; 2 -> "Sakuya Izayoi"; else -> "Sanae Kochiya" }
            }
        }
        "th19" -> {
            when (lang) {
                "zh" -> when (characterId) {
                    0 -> "博丽灵梦"; 1 -> "雾雨魔理沙"; 2 -> "东风谷早苗"; 3 -> "八云蓝"; 4 -> "高丽野阿吽"
                    5 -> "纳兹琳"; 6 -> "清兰"; 7 -> "火焰猫燐"; 8 -> "管牧典"; 9 -> "二ッ岩猯藏"
                    10 -> "吉吊八千慧"; 11 -> "骊驹早鬼"; 12 -> "饕餮尤魔"; 13 -> "伊吹萃香"; 14 -> "孙美天"
                    15 -> "山城惑衣"; 16 -> "天火人千亦"; 17 -> "豫母都日狭美"; 18 -> "日白残无"
                    else -> "未知人物 ($characterId)"
                }
                "ja" -> when (characterId) {
                    0 -> "博麗霊夢"; 1 -> "霧雨魔理沙"; 2 -> "東風谷早苗"; 3 -> "八雲藍"; 4 -> "高麗野アウン"
                    5 -> "ナズーリン"; 6 -> "清蘭"; 7 -> "火焔猫燐"; 8 -> "菅牧典"; 9 -> "二ッ岩マミゾウ"
                    10 -> "吉吊八千慧"; 11 -> "驪駒早鬼"; 12 -> "饕餮尤魔"; 13 -> "伊吹萃香"; 14 -> "孫美天"
                    15 -> "山城ゑのこ"; 16 -> "天火人ちやり"; 17 -> "豫母都日狭美"; 18 -> "日白残無"
                    else -> "未知人物 ($characterId)"
                }
                else -> when (characterId) {
                    0 -> "Reimu Hakurei"; 1 -> "Marisa Kirisame"; 2 -> "Sanae Kochiya"; 3 -> "Ran Yakumo"; 4 -> "Aunn Komano"
                    5 -> "Nazrin"; 6 -> "Seiran"; 7 -> "Rin Kaenbyou"; 8 -> "Tsukasa Kudamaki"; 9 -> "Mamizou Futatsuiwa"
                    10 -> "Yachie Kicchou"; 11 -> "Saki Kurokoma"; 12 -> "Yuuma Toutetsu"; 13 -> "Suika Ibuki"; 14 -> "Son Biten"
                    15 -> "Enoko Mitsugashira"; 16 -> "Chiyari Tenkajin"; 17 -> "Hisami Yomotsu"; 18 -> "Zanmu Nippaku"
                    else -> "Unknown Chara ($characterId)"
                }
            }
        }
        "th20" -> {
            when (lang) {
                "zh" -> when (characterId) { 0 -> "博丽灵梦"; 1 -> "雾雨魔理沙"; else -> "未知人物 ($characterId)" }
                "ja" -> when (characterId) { 0 -> "博麗霊夢"; 1 -> "霧雨魔理沙"; else -> "未知人物 ($characterId)" }
                else -> when (characterId) { 0 -> "Reimu Hakurei"; 1 -> "Marisa Kirisame"; else -> "Unknown Chara ($characterId)" }
            }
        }
        else -> "Character: $characterId, Subshot: $subshotId"
    }
}

fun getLocalizedCharaName(gameId: String, characterId: Int, subshotId: Int, lang: String): String {
    return getCharaAndShottypeName(gameId, characterId, subshotId, lang)
}
