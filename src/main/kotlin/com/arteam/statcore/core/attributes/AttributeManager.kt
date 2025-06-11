package com.arteam.statcore.core.attributes

import com.arteam.statcore.api.attributes.AttributeMap
import com.arteam.statcore.api.attributes.StatAttribute
import com.arteam.statcore.api.registry.AttributeRegistry
import com.arteam.statcore.core.registry.AttributeRegistryImpl
import net.minecraft.world.entity.LivingEntity
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 属性管理器
 * 管理全局的属性系统，包括属性注册和实体属性映射
 */
@Suppress("unused")
object AttributeManager {
    
    /**
     * 属性注册器实例
     */
    val registry: AttributeRegistry = AttributeRegistryImpl
    
    /**
     * 实体属性映射表 (EntityId -> AttributeMap)
     */
    private val entityAttributeMaps = ConcurrentHashMap<UUID, AttributeMap>()
    
    /**
     * 获取实体的属性映射
     * @param entity 目标实体
     * @return 实体的属性映射
     */
    fun getAttributeMap(entity: LivingEntity): AttributeMap {
        return entityAttributeMaps.computeIfAbsent(entity.uuid) { 
            AttributeMap(entity)
        }
    }
    
    /**
     * 检查实体是否有属性映射
     * @param entity 目标实体
     * @return 如果存在属性映射返回true
     */
    fun hasAttributeMap(entity: LivingEntity): Boolean {
        return entityAttributeMaps.containsKey(entity.uuid)
    }
    
    /**
     * 移除实体的属性映射
     * @param entity 目标实体
     * @return 如果成功移除返回true
     */
    fun removeAttributeMap(entity: LivingEntity): Boolean {
        return entityAttributeMaps.remove(entity.uuid) != null
    }
    
    /**
     * 初始化实体的属性
     * 为实体创建所有适用的属性实例
     * @param entity 目标实体
     */
    fun initializeEntityAttributes(entity: LivingEntity) {
        val attributeMap = getAttributeMap(entity)
        val applicableAttributes = registry.getApplicableAttributes(entity)
        
        for (attribute in applicableAttributes) {
            // 创建属性实例（如果还不存在）
            attributeMap.getOrCreateInstance(attribute)
        }
    }
    
    /**
     * 获取实体某个属性的值
     * @param entity 目标实体
     * @param attribute 属性定义
     * @return 属性值，如果属性不适用于该实体则返回默认值
     */
    fun getAttributeValue(entity: LivingEntity, attribute: StatAttribute): Double {
        // 检查属性是否适用于该实体
        if (!attribute.isApplicableTo(entity)) {
            return attribute.defaultValue
        }
        
        return getAttributeMap(entity).getValue(attribute)
    }
    
    /**
     * 设置实体某个属性的基础值
     * @param entity 目标实体
     * @param attribute 属性定义
     * @param baseValue 基础值
     */
    fun setAttributeBaseValue(entity: LivingEntity, attribute: StatAttribute, baseValue: Double) {
        if (!attribute.isApplicableTo(entity)) {
            return
        }
        
        getAttributeMap(entity).setBaseValue(attribute, baseValue)
    }
    
    /**
     * 设置实体某个属性的当前值（包括修改器）
     * 通过反向计算调整基础值来达到目标值
     * @param entity 目标实体
     * @param attribute 属性定义
     * @param value 要设置的值
     */
    fun setAttributeValue(entity: LivingEntity, attribute: StatAttribute, value: Double) {
        if (!attribute.isApplicableTo(entity)) {
            return
        }
        
        // 获取属性实例并设置目标值
        val attributeMap = getAttributeMap(entity)
        val instance = attributeMap.getOrCreateInstance(attribute)
        
        // 使用智能的setValue方法，会考虑修改器
        instance.setValue(value)
    }
    
    /**
     * 强制设置实体某个属性的值（通过基础值）
     * 忽略所有修改器，直接通过调整基础值来强制设定属性的最终值
     * @param entity 目标实体
     * @param attribute 属性定义
     * @param value 要强制设置的值
     */
    fun forceSetAttributeValueByBase(entity: LivingEntity, attribute: StatAttribute, value: Double) {
        if (!attribute.isApplicableTo(entity)) {
            return
        }
        
        // 获取属性实例并强制设置基础值
        val attributeMap = getAttributeMap(entity)
        val instance = attributeMap.getOrCreateInstance(attribute)
        
        // 使用强制设置方法，忽略修改器
        instance.forceSetValueByBase(value)
    }
    
    /**
     * 清理所有实体属性映射
     * 主要用于世界重载或服务器关闭时
     */
    fun clearAllEntityMaps() {
        entityAttributeMaps.clear()
    }
    
    /**
     * 获取当前管理的实体数量
     */
    fun getManagedEntityCount(): Int {
        return entityAttributeMaps.size
    }
    
    /**
     * 获取所有管理的实体UUID
     */
    fun getManagedEntityIds(): Set<UUID> {
        return entityAttributeMaps.keys.toSet()
    }
    
    /**
     * 注册一个新属性
     * @param attribute 要注册的属性
     * @return 注册后的属性实例
     */
    fun registerAttribute(attribute: StatAttribute): StatAttribute {
        return registry.register(attribute)
    }
    
    /**
     * 获取所有已注册的属性
     */
    fun getAllAttributes(): Collection<StatAttribute> {
        return registry.getAttributes()
    }
    
    /**
     * 根据ID获取属性
     */
    fun getAttribute(namespace: String, path: String): StatAttribute? {
        return registry.getAttribute(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, path))
    }
} 