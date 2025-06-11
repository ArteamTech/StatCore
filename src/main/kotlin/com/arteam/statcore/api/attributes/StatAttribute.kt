package com.arteam.statcore.api.attributes

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

/**
 * 实体类型枚举
 * 用于更精确地控制属性的适用范围
 */
enum class EntityCategory {
    PLAYER,       // 玩家
    MONSTER,      // 怪物
    ANIMAL,       // 动物
    NPC,          // NPC
    OTHER         // 其他生物
}

/**
 * StatCore 属性接口
 * 定义属性的基本结构和行为
 */
interface StatAttribute {
    /**
     * 属性的唯一标识符
     */
    val id: ResourceLocation
    
    /**
     * 属性的默认基础值
     */
    val defaultValue: Double
    
    /**
     * 属性的最小值
     */
    val minValue: Double
    
    /**
     * 属性的最大值
     * 可以设置为 Double.POSITIVE_INFINITY 表示无最大值限制
     */
    val maxValue: Double
    
    /**
     * 属性是否可以叠加（多个修改器能否叠加应用）
     */
    val isStackable: Boolean
    
    /**
     * 获取属性适用的实体类型集合
     * @return 适用的实体类型集合
     */
    fun getApplicableCategories(): Set<EntityCategory>
    
    /**
     * 检查属性是否适用于指定实体
     * @param entity 目标实体
     * @return 如果适用返回true
     */
    fun isApplicableTo(entity: LivingEntity): Boolean
    
    /**
     * 计算应用修改器后的最终属性值
     * 使用公式: (基础值 + 所有加法修改值) * (1 + 所有乘法修改值)
     * @param baseValue 基础属性值
     * @param modifiers 要应用的修改器列表
     * @return 计算后的最终值
     */
    fun calculate(baseValue: Double, modifiers: List<AttributeModifier>): Double
    
    /**
     * 自定义修改器计算（供开发者重写）
     * 如果返回null，则使用默认计算逻辑
     * @param baseValue 基础值
     * @param modifiers 修改器列表
     * @return 自定义计算结果，或null表示使用默认计算
     */
    fun customCalculate(baseValue: Double, modifiers: List<AttributeModifier>): Double? = null
    
    /**
     * 检查属性是否有最大值限制
     * @return 如果有最大值限制返回true，如果可以无限大返回false
     */
    fun hasMaxValueLimit(): Boolean {
        return maxValue != Double.POSITIVE_INFINITY && !maxValue.isInfinite()
    }
    
    /**
     * 检查属性是否有最小值限制
     * @return 如果有最小值限制返回true，如果可以无限小返回false
     */
    fun hasMinValueLimit(): Boolean {
        return minValue != Double.NEGATIVE_INFINITY && !minValue.isInfinite()
    }
    
    /**
     * 将值限制在有效范围内
     * @param value 要限制的值
     * @return 限制后的值
     */
    fun clampValue(value: Double): Double {
        var result = value
        
        // 只有在有最小值限制时才应用最小值限制
        if (hasMinValueLimit()) {
            result = maxOf(result, minValue)
        }
        
        // 只有在有最大值限制时才应用最大值限制
        if (hasMaxValueLimit()) {
            result = minOf(result, maxValue)
        }
        
        return result
    }
    
    /**
     * 获取属性的本地化键
     */
    fun getTranslationKey(): String {
        return "attribute.${id.namespace}.${id.path}"
    }
} 