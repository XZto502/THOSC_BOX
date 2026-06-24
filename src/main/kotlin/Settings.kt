import java.awt.*
import java.awt.Color
import java.awt.Dimension
import com.illposed.osc.transport.OSCPortOut
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class Settings(
    val language: String,
    val oscPort: Int = 9000,
    val enableChatbox: Boolean = true
)

@Volatile
var activeLang: String = "zh"

@Volatile
var activeOscPort: Int = 9000

@Volatile
var activeEnableChatbox: Boolean = true

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
            override fun paintComponent(g: Graphics) {
                val g2d = g.create() as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
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
        titleLabel.alignmentX = Component.CENTER_ALIGNMENT
        titleLabel.font = getAppFont(Font.BOLD, 18)
        
        mainPanel.add(titleLabel)
        mainPanel.add(javax.swing.Box.createRigidArea(Dimension(0, 25)))

        var dragStart: Point? = null
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
                    font = getAppFont(Font.PLAIN, 15)
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
                override fun paintComponent(g: Graphics) {
                    val g2d = g.create() as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2d.color = if (isHovered) MD3Color.SecondaryContainer else MD3Color.Surface.brighter()
                    g2d.fillRoundRect(1, 1, width - 2, height - 2, 16, 16)
                    g2d.color = MD3Color.Outline
                    g2d.drawRoundRect(1, 1, width - 3, height - 3, 16, 16)
                    super.paintComponent(g)
                    g2d.dispose()
                }
            }
            btn.alignmentX = Component.CENTER_ALIGNMENT
            btn.maximumSize = Dimension(300, 40)
            btn.preferredSize = Dimension(300, 40)
            btn.addActionListener {
                choice = lang
                dialog.dispose()
            }
            return btn
        }

        mainPanel.add(createLangBtn("简体中文 (Chinese)", "zh"))
        mainPanel.add(javax.swing.Box.createRigidArea(Dimension(0, 10)))
        mainPanel.add(createLangBtn("English (English)", "en"))
        mainPanel.add(javax.swing.Box.createRigidArea(Dimension(0, 10)))
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
        val settings = Settings(choice, activeOscPort, activeEnableChatbox)
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

fun changeLanguage(lang: String) {
    activeLang = lang
    try {
        val settings = Settings(lang, activeOscPort, activeEnableChatbox)
        java.io.File("settings.json").writeText(Json.encodeToString(settings))
    } catch (e: Exception) {
        // Ignore
    }
    java.awt.EventQueue.invokeLater {
        updateTrayLabels()
        mainWindow?.refreshUILabels()
    }
}

fun changeSettings(lang: String, port: Int, enableChatbox: Boolean) {
    activeLang = lang
    activeEnableChatbox = enableChatbox
    
    if (activeOscPort != port) {
        try {
            activeOscSender?.close()
        } catch (e: Exception) {
            // Ignore
        }
        activeOscPort = port
        try {
            activeOscSender = OSCPortOut(java.net.InetSocketAddress("127.0.0.1", port))
        } catch (e: Exception) {
            println("Error updating OSC port: ${e.message}")
        }
    }

    try {
        val settings = Settings(lang, port, enableChatbox)
        java.io.File("settings.json").writeText(Json.encodeToString(settings))
    } catch (e: Exception) {
        println("Warning: Failed to save settings.json: ${e.message}")
    }

    java.awt.EventQueue.invokeLater {
        mainWindow?.refreshUILabels()
        updateTrayLabels()
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
            "zh" -> "东方花映塚 ~ Phantasmagoria of Flower View"
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
            "zh" -> "东方バブルダスト ~ Bubble Dust"
            "ja" -> "東方バブルダスト ~ Bubble Dust"
            else -> "Bubble Dust"
        }
        else -> gameId
    }
}

