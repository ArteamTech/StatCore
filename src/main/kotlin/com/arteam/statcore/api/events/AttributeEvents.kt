package com.arteam.statcore.api.events

import com.arteam.statcore.api.attributes.AttributeModifier
import com.arteam.statcore.api.attributes.StatAttribute
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent

/**
 * 属性计算事件
 * 在属性值计算时触发，允许其他模组修改计算结果
 */
class AttributeCalculationEvent(
    /**
     * 拥有属性的实体
     */
    val entity: LivingEntity,
    
    /**
     * 被计算的属性
     */
    val attribute: StatAttribute,
    
    /**
     * 基础值（不包含修改器）
     */
    val baseValue: Double,
    
    /**
     * 应用的修改器列表
     */
    val modifiers: List<AttributeModifier>,
    
    /**
     * 计算结果（可以被修改）
     */
    var result: Double
) : Event(), ICancellableEvent

/**
 * 属性修改器添加事件
 * 在修改器被添加到属性实例时触发
 */
class AttributeModifierAddEvent(
    /**
     * 拥有属性的实体
     */
    val entity: LivingEntity,
    
    /**
     * 目标属性
     */
    val attribute: StatAttribute,
    
    /**
     * 要添加的修改器
     */
    val modifier: AttributeModifier
) : Event(), ICancellableEvent

/**
 * 属性修改器移除事件
 * 在修改器从属性实例中移除时触发
 */
class AttributeModifierRemoveEvent(
    /**
     * 拥有属性的实体
     */
    val entity: LivingEntity,
    
    /**
     * 目标属性
     */
    val attribute: StatAttribute,
    
    /**
     * 被移除的修改器
     */
    val modifier: AttributeModifier
) : Event(), ICancellableEvent

/**
 * 实体属性初始化事件
 * 在实体的属性系统被初始化时触发
 */
class EntityAttributeInitEvent(
    /**
     * 被初始化的实体
     */
    val entity: LivingEntity,
    
    /**
     * 适用于该实体的属性列表（可以被修改）
     */
    val applicableAttributes: MutableList<StatAttribute>
) : Event()

/**
 * 属性注册事件
 * 在新属性被注册时触发
 */
class AttributeRegistrationEvent(
    /**
     * 被注册的属性
     */
    val attribute: StatAttribute
) : Event() 