package com.arteam.statcore.util

import com.arteam.statcore.api.attributes.AttributeModifier
import com.arteam.statcore.api.attributes.StatAttribute
import com.arteam.statcore.core.attributes.AttributeManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import java.util.*

/**
 * 属性工具类
 * 提供常用的属性操作辅助方法
 */
@Suppress("unused")
object AttributeUtils {
    
    /**
     * 检查实体是否拥有指定属性
     * @param entity 目标实体
     * @param attribute 属性定义
     * @return 如果实体拥有该属性返回true
     */
    fun hasAttribute(entity: LivingEntity, attribute: StatAttribute): Boolean {
        return AttributeManager.getAttributeMap(entity).hasInstance(attribute)
    }
    
    /**
     * 安全地获取实体的属性值
     * @param entity 目标实体
     * @param attribute 属性定义
     * @return 属性值，如果属性不存在或不适用则返回默认值
     */
    fun getAttributeValue(entity: LivingEntity, attribute: StatAttribute): Double {
        return AttributeManager.getAttributeValue(entity, attribute)
    }
    
    /**
     * 安全地设置实体的属性基础值
     * @param entity 目标实体
     * @param attribute 属性定义
     * @param baseValue 基础值
     */
    fun setAttributeBaseValue(entity: LivingEntity, attribute: StatAttribute, baseValue: Double) {
        AttributeManager.setAttributeBaseValue(entity, attribute, baseValue)
    }
    
    /**
     * 为实体添加临时属性修改器
     * @param entity 目标实体
     * @param attribute 属性定义
     * @param name 修改器名称
     * @param amount 修改值
     * @param operation 操作类型
     * @param source 来源标识
     * @return 添加的修改器ID
     */
    fun addTemporaryModifier(
        entity: LivingEntity,
        attribute: StatAttribute,
        name: String,
        amount: Double,
        operation: com.arteam.statcore.api.attributes.AttributeOperation,
        source: ResourceLocation
    ): UUID {
        val modifier = AttributeModifier(
            id = UUID.randomUUID(),
            name = name,
            amount = amount,
            operation = operation,
            source = source
        )
        
        AttributeManager.getAttributeMap(entity).addModifier(attribute, modifier)
        return modifier.id
    }
    
    /**
     * 移除实体的指定修改器
     * @param entity 目标实体
     * @param attribute 属性定义
     * @param modifierId 修改器ID
     * @return 如果成功移除返回true
     */
    fun removeModifier(entity: LivingEntity, attribute: StatAttribute, modifierId: UUID): Boolean {
        return AttributeManager.getAttributeMap(entity).removeModifier(attribute, modifierId)
    }
    
    /**
     * 根据来源移除实体的所有修改器
     * @param entity 目标实体
     * @param source 来源标识
     * @return 移除的修改器数量
     */
    fun removeModifiersBySource(entity: LivingEntity, source: ResourceLocation): Int {
        return AttributeManager.getAttributeMap(entity).removeModifiersBySource(source)
    }
    
    /**
     * 检查实体是否为玩家
     * @param entity 目标实体
     * @return 如果是玩家返回true
     */
    fun isPlayer(entity: LivingEntity): Boolean {
        return entity is Player
    }
    
    /**
     * 获取实体适用的所有属性
     * @param entity 目标实体
     * @return 适用的属性列表
     */
    fun getApplicableAttributes(entity: LivingEntity): List<StatAttribute> {
        return AttributeManager.registry.getApplicableAttributes(entity)
    }
    