fun getSpellName(gameId: String, spellId: Int, lang: String): String {
    val zh = when (gameId) {
        "th17" -> when (spellId) {
            0 -> "石符「生石花的孤独」"
            1 -> "石符「生石花的孤独」"
            2 -> "石符「生石花的孤独」"
            else -> "未知符卡 ($spellId)"
        }
        else -> "未知符卡 ($spellId)"
    }
    val ja = when (gameId) {
        "th17" -> when (spellId) {
            0 -> "石符「リトスの孤独」"
            1 -> "石符「リトスの孤独」"
            2 -> "石符「リトスの孤独」"
            else -> "未知のスペルカード ($spellId)"
        }
        else -> "未知のスペルカード ($spellId)"
    }
    val en = when (gameId) {
        "th17" -> when (spellId) {
            0 -> "Stone Sign \"Solitary Lithops\""
            1 -> "Stone Sign \"Solitary Lithops\""
            2 -> "Stone Sign \"Solitary Lithops\""
            else -> "Unknown Spellcard ($spellId)"
        }
        else -> "Unknown Spellcard ($spellId)"
    }
    return when (lang) {
        "zh" -> zh
        "ja" -> ja
        else -> en
    }
}

fun getDifficultyName(gameId: String, difficultyId: Int, lang: String): String {
    return when (lang) {
        "zh" -> when (difficultyId) {
            0 -> "Easy"
            1 -> "Normal"
            2 -> "Hard"
            3 -> "Lunatic"
            4 -> "Extra"
            5 -> "Phantasm"
            else -> "Unknown ($difficultyId)"
        }
        "ja" -> when (difficultyId) {
            0 -> "Easy"
            1 -> "Normal"
            2 -> "Hard"
            3 -> "Lunatic"
            4 -> "Extra"
            5 -> "Phantasm"
            else -> "Unknown ($difficultyId)"
        }
        else -> when (difficultyId) {
            0 -> "Easy"
            1 -> "Normal"
            2 -> "Hard"
            3 -> "Lunatic"
            4 -> "Extra"
            5 -> "Phantasm"
            else -> "Unknown ($difficultyId)"
        }
    }
}

