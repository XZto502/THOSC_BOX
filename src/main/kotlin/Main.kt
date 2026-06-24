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

import com.illposed.osc.transport.OSCPortOut
import kotlinx.serialization.json.Json
import java.awt.*
import java.awt.image.BufferedImage
import java.net.InetSocketAddress

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

@Volatile
var activeOscSender: OSCPortOut? = null

var openMenuItem: javax.swing.JMenuItem? = null
var statusMenuItem: javax.swing.JMenuItem? = null
var langMenu: javax.swing.JMenu? = null
var zhItem: javax.swing.JMenuItem? = null
var enItem: javax.swing.JMenuItem? = null
var jaItem: javax.swing.JMenuItem? = null
var exitItem: javax.swing.JMenuItem? = null
var trayIcon: TrayIcon? = null

fun createTrayIconImage(): Image {
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