    /**
     * 创建资源位置
     * @param namespace 命名空间
     * @param path 路径
     * @return 资源位置实例
     */
    fun createResourceLocation(namespace: String, path: String): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath(namespace, path)
    }
    
    /**
     * 创建 StatCore 命名空间的资源位置
     * @param path 路径
     * @return 资源位置实例
     */
    fun createStatCoreLocation(path: String): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath("statcore", path)
    }
    
    /**
     * 格式化属性值为显示字符串
     * @param value 属性值
     * @param precision 小数精度（默认2位）
     * @return 格式化后的字符串
     */
    fun formatAttributeValue(value: Double, precision: Int = 2): String {
        return if (value == value.toLong().toDouble()) {
            // 整数值
            value.toLong().toString()
        } else {
            // 小数值
            String.format("%.${precision}f", value)
        }
    }
    
    /**
     * 比较两个属性值是否近似相等
     * @param value1 第一个值
     * @param value2 第二个值
     * @param epsilon 误差范围（默认0.001）
     * @return 如果近似相等返回true
     */
    fun isValueEqual(value1: Double, value2: Double, epsilon: Double = 0.001): Boolean {
        return kotlin.math.abs(value1 - value2) < epsilon
    }
    
    /**
     * 计算百分比修改器的实际影响
     * @param baseValue 基础值
     * @param percentage 百分比（例如0.1表示10%）
     * @return 实际影响值
     */
    fun calculatePercentageImpact(baseValue: Double, percentage: Double): Double {
        return baseValue * percentage
    }
    
    /**
     * 验证属性ID是否有效
     * @param id 资源位置ID
     * @return 如果有效返回true
     */
    fun isValidAttributeId(id: ResourceLocation): Boolean {
        return id.namespace.isNotBlank() && 
               id.path.isNotBlank() &&
               id.namespace.matches(Regex("[a-z0-9_.-]+")) &&
               id.path.matches(Regex("[a-z0-9_./]+"))
    }
    
    /**
     * 检查属性是否有最大值限制
     * @param attribute 属性定义
     * @return 如果有最大值限制返回true，无限制返回false
     */
    fun hasMaxValueLimit(attribute: StatAttribute): Boolean {
        return attribute.hasMaxValueLimit()
    }
    
    /**
     * 检查属性是否有最小值限制
     * @param attribute 属性定义
     * @return 如果有最小值限制返回true，无限制返回false
     */
    fun hasMinValueLimit(attribute: StatAttribute): Boolean {
        return attribute.hasMinValueLimit()
    }
    
    /**
     * 格式化属性值为显示字符串（支持无限大值）
     * @param value 属性值
     * @param precision 小数精度（默认2位）
     * @return 格式化后的字符串
     */
    fun formatAttributeValueSafe(value: Double, precision: Int = 2): String {
        return when {
            value.isInfinite() && value > 0 -> "∞"
            value.isInfinite() && value < 0 -> "-∞"
            value.isNaN() -> "NaN"
            value == value.toLong().toDouble() -> value.toLong().toString()
            else -> String.format("%.${precision}f", value)
        }
    }
    
    /**
     * 检查属性值是否安全（不是无限或NaN）
     * @param value 属性值
     * @return 如果值是有限数字返回true
     */
    fun isValueSafe(value: Double): Boolean {
        return value.isFinite() && !value.isNaN()
    }
    
    /**
     * 获取属性的显示名称
     * @param attribute 属性定义
     * @return 属性的本地化键或友好名称
     */
    fun getAttributeDisplayName(attribute: StatAttribute): String {
        // 可以在这里添加翻译逻辑
        return attribute.getTranslationKey()
    }
    
    /**
     * 检查属性值是否超出推荐范围
     * 虽然属性可以无限大，但过大的值可能影响游戏体验
     * @param value 属性值
     * @param recommendedMax 推荐的最大值（默认为StatCore定义的推荐值）
     * @return 如果超出推荐范围返回true
     */
    fun isValueBeyondRecommended(value: Double, recommendedMax: Double = com.arteam.statcore.StatCore.RECOMMENDED_ATTRIBUTE_MAX_VALUE): Boolean {
        return value > recommendedMax
    }
    
    /**
     * 创建无最大值限制的安全值
     * 在某些情况下，即使属性支持无限大，也需要一个"安全"的大值作为替代
     * @return 一个很大但有限的安全值
     */
    fun createSafeMaxValue(): Double {
        return com.arteam.statcore.StatCore.SAFE_INFINITY_VALUE  // 100万，对大多数游戏场景都足够大
    }
} 