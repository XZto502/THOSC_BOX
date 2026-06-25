import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*

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

fun updateUIManagerColors() {
    javax.swing.UIManager.put("MenuItem.selectionBackground", MD3Color.SecondaryContainer)
    javax.swing.UIManager.put("MenuItem.selectionForeground", MD3Color.OnSecondaryContainer)
    javax.swing.UIManager.put("Menu.selectionBackground", MD3Color.SecondaryContainer)
    javax.swing.UIManager.put("Menu.selectionForeground", MD3Color.OnSecondaryContainer)
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
        val isEnabled = c?.isEnabled ?: true

        if (!isEnabled) {
            if (isSelected) {
                g2d.color = Color(MD3Color.Outline.red, MD3Color.Outline.green, MD3Color.Outline.blue, 120)
                g2d.fillRoundRect(x + 1, y + 1, size - 2, size - 2, 4, 4)

                g2d.color = MD3Color.Surface
                g2d.stroke = java.awt.BasicStroke(2.0f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND)
                
                val px1 = x + (0.28 * size).toInt()
                val py1 = y + (0.5 * size).toInt()
                val px2 = x + (0.45 * size).toInt()
                val py2 = y + (0.7 * size).toInt()
                val px3 = x + (0.75 * size).toInt()
                val py3 = y + (0.3 * size).toInt()

                g2d.drawLine(px1, py1, px2, py2)
                g2d.drawLine(px2, py2, px3, py3)
            } else {
                g2d.color = Color(MD3Color.Outline.red, MD3Color.Outline.green, MD3Color.Outline.blue, 80)
                g2d.stroke = java.awt.BasicStroke(1.5f)
                g2d.drawRoundRect(x + 1, y + 1, size - 3, size - 3, 4, 4)
            }
        } else if (isSelected) {
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
