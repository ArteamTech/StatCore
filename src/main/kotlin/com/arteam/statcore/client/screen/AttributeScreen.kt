package com.arteam.statcore.client.screen

import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.attributes.DefenseAttributes
import com.arteam.statcore.attributes.DefenseType
import com.arteam.statcore.core.attributes.AttributeManager
import com.arteam.statcore.util.AttributeUtils
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/**
 * 属性显示屏幕
 * 显示玩家的血量和防御信息
 */
class AttributeScreen(private val player: Player) : Screen(Component.translatable("screen.statcore.attributes")) {
    
    companion object {
        private const val BACKGROUND_WIDTH = 176
        private const val BACKGROUND_HEIGHT = 166
        private const val TEXT_COLOR = 0xE0E0E0 // 浅灰色，更柔和
        private const val VALUE_COLOR = 0xAAFFAA // 浅绿色
        private const val HEALTH_COLOR = 0xFFAAAA // 浅红色
        private const val HOVER_COLOR = 0xFFFF55 // 亮黄色高亮
        private const val BACKGROUND_COLOR = 0xFF1E1E1E.toInt() // 完全不透明的深灰色背景
    }
    
    private var leftPos = 0
    private var topPos = 0
    
    override fun init() {
        super.init()
        
        // 计算屏幕居中位置
        leftPos = (width - BACKGROUND_WIDTH) / 2
        topPos = (height - BACKGROUND_HEIGHT) / 2
        
        // 添加返回背包按钮
        addRenderableWidget(
            Button.builder(Component.translatable("gui.statcore.back_to_inventory")) { button ->
                minecraft?.setScreen(InventoryScreen(player))
            }
            .bounds(leftPos + 7, topPos + BACKGROUND_HEIGHT - 26, 80, 20)
            .build()
        )
    }
    
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // 首先渲染整个屏幕的变暗背景
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        
        // 然后渲染我们的不透明GUI面板
        renderBg(guiGraphics, partialTick, mouseX, mouseY)
        