fun getCharaAndShottypeName(gameId: String, characterId: Int, subshotId: Int, lang: String): String {
    return when (gameId) {
        "th06" -> {
            val charStr = when (characterId) {
                0 -> when (lang) { "zh" -> "博丽灵梦"; "ja" -> "博麗霊夢"; else -> "Reimu Hakurei" }
                1 -> when (lang) { "zh" -> "雾雨魔理沙"; "ja" -> "霧雨魔理沙"; else -> "Marisa Kirisame" }
                else -> "Unknown"
            }
            val subStr = when (characterId) {
                0 -> when (subshotId) {
                    0 -> when (lang) { "zh" -> "灵符 (追尾) [A]"; "ja" -> "霊符 (誘導) [A]"; else -> "Reimu A (Homing)" }
                    1 -> when (lang) { "zh" -> "梦符 (针) [B]"; "ja" -> "夢符 (針) [B]"; else -> "Reimu B (Needle)" }
                    else -> "Unknown"
                }
                1 -> when (subshotId) {
                    0 -> when (lang) { "zh" -> "魔符 (前向) [A]"; "ja" -> "魔符 (前方) [A]"; else -> "Marisa A (Forward)" }
                    1 -> when (lang) { "zh" -> "恋符 (激光) [B]"; "ja" -> "恋符 (レーザー) [B]"; else -> "Marisa B (Laser)" }
                    else -> "Unknown"
                }
                else -> "Unknown"
            }
            "$charStr ($subStr)"
        }
        "th07" -> {
            val charStr = when (characterId) {
                0 -> when (lang) { "zh" -> "博丽灵梦"; "ja" -> "博麗霊夢"; else -> "Reimu Hakurei" }
                1 -> when (lang) { "zh" -> "雾雨魔理沙"; "ja" -> "霧雨魔理沙"; else -> "Marisa Kirisame" }
                2 -> when (lang) { "zh" -> "十六夜咲夜"; "ja" -> "十六夜咲夜"; else -> "Sakuya Izayoi" }
                else -> "Unknown"
            }
            val subStr = when (characterId) {
                0 -> when (subshotId) {
                    0 -> "A"
                    1 -> "B"
                    else -> "Unknown"
                }
                1 -> when (subshotId) {
                    0 -> "A"
                    1 -> "B"
                    else -> "Unknown"
                }
                2 -> when (subshotId) {
                    0 -> "A"
                    1 -> "B"
                    else -> "Unknown"
                }
                else -> "Unknown"
            }
            "$charStr ($subStr)"
        }
        "th08" -> {
            val charStr = when (characterId) {
                0 -> when (lang) { "zh" -> "结界组 (灵梦 & 紫)"; "ja" -> "結界組 (霊夢 & 紫)"; else -> "Boundary Team" }
                1 -> when (lang) { "zh" -> "咏唱组 (魔理沙 & 爱丽丝)"; "ja" -> "詠唱組 (魔理沙 & アリス)"; else -> "Magic Team" }
                2 -> when (lang) { "zh" -> "红魔组 (咲夜 & 雷米莉亚)"; "ja" -> "紅魔組 (咲夜 & レミリア)"; else -> "Scarlet Team" }
                3 -> when (lang) { "zh" -> "幽冥组 (妖梦 & 幽幽子)"; "ja" -> "幽冥組 (妖夢 & 幽々子)"; else -> "Netherworld Team" }
                4 -> when (lang) { "zh" -> "单人 博丽灵梦"; "ja" -> "単体 博麗霊夢"; else -> "Reimu Hakurei" }
                5 -> when (lang) { "zh" -> "单人 八云紫"; "ja" -> "単体 八雲紫"; else -> "Yukari Yakumo" }
                6 -> when (lang) { "zh" -> "单人 雾雨魔理沙"; "ja" -> "単体 霧雨魔理沙"; else -> "Marisa Kirisame" }
                7 -> when (lang) { "zh" -> "单人 爱丽丝"; "ja" -> "単体 アリス"; else -> "Alice Margatroid" }
                8 -> when (lang) { "zh" -> "单人 十六夜咲夜"; "ja" -> "単体 十六夜咲夜"; else -> "Sakuya Izayoi" }
                9 -> when (lang) { "zh" -> "单人 雷米莉亚"; "ja" -> "単体 レミリア"; else -> "Remilia Scarlet" }
                10 -> when (lang) { "zh" -> "单人 魂魄妖梦"; "ja" -> "単体 魂魄妖夢"; else -> "Youmu Konpaku" }
                11 -> when (lang) { "zh" -> "单人 西行寺幽幽子"; "ja" -> "単体 西行寺幽々子"; else -> "Yuyuko Saigyouji" }
                else -> "Unknown"
            }
            charStr
        }
        "th10" -> {
            val charStr = when (characterId) {
                0 -> when (lang) { "zh" -> "博丽灵梦"; "ja" -> "博麗霊夢"; else -> "Reimu Hakurei" }
                1 -> when (lang) { "zh" -> "雾雨魔理沙"; "ja" -> "霧雨魔理沙"; else -> "Marisa Kirisame" }
                else -> "Unknown"
            }
            val subStr = when (characterId) {
                0 -> when (subshotId) {
                    0 -> when (lang) { "zh" -> "追踪"; "ja" -> "誘導"; else -> "Homing" }
                    1 -> when (lang) { "zh" -> "针"; "ja" -> "前方集中"; else -> "Needle" }
                    2 -> when (lang) { "zh" -> "封魔阵"; "ja" -> "封魔陣"; else -> "Spread" }
                    else -> "Unknown"
                }
                1 -> when (subshotId) {
                    0 -> when (lang) { "zh" -> "高威力"; "ja" -> "高威力"; else -> "High Power" }
                    1 -> when (lang) { "zh" -> "穿透激光"; "ja" -> "貫通レーザー"; else -> "Laser" }
                    2 -> when (lang) { "zh" -> "单向集中"; "ja" -> "後方装備"; else -> "Spread" }
                    else -> "Unknown"
                }
                else -> "Unknown"
            }
            "$charStr ($subStr)"
        }
        "th11" -> {
            val charStr = when (characterId) {
                0 -> when (lang) { "zh" -> "博丽灵梦"; "ja" -> "博麗霊夢"; else -> "Reimu Hakurei" }
                1 -> when (lang) { "zh" -> "雾雨魔理沙"; "ja" -> "霧雨魔理沙"; else -> "Marisa Kirisame" }
                else -> "Unknown"
            }
            val subStr = when (characterId) {
                0 -> when (subshotId) {
                    0 -> when (lang) { "zh" -> "八云紫 (追踪)"; "ja" -> "八雲紫 (誘導)"; else -> "Yukari Support" }
                    1 -> when (lang) { "zh" -> "伊吹萃香 (贯通)"; "ja" -> "伊吹萃香 (貫通)"; else -> "Suika Support" }
                    2 -> when (lang) { "zh" -> "射命丸文 (特化)"; "ja" -> "射命丸文 (特化)"; else -> "Aya Support" }
                    else -> "Unknown"
                }
                1 -> when (subshotId) {
                    0 -> when (lang) { "zh" -> "爱丽丝 (人形)"; "ja" -> "アリス (人形)"; else -> "Alice Support" }
                    1 -> when (lang) { "zh" -> "帕秋莉 (属性)"; "ja" -> "パチュリー (属性)"; else -> "Patchouli Support" }
                    2 -> when (lang) { "zh" -> "河城荷取 (防御)"; "ja" -> "河城にとり (防御)"; else -> "Nitori Support" }
                    else -> "Unknown"
                }
                else -> "Unknown"
            }
            "$charStr ($subStr)"
        }
        "th12" -> {
            val charStr = when (characterId) {
                0 -> when (lang) { "zh" -> "博丽灵梦"; "ja" -> "博麗霊夢"; else -> "Reimu Hakurei" }
                1 -> when (lang) { "zh" -> "雾雨魔理沙"; "ja" -> "霧雨魔理沙"; else -> "Marisa Kirisame" }
                2 -> when (lang) { "zh" -> "东风谷早苗"; "ja" -> "東風谷早苗"; else -> "Sanae Kochiya" }
                else -> "Unknown"
            }
            val subStr = when (characterId) {
                0 -> when (subshotId) {
                    0 -> "A"
                    1 -> "B"
                    else -> "Unknown"
                }
                1 -> when (subshotId) {
                    0 -> "A"
                    1 -> "B"
                    else -> "Unknown"
                }
                2 -> when (subshotId) {
                    0 -> "A"
                    1 -> "B"
                    else -> "Unknown"
                }
                else -> "Unknown"
            }
            "$charStr ($subStr)"
        }
        "th13" -> {
            val charStr = when (characterId) {
                0 -> when (lang) { "zh" -> "博丽灵梦"; "ja" -> "博麗霊夢"; else -> "Reimu Hakurei" }
                1 -> when (lang) { "zh" -> "雾雨魔理沙"; "ja" -> "霧雨魔理沙"; else -> "Marisa Kirisame" }
                2 -> when (lang) { "zh" -> "东风谷早苗"; "ja" -> "東風谷早苗"; else -> "Sanae Kochiya" }
                3 -> when (lang) { "zh" -> "魂魄妖梦"; "ja" -> "魂魄妖夢"; else -> "Youmu Konpaku" }
                else -> "Unknown"
            }
            charStr
        }
        "th14" -> {
            val charStr = when (characterId) {
                0 -> when (lang) { "zh" -> "博丽灵梦"; "ja" -> "博麗霊夢"; else -> "Reimu Hakurei" }
                1 -> when (lang) { "zh" -> "雾雨魔理沙"; "ja" -> "霧雨魔理沙"; else -> "Marisa Kirisame" }
                2 -> when (lang) { "zh" -> "十六夜咲夜"; "ja" -> "十六夜咲夜"; else -> "Sakuya Izayoi" }
                else -> "Unknown"
            }
            val subStr = when (characterId) {
                0 -> when (subshotId) {
                    0 -> when (lang) { "zh" -> "妖器"; "ja" -> "妖器"; else -> "Weapon" }
                    1 -> when (lang) { "zh" -> "无武器"; "ja" -> "無武器"; else -> "No Weapon" }
                    else -> "Unknown"
                }
                1 -> when (subshotId) {
                    0 -> when (lang) { "zh" -> "妖器"; "ja" -> "妖器"; else -> "Weapon" }
                    1 -> when (lang) { "zh" -> "无武器"; "ja" -> "無武器"; else -> "No Weapon" }
                    else -> "Unknown"
                }
                2 -> when (subshotId) {
                    0 -> when (lang) { "zh" -> "妖器"; "ja" -> "妖器"; else -> "Weapon" }
                    1 -> when (lang) { "zh" -> "无武器"; "ja" -> "無武器"; else -> "No Weapon" }
                    else -> "Unknown"
                }
                else -> "Unknown"
            }
            "$charStr ($subStr)"
        }
        "th15" -> {
            val charStr = when (characterId) {
                0 -> when (lang) { "zh" -> "博丽灵梦"; "ja" -> "博麗霊夢"; else -> "Reimu Hakurei" }
                1 -> when (lang) { "zh" -> "雾雨魔理沙"; "ja" -> "霧雨魔理沙"; else -> "Marisa Kirisame" }
                2 -> when (lang) { "zh" -> "东风谷早苗"; "ja" -> "東風谷早苗"; else -> "Sanae Kochiya" }
                3 -> when (lang) { "zh" -> "铃仙·优昙华院·因幡"; "ja" -> "鈴仙・優曇華院・イナバ"; else -> "Reisen U. Inaba" }
                else -> "Unknown"
            }
            charStr
        }
        "th16" -> {
            val charStr = when (characterId) {
                0 -> when (lang) { "zh" -> "博丽灵梦"; "ja" -> "博麗霊夢"; else -> "Reimu Hakurei" }
                1 -> when (lang) { "zh" -> "雾雨魔理沙"; "ja" -> "霧雨魔理沙"; else -> "Marisa Kirisame" }
                2 -> when (lang) { "zh" -> "射命丸文"; "ja" -> "射命丸文"; else -> "Aya Shameimaru" }
                3 -> when (lang) { "zh" -> "琪露诺"; "ja" -> "チルノ"; else -> "Cirno" }
                else -> "Unknown"
            }
            val subStr = when (subshotId) {
                0 -> when (lang) { "zh" -> "春"; "ja" -> "春"; else -> "Spring" }
                1 -> when (lang) { "zh" -> "夏"; "ja" -> "夏"; else -> "Summer" }
                2 -> when (lang) { "zh" -> "秋"; "ja" -> "秋"; else -> "Autumn" }
                3 -> when (lang) { "zh" -> "冬"; "ja" -> "冬"; else -> "Winter" }
                4 -> when (lang) { "zh" -> "超二度"; "ja" -> "超二度"; else -> "Extra" }
                else -> "Unknown"
            }
            "$charStr ($subStr)"
        }
        "th17" -> {
            val charStr = when (characterId) {
                0 -> when (lang) { "zh" -> "博丽灵梦"; "ja" -> "博麗霊夢"; else -> "Reimu Hakurei" }
                1 -> when (lang) { "zh" -> "雾雨魔理沙"; "ja" -> "霧雨魔理沙"; else -> "Marisa Kirisame" }
                2 -> when (lang) { "zh" -> "魂魄妖梦"; "ja" -> "魂魄妖夢"; else -> "Youmu Konpaku" }
                else -> "Unknown"
            }
            val subStr = when (subshotId) {
                0 -> when (lang) { "zh" -> "狼灵"; "ja" -> "オオカミ"; else -> "Wolf" }
                1 -> when (lang) { "zh" -> "水獭灵"; "ja" -> "カワウソ"; else -> "Otter" }
                2 -> when (lang) { "zh" -> "雕灵"; "ja" -> "オオワシ"; else -> "Eagle" }
                else -> "Unknown"
            }
            "$charStr ($subStr)"
        }
        "th18" -> {
            val charStr = when (characterId) {
                0 -> when (lang) { "zh" -> "博丽灵梦"; "ja" -> "博麗霊夢"; else -> "Reimu Hakurei" }
                1 -> when (lang) { "zh" -> "雾雨魔理沙"; "ja" -> "霧雨魔理沙"; else -> "Marisa Kirisame" }
                2 -> when (lang) { "zh" -> "东风谷早苗"; "ja" -> "東風谷早苗"; else -> "Sanae Kochiya" }
                3 -> when (lang) { "zh" -> "十六夜咲夜"; "ja" -> "十六夜咲夜"; else -> "Sakuya Izayoi" }
                else -> "Unknown"
            }
            charStr
        }
        "th19" -> {
            when (lang) {
                "zh" -> when (characterId) {
                    0 -> "博丽灵梦"; 1 -> "雾雨魔理沙"; 2 -> "东风谷早苗"; 3 -> "八云蓝"; 4 -> "高丽野阿吽"
                    5 -> "纳兹琳"; 6 -> "清兰"; 7 -> "火焰猫燐"; 8 -> "菅牧典"; 9 -> "二岩猯藏"
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
