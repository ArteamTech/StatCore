package com.arteam.statcore.client.screen

import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.attributes.DefenseAttributes
import com.arteam.statcore.attributes.DefenseType
import com.arteam.statcore.client.gui.StatCoreTabButton
import com.arteam.statcore.core.attributes.AttributeManager
import com.arteam.statcore.util.AttributeUtils
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * 属性显示屏幕
 * 显示玩家的血量和防御信息，使用与物品栏相同的背景遮罩效果
 */
@Suppress("unused")
class AttributeScreen(private val player: Player) : Screen(Component.translatable("screen.statcore.attributes")) {
    
    companion object {
        private const val BACKGROUND_WIDTH = 176
        private const val BACKGROUND_HEIGHT = 166
        private const val TEXT_COLOR = 0x404040 // 深灰色文字
        private const val VALUE_COLOR = 0x2E8B57 // 海绿色数值
        private const val HEALTH_COLOR = 0xCD5C5C // 印第安红色血量
        private const val HOVER_COLOR = 0xFFFF55 // 亮黄色高亮
        
        // 原版纹理资源
        private val STAT_BACKGROUND = ResourceLocation.fromNamespaceAndPath("statcore", "textures/gui/stat.png")
        private val CREATIVE_INVENTORY_TABS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/creative_inventory/tabs.png")
    }
    
    private var leftPos = 0
    private var topPos = 0
    private var inventoryButton: StatCoreTabButton? = null
    private var attributesButton: StatCoreTabButton? = null
    
    override fun init() {
        super.init()
        
        // 计算屏幕居中位置
        leftPos = (width - BACKGROUND_WIDTH) / 2
        topPos = (height - BACKGROUND_HEIGHT) / 2
        
        // 创建按钮纹理精灵（使用创造模式标签页纹理）
        val inventoryButtonSprites = WidgetSprites(
            ResourceLocation.fromNamespaceAndPath("minecraft", "container/creative_inventory/tab_top_unselected_1"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "container/creative_inventory/tab_top_selected_1")
        )
        
        val attributesButtonSprites = WidgetSprites(
            ResourceLocation.fromNamespaceAndPath("minecraft", "container/creative_inventory/tab_top_selected_2"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "container/creative_inventory/tab_top_unselected_2")
        )
        
        // 添加背包按钮（左侧第一个，未激活状态）
        inventoryButton = StatCoreTabButton(
            leftPos, topPos - 28,       // 位置：与物品栏左侧对齐，在物品栏上方
            28, 32,                      // 大小
            inventoryButtonSprites,      // 纹理精灵
            { minecraft?.setScreen(InventoryScreen(player)) },
            Component.translatable("gui.statcore.inventory.tooltip"),
            false,                       // 未选中状态
            ItemStack(Items.CHEST)       // 箱子图标
        )
        
        // 添加属性按钮（左侧第二个，激活状态）
        attributesButton = StatCoreTabButton(
            leftPos + 28, topPos - 28,  // 位置：紧贴背包按钮右侧
            28, 32,                      // 大小
            attributesButtonSprites,     // 纹理精灵
            { /* 点击属性按钮，不做任何操作（已经在属性界面） */ },
            Component.translatable("gui.statcore.attributes.tooltip"),
            true,                        // 选中状态
            ItemStack(Items.IRON_SWORD)  // 铁剑图标
        )
        
        // 注意：不添加到渲染系统，我们手动控制渲染
    }
    
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // 渲染透明背景（与物品栏相同的变暗效果）
        this.renderTransparentBackground(guiGraphics)
        
        // 先渲染未选中的按钮（在背景之前）
        inventoryButton?.renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        
        // 渲染原版样式的GUI背景
        renderBg(guiGraphics, partialTick, mouseX, mouseY)
        
        // 手动渲染所有需要渲染的元素
        renderAttributes(guiGraphics, mouseX, mouseY)
        
        // 最后渲染选中的按钮（在前景）
        attributesButton?.renderForeground(guiGraphics, mouseX, mouseY, partialTick)
        
