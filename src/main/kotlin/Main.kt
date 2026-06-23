/*
 * THOSC_BOX - Touhou Project VRChat OSC Bridge
 * Copyright (C) 2026 Sanae-Koishi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import com.illposed.osc.OSCMessage
import com.illposed.osc.transport.OSCPortOut
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.Tlhelp32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.encodeToString
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.Duration
import java.net.InetSocketAddress
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File

fun getAppFont(style: Int, size: Int): Font {
    return Font("Dialog", style, size)
}

fun getMonospacedFont(size: Int): Font {
    return Font("DialogInput", Font.PLAIN, size)
}

fun showMD3MessageDialog(parent: java.awt.Frame, messageText: String) {
    val dialog = javax.swing.JDialog(parent, "", true)
    dialog.isUndecorated = true
    dialog.background = Color(0, 0, 0, 0)
    dialog.size = Dimension(350, 160)
    dialog.setLocationRelativeTo(parent)

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

    val titleVal = when (activeLang) {
        "zh" -> "提示"
        "ja" -> "お知らせ"
        else -> "Notification"
    }

    val titleLabel = object : javax.swing.JLabel(titleVal) {
        override fun getForeground(): Color = MD3Color.Primary
    }.apply {
        font = getAppFont(Font.BOLD, 16)
        alignmentX = Component.CENTER_ALIGNMENT
    }
    
    val msgLabel = javax.swing.JLabel(messageText).apply {
        foreground = MD3Color.TextPrimary
        font = getAppFont(Font.PLAIN, 14)
        alignmentX = Component.CENTER_ALIGNMENT
    }

    val btnText = when (activeLang) {
        "zh" -> "确定"
        "ja" -> "確定"
        else -> "OK"
    }

    val okBtn = MD3Button(btnText, MD3Button.ButtonType.FILLED, radius = 16).apply {
        alignmentX = Component.CENTER_ALIGNMENT
        maximumSize = Dimension(100, 36)
        preferredSize = Dimension(100, 36)
        addActionListener {
            dialog.dispose()
        }
    }

    mainPanel.add(titleLabel)
    mainPanel.add(javax.swing.Box.createRigidArea(Dimension(0, 12)))
    mainPanel.add(msgLabel)
    mainPanel.add(javax.swing.Box.createRigidArea(Dimension(0, 16)))
    mainPanel.add(okBtn)

    dialog.contentPane = mainPanel
    dialog.isVisible = true
}

fun showMD3ColorChooser(parent: java.awt.Frame, titleText: String, initialColor: Color, onColorSelected: (Color) -> Unit) {
    val dialog = javax.swing.JDialog(parent, "", true)
    dialog.isUndecorated = true
    dialog.background = Color(0, 0, 0, 0)
    dialog.size = Dimension(380, 360)
    dialog.setLocationRelativeTo(parent)

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

    val titleLabel = javax.swing.JLabel(titleText).apply {
        foreground = MD3Color.Primary
        font = getAppFont(Font.BOLD, 18)
        alignmentX = Component.CENTER_ALIGNMENT
    }
    mainPanel.add(titleLabel)
    mainPanel.add(javax.swing.Box.createRigidArea(Dimension(0, 16)))

    // Grid of preset colors (curated MD3 tones)
    val presets = listOf(
        Color(0xD0, 0xBC, 0xFF), // Lavender (Default)
        Color(0xFF, 0xB0, 0xC8), // Sakura Pink
        Color(0xA8, 0xDA, 0xB5), // Mint Green
        Color(0xA8, 0xC7, 0xFA), // Sky Blue
        Color(0x80, 0xF0, 0xDC), // Teal
        Color(0xFF, 0xB8, 0x99), // Peach Orange
        Color(0xF2, 0xE0, 0x88), // Lemon Yellow
        Color(0xFF, 0xB4, 0xAB), // Coral Red
        Color(0xE0, 0xE0, 0xE0), // Light Gray
        Color(0xF8, 0xBD, 0xE6)  // Orchid Pink
    )

    var selectedColor = initialColor

    val presetGridPanel = javax.swing.JPanel(java.awt.GridLayout(2, 5, 10, 10)).apply {
        background = MD3Color.Surface
        isOpaque = false
        alignmentX = Component.CENTER_ALIGNMENT
        maximumSize = Dimension(340, 90)
    }

    // We want a live preview panel
    val livePreview = object : javax.swing.JPanel() {
        init {
            preferredSize = Dimension(32, 32)
            minimumSize = Dimension(32, 32)
            maximumSize = Dimension(32, 32)
            isOpaque = false
        }
        override fun paintComponent(g: Graphics) {
            val g2d = g.create() as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.color = selectedColor
            g2d.fillRoundRect(0, 0, width, height, 10, 10)
            g2d.color = MD3Color.Outline
            g2d.stroke = java.awt.BasicStroke(1.2f)
            g2d.drawRoundRect(0, 0, width - 1, height - 1, 10, 10)
            g2d.dispose()
        }
    }

    // Input hex field
    val hexField = MD3TextField(8).apply {
        text = selectedColor.toHex()
        alignmentX = Component.CENTER_ALIGNMENT
        maximumSize = Dimension(150, 36)
    }

    // Track button borders or states for presets
    val presetButtons = mutableListOf<javax.swing.JButton>()

    fun updateSelection(color: Color, updateField: Boolean = true) {
        selectedColor = color
        livePreview.repaint()
        if (updateField) {
            hexField.text = color.toHex()
        }
        // Repaint all preset buttons to show selection outline
        presetGridPanel.repaint()
    }

    presets.forEach { color ->
        val btn = object : javax.swing.JButton() {
            init {
                isContentAreaFilled = false
                isBorderPainted = false
                isFocusable = false
                preferredSize = Dimension(32, 32)
            }
            override fun paintComponent(g: Graphics) {
                val g2d = g.create() as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2d.color = color
                g2d.fillRoundRect(2, 2, width - 4, height - 4, 8, 8)
                
                // If this color matches selectedColor (approximate or exact)
                if (color.rgb == selectedColor.rgb) {
                    g2d.color = MD3Color.Primary
                    g2d.stroke = java.awt.BasicStroke(2.0f)
                    g2d.drawRoundRect(0, 0, width - 1, height - 1, 10, 10)
                } else {
                    g2d.color = MD3Color.Outline
                    g2d.stroke = java.awt.BasicStroke(1.0f)
                    g2d.drawRoundRect(2, 2, width - 5, height - 5, 8, 8)
                }
                g2d.dispose()
            }
        }
        btn.addActionListener {
            updateSelection(color)
        }
        presetButtons.add(btn)
        presetGridPanel.add(btn)
    }

    mainPanel.add(presetGridPanel)
    mainPanel.add(javax.swing.Box.createRigidArea(Dimension(0, 20)))

    // Custom Hex input row
    val hexRow = javax.swing.JPanel(java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 0)).apply {
        background = MD3Color.Surface
        isOpaque = false
        alignmentX = Component.CENTER_ALIGNMENT
    }
    val hexLabel = javax.swing.JLabel(when (activeLang) {
        "zh" -> "十六进制:"
        "ja" -> "16進数:"
        else -> "Hex:"
    }).apply {
        foreground = MD3Color.TextPrimary
        font = getAppFont(Font.PLAIN, 14)
    }
    hexRow.add(hexLabel)
    hexRow.add(hexField)
    hexRow.add(livePreview)

    mainPanel.add(hexRow)
    mainPanel.add(javax.swing.Box.createRigidArea(Dimension(0, 24)))

    // Action Listener for live text typing
    hexField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
        override fun insertUpdate(e: javax.swing.event.DocumentEvent?) { checkHex() }
        override fun removeUpdate(e: javax.swing.event.DocumentEvent?) { checkHex() }
        override fun changedUpdate(e: javax.swing.event.DocumentEvent?) { checkHex() }
        private fun checkHex() {
            val text = hexField.text.trim()
            parseHexColor(text)?.let {
                updateSelection(it, updateField = false)
            }
        }
    })

    // OK and Cancel buttons
    val actionRow = javax.swing.JPanel(java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 16, 0)).apply {
        background = MD3Color.Surface
        isOpaque = false
        alignmentX = Component.CENTER_ALIGNMENT
    }
    val cancelBtnText = when (activeLang) {
        "zh" -> "取消"
        "ja" -> "キャンセル"
        else -> "Cancel"
    }
    val cancelBtn = MD3Button(cancelBtnText, MD3Button.ButtonType.OUTLINED, radius = 18).apply {
        preferredSize = Dimension(100, 36)
        addActionListener {
            dialog.dispose()
        }
    }
    val okBtnText = when (activeLang) {
        "zh" -> "确定"
        "ja" -> "確定"
        else -> "OK"
    }
    val okBtn = MD3Button(okBtnText, MD3Button.ButtonType.FILLED, radius = 18).apply {
        preferredSize = Dimension(100, 36)
        addActionListener {
            onColorSelected(selectedColor)
            dialog.dispose()
        }
    }
    actionRow.add(cancelBtn)
    actionRow.add(okBtn)

    mainPanel.add(actionRow)

    // Allow dragging the undecorated dialog
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

    dialog.contentPane = mainPanel
    dialog.isVisible = true
}

object LogManager {
    private val logBuffer = StringBuilder()
    private var logArea: javax.swing.JTextArea? = null
    private val originalOut = System.out
    private val originalErr = System.err
    private var logFile: java.io.File? = null

    fun init() {
        try {
            logFile = java.io.File("osc_box.log")
            logFile?.writeText("")
        } catch (e: Exception) {
            // Ignore
        }

        val redirectOut = object : java.io.OutputStream() {
            private val lineBuffer = java.io.ByteArrayOutputStream()
            override fun write(b: Int) {
                originalOut.write(b)
                lineBuffer.write(b)
                if (b == '\n'.code || b == '\r'.code) {
                    flushLine()
                }
            }
            override fun write(b: ByteArray, off: Int, len: Int) {
                originalOut.write(b, off, len)
                lineBuffer.write(b, off, len)
                var hasLineEnd = false
                for (i in off until (off + len)) {
                    if (b[i] == '\n'.code.toByte() || b[i] == '\r'.code.toByte()) {
                        hasLineEnd = true
                        break
                    }
                }
                if (hasLineEnd) {
                    flushLine()
                }
            }
            private fun flushLine() {
                val s = lineBuffer.toString("UTF-8")
                lineBuffer.reset()
                if (s.isNotEmpty()) {
                    appendLog(s)
                }
            }
        }

        val redirectErr = object : java.io.OutputStream() {
            private val lineBuffer = java.io.ByteArrayOutputStream()
            override fun write(b: Int) {
                originalErr.write(b)
                lineBuffer.write(b)
                if (b == '\n'.code || b == '\r'.code) {
                    flushLine()
                }
            }
            override fun write(b: ByteArray, off: Int, len: Int) {
                originalErr.write(b, off, len)
                lineBuffer.write(b, off, len)
                var hasLineEnd = false
                for (i in off until (off + len)) {
                    if (b[i] == '\n'.code.toByte() || b[i] == '\r'.code.toByte()) {
                        hasLineEnd = true
                        break
                    }
                }
                if (hasLineEnd) {
                    flushLine()
                }
            }
            private fun flushLine() {
                val s = lineBuffer.toString("UTF-8")
                lineBuffer.reset()
                if (s.isNotEmpty()) {
                    appendLog(s)
                }
            }
        }

        System.setOut(java.io.PrintStream(redirectOut, true, "UTF-8"))
        System.setErr(java.io.PrintStream(redirectErr, true, "UTF-8"))
    }

    private fun appendLog(s: String) {
        try {
            logFile?.appendText(s)
        } catch (e: Exception) {
            // Ignore
        }
        synchronized(logBuffer) {
            val area = logArea
            if (area != null) {
                javax.swing.SwingUtilities.invokeLater {
                    val currentText = area.text
                    if (s.startsWith("\r")) {
                        val lastNewline = currentText.lastIndexOf('\n')
                        if (lastNewline >= 0) {
                            area.text = currentText.substring(0, lastNewline + 1) + s.substring(1)
                        } else {
                            area.text = s.substring(1)
                        }
                    } else {
                        area.append(s)
                    }
                    if (area.text.length > 50000) {
                        area.text = area.text.substring(20000)
                    }
                    area.caretPosition = area.document.length
                }
            } else {
                if (s.startsWith("\r")) {
                    val lastNewline = logBuffer.lastIndexOf("\n")
                    if (lastNewline >= 0) {
                        logBuffer.setLength(lastNewline + 1)
                        logBuffer.append(s.substring(1))
                    } else {
                        logBuffer.setLength(0)
                        logBuffer.append(s.substring(1))
                    }
                } else {
                    logBuffer.append(s)
                }
                if (logBuffer.length > 50000) {
                    logBuffer.delete(0, 20000)
                }
            }
        }
    }

    fun setTextArea(area: javax.swing.JTextArea) {
        synchronized(logBuffer) {
            logArea = area
            area.text = logBuffer.toString()
            area.caretPosition = area.document.length
        }
    }
}

interface PsapiExt : StdCallLibrary {
    fun EnumProcessModulesEx(
        hProcess: WinNT.HANDLE,
        lphModule: Pointer?,
        cb: Int,
        lpcbNeeded: IntByReference,
        dwFilterFlag: Int
    ): Boolean

    companion object {
        val INSTANCE: PsapiExt = Native.load(
            "psapi",
            PsapiExt::class.java,
            W32APIOptions.DEFAULT_OPTIONS
        )
    }
}

@Serializable
data class GameConfig(
    val id: String,
    val name: String,
    val processName: String,
    val defaultBase: String,
    val scoreOffset: List<String>,
    val missOffset: List<String>,
    val bombOffset: List<String>,
    val stageOffset: List<String> = emptyList(),
    val characterOffset: List<String> = emptyList(),
    val characterType: String = "int32",
    val subshotOffset: List<String> = emptyList(),
    val subshotType: String = "int32",
    val stageStartsFrom: Int = 1,
    val bossManagerOffset: String? = null,
    val bossIndexOffset: String? = null,
    val bossSpellIdOffset: String? = null,
    val difficultyOffset: List<String> = emptyList(),
    val difficultyType: String = "int32",
    val scoreType: String = "int32",
    val missType: String = "int32",
    val bombType: String = "int32",
    val scoreMultiplier: Int = 1,
    val onSteam: Boolean = false,
    val grazeOffset: List<String> = emptyList(),
    val grazeType: String = "none",
    val powerOffset: List<String> = emptyList(),
    val powerType: String = "none",
    val pointOffset: List<String> = emptyList(),
    val pointType: String = "none",
    val cherryMaxOffset: List<String> = emptyList(),
    val cherryMaxType: String = "none"
)

@Serializable
data class Settings(
    val language: String,
    val oscPort: Int = 9000,
    val enableChatbox: Boolean = true,
    val appMode: String = "touhou",
    val themeColorHex: String = "#D0BCFF"
)

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
        val settings = Settings(choice, activeOscPort, activeEnableChatbox, activeMode, activeThemeColorHex)
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



object MD3Color {
    val Background = Color(0x1C, 0x1B, 0x1F)
    val Surface = Color(0x25, 0x23, 0x2A)
    @Volatile var Primary = Color(0xD0, 0xBC, 0xFF)
    @Volatile var OnPrimary = Color(0x38, 0x1E, 0x72)
    @Volatile var SecondaryContainer = Color(0x4A, 0x44, 0x58)
    @Volatile var OnSecondaryContainer = Color(0xE8, 0xDE, 0xF8)
    val Outline = Color(0x93, 0x8F, 0x99)
    val TextPrimary = Color(0xE6, 0xE1, 0xE5)
    val TextSecondary = Color(0xCA, 0xC4, 0xD0)
    val AccentGreen = Color(0xA8, 0xDA, 0xB5)
    val AccentRed = Color(0xFF, 0xB4, 0xAB)

    fun updateThemeColor(primaryColor: Color) {
        Primary = primaryColor
        val r = primaryColor.red / 255.0
        val g = primaryColor.green / 255.0
        val b = primaryColor.blue / 255.0
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        OnPrimary = if (luminance > 0.5) Color(0x1C, 0x1B, 0x1F) else Color(0xFF, 0xFF, 0xFF)
        val scr = (primaryColor.red * 0.3 + 0x25 * 0.7).toInt().coerceIn(0, 255)
        val scg = (primaryColor.green * 0.3 + 0x23 * 0.7).toInt().coerceIn(0, 255)
        val scb = (primaryColor.blue * 0.3 + 0x2A * 0.7).toInt().coerceIn(0, 255)
        SecondaryContainer = Color(scr, scg, scb)
        val oscr = (primaryColor.red * 0.5 + 255 * 0.5).toInt().coerceIn(0, 255)
        val oscg = (primaryColor.green * 0.5 + 255 * 0.5).toInt().coerceIn(0, 255)
        val oscb = (primaryColor.blue * 0.5 + 255 * 0.5).toInt().coerceIn(0, 255)
        OnSecondaryContainer = Color(oscr, oscg, oscb)
        updateUIManagerColors()
    }
}

open class MD3Card(val radius: Int = 16) : javax.swing.JPanel() {
    init {
        isOpaque = false
        background = MD3Color.Surface
        border = javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12)
    }
    override fun paintComponent(g: Graphics) {
        val g2d = g.create() as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.color = background
        g2d.fillRoundRect(0, 0, width, height, radius, radius)
        g2d.dispose()
    }
}

class MD3Button(
    text: String,
    initialType: ButtonType = ButtonType.FILLED,
    val radius: Int = 16
) : javax.swing.JButton(text) {
    enum class ButtonType { FILLED, OUTLINED, TEXT }
    
    override fun getForeground(): Color {
        val t = @Suppress("SENSELESS_COMPARISON") (if (type == null) ButtonType.FILLED else type)
        return when (t) {
            ButtonType.FILLED -> MD3Color.OnPrimary
            ButtonType.OUTLINED -> MD3Color.Primary
            ButtonType.TEXT -> MD3Color.Primary
        }
    }
    
    var type: ButtonType = initialType
        set(value) {
            field = value
            repaint()
        }

    private var isHovered = false
    private var isPressed = false

    init {
        isContentAreaFilled = false
        isBorderPainted = false
        isFocusable = false
        foreground = when (type) {
            ButtonType.FILLED -> MD3Color.OnPrimary
            ButtonType.OUTLINED -> MD3Color.Primary
            ButtonType.TEXT -> MD3Color.Primary
        }
        font = getAppFont(Font.BOLD, 14)
        border = javax.swing.BorderFactory.createEmptyBorder(6, 16, 6, 16)
        addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseEntered(e: java.awt.event.MouseEvent?) {
                isHovered = true
                repaint()
            }
            override fun mouseExited(e: java.awt.event.MouseEvent?) {
                isHovered = false
                repaint()
            }
            override fun mousePressed(e: java.awt.event.MouseEvent?) {
                isPressed = true
                repaint()
            }
            override fun mouseReleased(e: java.awt.event.MouseEvent?) {
                isPressed = false
                repaint()
            }
        })
    }

    override fun paintComponent(g: Graphics) {
        val g2d = g.create() as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val paintRadius = minOf(radius, height / 2)
        when (type) {
            ButtonType.FILLED -> {
                g2d.color = when {
                    isPressed -> MD3Color.Primary.darker()
                    isHovered -> MD3Color.Primary.brighter()
                    else -> MD3Color.Primary
                }
                g2d.fillRoundRect(1, 1, width - 2, height - 2, paintRadius, paintRadius)
            }
            ButtonType.OUTLINED -> {
                if (isHovered || isPressed) {
                    g2d.color = if (isPressed) MD3Color.SecondaryContainer.darker() else MD3Color.SecondaryContainer
                    g2d.fillRoundRect(1, 1, width - 2, height - 2, paintRadius, paintRadius)
                }
                g2d.color = MD3Color.Outline
                g2d.stroke = java.awt.BasicStroke(1.2f)
                g2d.drawRoundRect(1, 1, width - 3, height - 3, paintRadius, paintRadius)
            }
            ButtonType.TEXT -> {
                if (isHovered || isPressed) {
                    g2d.color = if (isPressed) MD3Color.SecondaryContainer.darker() else MD3Color.SecondaryContainer
                    g2d.fillRoundRect(1, 1, width - 2, height - 2, paintRadius, paintRadius)
                }
            }
        }
        super.paintComponent(g)
        g2d.dispose()
    }
}

class MD3TextField(columns: Int) : javax.swing.JTextField(columns) {
    override fun getCaretColor(): Color {
        return MD3Color.Primary
    }
    init {
        isOpaque = false
        foreground = MD3Color.TextPrimary
        background = MD3Color.Surface
        font = getAppFont(Font.PLAIN, 14)
        border = javax.swing.BorderFactory.createCompoundBorder(
            object : javax.swing.border.Border {
                override fun paintBorder(c: Component?, g: Graphics?, x: Int, y: Int, width: Int, height: Int) {
                    val g2d = g?.create() as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2d.color = MD3Color.Outline
                    g2d.stroke = java.awt.BasicStroke(1.2f)
                    g2d.drawRoundRect(x + 1, y + 1, width - 3, height - 3, 8, 8)
                    g2d.dispose()
                }
                override fun getBorderInsets(c: Component?): Insets = Insets(6, 10, 6, 10)
                override fun isBorderOpaque(): Boolean = false
            },
            javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4)
        )
    }
    override fun paintComponent(g: Graphics) {
        val g2d = g.create() as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.color = background
        g2d.fillRoundRect(1, 1, width - 2, height - 2, 8, 8)
        super.paintComponent(g)
        g2d.dispose()
    }
}

class MD3CheckboxIcon(val size: Int = 18) : javax.swing.Icon {
    override fun getIconWidth(): Int = size
    override fun getIconHeight(): Int = size

    override fun paintIcon(c: java.awt.Component?, g: java.awt.Graphics?, x: Int, y: Int) {
        val g2d = g?.create() as? Graphics2D ?: return
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val isSelected = if (c is javax.swing.JCheckBox) c.isSelected else false
        val isPressed = if (c is javax.swing.AbstractButton) c.model.isPressed else false
        val isHovered = if (c is javax.swing.AbstractButton) c.model.isRollover else false

        if (isSelected) {
            g2d.color = if (isPressed) MD3Color.Primary.darker() else if (isHovered) MD3Color.Primary.brighter() else MD3Color.Primary
            g2d.fillRoundRect(x + 1, y + 1, size - 2, size - 2, 4, 4)

            g2d.color = MD3Color.OnPrimary
            g2d.stroke = java.awt.BasicStroke(2.5f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND)
            
            val px1 = x + (0.28 * size).toInt()
            val py1 = y + (0.5 * size).toInt()
            val px2 = x + (0.45 * size).toInt()
            val py2 = y + (0.7 * size).toInt()
            val px3 = x + (0.75 * size).toInt()
            val py3 = y + (0.3 * size).toInt()

            g2d.drawLine(px1, py1, px2, py2)
            g2d.drawLine(px2, py2, px3, py3)
        } else {
            g2d.color = if (isHovered) MD3Color.TextPrimary else MD3Color.Outline
            g2d.stroke = java.awt.BasicStroke(1.5f)
            g2d.drawRoundRect(x + 1, y + 1, size - 3, size - 3, 4, 4)
        }

        g2d.dispose()
    }
}


class MD3TabButton(text: String, var isActive: Boolean = false) : javax.swing.JButton(text) {
    private var isHovered = false
    override fun getForeground(): Color {
        return if (isActive) MD3Color.OnSecondaryContainer else MD3Color.TextSecondary
    }
    init {
        isContentAreaFilled = false
        isBorderPainted = false
        isFocusable = false
        font = getAppFont(Font.BOLD, 14)
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

    fun updateActive(active: Boolean) {
        isActive = active
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        val g2d = g.create() as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        if (isActive) {
            g2d.color = MD3Color.SecondaryContainer
            g2d.fillRoundRect(0, 0, width, height, 20, 20)
        } else if (isHovered) {
            g2d.color = Color(0x35, 0x33, 0x3A)
            g2d.fillRoundRect(0, 0, width, height, 20, 20)
        }
        super.paintComponent(g)
        g2d.dispose()
    }
}

class StatCard(var labelText: String, var valueText: String) : MD3Card(radius = 12) {
    private val titleLabel = javax.swing.JLabel(labelText).apply {
        foreground = MD3Color.TextSecondary
        font = getAppFont(Font.BOLD, 12)
        alignmentX = Component.LEFT_ALIGNMENT
    }
    private val valLabel = javax.swing.JLabel(valueText).apply {
        foreground = MD3Color.TextPrimary
        font = getAppFont(Font.BOLD, 22)
        alignmentX = Component.LEFT_ALIGNMENT
    }
    init {
        layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
        add(titleLabel)
        add(javax.swing.Box.createRigidArea(Dimension(0, 8)))
        add(valLabel)
    }
    fun updateLabel(newLabel: String) {
        titleLabel.text = newLabel
    }
    fun updateValue(newValue: String) {
        valLabel.text = newValue
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

        // Theme color row
        gbc.gridx = 0
        gbc.gridy = 3
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
        gbc.gridy = 4
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

        btnSaveSettings.addActionListener {
            val selectedLang = when (langComboBox.selectedIndex) {
                0 -> "zh"
                1 -> "en"
                2 -> "ja"
                else -> "zh"
            }
            val enteredPort = portField.text.toIntOrNull() ?: 9000
            val isChatboxEnabled = chatboxCheckBox.isSelected

            changeSettings(selectedLang, enteredPort, isChatboxEnabled)

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


/**
 * Finds process ID by process name.
 */
