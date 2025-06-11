package com.arteam.statcore.api.attributes

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

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
     */
    val maxValue: Double
    
    /**
     * 属性是否可以叠加（多个修改器能否叠加应用）
     */
    val isStackable: Boolean
    
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
     * 将值限制在有效范围内
     * @param value 要限制的值
     * @return 限制后的值
     */
    fun clampValue(value: Double): Double {
        return value.coerceIn(minValue, maxValue)
    }
    
    /**
     * 获取属性的本地化键
     */
    fun getTranslationKey(): String {
        return "attribute.${id.namespace}.${id.path}"
    }
} 