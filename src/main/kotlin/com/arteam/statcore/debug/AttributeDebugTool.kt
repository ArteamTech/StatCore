package com.arteam.statcore.debug

import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.attributes.DefenseAttributes
import com.arteam.statcore.attributes.DefenseType
import com.arteam.statcore.core.attributes.AttributeManager
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player

/**
 * 属性调试工具
 * 提供详细的属性信息显示和测试功能
 */
object AttributeDebugTool {
    
    /**
     * 生成实体的详细属性报告
     * @param entity 目标实体
     * @return 格式化的属性报告文本列表
     */
    fun generateDetailedReport(entity: LivingEntity): List<Component> {
        val report = mutableListOf<Component>()
        
        // 标题
        report.add(Component.literal("=== ${getEntityName(entity)} 属性详情 ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
        
        // 基础信息
        report.add(Component.literal("实体类型: ${entity.type.description.string}")
            .withStyle(ChatFormatting.GRAY))
        report.add(Component.literal("UUID: ${entity.uuid}")
            .withStyle(ChatFormatting.GRAY))
        
        // 生命值信息
        addHealthInfo(entity, report)
        
        // 防御信息
        addDefenseInfo(entity, report)
        
        // 原版属性对比
        addVanillaComparison(entity, report)
        
        return report
    }
    
    /**
     * 添加生命值信息
     */
    private fun addHealthInfo(entity: LivingEntity, report: MutableList<Component>) {
        report.add(Component.literal("--- 生命值系统 ---")
            .withStyle(ChatFormatting.AQUA))
        
        val customMaxHealth = AttributeManager.getAttributeValue(entity, CoreAttributes.MAX_HEALTH)
        val vanillaMaxHealth = entity.maxHealth
        val currentHealth = entity.health
        val healthRegen = AttributeManager.getAttributeValue(entity, CoreAttributes.HEALTH_REGENERATION)
        
        report.add(Component.literal(String.format("当前生命值: %.1f / %.1f", 
            currentHealth, customMaxHealth)).withStyle(ChatFormatting.GREEN))
        report.add(Component.literal(String.format("原版最大生命值: %.1f", vanillaMaxHealth))
            .withStyle(ChatFormatting.GRAY))
        report.add(Component.literal(String.format("生命恢复: %.2f/秒", healthRegen))
            .withStyle(ChatFormatting.GREEN))
    }
    
    /**
     * 添加防御信息
     */
    private fun addDefenseInfo(entity: LivingEntity, report: MutableList<Component>) {
        report.add(Component.literal("--- 防御系统 ---")
            .withStyle(ChatFormatting.BLUE))
        
        // 各种防御类型
        DefenseType.entries.forEach { defenseType ->
            val defenseValue = DefenseAttributes.calculateEffectiveDefense(entity, defenseType)
            val damageReduction = DefenseAttributes.calculateDamageReduction(defenseValue)
            val percentage = damageReduction * 100
            
            val color = when {
                percentage >= 50 -> ChatFormatting.DARK_GREEN
                percentage >= 25 -> ChatFormatting.YELLOW
                percentage > 0 -> ChatFormatting.WHITE
                else -> ChatFormatting.GRAY
            }
            
            report.add(Component.literal(String.format("%s防御: %.1f (减免 %.1f%%)", 
                getDefenseTypeName(defenseType), defenseValue, percentage)).withStyle(color))
        }
        
        // 原版护甲对比
        val vanillaArmor = entity.armorValue
        report.add(Component.literal("原版护甲: $vanillaArmor 点")
            .withStyle(ChatFormatting.GRAY))
    }
    
    /**
     * 添加原版属性对比
     */
    private fun addVanillaComparison(entity: LivingEntity, report: MutableList<Component>) {
        report.add(Component.literal("--- 原版对比 ---")
            .withStyle(ChatFormatting.YELLOW))
        
        val scalingFactor = if (entity is Player) "玩家特殊" else "5倍放大"
        report.add(Component.literal("缩放模式: $scalingFactor")
            .withStyle(ChatFormatting.YELLOW))
        
        // 计算理论原版伤害减免
        val vanillaArmor = entity.armorValue
        val vanillaReduction = calculateVanillaArmorReduction(vanillaArmor)
        val ourPhysicalReduction = DefenseAttributes.calculateDamageReduction(entity, DefenseType.PHYSICAL) * 100
        
        report.add(Component.literal(String.format("原版护甲减免: %.1f%%", vanillaReduction))
            .withStyle(ChatFormatting.GRAY))
        report.add(Component.literal(String.format("我们的物理减免: %.1f%%", ourPhysicalReduction))
            .withStyle(ChatFormatting.WHITE))
    }
    
    /**
     * 模拟伤害测试
     * @param entity 目标实体
     * @param damageAmount 伤害量
     * @param defenseType 防御类型
     * @return 格式化的测试结果
     */
    fun simulateDamage(entity: LivingEntity, damageAmount: Float, defenseType: DefenseType): List<Component> {
        val result = mutableListOf<Component>()
        
        result.add(Component.literal("=== 伤害模拟测试 ===")
            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
        
        // 原始伤害
        result.add(Component.literal("原始伤害: $damageAmount")
            .withStyle(ChatFormatting.WHITE))
        
        // 放大后伤害
        val scaledDamage = damageAmount * 5.0f
        result.add(Component.literal("放大伤害: $scaledDamage (×5)")
            .withStyle(ChatFormatting.YELLOW))
        
        // 防御计算
        val damageReduction = DefenseAttributes.calculateDamageReduction(entity, defenseType)
        val finalDamage = scaledDamage * (1.0f - damageReduction.toFloat())
        
        result.add(Component.literal(String.format("防御类型: %s", getDefenseTypeName(defenseType)))
            .withStyle(ChatFormatting.BLUE))
        result.add(Component.literal(String.format("伤害减免: %.1f%%", damageReduction * 100))
            .withStyle(ChatFormatting.BLUE))
        result.add(Component.literal(String.format("最终伤害: %.1f", finalDamage))
            .withStyle(ChatFormatting.RED))
        
        return result
    }
    
    /**
     * 获取实体显示名称
     */
    private fun getEntityName(entity: LivingEntity): String {
        return when (entity) {
            is Player -> entity.name.string
            else -> entity.type.description.string
        }
    }
    
    /**
     * 获取防御类型的中文名称
     */
    private fun getDefenseTypeName(defenseType: DefenseType): String {
        return when (defenseType) {
            DefenseType.PHYSICAL -> "物理"
            DefenseType.PROJECTILE -> "弹射物"
            DefenseType.EXPLOSION -> "爆炸"
            DefenseType.FIRE -> "火焰"
            DefenseType.TRUE_DEFENSE -> "真实"
        }
    }
    
    /**
     * 计算原版护甲的伤害减免
     * 使用原版护甲公式进行计算
     */
    private fun calculateVanillaArmorReduction(armorValue: Int): Double {
        // 原版护甲公式：armor / (armor + 50) * 100%，最大80%
        val reduction = armorValue.toDouble() / (armorValue + 50.0)
        return minOf(reduction * 100.0, 80.0)
    }
} 