fun getProcessIdByName(processName: String): Int? {
    val snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, WinDef.DWORD(0))
    val entry = Tlhelp32.PROCESSENTRY32.ByReference()
    try {
        if (Kernel32.INSTANCE.Process32First(snapshot, entry)) {
            do {
                val exeFile = Native.toString(entry.szExeFile)
                if (exeFile.equals(processName, ignoreCase = true)) {
                    return entry.th32ProcessID.toInt()
                }
            } while (Kernel32.INSTANCE.Process32Next(snapshot, entry))
        }
    } finally {
        Kernel32.INSTANCE.CloseHandle(snapshot)
    }
    return null
}

// Tracks the last print time for memory read errors to prevent console spamming.
private val lastReadErrorTimes = mutableMapOf<Long, Long>()

fun getModuleBaseAddress(processId: Int, processHandle: WinNT.HANDLE, moduleName: String): Long? {
    // Method 1: Try using CreateToolhelp32Snapshot and Module32FirstW/NextW to enumerate process modules and get base address
    var snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(
        Tlhelp32.TH32CS_SNAPMODULE,
        WinDef.DWORD(processId.toLong())
    )
    if (snapshot == WinNT.INVALID_HANDLE_VALUE) {
        // If the 64-bit module snapshot fails, attempt to use the 32-bit module snapshot for WOW64 processes
        snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(
            Tlhelp32.TH32CS_SNAPMODULE32,
            WinDef.DWORD(processId.toLong())
        )
    }
    
    if (snapshot != WinNT.INVALID_HANDLE_VALUE) {
        val entry = Tlhelp32.MODULEENTRY32W.ByReference()
        try {
            if (Kernel32.INSTANCE.Module32FirstW(snapshot, entry)) {
                do {
                    val name = Native.toString(entry.szModule)
                    if (name.equals(moduleName, ignoreCase = true)) {
                        return Pointer.nativeValue(entry.modBaseAddr)
                    }
                } while (Kernel32.INSTANCE.Module32NextW(snapshot, entry))
            }
        } catch (e: Exception) {
            // Ignore enumeration exceptions and proceed to try other methods
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot)
        }
    }

    // Method 2: If the Toolhelp snapshot fails, use EnumProcessModulesEx (supports cross-bitness 64-bit -> 32-bit module enumeration)
    val psapi = com.sun.jna.platform.win32.Psapi.INSTANCE
    val cbNeeded = IntByReference()
    
    // LIST_MODULES_ALL (3), LIST_MODULES_32BIT (1), LIST_MODULES_64BIT (2)
    for (filterFlag in listOf(3, 1, 2)) {
        cbNeeded.value = 0
        val success = PsapiExt.INSTANCE.EnumProcessModulesEx(
            processHandle,
            null,
            0,
            cbNeeded,
            filterFlag
        )
        if (success && cbNeeded.value > 0) {
            val size = cbNeeded.value
            val modulesMem = Memory(size.toLong())
            val successRead = PsapiExt.INSTANCE.EnumProcessModulesEx(
                processHandle,
                modulesMem,
                size,
                cbNeeded,
                filterFlag
            )
            if (successRead) {
                val moduleCount = cbNeeded.value / Native.POINTER_SIZE
                for (i in 0 until moduleCount) {
                    val ptrVal = modulesMem.getPointer(i.toLong() * Native.POINTER_SIZE) ?: continue
                    val pathBuffer = CharArray(512)
                    val hModule = WinDef.HMODULE()
                    hModule.pointer = ptrVal
                    val len = psapi.GetModuleFileNameExW(processHandle, hModule, pathBuffer, pathBuffer.size)
                    if (len > 0) {
                        val fullPath = String(pathBuffer, 0, len)
                        val name = fullPath.substringAfterLast('\\').substringAfterLast('/')
                        if (name.equals(moduleName, ignoreCase = true)) {
                            return Pointer.nativeValue(ptrVal)
                        }
                    }
                }
            }
        }
    }

    // Method 3: Fallback using standard EnumProcessModules
    cbNeeded.value = 0
    val hModulesFallback = arrayOfNulls<WinDef.HMODULE>(1024)
    if (psapi.EnumProcessModules(processHandle, hModulesFallback, hModulesFallback.size * Native.POINTER_SIZE, cbNeeded)) {
        val moduleCount = cbNeeded.value / Native.POINTER_SIZE
        for (i in 0 until moduleCount) {
            val hModule = hModulesFallback[i] ?: continue
            val pathBuffer = CharArray(512)
            val len = psapi.GetModuleFileNameExW(processHandle, hModule, pathBuffer, pathBuffer.size)
            if (len > 0) {
                val fullPath = String(pathBuffer, 0, len)
                val name = fullPath.substringAfterLast('\\').substringAfterLast('/')
                if (name.equals(moduleName, ignoreCase = true)) {
                    return Pointer.nativeValue(hModule.pointer)
                }
            }
        }
    }

    val err = Kernel32.INSTANCE.GetLastError()
    println("\n[Warning] Failed to retrieve module base address (both Toolhelp and PSAPI methods failed), error code: $err")
    return null
}

