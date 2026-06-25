import java.awt.*
import javax.swing.*

var mainWindow: MainWindow? = null

fun updateLiveStats(
    gameName: String,
    characterName: String,
    stage: String,
    score: Int,
    miss: Int,
    bomb: Int,
    graze: Int,
    power: Float,
    point: Int
) {
    java.awt.EventQueue.invokeLater {
        mainWindow?.apply {
            gameCard.updateValue(gameName)
            charaCard.updateValue(characterName)
            stageCard.updateValue(stage)
            scoreCard.updateValue(score.toString())
            livesCard.updateValue(miss.toString())
            bombsCard.updateValue(bomb.toString())
            grazeCard.updateValue(graze.toString())
            powerCard.updateValue(String.format(java.util.Locale.US, "%.2f", power))
            pointCard.updateValue(point.toString())
        }
    }
}

class MainWindow : javax.swing.JFrame() {
    val btnDashboard = MD3TabButton("Dashboard", true)
    val btnSettings = MD3TabButton("Settings", false)
    val btnLogs = MD3TabButton("Logs", false)

    val btnTouhouMode = MD3Button("Touhou / 东方", if (activeMode == "touhou") MD3Button.ButtonType.FILLED else MD3Button.ButtonType.OUTLINED, radius = 20)
    val btnOsuMode = MD3Button("osu!", if (activeMode == "osu") MD3Button.ButtonType.FILLED else MD3Button.ButtonType.OUTLINED, radius = 20)

    val gameCard = StatCard("Active Game", "Scanning...")
    val charaCard = StatCard("Character", "N/A")
    val stageCard = StatCard("Stage", "N/A")
    val scoreCard = StatCard("Score", "0")
    val livesCard = StatCard("Lives/Miss", "0")
    val bombsCard = StatCard("Bombs", "0")
    val grazeCard = StatCard("Graze", "0")
    val powerCard = StatCard("Power", "0.00")
    val pointCard = StatCard("Point/PIV", "0")

    val langLabel = javax.swing.JLabel()
    val langComboBox = javax.swing.JComboBox(arrayOf("简体中文", "English", "日本語"))
    val portLabel = javax.swing.JLabel()
    val portField = MD3TextField(8)
    val chatboxCheckBox = javax.swing.JCheckBox()
    val cbShowGame = javax.swing.JCheckBox()
    val cbShowChara = javax.swing.JCheckBox()
    val cbShowStage = javax.swing.JCheckBox()
    val cbShowScore = javax.swing.JCheckBox()
    val cbShowMiss = javax.swing.JCheckBox()
    val cbShowBomb = javax.swing.JCheckBox()
    val cbShowAcc = javax.swing.JCheckBox()
    val themeColorLabel = javax.swing.JLabel()
    val btnChooseColor = MD3Button("", MD3Button.ButtonType.OUTLINED, radius = 12)
    val btnSaveSettings = MD3Button("Save Settings", MD3Button.ButtonType.FILLED)

