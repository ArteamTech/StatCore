package com.arteam.statcore.client.gui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.ImageButton
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * StatCore标签页按钮
 * 禁用悬浮状态变化，支持分层渲染，支持物品图标
 */
class StatCoreTabButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    sprites: WidgetSprites,
    onPress: OnPress,
    narration: Component,
    private val isSelected: Boolean = false,
    private val iconItem: ItemStack = ItemStack.EMPTY
) : ImageButton(x, y, width, height, sprites, onPress, narration) {
    
    // 控制是否允许自动渲染
    private var allowAutoRender = false
    
    /**
     * 重写isHovered检查，始终返回false以禁用悬浮效果
     */
    override fun isHovered(): Boolean {
        return false
    }
    
    /**
     * 重写渲染方法，只在允许时才渲染
     */
    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (allowAutoRender) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
        }
        // 否则不渲染，由我们的自定义方法控制
    }
    
    /**
     * 检查是否为选中状态
     */
    fun isSelectedTab(): Boolean {
        return isSelected
    }
    
    /**
     * 强制渲染（用于手动控制渲染时机）
     */
    fun forceRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // 暂时启用自动渲染
        val oldAutoRender = allowAutoRender
        allowAutoRender = true
        
        // 强制设置为非悬浮状态
        try {
            val hoveredField = net.minecraft.client.gui.components.AbstractWidget::class.java.getDeclaredField("isHovered")
            hoveredField.isAccessible = true
            hoveredField.setBoolean(this, false)
            
            // 调用渲染组件方法
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
            
            // 渲染物品图标
            renderIcon(guiGraphics)
        } catch (e: Exception) {
            // 如果反射失败，直接调用渲染组件方法
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
            // 仍然渲染图标
            renderIcon(guiGraphics)
        } finally {
            // 恢复原始自动渲染状态
            allowAutoRender = oldAutoRender
        }
    }
    
    /**
     * 渲染物品图标
     */
    private fun renderIcon(guiGraphics: GuiGraphics) {
        if (!iconItem.isEmpty) {
            // 计算图标位置：居中在按钮内
            val iconX = x + (width - 16) / 2
            val iconY = y + (height - 16) / 2
            
            // 渲染物品图标
            guiGraphics.renderItem(iconItem, iconX, iconY)
        }
    }
    
    /**
     * 渲染在背景之前（未选中状态）
     */
    fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (!isSelected) {
            forceRender(guiGraphics, mouseX, mouseY, partialTick)
        }
    }
    
    /**
     * 渲染在前景（选中状态）
     */
    fun renderForeground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (isSelected) {
            forceRender(guiGraphics, mouseX, mouseY, partialTick)
        }
    }
} 