fun resolveAddressPath(processHandle: WinNT.HANDLE, baseAddr: Long, path: List<String>, name: String): Long {
    if (path.isEmpty()) return 0L
    var addr = baseAddr
    for (i in 0 until path.size - 1) {
        val offset = hexToLong(path[i])
        val nextAddr = addr + offset
        val ptrVal = readPointer(processHandle, nextAddr)
        // Invalid pointer validation: if the pointer read is 0, less than 0x10000 (reserved zero page), or greater than/equal to 0xFFFF0000 (invalid high memory), it is determined as invalid.
        if (ptrVal < 0x10000L || ptrVal >= 0xFFFF0000L) {
            val now = System.currentTimeMillis()
            val lastTime = lastReadErrorTimes[nextAddr] ?: 0L
            if (now - lastTime > 3000) {
                val ptrHex = "0x" + ptrVal.toString(16).uppercase()
                println("\n[Warning] Failed to resolve pointer chain for $name (read invalid pointer: $ptrHex at address: 0x${nextAddr.toString(16).uppercase()})")
                lastReadErrorTimes[nextAddr] = now
            }
            return 0L
        }
        addr = ptrVal
    }
    return addr + hexToLong(path.last())
}

fun readPointer(processHandle: WinNT.HANDLE, address: Long): Long {
    val output = Memory(4)
    val bytesRead = IntByReference()
    val success = Kernel32.INSTANCE.ReadProcessMemory(
        processHandle,
        Pointer(address),
        output,
        4,
        bytesRead
    )
    if (!success) {
        return 0L
    }
    return output.getInt(0).toLong() and 0xFFFFFFFFL
}