        // 最后在不透明面板上渲染所有元素
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderAttributes(guiGraphics, mouseX, mouseY)
        renderTooltips(guiGraphics, mouseX, mouseY)
    }
    
    private fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        // 渲染完全不透明的背景
        guiGraphics.fill(leftPos, topPos, leftPos + BACKGROUND_WIDTH, topPos + BACKGROUND_HEIGHT, BACKGROUND_COLOR)
        
        // 使用更亮的颜色添加边框，使其在深色背景上更突出
        val borderColor = 0xFF888888.toInt()
        guiGraphics.hLine(leftPos - 1, leftPos + BACKGROUND_WIDTH, topPos - 1, borderColor) // 上
        guiGraphics.hLine(leftPos - 1, leftPos + BACKGROUND_WIDTH, topPos + BACKGROUND_HEIGHT, borderColor) // 下
        guiGraphics.vLine(leftPos - 1, topPos - 1, topPos + BACKGROUND_HEIGHT, borderColor) // 左
        guiGraphics.vLine(leftPos + BACKGROUND_WIDTH, topPos - 1, topPos + BACKGROUND_HEIGHT + 1, borderColor) // 右
    }
    
    private fun renderAttributes(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // 标题
        val titleText = Component.translatable("gui.statcore.attributes.title")
        val titleWidth = font.width(titleText)
        guiGraphics.drawString(font, titleText, leftPos + (BACKGROUND_WIDTH - titleWidth) / 2, topPos + 6, TEXT_COLOR, true)
        
        var yOffset = 25
        
        // 血量信息
        renderHealthInfo(guiGraphics, yOffset)
        yOffset += 40
        
        // 防御信息
        renderDefenseInfo(guiGraphics, yOffset, mouseX, mouseY)
    }
    
    private fun renderHealthInfo(guiGraphics: GuiGraphics, yOffset: Int) {
        val currentHealth = AttributeUtils.getAttributeValue(player, CoreAttributes.CURRENT_HEALTH)
        val maxHealth = AttributeUtils.getAttributeValue(player, CoreAttributes.MAX_HEALTH)
        
        // 血量标题
        val healthTitle = Component.translatable("gui.statcore.health")
        guiGraphics.drawString(font, healthTitle, leftPos + 12, topPos + yOffset, TEXT_COLOR, true)
        
        // 血量值 - 使用专门的血量颜色，启用阴影让文字更清晰
        val healthText = String.format("%.1f / %.1f", currentHealth, maxHealth)
        guiGraphics.drawString(font, healthText, leftPos + 80, topPos + yOffset, HEALTH_COLOR, true)
        
        // 血量条
        renderHealthBar(guiGraphics, leftPos + 12, topPos + yOffset + 12, currentHealth, maxHealth)
    }
    
    private fun renderHealthBar(guiGraphics: GuiGraphics, x: Int, y: Int, current: Double, max: Double) {
        val barWidth = 152
        val barHeight = 10 // 稍微增高一点
        
        // 背景（中灰色）
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF555555.toInt())
        
        // 血量条（明亮的红色）
        if (max > 0) {
            val healthPercent = (current / max).coerceIn(0.0, 1.0)
            val healthWidth = (barWidth * healthPercent).toInt()
            guiGraphics.fill(x, y, x + healthWidth, y + barHeight, 0xFFFF6666.toInt())
        }
        
        // 边框（深灰色）
        val borderColor = 0xFF333333.toInt()
        guiGraphics.hLine(x - 1, x + barWidth, y - 1, borderColor) // 上
        guiGraphics.hLine(x, x + barWidth, y + barHeight, borderColor) // 下
        guiGraphics.vLine(x - 1, y - 1, y + barHeight, borderColor) // 左
        guiGraphics.vLine(x + barWidth, y - 1, y + barHeight + 1, borderColor) // 右
    }
    
    private fun renderDefenseInfo(guiGraphics: GuiGraphics, yOffset: Int, mouseX: Int, mouseY: Int) {
        // 防御标题
        val defenseTitle = Component.translatable("gui.statcore.defense")
        guiGraphics.drawString(font, defenseTitle, leftPos + 12, topPos + yOffset, TEXT_COLOR, true)
        
        // 物理防御值
        val damageReduction = DefenseAttributes.calculateDamageReduction(player, DefenseType.PHYSICAL)
        val physicalDefense = AttributeUtils.getAttributeValue(player, CoreAttributes.PHYSICAL_DEFENSE)
        val defenseText = String.format("%.1f (%.1f%%)", physicalDefense, damageReduction * 100)
        guiGraphics.drawString(font, defenseText, leftPos + 80, topPos + yOffset, VALUE_COLOR, true)
        
        // 检查是否hover在防御文本上
        val textWidth = font.width(defenseText)
        this.hoveredDefenseArea = isMouseOverArea(mouseX, mouseY, leftPos + 80, topPos + yOffset, textWidth, font.lineHeight)
    }
    
    private var hoveredDefenseArea = false
    
    private fun renderTooltips(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (hoveredDefenseArea) {
            renderDefenseTooltip(guiGraphics, mouseX, mouseY)
        }
    }
    
    private fun renderDefenseTooltip(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val tooltipLines = mutableListOf<Component>()
        
        // 标题
        tooltipLines.add(Component.translatable("gui.statcore.defense.tooltip.title"))
        tooltipLines.add(Component.empty())
        
        // 获取所有防御类型的值
        val physicalDefense = AttributeUtils.getAttributeValue(player, CoreAttributes.PHYSICAL_DEFENSE)
        val projectileDefense = AttributeUtils.getAttributeValue(player, CoreAttributes.PROJECTILE_DEFENSE)
        val explosionDefense = AttributeUtils.getAttributeValue(player, CoreAttributes.EXPLOSION_DEFENSE)
        val fireDefense = AttributeUtils.getAttributeValue(player, CoreAttributes.FIRE_DEFENSE)
        val trueDefense = AttributeUtils.getAttributeValue(player, CoreAttributes.TRUE_DEFENSE)
        
        // 添加各种防御类型
        addDefenseTooltipLine(tooltipLines, "gui.statcore.defense.physical", physicalDefense)
        addDefenseTooltipLine(tooltipLines, "gui.statcore.defense.projectile", projectileDefense)
        addDefenseTooltipLine(tooltipLines, "gui.statcore.defense.explosion", explosionDefense)
        addDefenseTooltipLine(tooltipLines, "gui.statcore.defense.fire", fireDefense)
        addDefenseTooltipLine(tooltipLines, "gui.statcore.defense.true", trueDefense)
        
        // 显示基础值和修改器信息
        tooltipLines.add(Component.empty())
        addModifierTooltipInfo(tooltipLines, CoreAttributes.PHYSICAL_DEFENSE)
        
        // 渲染tooltip - 转换为正确的类型
        val formattedTooltips = tooltipLines.map { it.visualOrderText }.toMutableList()
        guiGraphics.renderTooltip(font, formattedTooltips, mouseX, mouseY)
    }
    
    private fun addDefenseTooltipLine(tooltipLines: MutableList<Component>, translationKey: String, value: Double) {
        val reduction = DefenseAttributes.calculateDamageReduction(value)
        val line = Component.translatable(translationKey)
            .append(": ")
            .append(String.format("%.1f (%.1f%%)", value, reduction * 100))
            .withStyle { it.withColor(if (value > 0) 0x55FF55 else 0x888888) }
        tooltipLines.add(line)
    }
    
    private fun addModifierTooltipInfo(tooltipLines: MutableList<Component>, attribute: com.arteam.statcore.api.attributes.StatAttribute) {
        val attributeMap = AttributeManager.getAttributeMap(player)
        val instance = attributeMap.getInstance(attribute)
        
        if (instance != null) {
            val baseValue = instance.baseValue
            val modifiers = instance.getModifiers()
            
            // 基础值
            tooltipLines.add(Component.translatable("gui.statcore.tooltip.base_value")
                .append(": ")
                .append(String.format("%.1f", baseValue))
                .withStyle { it.withColor(0xFFFFFF) })
            
            // 修改器
            if (modifiers.isNotEmpty()) {
                tooltipLines.add(Component.translatable("gui.statcore.tooltip.modifiers").withStyle { it.withColor(0xAAAAAA) })
                for (modifier in modifiers.take(5)) { // 最多显示5个修改器
                    val modifierLine = Component.literal("  +")
                        .append(String.format("%.1f", modifier.amount))
                        .append(" (")
                        .append(modifier.name)
                        .append(")")
                        .withStyle { it.withColor(0x55FF55) }
                    tooltipLines.add(modifierLine)
                }
                
                if (modifiers.size > 5) {
                    tooltipLines.add(Component.translatable("gui.statcore.tooltip.more_modifiers", modifiers.size - 5)
                        .withStyle { it.withColor(0x888888) })
                }
            }
        }
    }
    
    private fun isMouseOverArea(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int): Boolean {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
    }
    
    override fun isPauseScreen(): Boolean = false
}