import java.awt.*

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

    val titleLabel = javax.swing.JLabel(titleVal).apply {
        foreground = MD3Color.Primary
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

object MD3Color {
    val Background = Color(0x1C, 0x1B, 0x1F)
    val Surface = Color(0x25, 0x23, 0x2A)
    val Primary = Color(0xD0, 0xBC, 0xFF)
    val OnPrimary = Color(0x38, 0x1E, 0x72)
    val SecondaryContainer = Color(0x4A, 0x44, 0x58)
    val OnSecondaryContainer = Color(0xE8, 0xDE, 0xF8)
    val Outline = Color(0x93, 0x8F, 0x99)
    val TextPrimary = Color(0xE6, 0xE1, 0xE5)
    val TextSecondary = Color(0xCA, 0xC4, 0xD0)
    val AccentGreen = Color(0xA8, 0xDA, 0xB5)
    val AccentRed = Color(0xFF, 0xB4, 0xAB)
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
    val type: ButtonType = ButtonType.FILLED,
    val radius: Int = 16
) : javax.swing.JButton(text) {
    enum class ButtonType { FILLED, OUTLINED, TEXT }
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
    init {
        isOpaque = false
        caretColor = MD3Color.Primary
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
    init {
        isContentAreaFilled = false
        isBorderPainted = false
        isFocusable = false
        foreground = if (isActive) MD3Color.OnSecondaryContainer else MD3Color.TextSecondary
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
        foreground = if (isActive) MD3Color.OnSecondaryContainer else MD3Color.TextSecondary
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