fun readMemoryValue(processHandle: WinNT.HANDLE, address: Long, type: String, name: String): Number {
    if (address == 0L) return 0
    val byteCount = when (type.lowercase()) {
        "byte", "int8" -> 1
        "int16", "short" -> 2
        "int32", "int", "float" -> 4
        "int64", "long" -> 8
        else -> 4
    }
    val output = Memory(byteCount.toLong())
    val bytesRead = IntByReference()
    val success = Kernel32.INSTANCE.ReadProcessMemory(
        processHandle,
        Pointer(address),
        output,
        byteCount,
        bytesRead
    )
    if (!success) {
        val err = Kernel32.INSTANCE.GetLastError()
        val now = System.currentTimeMillis()
        val lastTime = lastReadErrorTimes[address] ?: 0L
        if (now - lastTime > 3000) {
            println("\n[Error] Failed to read $name (address: 0x${address.toString(16).uppercase()}), error code: $err")
            lastReadErrorTimes[address] = now
        }
        return 0
    }
    return when (type.lowercase()) {
        "byte", "int8" -> output.getByte(0).toInt() and 0xFF
        "int16", "short" -> output.getShort(0).toInt() and 0xFFFF
        "int32", "int" -> output.getInt(0)
        "int64", "long" -> output.getLong(0)
        "float" -> output.getFloat(0)
        else -> output.getInt(0)
    }
}

fun hexToLong(hex: String): Long = hex.removePrefix("0x").toLong(16)

fun readActiveSpellId(processHandle: WinNT.HANDLE, baseAddr: Long, config: GameConfig): Int? {
    val managerOffset = config.bossManagerOffset ?: return null
    val indexOffset = config.bossIndexOffset ?: return null
    val spellIdOffset = config.bossSpellIdOffset ?: return null

    val bossManagerPtr = readPointer(processHandle, baseAddr + hexToLong(managerOffset))
    if (bossManagerPtr < 0x10000L || bossManagerPtr >= 0xFFFF0000L) return null

    val bossIndex = readMemoryValue(processHandle, bossManagerPtr + hexToLong(indexOffset), "int32", "BossIndex").toInt()
    if (bossIndex < 0 || bossIndex > 100) return null

    val spellId = readMemoryValue(processHandle, bossManagerPtr + bossIndex * 4 + hexToLong(spellIdOffset), "int32", "SpellID").toInt()
    return spellId
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

sealed class ScannerStatus {
    object SCANNING : ScannerStatus()
    data class PLAYING(val gameName: String) : ScannerStatus()
}

@Volatile
var activeLang: String = "zh"

@Volatile
var activeStatus: ScannerStatus = ScannerStatus.SCANNING

@Volatile
var activeOscPort: Int = 9000

@Volatile
var activeEnableChatbox: Boolean = true

@Volatile
var activeMode: String = "touhou"

@Volatile
var activeOsuHelperProcess: Process? = null

@Volatile
var activeThemeColorHex: String = "#D0BCFF"

@Volatile
var activeOscSender: OSCPortOut? = null

var openMenuItem: javax.swing.JMenuItem? = null
var statusMenuItem: javax.swing.JMenuItem? = null
var langMenu: javax.swing.JMenu? = null
var zhItem: javax.swing.JMenuItem? = null
var enItem: javax.swing.JMenuItem? = null
var jaItem: javax.swing.JMenuItem? = null
var exitItem: javax.swing.JMenuItem? = null
var trayIcon: java.awt.TrayIcon? = null

fun createTrayIconImage(): java.awt.Image {
    val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    
    // Draw outer circle border
    g.color = Color.BLACK
    g.drawOval(0, 0, 15, 15)
    
    // Fill left half with white, right half with red
    g.color = Color.WHITE
    g.fillOval(0, 0, 15, 15)
    g.color = Color.RED
    g.fillArc(0, 0, 15, 15, 270, 180) // sweeps counter-clockwise, from bottom to top (fills right half)
    
    // Draw S-curve circles
    // Upper circle (white, diameter 7.5, centered at y = 3.75, x = 7.5) -> radius 3.75
    g.color = Color.WHITE
    g.fillOval(4, 0, 8, 8)
    // Lower circle (red, diameter 7.5, centered at y = 11.25, x = 7.5) -> radius 3.75
    g.color = Color.RED
    g.fillOval(4, 7, 8, 8)
    
    // Draw smaller dots
    // Red dot in upper white circle
    g.color = Color.RED
    g.fillOval(7, 3, 2, 2)
    // White dot in lower red circle
    g.color = Color.WHITE
    g.fillOval(7, 10, 2, 2)
    
    g.dispose()
    return image
}

fun updateTrayLabels() {
    val statusText = when (val s = activeStatus) {
        ScannerStatus.SCANNING -> when (activeLang) {
            "zh" -> "状态: 正在扫描游戏..."
            "ja" -> "ステータス: ゲームをスキャン中..."
            else -> "Status: Scanning for games..."
        }
        is ScannerStatus.PLAYING -> {
            when (activeLang) {
                "zh" -> "状态: 正在玩 ${s.gameName}"
                "ja" -> "ステータス: プレイ中 ${s.gameName}"
                else -> "Status: Playing ${s.gameName}"
            }
        }
    }
    statusMenuItem?.text = statusText

    openMenuItem?.text = when (activeLang) {
        "zh" -> "打开控制面板"
        "ja" -> "コントロールパネルを開く"
        else -> "Open Control Panel"
    }

    langMenu?.text = when (activeLang) {
        "zh" -> "语言选择 (Language)"
        "ja" -> "言語選択 (Language)"
        else -> "Language (语言选择)"
    }

    exitItem?.text = when (activeLang) {
        "zh" -> "退出"
        "ja" -> "終了"
        else -> "Exit"
    }

    trayIcon?.toolTip = when (activeLang) {
        "zh" -> "Touhou OSC Bridge - 实时游戏数据发送器"
        "ja" -> "Touhou OSC Bridge - リアルタイムゲームデータ送信機"
        else -> "Touhou OSC Bridge - Live Game Data Sender"
    }
}

fun updateUIManagerColors() {
    javax.swing.UIManager.put("MenuItem.selectionBackground", MD3Color.SecondaryContainer)
    javax.swing.UIManager.put("MenuItem.selectionForeground", MD3Color.OnSecondaryContainer)
    javax.swing.UIManager.put("Menu.selectionBackground", MD3Color.SecondaryContainer)
    javax.swing.UIManager.put("Menu.selectionForeground", MD3Color.OnSecondaryContainer)
}

fun changeThemeColor(color: Color) {
    MD3Color.updateThemeColor(color)
    activeThemeColorHex = color.toHex()
    try {
        val settings = Settings(activeLang, activeOscPort, activeEnableChatbox, activeMode, activeThemeColorHex)
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
        val settings = Settings(lang, activeOscPort, activeEnableChatbox, activeMode, activeThemeColorHex)
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
            activeOscSender = OSCPortOut(InetSocketAddress("127.0.0.1", port))
        } catch (e: Exception) {
            println("Error updating OSC port: ${e.message}")
        }
    }

    try {
        val settings = Settings(lang, port, enableChatbox, activeMode, activeThemeColorHex)
        java.io.File("settings.json").writeText(Json.encodeToString(settings))
    } catch (e: Exception) {
        println("Warning: Failed to save settings.json: ${e.message}")
    }

    java.awt.EventQueue.invokeLater {
        mainWindow?.refreshUILabels()
        updateTrayLabels()
    }
}

