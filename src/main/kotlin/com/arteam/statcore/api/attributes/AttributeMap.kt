package com.arteam.statcore.api.attributes

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import java.util.concurrent.ConcurrentHashMap

/**
 * 属性映射
 * 管理单个实体的所有属性实例
 */
class AttributeMap(
    /**
     * 拥有此属性映射的实体
     */
    val owner: LivingEntity
) {
    /**
     * 属性实例映射表 (ResourceLocation -> AttributeInstance)
     */
    private val instances = ConcurrentHashMap<ResourceLocation, AttributeInstance>()
    
    /**
     * 获取或创建属性实例
     * @param attribute 属性定义
     * @return 对应的属性实例
     */
    fun getOrCreateInstance(attribute: StatAttribute): AttributeInstance {
        return instances.computeIfAbsent(attribute.id) { 
            AttributeInstance(attribute, owner)
        }
    }
    
    /**
     * 获取属性实例
     * @param attributeId 属性ID
     * @return 对应的属性实例，如果不存在则返回null
     */
    fun getInstance(attributeId: ResourceLocation): AttributeInstance? {
        return instances[attributeId]
    }
    
    /**
     * 获取属性实例
     * @param attribute 属性定义
     * @return 对应的属性实例，如果不存在则返回null
     */
    fun getInstance(attribute: StatAttribute): AttributeInstance? {
        return getInstance(attribute.id)
    }
    
    /**
     * 检查是否存在某个属性的实例
     * @param attributeId 属性ID
     * @return 如果存在返回true
     */
    fun hasInstance(attributeId: ResourceLocation): Boolean {
        return instances.containsKey(attributeId)
    }
    
    /**
     * 检查是否存在某个属性的实例
     * @param attribute 属性定义
     * @return 如果存在返回true
     */
    fun hasInstance(attribute: StatAttribute): Boolean {
        return hasInstance(attribute.id)
    }
    
    /**
     * 获取所有属性实例
     * @return 所有属性实例的集合
     */
    fun getAllInstances(): Collection<AttributeInstance> {
        return instances.values.toList()
    }
    
    /**
     * 获取所有属性ID
     * @return 所有属性ID的集合
     */
    fun getAllAttributeIds(): Set<ResourceLocation> {
        return instances.keys.toSet()
    }
    
    /**
     * 移除属性实例
     * @param attributeId 要移除的属性ID
     * @return 如果成功移除返回true
     */
    fun removeInstance(attributeId: ResourceLocation): Boolean {
        return instances.remove(attributeId) != null
    }
    
    /**
     * 移除属性实例
     * @param attribute 要移除的属性定义
     * @return 如果成功移除返回true
     */
    fun removeInstance(attribute: StatAttribute): Boolean {
        return removeInstance(attribute.id)
    }
    
    /**
     * 清空所有属性实例
     */
    fun clear() {
        instances.clear()
    }
    
    /**
     * 获取属性的最终值
     * @param attribute 属性定义
     * @return 属性的最终值，如果属性不存在则返回默认值
     */
    fun getValue(attribute: StatAttribute): Double {
        return getInstance(attribute)?.getValue() ?: attribute.defaultValue
    }
    
    /**
     * 获取属性的基础值
     * @param attribute 属性定义
     * @return 属性的基础值，如果属性不存在则返回默认值
     */
    fun getBaseValue(attribute: StatAttribute): Double {
        return getInstance(attribute)?.baseValue ?: attribute.defaultValue
    }
    
    /**
     * 设置属性的基础值
     * @param attribute 属性定义
     * @param baseValue 要设置的基础值
     */
    fun setBaseValue(attribute: StatAttribute, baseValue: Double) {
        getOrCreateInstance(attribute).baseValue = baseValue
    }
    
    /**
     * 添加属性修改器
     * @param attribute 属性定义
     * @param modifier 要添加的修改器
     */
    fun addModifier(attribute: StatAttribute, modifier: AttributeModifier) {
        getOrCreateInstance(attribute).addModifier(modifier)
    }
    
    /**
     * 移除属性修改器
     * @param attribute 属性定义
     * @param modifierId 要移除的修改器ID
     * @return 如果成功移除返回true
     */
    fun removeModifier(attribute: StatAttribute, modifierId: java.util.UUID): Boolean {
        return getInstance(attribute)?.removeModifier(modifierId) ?: false
    }
    
    /**
     * 根据来源移除修改器
     * @param source 修改器来源
     * @return 移除的修改器总数
     */
    fun removeModifiersBySource(source: ResourceLocation): Int {
        var totalRemoved = 0
        for (instance in instances.values) {
            totalRemoved += instance.removeModifiersBySource(source)
        }
        return totalRemoved
    }
    
    /**
     * 重置所有属性到默认状态
     */
    fun resetAll() {
        for (instance in instances.values) {
            instance.reset()
        }
    }
    
    /**
     * 获取属性实例数量
     */
    fun size(): Int {
        return instances.size
    }
    
    /**
     * 检查属性映射是否为空
     */
    fun isEmpty(): Boolean {
        return instances.isEmpty()
    }
    
    override fun toString(): String {
        return "AttributeMap(owner=${owner.displayName}, instances=${instances.size})"
    }
} 