    val logArea = javax.swing.JTextArea().apply {
        isEditable = false
        background = Color(0x15, 0x14, 0x19)
        foreground = Color(0xE6, 0xE1, 0xE5)
        font = getMonospacedFont(12)
        border = javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)
    }

    val btnClear = MD3Button("", MD3Button.ButtonType.OUTLINED, radius = 20)
    val btnCopy = MD3Button("", MD3Button.ButtonType.OUTLINED, radius = 20)

    init {
        title = "THOSC_BOX"
        defaultCloseOperation = javax.swing.JFrame.EXIT_ON_CLOSE
        setSize(850, 600)
        setLocationRelativeTo(null)
        background = MD3Color.Background
        contentPane.background = MD3Color.Background
        iconImage = createTrayIconImage()

        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent?) {
                System.exit(0)
            }
        })

        val rootPanel = javax.swing.JPanel(BorderLayout()).apply {
            background = MD3Color.Background
            border = javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16)
        }

        val headerPanel = javax.swing.JPanel(BorderLayout()).apply {
            background = MD3Color.Background
            isOpaque = false
            border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 16, 0)
        }
        val titleTextPanel = javax.swing.JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            background = MD3Color.Background
            isOpaque = false
        }
        val mainTitleLabel = object : javax.swing.JLabel("THOSC_BOX") {
            override fun getForeground(): Color = MD3Color.Primary
        }.apply {
            font = getAppFont(Font.BOLD, 22)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        val subTitleLabel = javax.swing.JLabel("VRChat OSC Bridge v1.0").apply {
            foreground = MD3Color.TextSecondary
            font = getAppFont(Font.PLAIN, 12)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        titleTextPanel.add(mainTitleLabel)
        titleTextPanel.add(subTitleLabel)
        headerPanel.add(titleTextPanel, BorderLayout.WEST)

        val modePanel = javax.swing.JPanel(java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0)).apply {
            background = MD3Color.Background
            isOpaque = false
            add(btnTouhouMode)
            add(btnOsuMode)
        }
        headerPanel.add(modePanel, BorderLayout.EAST)

        rootPanel.add(headerPanel, BorderLayout.NORTH)

        btnTouhouMode.addActionListener {
            if (activeMode != "touhou") {
                btnTouhouMode.type = MD3Button.ButtonType.FILLED
                btnOsuMode.type = MD3Button.ButtonType.OUTLINED
                changeMode("touhou")
            }
        }
        btnOsuMode.addActionListener {
            if (activeMode != "osu") {
                btnTouhouMode.type = MD3Button.ButtonType.OUTLINED
                btnOsuMode.type = MD3Button.ButtonType.FILLED
                changeMode("osu")
            }
        }

        updateCardLabels()

        val bodyPanel = javax.swing.JPanel(BorderLayout()).apply {
            background = MD3Color.Background
            isOpaque = false
        }

        val sidebarPanel = javax.swing.JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            background = MD3Color.Background
            isOpaque = false
            preferredSize = Dimension(180, 0)
            border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 16)
        }

        btnDashboard.maximumSize = Dimension(160, 40)
        btnSettings.maximumSize = Dimension(160, 40)
        btnLogs.maximumSize = Dimension(160, 40)

        sidebarPanel.add(btnDashboard)
        sidebarPanel.add(javax.swing.Box.createRigidArea(Dimension(0, 8)))
        sidebarPanel.add(btnSettings)
        sidebarPanel.add(javax.swing.Box.createRigidArea(Dimension(0, 8)))
        sidebarPanel.add(btnLogs)

        bodyPanel.add(sidebarPanel, BorderLayout.WEST)

        val contentCardLayout = CardLayout()
        val contentPanel = javax.swing.JPanel(contentCardLayout).apply {
            background = MD3Color.Background
            isOpaque = false
        }

        btnDashboard.addActionListener {
            btnDashboard.updateActive(true)
            btnSettings.updateActive(false)
            btnLogs.updateActive(false)
            contentCardLayout.show(contentPanel, "dashboard")
        }
        btnSettings.addActionListener {
            btnDashboard.updateActive(false)
            btnSettings.updateActive(true)
            btnLogs.updateActive(false)
            contentCardLayout.show(contentPanel, "settings")
        }
        btnLogs.addActionListener {
            btnDashboard.updateActive(false)
            btnSettings.updateActive(false)
            btnLogs.updateActive(true)
            contentCardLayout.show(contentPanel, "logs")
        }

        val dashboardPanel = javax.swing.JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            background = MD3Color.Background
            isOpaque = false
        }

        val topCardsPanel = javax.swing.JPanel(GridLayout(1, 2, 12, 12)).apply {
            background = MD3Color.Background
            isOpaque = false
            maximumSize = Dimension(Short.MAX_VALUE.toInt(), 80)
        }
        topCardsPanel.add(gameCard)
        topCardsPanel.add(charaCard)

        dashboardPanel.add(topCardsPanel)
        dashboardPanel.add(javax.swing.Box.createRigidArea(Dimension(0, 12)))

        val statsGridPanel = javax.swing.JPanel(GridLayout(0, 3, 12, 12)).apply {
            background = MD3Color.Background
            isOpaque = false
        }
        statsGridPanel.add(stageCard)
        statsGridPanel.add(scoreCard)
        statsGridPanel.add(livesCard)
        
        statsGridPanel.add(bombsCard)
        statsGridPanel.add(powerCard)
        statsGridPanel.add(grazeCard)
        
        statsGridPanel.add(pointCard)

        dashboardPanel.add(statsGridPanel)
        contentPanel.add(dashboardPanel, "dashboard")

        val settingsPanel = javax.swing.JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            background = MD3Color.Background
            isOpaque = false
        }

        val settingsCard = MD3Card().apply {
            layout = GridBagLayout()
            background = MD3Color.Surface
        }

        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(8, 8, 8, 8)
            gridx = 0
            gridy = 0
            weightx = 0.3
        }

        langLabel.foreground = MD3Color.TextPrimary
        langLabel.font = getAppFont(Font.BOLD, 14)
        settingsCard.add(langLabel, gbc)

        gbc.gridx = 1
        gbc.weightx = 0.7
        langComboBox.apply {
            background = MD3Color.Surface
            foreground = MD3Color.TextPrimary
            font = getAppFont(Font.PLAIN, 14)
        }
        settingsCard.add(langComboBox, gbc)

        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.3
        portLabel.foreground = MD3Color.TextPrimary
        portLabel.font = getAppFont(Font.BOLD, 14)
        settingsCard.add(portLabel, gbc)

        gbc.gridx = 1
        gbc.weightx = 0.7
        settingsCard.add(portField, gbc)

        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        chatboxCheckBox.apply {
            background = MD3Color.Surface
            foreground = MD3Color.TextPrimary
            isFocusable = false
            isRolloverEnabled = true
            icon = MD3CheckboxIcon()
            iconTextGap = 8
            font = getAppFont(Font.PLAIN, 14)
        }
        settingsCard.add(chatboxCheckBox, gbc)

        // Chatbox options sub-panel
        val chatboxOptionsPanel = javax.swing.JPanel(java.awt.GridLayout(0, 2, 16, 8)).apply {
            background = MD3Color.Surface
            isOpaque = false
            border = javax.swing.BorderFactory.createEmptyBorder(4, 24, 8, 8)
        }
        val detailCheckboxes = listOf(cbShowGame, cbShowChara, cbShowStage, cbShowScore, cbShowMiss, cbShowBomb, cbShowAcc)
        detailCheckboxes.forEach { cb ->
            cb.apply {
                background = MD3Color.Surface
                foreground = MD3Color.TextPrimary
                isFocusable = false
                isRolloverEnabled = true
                icon = MD3CheckboxIcon()
                iconTextGap = 8
                font = getAppFont(Font.PLAIN, 14)
            }
            chatboxOptionsPanel.add(cb)
        }

        gbc.gridx = 0
        gbc.gridy = 3
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        settingsCard.add(chatboxOptionsPanel, gbc)

        // Theme color row
        gbc.gridx = 0
        gbc.gridy = 4
        gbc.gridwidth = 1
        gbc.weightx = 0.3
        themeColorLabel.foreground = MD3Color.TextPrimary
        themeColorLabel.font = getAppFont(Font.BOLD, 14)
        settingsCard.add(themeColorLabel, gbc)

        gbc.gridx = 1
        gbc.weightx = 0.7
        val colorSelectPanel = javax.swing.JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0)).apply {
            background = MD3Color.Surface
            isOpaque = false
        }
        val colorPreview = object : javax.swing.JPanel() {
            init {
                preferredSize = Dimension(28, 28)
                isOpaque = false
            }
            override fun paintComponent(g: Graphics) {
                val g2d = g.create() as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2d.color = MD3Color.Primary
                g2d.fillRoundRect(0, 0, width, height, 8, 8)
                g2d.color = MD3Color.Outline
                g2d.stroke = java.awt.BasicStroke(1.2f)
                g2d.drawRoundRect(0, 0, width - 1, height - 1, 8, 8)
                g2d.dispose()
            }
        }
        colorSelectPanel.add(colorPreview)
        colorSelectPanel.add(btnChooseColor)
        settingsCard.add(colorSelectPanel, gbc)

        btnChooseColor.addActionListener {
            showMD3ColorChooser(
                this@MainWindow,
                when (activeLang) {
                    "zh" -> "选择主题颜色"
                    "ja" -> "テーマカラーを選択"
                    else -> "Choose Theme Color"
                },
                MD3Color.Primary
            ) { chosenColor ->
                changeThemeColor(chosenColor)
                colorPreview.repaint()
            }
        }

        gbc.gridx = 0
        gbc.gridy = 5
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        gbc.insets = Insets(16, 8, 8, 8)
        settingsCard.add(btnSaveSettings, gbc)

        val settingsWrapper = javax.swing.JPanel(BorderLayout()).apply {
            background = MD3Color.Background
            isOpaque = false
        }
        settingsWrapper.add(settingsCard, BorderLayout.NORTH)
        settingsPanel.add(settingsWrapper)
        contentPanel.add(settingsPanel, "settings")

        val logsPanel = javax.swing.JPanel(BorderLayout()).apply {
            background = MD3Color.Background
            isOpaque = false
        }
        
        btnClear.apply {
            font = getAppFont(Font.BOLD, 12)
            addActionListener {
                logArea.text = ""
            }
        }
        btnCopy.apply {
            font = getAppFont(Font.BOLD, 12)
            addActionListener {
                val selection = java.awt.datatransfer.StringSelection(logArea.text)
                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
            }
        }
        val debugControlsPanel = javax.swing.JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 4)).apply {
            background = MD3Color.Background
            isOpaque = false
            add(btnClear)
            add(btnCopy)
        }
        logsPanel.add(debugControlsPanel, BorderLayout.NORTH)

        val logScrollPane = javax.swing.JScrollPane(logArea).apply {
            border = javax.swing.BorderFactory.createLineBorder(MD3Color.Outline, 1)
            background = Color(0x15, 0x14, 0x19)
        }
        logsPanel.add(logScrollPane, BorderLayout.CENTER)
        contentPanel.add(logsPanel, "logs")

        bodyPanel.add(contentPanel, BorderLayout.CENTER)
        rootPanel.add(bodyPanel, BorderLayout.CENTER)

        contentPane.add(rootPanel)

        val initialIndex = when (activeLang) {
            "zh" -> 0
            "en" -> 1
            "ja" -> 2
            else -> 0
        }
        langComboBox.selectedIndex = initialIndex
        portField.text = activeOscPort.toString()
        chatboxCheckBox.isSelected = activeEnableChatbox
        cbShowGame.isSelected = activeChatboxShowGame
        cbShowChara.isSelected = activeChatboxShowChara
        cbShowStage.isSelected = activeChatboxShowStage
        cbShowScore.isSelected = activeChatboxShowScore
        cbShowMiss.isSelected = activeChatboxShowMiss
        cbShowBomb.isSelected = activeChatboxShowBomb
        cbShowAcc.isSelected = activeChatboxShowAcc

        val updateOptionsEnabledState = {
            val isEnabled = chatboxCheckBox.isSelected
            cbShowGame.isEnabled = isEnabled
            cbShowChara.isEnabled = isEnabled
            cbShowStage.isEnabled = isEnabled
            cbShowScore.isEnabled = isEnabled
            cbShowMiss.isEnabled = isEnabled
            cbShowBomb.isEnabled = isEnabled
            cbShowAcc.isEnabled = isEnabled
        }
        updateOptionsEnabledState()

        chatboxCheckBox.addActionListener {
            updateOptionsEnabledState()
        }

        btnSaveSettings.addActionListener {
            val selectedLang = when (langComboBox.selectedIndex) {
                0 -> "zh"
                1 -> "en"
                2 -> "ja"
                else -> "zh"
            }
            val enteredPort = portField.text.toIntOrNull() ?: 9000
            val isChatboxEnabled = chatboxCheckBox.isSelected

            changeSettings(
                selectedLang, enteredPort, isChatboxEnabled,
                cbShowGame.isSelected, cbShowChara.isSelected, cbShowStage.isSelected,
                cbShowScore.isSelected, cbShowMiss.isSelected, cbShowBomb.isSelected, cbShowAcc.isSelected
            )

            showMD3MessageDialog(
                this@MainWindow,
                getLocalizedString("save_success", activeLang)
            )
        }

        refreshUILabels()
    }

    fun updateCardLabels() {
        if (activeMode == "osu") {
            gameCard.updateLabel(when (activeLang) { "zh" -> "活动游戏"; "ja" -> "アクティブゲーム"; else -> "Active Game" })
            charaCard.updateLabel(when (activeLang) { "zh" -> "歌名"; "ja" -> "曲名"; else -> "Song Title" })
            stageCard.updateLabel(when (activeLang) { "zh" -> "难度"; "ja" -> "難易度"; else -> "Difficulty" })
            scoreCard.updateLabel(when (activeLang) { "zh" -> "得分"; "ja" -> "スコア"; else -> "Score" })
            livesCard.updateLabel(when (activeLang) { "zh" -> "Miss数"; "ja" -> "ミス数"; else -> "Misses" })
            bombsCard.updateLabel(when (activeLang) { "zh" -> "当前连击"; "ja" -> "コンボ"; else -> "Combo" })
            powerCard.updateLabel(when (activeLang) { "zh" -> "最大连击"; "ja" -> "最大コンボ"; else -> "Max Combo" })
            grazeCard.updateLabel(when (activeLang) { "zh" -> "准确率"; "ja" -> "精度"; else -> "Accuracy" })
            pointCard.updateLabel(when (activeLang) { "zh" -> "评级"; "ja" -> "ランク"; else -> "Grade" })
        } else {
            gameCard.updateLabel(getLocalizedString("stat_game", activeLang))
            charaCard.updateLabel(getLocalizedString("stat_chara", activeLang))
            stageCard.updateLabel(getLocalizedString("stat_stage", activeLang))
            scoreCard.updateLabel(getLocalizedString("stat_score", activeLang))
            livesCard.updateLabel(getLocalizedString("stat_lives", activeLang))
            bombsCard.updateLabel(getLocalizedString("stat_bombs", activeLang))
            grazeCard.updateLabel(getLocalizedString("stat_graze", activeLang))
            powerCard.updateLabel(getLocalizedString("stat_power", activeLang))
            pointCard.updateLabel(getLocalizedString("stat_point", activeLang))
        }
    }

    fun refreshUILabels() {
        btnDashboard.text = getLocalizedString("tab_dashboard", activeLang)
        btnSettings.text = getLocalizedString("tab_settings", activeLang)
        btnLogs.text = getLocalizedString("tab_logs", activeLang)

        updateCardLabels()

        btnTouhouMode.text = when (activeLang) {
            "zh" -> "东方 / Touhou"
            "ja" -> "東方 / Touhou"
            else -> "Touhou"
        }
        btnOsuMode.text = "osu!"

        langLabel.text = getLocalizedString("settings_lang", activeLang)
        portLabel.text = getLocalizedString("settings_port", activeLang)
        chatboxCheckBox.text = getLocalizedString("settings_chatbox", activeLang)
        cbShowGame.text = getLocalizedString("settings_chatbox_game", activeLang)
        cbShowChara.text = getLocalizedString("settings_chatbox_chara", activeLang)
        cbShowStage.text = getLocalizedString("settings_chatbox_stage", activeLang)
        cbShowScore.text = getLocalizedString("settings_chatbox_score", activeLang)
        cbShowMiss.text = getLocalizedString("settings_chatbox_miss", activeLang)
        cbShowBomb.text = getLocalizedString("settings_chatbox_bomb", activeLang)
        cbShowAcc.text = getLocalizedString("settings_chatbox_acc", activeLang)
        themeColorLabel.text = when (activeLang) {
            "zh" -> "主题颜色"
            "ja" -> "テーマカラー"
            else -> "Theme Color"
        }
        btnChooseColor.text = when (activeLang) {
            "zh" -> "选择"
            "ja" -> "選択"
            else -> "Choose"
        }
        btnSaveSettings.text = getLocalizedString("btn_save", activeLang)
        btnClear.text = getLocalizedString("btn_clear", activeLang)
        btnCopy.text = getLocalizedString("btn_copy", activeLang)

        title = getLocalizedString("title_app", activeLang)
    }

    fun updateScanningStatus() {
        val scanText = getLocalizedString("status_scanning", activeLang)
        gameCard.updateValue(scanText)
        charaCard.updateValue("N/A")
        stageCard.updateValue("N/A")
        scoreCard.updateValue("0")
        livesCard.updateValue("0")
        bombsCard.updateValue("0")
        grazeCard.updateValue("0")
        powerCard.updateValue("0.00")
        pointCard.updateValue("0")
    }
}