fun checkAndStartOsuHelper() {
    if (activeMode != "osu") return
    if (activeOsuHelperProcess != null && activeOsuHelperProcess!!.isAlive) {
        return
    }
    val candidates = listOf(
        java.io.File("gosumemory.exe"),
        java.io.File("gosumemory/gosumemory.exe"),
        java.io.File("tosu.exe"),
        java.io.File("tosu/tosu.exe")
    )
    val exeFile = candidates.firstOrNull { it.exists() && it.isFile }
    if (exeFile != null) {
        try {
            val pb = ProcessBuilder(exeFile.absolutePath)
                .directory(exeFile.parentFile ?: java.io.File("."))
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD)
            pb.redirectError(ProcessBuilder.Redirect.DISCARD)
            activeOsuHelperProcess = pb.start()
            println("Info: Automatically started osu! helper from ${exeFile.absolutePath}")
        } catch (e: Exception) {
            println("Warning: Failed to start osu! helper: ${e.message}")
        }
    }
}

fun stopOsuHelper() {
    activeOsuHelperProcess?.let { process ->
        if (process.isAlive) {
            process.destroy()
            println("Info: Stopped osu! helper process.")
        }
        activeOsuHelperProcess = null
    }
}

fun changeMode(mode: String) {
    activeMode = mode
    try {
        val settings = Settings(activeLang, activeOscPort, activeEnableChatbox, mode, activeThemeColorHex)
        java.io.File("settings.json").writeText(Json.encodeToString(settings))
    } catch (e: Exception) {
        println("Warning: Failed to save settings.json: ${e.message}")
    }
    if (mode != "osu") {
        stopOsuHelper()
    } else {
        checkAndStartOsuHelper()
    }
    java.awt.EventQueue.invokeLater {
        mainWindow?.updateCardLabels()
        mainWindow?.refreshUILabels()
        updateTrayLabels()
    }
}

fun styleMenuItem(item: javax.swing.JMenuItem) {
    item.apply {
        background = MD3Color.Surface
        foreground = MD3Color.TextPrimary
        font = getAppFont(Font.PLAIN, 13)
        border = javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16)
        isOpaque = true
    }
}

fun styleMenu(menu: javax.swing.JMenu) {
    menu.apply {
        background = MD3Color.Surface
        foreground = MD3Color.TextPrimary
        font = getAppFont(Font.PLAIN, 13)
        border = javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16)
        isOpaque = true
    }
}

private fun showSwingPopupMenu(e: java.awt.event.MouseEvent) {
    java.awt.EventQueue.invokeLater {
        val popup = javax.swing.JPopupMenu().apply {
            background = MD3Color.Surface
            border = javax.swing.BorderFactory.createLineBorder(MD3Color.Outline, 1)
        }

        popup.add(openMenuItem)
        popup.add(javax.swing.JSeparator().apply {
            foreground = MD3Color.Outline
            background = MD3Color.Surface
        })
        popup.add(statusMenuItem)
        popup.add(javax.swing.JSeparator().apply {
            foreground = MD3Color.Outline
            background = MD3Color.Surface
        })
        popup.add(langMenu)
        popup.add(javax.swing.JSeparator().apply {
            foreground = MD3Color.Outline
            background = MD3Color.Surface
        })
        popup.add(exitItem)

        val dummyDialog = javax.swing.JDialog()
        dummyDialog.isUndecorated = true
        dummyDialog.setSize(1, 1)
        
        val screenPoint = e.locationOnScreen
        
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screens = ge.screenDevices
        var activeScreen = screens[0]
        for (screen in screens) {
            val bounds = screen.defaultConfiguration.bounds
            if (bounds.contains(screenPoint)) {
                activeScreen = screen
                break
            }
        }
        val screenBounds = activeScreen.defaultConfiguration.bounds
        
        val size = popup.preferredSize
        var x = screenPoint.x
        var y = screenPoint.y
        
        if (x + size.width > screenBounds.x + screenBounds.width) {
            x = screenBounds.x + screenBounds.width - size.width
        }
        if (y + size.height > screenBounds.y + screenBounds.height) {
            y = screenBounds.y + screenBounds.height - size.height
        }
        if (x < screenBounds.x) {
            x = screenBounds.x
        }
        if (y < screenBounds.y) {
            y = screenBounds.y
        }
        
        dummyDialog.setLocation(x, y)
        dummyDialog.isVisible = true
        
        popup.addPopupMenuListener(object : javax.swing.event.PopupMenuListener {
            override fun popupMenuWillBecomeVisible(ev: javax.swing.event.PopupMenuEvent?) {}
            override fun popupMenuWillBecomeInvisible(ev: javax.swing.event.PopupMenuEvent?) {
                java.awt.EventQueue.invokeLater {
                    dummyDialog.dispose()
                }
            }
            override fun popupMenuCanceled(ev: javax.swing.event.PopupMenuEvent?) {
                java.awt.EventQueue.invokeLater {
                    dummyDialog.dispose()
                }
            }
        })
        
        popup.show(dummyDialog.contentPane, 0, 0)
    }
}

fun initSystemTray() {
    if (GraphicsEnvironment.isHeadless()) {
        println("GraphicsEnvironment is headless. System Tray is not supported.")
        return
    }
    if (!SystemTray.isSupported()) {
        println("System Tray is not supported on this platform.")
        return
    }
    
    // Style popup menus globally via UIManager
    javax.swing.UIManager.put("MenuItem.selectionBackground", MD3Color.SecondaryContainer)
    javax.swing.UIManager.put("MenuItem.selectionForeground", MD3Color.OnSecondaryContainer)
    javax.swing.UIManager.put("Menu.selectionBackground", MD3Color.SecondaryContainer)
    javax.swing.UIManager.put("Menu.selectionForeground", MD3Color.OnSecondaryContainer)
    javax.swing.UIManager.put("PopupMenu.background", MD3Color.Surface)
    javax.swing.UIManager.put("PopupMenu.border", javax.swing.BorderFactory.createLineBorder(MD3Color.Outline, 1))
    
    val tray = SystemTray.getSystemTray()
    
    openMenuItem = javax.swing.JMenuItem("").apply {
        styleMenuItem(this)
        addActionListener {
            java.awt.EventQueue.invokeLater {
                mainWindow?.isVisible = true
                mainWindow?.extendedState = Frame.NORMAL
                mainWindow?.toFront()
            }
        }
    }

    statusMenuItem = javax.swing.JMenuItem("").apply {
        styleMenuItem(this)
        isEnabled = false
    }
    
    langMenu = javax.swing.JMenu("").apply {
        styleMenu(this)
    }
    
    zhItem = javax.swing.JMenuItem("简体中文 (Chinese)").apply {
        styleMenuItem(this)
        addActionListener { changeLanguage("zh") }
    }
    enItem = javax.swing.JMenuItem("English (English)").apply {
        styleMenuItem(this)
        addActionListener { changeLanguage("en") }
    }
    jaItem = javax.swing.JMenuItem("日本語 (Japanese)").apply {
        styleMenuItem(this)
        addActionListener { changeLanguage("ja") }
    }
    langMenu?.add(zhItem)
    langMenu?.add(enItem)
    langMenu?.add(jaItem)
    
    exitItem = javax.swing.JMenuItem("").apply {
        styleMenuItem(this)
        addActionListener {
            System.exit(0)
        }
    }
    
    val image = createTrayIconImage()
    trayIcon = TrayIcon(image, "Touhou OSC Bridge").apply {
        isImageAutoSize = true
        addActionListener {
            java.awt.EventQueue.invokeLater {
                mainWindow?.isVisible = true
                mainWindow?.extendedState = Frame.NORMAL
                mainWindow?.toFront()
            }
        }
        addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseReleased(e: java.awt.event.MouseEvent) {
                if (e.isPopupTrigger || e.button == java.awt.event.MouseEvent.BUTTON3) {
                    showSwingPopupMenu(e)
                }
            }
            override fun mousePressed(e: java.awt.event.MouseEvent) {
                if (e.isPopupTrigger || e.button == java.awt.event.MouseEvent.BUTTON3) {
                    showSwingPopupMenu(e)
                }
            }
        })
    }
    
    try {
        tray.add(trayIcon)
    } catch (e: Exception) {
        println("Error adding system tray icon: ${e.message}")
    }
    
    updateTrayLabels()
}