        renderTooltips(guiGraphics, mouseX, mouseY)
    }
    
    private fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        // 使用StatCore自定义背景纹理
        guiGraphics.blit(
            RenderType::guiTextured,
            STAT_BACKGROUND,
            leftPos, topPos, 0.0f, 0.0f,
            BACKGROUND_WIDTH, BACKGROUND_HEIGHT,
            256, 256
        )
    }
    
    private fun renderAttributes(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // 标题
        val titleText = Component.translatable("gui.statcore.attributes.title")
        val titleWidth = font.width(titleText)
        guiGraphics.drawString(font, titleText, leftPos + (BACKGROUND_WIDTH - titleWidth) / 2, topPos + 6, TEXT_COLOR, false)
        
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
        guiGraphics.drawString(font, healthTitle, leftPos + 12, topPos + yOffset, TEXT_COLOR, false)
        
        // 血量值
        val healthText = String.format("%.1f / %.1f", currentHealth, maxHealth)
        guiGraphics.drawString(font, healthText, leftPos + 80, topPos + yOffset, HEALTH_COLOR, false)
        
        // 血量条
        renderHealthBar(guiGraphics, leftPos + 12, topPos + yOffset + 12, currentHealth, maxHealth)
    }
    
    private fun renderHealthBar(guiGraphics: GuiGraphics, x: Int, y: Int, current: Double, max: Double) {
        val barWidth = 152
        val barHeight = 8
        
        // 背景（深灰色）
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF8B8B8B.toInt())
        
        // 血量条（红色）
        if (max > 0) {
            val healthPercent = (current / max).coerceIn(0.0, 1.0)
            val healthWidth = (barWidth * healthPercent).toInt()
            guiGraphics.fill(x, y, x + healthWidth, y + barHeight, 0xFFDC143C.toInt())
        }
        
        // 边框（深色）
        val borderColor = 0xFF373737.toInt()
        guiGraphics.hLine(x - 1, x + barWidth, y - 1, borderColor) // 上
        guiGraphics.hLine(x - 1, x + barWidth, y + barHeight, borderColor) // 下
        guiGraphics.vLine(x - 1, y - 1, y + barHeight, borderColor) // 左
        guiGraphics.vLine(x + barWidth, y - 1, y + barHeight + 1, borderColor) // 右
    }
    
    private fun renderDefenseInfo(guiGraphics: GuiGraphics, yOffset: Int, mouseX: Int, mouseY: Int) {
        // 防御标题
        val defenseTitle = Component.translatable("gui.statcore.defense")
        guiGraphics.drawString(font, defenseTitle, leftPos + 12, topPos + yOffset, TEXT_COLOR, false)
        
        // 物理防御值
        val damageReduction = DefenseAttributes.calculateDamageReduction(player, DefenseType.PHYSICAL)
        val physicalDefense = AttributeUtils.getAttributeValue(player, CoreAttributes.PHYSICAL_DEFENSE)
        val defenseText = String.format("%.1f (%.1f%%)", physicalDefense, damageReduction * 100)
        guiGraphics.drawString(font, defenseText, leftPos + 80, topPos + yOffset, VALUE_COLOR, false)
        
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
        
        // 渲染tooltip
        val formattedTooltips = tooltipLines.map { it.visualOrderText }.toMutableList()
        guiGraphics.renderTooltip(font, formattedTooltips, mouseX, mouseY)
    }
    
    private fun addDefenseTooltipLine(tooltipLines: MutableList<Component>, translationKey: String, value: Double) {
        val reduction = DefenseAttributes.calculateDamageReduction(value)
        val line = Component.translatable(translationKey)
            .append(": ")
            .append(String.format("%.1f (%.1f%%)", value, reduction * 100))
            .withStyle { it.withColor(if (value > 0) 0x228B22 else 0x696969) }
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
                .withStyle { it.withColor(0x000000) })
            
            // 修改器
            if (modifiers.isNotEmpty()) {
                tooltipLines.add(Component.translatable("gui.statcore.tooltip.modifiers").withStyle { it.withColor(0x555555) })
                for (modifier in modifiers.take(5)) { // 最多显示5个修改器
                    val modifierLine = Component.literal("  +")
                        .append(String.format("%.1f", modifier.amount))
                        .append(" (")
                        .append(modifier.name)
                        .append(")")
                        .withStyle { it.withColor(0x228B22) }
                    tooltipLines.add(modifierLine)
                }
                
                if (modifiers.size > 5) {
                    tooltipLines.add(Component.translatable("gui.statcore.tooltip.more_modifiers", modifiers.size - 5)
                        .withStyle { it.withColor(0x696969) })
                }
            }
        }
    }
    
    private fun isMouseOverArea(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int): Boolean {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // 当按下ESC或物品栏键时关闭屏幕
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || this.minecraft!!.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose()
            return true
        }
        // 其他按键事件交由父类处理
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    override fun isPauseScreen(): Boolean = false
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // 检查是否点击了我们的按钮
        inventoryButton?.let { btn ->
            if (btn.mouseClicked(mouseX, mouseY, button)) {
                return true
            }
        }
        
        attributesButton?.let { btn ->
            if (btn.mouseClicked(mouseX, mouseY, button)) {
                return true
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
}