fun main() {
    Runtime.getRuntime().addShutdownHook(Thread {
        stopOsuHelper()
    })
    updateUIManagerColors()
    LogManager.init()
    selectLanguage()

    activeOscSender = OSCPortOut(InetSocketAddress("127.0.0.1", activeOscPort))

    initSystemTray()

    val configStream = object {}.javaClass.getResourceAsStream("/game_data.json")
    if (configStream == null) {
        val errJsonMsg = when (activeLang) {
            "zh" -> "找不到配置文件：game_data.json"
            "ja" -> "設定ファイルが見つかりません: game_data.json"
            else -> "Configuration file not found: game_data.json"
        }
        println(errJsonMsg)
        return
    }

    val configJson = configStream.bufferedReader().use { it.readText() }
    val games = Json.decodeFromString<List<GameConfig>>(configJson)

    // Open MainWindow in Swing Event Queue
    java.awt.EventQueue.invokeLater {
        mainWindow = MainWindow()
        mainWindow?.isVisible = true
        LogManager.setTextArea(mainWindow!!.logArea)
    }

    println("=== THOSC_BOX - Touhou Project OSC Bridge ===")
    val supportedGamesTitle = when (activeLang) {
        "zh" -> "支持的游戏 (${games.size}):"
        "ja" -> "対応游戏 (${games.size}):"
        else -> "Supported games (${games.size}):"
    }
    println(supportedGamesTitle)
    games.forEachIndexed { index, game ->
        val steamTag = if (game.onSteam) " [Steam]" else ""
        println("  ${index + 1}. ${getLocalizedGameName(game.id, activeLang)}$steamTag")
    }
    println()
    val monitoringMsg = when (activeLang) {
        "zh" -> "正在开始监控游戏进程..."
        "ja" -> "ゲームプロセスの監視を開始します..."
        else -> "Starting to monitor game processes..."
    }
    println(monitoringMsg)

    Thread {
        runScannerLoop(games)
    }.start()

    Thread {
        runOsuScannerLoop()
    }.start()
}

fun runScannerLoop(games: List<GameConfig>) {
    while (true) {
        if (activeMode != "touhou") {
            Thread.sleep(1000)
            continue
        }
        var activeGameConfig: GameConfig? = null
        var processId: Int? = null

        for (game in games) {
            val pid = getProcessIdByName(game.processName)
            if (pid != null) {
                activeGameConfig = game
                processId = pid
                break
            }
        }

        if (activeGameConfig != null && processId != null) {
            val gameLocalizedName = getLocalizedGameName(activeGameConfig.id, activeLang)
            
            // Update Tray Status & GUI Status Pill
            activeStatus = ScannerStatus.PLAYING(gameLocalizedName)
            java.awt.EventQueue.invokeLater {
                updateTrayLabels()
            }

            val gameDetectedMsg = when (activeLang) {
                "zh" -> "检测到游戏正在运行: $gameLocalizedName (PID: $processId)"
                "ja" -> "ゲームの起動を検出しました: $gameLocalizedName (PID: $processId)"
                else -> "Game running detected: $gameLocalizedName (PID: $processId)"
            }
            println(gameDetectedMsg)

            val processHandle = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_VM_READ or WinNT.PROCESS_QUERY_INFORMATION or WinNT.SYNCHRONIZE,
                false,
                processId
            )

            if (processHandle == null) {
                val failMsg = when (activeLang) {
                    "zh" -> "无法打开进程句柄，可能需要管理员权限。错误代码: ${Kernel32.INSTANCE.GetLastError()}"
                    "ja" -> "プロセスハンドルのオープンに失敗しました。管理者権限が必要な可能性があります。エラーコード: ${Kernel32.INSTANCE.GetLastError()}"
                    else -> "Failed to open process handle, administrator privileges might be required. Error code: ${Kernel32.INSTANCE.GetLastError()}"
                }
                println(failMsg)
                Thread.sleep(5000)
                continue
            }

            try {
                val moduleBase = getModuleBaseAddress(processId, processHandle, activeGameConfig.processName)
                val baseAddr = if (moduleBase != null) {
                    val baseMsg = when (activeLang) {
                        "zh" -> "检测到模块基址: 0x${moduleBase.toString(16).uppercase()}"
                        "ja" -> "モジュールベースアドレスを検出しました: 0x${moduleBase.toString(16).uppercase()}"
                        else -> "Module base address detected: 0x${moduleBase.toString(16).uppercase()}"
                    }
                    println(baseMsg)
                    moduleBase
                } else {
                    val defaultBase = hexToLong(activeGameConfig.defaultBase)
                    val baseFallbackMsg = when (activeLang) {
                        "zh" -> "使用默认基址: 0x${defaultBase.toString(16).uppercase()}"
                        "ja" -> "デフォルトのベースアドレスを使用します: 0x${defaultBase.toString(16).uppercase()}"
                        else -> "Using default base address: 0x${defaultBase.toString(16).uppercase()}"
                    }
                    println(baseFallbackMsg)
                    defaultBase
                }

                println("Score offset: ${activeGameConfig.scoreOffset}")
                println("Miss offset: ${activeGameConfig.missOffset}")
                println("Bomb offset: ${activeGameConfig.bombOffset}")
                println("Stage offset: ${activeGameConfig.stageOffset}")
                if (activeGameConfig.bossManagerOffset != null) {
                    println("Boss manager offset: ${activeGameConfig.bossManagerOffset}")
                }
                if (activeGameConfig.difficultyOffset.isNotEmpty()) {
                    println("Difficulty offset: ${activeGameConfig.difficultyOffset}")
                }

                val gameIndex = games.indexOf(activeGameConfig)

                var lastChatboxTime = 0L
                var lastScore = -1
                var lastMiss = -1
                var lastBomb = -1
                var lastStageValue = -1
                var lastCharacter = -1
                var lastSubshot = -1

                var lastRawLives: Int? = null
                var lastRawBombs: Int? = null
                var cumulativeMisses = 0
                var cumulativeBombs = 0

                while (true) {
                    val waitResult = Kernel32.INSTANCE.WaitForSingleObject(processHandle, 100)
                    if (waitResult == 0) {
                        val exitMsg = when (activeLang) {
                            "zh" -> "\n游戏进程已退出。"
                            "ja" -> "\nゲームプロセスが終了しました。"
                            else -> "\nGame process has exited."
                        }
                        println(exitMsg)
                        break
                    } else if (waitResult != 258) {
                        val abnormalMsg = when (activeLang) {
                            "zh" -> "\nWaitForSingleObject 失败或返回异常值: $waitResult, 错误代码: ${Kernel32.INSTANCE.GetLastError()}"
                            "ja" -> "\nWaitForSingleObject が失敗したか、異常値を返しました: $waitResult, エラーコード: ${Kernel32.INSTANCE.GetLastError()}"
                            else -> "\nWaitForSingleObject failed or returned abnormal value: $waitResult, error code: ${Kernel32.INSTANCE.GetLastError()}"
                        }
                        println(abnormalMsg)
                        break
                    }

                    val scoreAddr = resolveAddressPath(processHandle, baseAddr, activeGameConfig.scoreOffset, "Score")
                    val missAddr = resolveAddressPath(processHandle, baseAddr, activeGameConfig.missOffset, "Miss")
                    val bombAddr = if (activeGameConfig.bombType.lowercase() != "none" && activeGameConfig.bombOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.bombOffset, "Bomb")
                    } else {
                        0L
                    }
                    val stageAddr = if (activeGameConfig.stageOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.stageOffset, "Stage")
                    } else {
                        0L
                    }
                    val difficultyAddr = if (activeGameConfig.difficultyOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.difficultyOffset, "Difficulty")
                    } else {
                        0L
                    }
                    val characterAddr = if (activeGameConfig.characterOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.characterOffset, "Character")
                    } else {
                        0L
                    }
                    val subshotAddr = if (activeGameConfig.subshotOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.subshotOffset, "Subshot")
                    } else {
                        0L
                    }
                    
                    // Resolve extended memory addresses
                    val grazeAddr = if (activeGameConfig.grazeOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.grazeOffset, "Graze")
                    } else {
                        0L
                    }
                    val powerAddr = if (activeGameConfig.powerOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.powerOffset, "Power")
                    } else {
                        0L
                    }
                    val pointAddr = if (activeGameConfig.pointOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.pointOffset, "Point")
                    } else {
                        0L
                    }
                    val cherryMaxAddr = if (activeGameConfig.cherryMaxOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.cherryMaxOffset, "CherryMax")
                    } else {
                        0L
                    }

                    // Read memory values
                    val rawScore = readMemoryValue(processHandle, scoreAddr, activeGameConfig.scoreType, "Score").toLong()
                    val score = (rawScore * activeGameConfig.scoreMultiplier).toInt()

                    val rawLives = if (missAddr != 0L) {
                        readMemoryValue(processHandle, missAddr, activeGameConfig.missType, "Miss").toFloat().toInt()
                    } else {
                        0
                    }

                    val rawBombs = if (bombAddr != 0L) {
                        readMemoryValue(processHandle, bombAddr, activeGameConfig.bombType, "Bomb").toFloat().toInt()
                    } else {
                        0
                    }

                    val rawStage = if (stageAddr != 0L) {
                        readMemoryValue(processHandle, stageAddr, "int32", "Stage").toInt()
                    } else {
                        0
                    }

                    val rawDifficulty = if (difficultyAddr != 0L) {
                        readMemoryValue(processHandle, difficultyAddr, activeGameConfig.difficultyType, "Difficulty").toInt()
                    } else {
                        -1
                    }

                    val rawCharacter = if (characterAddr != 0L) {
                        readMemoryValue(processHandle, characterAddr, activeGameConfig.characterType, "Character").toInt()
                    } else {
                        0
                    }
                    val rawSubshot = if (subshotAddr != 0L) {
                        readMemoryValue(processHandle, subshotAddr, activeGameConfig.subshotType, "Subshot").toInt()
                    } else {
                        0
                    }
                    
                    // Read extended variables
                    val rawGraze = if (grazeAddr != 0L) {
                        readMemoryValue(processHandle, grazeAddr, activeGameConfig.grazeType, "Graze").toInt()
                    } else {
                        0
                    }
                    val rawPower = if (powerAddr != 0L) {
                        readMemoryValue(processHandle, powerAddr, activeGameConfig.powerType, "Power")
                    } else {
                        0.0f
                    }
                    val rawPoint = if (pointAddr != 0L) {
                        readMemoryValue(processHandle, pointAddr, activeGameConfig.pointType, "Point").toLong()
                    } else {
                        0L
                    }
                    val rawCherryMax = if (cherryMaxAddr != 0L) {
                        readMemoryValue(processHandle, cherryMaxAddr, activeGameConfig.cherryMaxType, "CherryMax").toInt()
                    } else {
                        0
                    }

                    val characterName = getCharaAndShottypeName(activeGameConfig.id, rawCharacter, rawSubshot, activeLang)

                    val stageIndex = rawStage - activeGameConfig.stageStartsFrom
                    val stageStr = if (stageIndex >= 0) {
                        when (stageIndex) {
                            in 0..5 -> "Stage ${stageIndex + 1}"
                            6 -> "Extra"
                            7 -> if (activeGameConfig.id == "th07") "Phantasm" else "Stage 8"
                            else -> "Stage ${stageIndex + 1}"
                        }
                    } else {
                        "N/A"
                    }
                    val oscStageValue = if (stageIndex in 0..7) stageIndex + 1 else 0

                    if (lastScore != -1 && score < lastScore) {
                        cumulativeMisses = 0
                        cumulativeBombs = 0
                        lastRawLives = null
                        lastRawBombs = null
                    }

                    if (missAddr == 0L) {
                        lastRawLives = null
                    } else {
                        if (lastRawLives == null) {
                            lastRawLives = rawLives
                        } else {
                            if (rawLives < lastRawLives) {
                                val diff = lastRawLives - rawLives
                                if (diff in 1..8) {
                                    cumulativeMisses += diff
                                }
                            }
                            lastRawLives = rawLives
                        }
                    }

                    if (bombAddr == 0L) {
                        lastRawBombs = null
                    } else {
                        if (lastRawBombs == null) {
                            lastRawBombs = rawBombs
                        } else {
                            if (rawBombs < lastRawBombs) {
                                val diff = lastRawBombs - rawBombs
                                if (activeGameConfig.id == "th10" || activeGameConfig.id == "th11") {
                                    val playerDied = missAddr != 0L && rawLives < (lastRawLives ?: rawLives)
                                    if (!playerDied && diff in 15..25) {
                                        cumulativeBombs += 1
                                    }
                                } else {
                                    if (diff in 1..8) {
                                        cumulativeBombs += diff
                                    }
                                }
                            }
                            lastRawBombs = rawBombs
                        }
                    }

                    // Normalize Power and Point/PIV values
                    val (powerFloat, powerRawInt) = when (activeGameConfig.id) {
                        "th06", "th07", "th08" -> {
                            val p = rawPower.toFloat()
                            Pair(p, p.toInt())
                        }
                        "th10", "th11" -> {
                            val p = rawPower.toFloat() / 20.0f
                            Pair(p, rawPower.toInt())
                        }
                        "th12", "th13", "th14", "th15", "th16", "th17", "th18", "th20" -> {
                            val p = rawPower.toFloat() / 100.0f
                            Pair(p, rawPower.toInt())
                        }
                        else -> Pair(rawPower.toFloat(), rawPower.toInt())
                    }

                    val pointValue = when (activeGameConfig.id) {
                        "th13", "th14", "th15", "th16", "th17" -> {
                            (rawPoint / 100).toInt()
                        }
                        else -> rawPoint.toInt()
                    }

                    val difficultyStr = if (rawDifficulty in 0..10) {
                        getDifficultyName(activeGameConfig.id, rawDifficulty, activeLang)
                    } else {
                        null
                    }
                    val gameNameWithDiff = if (difficultyStr != null) {
                        "${getLocalizedGameName(activeGameConfig.id, activeLang)} [$difficultyStr]"
                    } else {
                        getLocalizedGameName(activeGameConfig.id, activeLang)
                    }

                    try {
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouGameID", listOf(gameIndex)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouGameName", listOf(getLocalizedGameName(activeGameConfig.id, activeLang))))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouScore", listOf(score)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouMiss", listOf(cumulativeMisses)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouBomb", listOf(cumulativeBombs)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouDifficulty", listOf(rawDifficulty)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouDifficultyName", listOf(difficultyStr ?: "")))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouCharacter", listOf(rawCharacter)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouSubshot", listOf(rawSubshot)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouCharacterName", listOf(characterName)))
                        
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouGraze", listOf(rawGraze)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouPower", listOf(powerFloat)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouPowerRaw", listOf(powerRawInt)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouPoint", listOf(pointValue)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouCherryMax", listOf(rawCherryMax)))
                    } catch (e: Exception) {
                        // Ignore
                    }

                    val activeSpellId = readActiveSpellId(processHandle, baseAddr, activeGameConfig)
                    val spellActive = activeSpellId != null && activeSpellId != -1
                    val spellStr = if (spellActive) getSpellName(activeGameConfig.id, activeSpellId!!, activeLang) else null

                    try {
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouStage", listOf(oscStageValue)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouSpellID", listOf(activeSpellId ?: -1)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouSpellActive", listOf(spellActive)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouSpellName", listOf(spellStr ?: "")))
                    } catch (e: Exception) {
                        // Ignore
                    }

                    // Update GUI live stats
                    updateLiveStats(
                        gameName = gameNameWithDiff,
                        characterName = characterName,
                        stage = stageStr,
                        score = score,
                        miss = cumulativeMisses,
                        bomb = cumulativeBombs,
                        graze = rawGraze,
                        power = powerFloat,
                        point = pointValue
                    )

                    val now = System.currentTimeMillis()
                    if (score != lastScore || cumulativeMisses != lastMiss || cumulativeBombs != lastBomb || rawStage != lastStageValue || rawCharacter != lastCharacter || rawSubshot != lastSubshot) {
                        val consoleText = when (activeLang) {
                            "zh" -> "\r[实时数据] 正在玩: $gameNameWithDiff | 机体: $characterName | 关卡: $stageStr | 分数: $score | Miss: $cumulativeMisses | Bomb: $cumulativeBombs                                "
                            "ja" -> "\r[リアルタイムデータ] プレイ中: $gameNameWithDiff | 自機: $characterName | ステージ: $stageStr | スコア: $score | 被弾: $cumulativeMisses | ボム: $cumulativeBombs                                "
                            else -> "\r[Live Data] Playing: $gameNameWithDiff | Chara: $characterName | Stage: $stageStr | Score: $score | Miss: $cumulativeMisses | Bomb: $cumulativeBombs                                "
                        }
                        print(consoleText)
                        System.out.flush()
                        lastScore = score
                        lastMiss = cumulativeMisses
                        lastBomb = cumulativeBombs
                        lastStageValue = rawStage
                        lastCharacter = rawCharacter
                        lastSubshot = rawSubshot
                    }

                    if (now - lastChatboxTime >= 2000) {
                        if (activeEnableChatbox) {
                            val chatboxText = when (activeLang) {
                                "zh" -> "正在玩: $gameNameWithDiff | 机体: $characterName | 关卡: $stageStr | 分数: $score | Miss: $cumulativeMisses | Bomb: $cumulativeBombs"
                                "ja" -> "プレイ中: $gameNameWithDiff | 自機: $characterName | ステージ: $stageStr | スコア: $score | 被弾: $cumulativeMisses | ボム: $cumulativeBombs"
                                else -> "Playing: $gameNameWithDiff | Chara: $characterName | Stage: $stageStr | Score: $score | Miss: $cumulativeMisses | Bomb: $cumulativeBombs"
                            }
                            try {
                                activeOscSender?.send(OSCMessage("/chatbox/input", listOf(chatboxText, true, false)))
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                        lastChatboxTime = now
                    }
                }
            } catch (e: Exception) {
                val errorMsg = when (activeLang) {
                    "zh" -> "\n读取内存时发生错误: ${e.message}"
                    "ja" -> "\nメモリ読み込み中にエラーが発生しました: ${e.message}"
                    else -> "\nError occurred while reading memory: ${e.message}"
                }
                println(errorMsg)
            } finally {
                Kernel32.INSTANCE.CloseHandle(processHandle)
                
                // Reset Status to Scanning
                activeStatus = ScannerStatus.SCANNING
                java.awt.EventQueue.invokeLater {
                    updateTrayLabels()
                    mainWindow?.updateScanningStatus()
                }
            }
        } else {
            java.awt.EventQueue.invokeLater {
                mainWindow?.updateScanningStatus()
            }
            Thread.sleep(2000)
        }
    }
}

fun runOsuScannerLoop() {
    val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()

    var lastOsuState = -1
    var lastOsuMiss = -1
    var lastChatboxTime = 0L

    while (true) {
        if (activeMode != "osu") {
            Thread.sleep(1000)
            continue
        }

        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:24050/json"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val jsonText = response.body()
                val root = Json.parseToJsonElement(jsonText).jsonObject
                
                val menu = root["menu"]?.jsonObject
                val state = menu?.get("state")?.jsonPrimitive?.intOrNull ?: 0
                
                val bm = menu?.get("bm")?.jsonObject
                val metadata = bm?.get("metadata")?.jsonObject
                val artist = metadata?.get("artist")?.jsonPrimitive?.content ?: ""
                val title = metadata?.get("title")?.jsonPrimitive?.content ?: ""
                val difficulty = metadata?.get("difficulty")?.jsonPrimitive?.content ?: ""

                val stats = bm?.get("stats")?.jsonObject
                val stars = stats?.get("SR")?.jsonPrimitive?.floatOrNull ?: 0f
                
                val bpmObj = stats?.get("BPM")?.jsonObject
                val bpmMin = bpmObj?.get("min")?.jsonPrimitive?.floatOrNull ?: 0f
                val bpmMax = bpmObj?.get("max")?.jsonPrimitive?.floatOrNull ?: 0f
                val bpm = maxOf(bpmMin, bpmMax)

                val modsObj = menu?.get("mods")?.jsonObject
                val modsNum = modsObj?.get("num")?.jsonPrimitive?.intOrNull ?: 0
                val modsStrRaw = modsObj?.get("str")?.jsonPrimitive?.content ?: ""
                val modsStr = if (modsStrRaw.isEmpty() || modsStrRaw.equals("nm", ignoreCase = true)) "NM" else modsStrRaw

                val gameplay = root["gameplay"]?.jsonObject
                val ppObj = gameplay?.get("pp")?.jsonObject
                val ppCurrent = ppObj?.get("current")?.jsonPrimitive?.floatOrNull ?: 0f
                val ppFc = ppObj?.get("fc")?.jsonPrimitive?.floatOrNull ?: 0f

                val score = gameplay?.get("score")?.jsonPrimitive?.intOrNull ?: 0
                val accuracy = gameplay?.get("accuracy")?.jsonPrimitive?.floatOrNull ?: 0f
                
                val combo = gameplay?.get("combo")?.jsonObject
                val currentCombo = combo?.get("current")?.jsonPrimitive?.intOrNull ?: 0
                val maxCombo = combo?.get("max")?.jsonPrimitive?.intOrNull ?: 0

                val hits = gameplay?.get("hits")?.jsonObject
                val miss = hits?.get("0")?.jsonPrimitive?.intOrNull ?: 0
                val hit300 = hits?.get("300")?.jsonPrimitive?.intOrNull ?: 0
                val hit100 = hits?.get("100")?.jsonPrimitive?.intOrNull ?: 0
                val hit50 = hits?.get("50")?.jsonPrimitive?.intOrNull ?: 0
                val gradeObj = hits?.get("grade")?.jsonObject
                val grade = gradeObj?.get("current")?.jsonPrimitive?.content ?: ""

                val hp = gameplay?.get("hp")?.jsonObject
                val hpNormal = hp?.get("normal")?.jsonPrimitive?.floatOrNull ?: 0f

                val stateText = when (state) {
                    2 -> "Menu"
                    4 -> "Playing"
                    else -> "Idle"
                }
                activeStatus = ScannerStatus.PLAYING("osu! ($stateText) [$modsStr]")
                java.awt.EventQueue.invokeLater {
                    updateTrayLabels()
                }

                java.awt.EventQueue.invokeLater {
                    mainWindow?.apply {
                        gameCard.updateValue("osu! ($stateText) [$modsStr]")
                        charaCard.updateValue(if (title.isNotEmpty()) "$artist - $title" else "N/A")
                        stageCard.updateValue(if (difficulty.isNotEmpty()) "$difficulty (${String.format(java.util.Locale.US, "%.2f", stars)}*)" else "N/A")
                        
                        if (state == 4) {
                            scoreCard.updateValue(score.toString())
                            livesCard.updateValue(miss.toString())
                            bombsCard.updateValue(currentCombo.toString())
                            powerCard.updateValue(maxCombo.toString())
                            grazeCard.updateValue(String.format(java.util.Locale.US, "%.2f%%", accuracy))
                            
                            val ppCurrentStr = String.format(java.util.Locale.US, "%.0f", ppCurrent)
                            val ppFcStr = String.format(java.util.Locale.US, "%.0f", ppFc)
                            pointCard.updateValue("${grade.ifEmpty { "N/A" }} [PP: $ppCurrentStr / $ppFcStr]")
                        } else {
                            scoreCard.updateValue("0")
                            livesCard.updateValue("0")
                            bombsCard.updateValue("0")
                            powerCard.updateValue("0")
                            grazeCard.updateValue("0.00%")
                            pointCard.updateValue("N/A")
                        }
                    }
                }

                try {
                    activeOscSender?.apply {
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuStatus", listOf(state)))
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuScore", listOf(if (state == 4) score else 0)))
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuCombo", listOf(if (state == 4) currentCombo else 0)))
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuMaxCombo", listOf(if (state == 4) maxCombo else 0)))
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuAccuracy", listOf(if (state == 4) (accuracy / 100f) else 0f)))
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuMiss", listOf(if (state == 4) miss else 0)))
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuGrade", listOf(if (state == 4) grade else "")))
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuBPM", listOf(bpm)))
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuStars", listOf(stars)))
                        
                        val oscHp = if (hpNormal > 1f) hpNormal / 200f else hpNormal
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuHP", listOf(if (state == 4) oscHp else 0f)))

                        // New parameters
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuPPCurrent", listOf(if (state == 4) ppCurrent else 0f)))
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuPPFC", listOf(if (state == 4) ppFc else 0f)))
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuModsNum", listOf(modsNum)))
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuModsStr", listOf(modsStr)))
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuHit300", listOf(if (state == 4) hit300 else 0)))
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuHit100", listOf(if (state == 4) hit100 else 0)))
                        send(com.illposed.osc.OSCMessage("/avatar/parameters/OsuHit50", listOf(if (state == 4) hit50 else 0)))
                    }
                } catch (e: Exception) {
                    // Ignore
                }

                if (activeEnableChatbox && state == 4) {
                    val now = System.currentTimeMillis()
                    if (state != lastOsuState || miss != lastOsuMiss || (now - lastChatboxTime > 8000)) {
                        val ppCurrentStr = String.format(java.util.Locale.US, "%.0f", ppCurrent)
                        val ppFcStr = String.format(java.util.Locale.US, "%.0f", ppFc)
                        val chatboxText = when (activeLang) {
                            "zh" -> "[osu!] 正在玩: $artist - $title [$difficulty] (${String.format(java.util.Locale.US, "%.2f", stars)}*) [$modsStr] | Combo: ${currentCombo}x | PP: $ppCurrentStr/$ppFcStr | Acc: ${String.format(java.util.Locale.US, "%.2f%%", accuracy)} | Miss: $miss"
                            "ja" -> "[osu!] プレイ中: $artist - $title [$difficulty] (${String.format(java.util.Locale.US, "%.2f", stars)}*) [$modsStr] | Combo: ${currentCombo}x | PP: $ppCurrentStr/$ppFcStr | Acc: ${String.format(java.util.Locale.US, "%.2f%%", accuracy)} | Miss: $miss"
                            else -> "[osu!] Playing: $artist - $title [$difficulty] (${String.format(java.util.Locale.US, "%.2f", stars)}*) [$modsStr] | Combo: ${currentCombo}x | PP: $ppCurrentStr/$ppFcStr | Acc: ${String.format(java.util.Locale.US, "%.2f%%", accuracy)} | Miss: $miss"
                        }
                        try {
                            activeOscSender?.send(com.illposed.osc.OSCMessage("/chatbox/input", listOf(chatboxText, true, false)))
                        } catch (e: Exception) {
                            // Ignore
                        }
                        lastChatboxTime = now
                        lastOsuState = state
                        lastOsuMiss = miss
                    }
                }
            } else {
                handleOsuOffline()
            }
        } catch (e: Exception) {
            handleOsuOffline()
        }

        Thread.sleep(300)
    }
}

private fun handleOsuOffline() {
    checkAndStartOsuHelper()
    activeStatus = ScannerStatus.SCANNING
    java.awt.EventQueue.invokeLater {
        updateTrayLabels()
        mainWindow?.apply {
            val statusText = when (activeLang) {
                "zh" -> "osu! (gosumemory 未运行)"
                "ja" -> "osu! (gosumemory 未起動)"
                else -> "osu! (gosumemory not running)"
            }
            gameCard.updateValue(statusText)
            charaCard.updateValue("N/A")
            stageCard.updateValue("N/A")
            scoreCard.updateValue("0")
            livesCard.updateValue("0")
            bombsCard.updateValue("0")
            powerCard.updateValue("0")
            grazeCard.updateValue("0.00%")
            pointCard.updateValue("N/A")
        }
    }
    Thread.sleep(2000)
}
