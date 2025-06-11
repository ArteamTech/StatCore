package com.arteam.statcore.core.registry

import com.arteam.statcore.api.attributes.StatAttribute
import com.arteam.statcore.api.attributes.EntityCategory
import com.arteam.statcore.api.registry.AttributeRegistry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import java.util.concurrent.ConcurrentHashMap

/**
 * 属性注册的具体实现
 * 使用线程安全的ConcurrentHashMap来存储属性
 * 单例对象，确保全局唯一
 */
@Suppress("unused")
object AttributeRegistryImpl : AttributeRegistry {
    
    /**
     * 存储所有注册属性的映射表
     */
    private val attributes = ConcurrentHashMap<ResourceLocation, StatAttribute>()
    
    override fun register(attribute: StatAttribute): StatAttribute {
        val existing = attributes.putIfAbsent(attribute.id, attribute)
        if (existing != null) {
            throw IllegalArgumentException("属性 ${attribute.id} 已经注册过了")
        }
        return attribute
    }
    
    override fun getAttributes(): Collection<StatAttribute> {
        return attributes.values.toList()
    }
    
    override fun getAttribute(id: ResourceLocation): StatAttribute? {
        return attributes[id]
    }
    
    override fun getApplicableAttributes(entity: LivingEntity): List<StatAttribute> {
        return attributes.values.filter { attribute ->
            attribute.isApplicableTo(entity)
        }
    }
    
    override fun getPlayerAttributes(): List<StatAttribute> {
        return attributes.values.filter { attribute ->
            attribute.getApplicableCategories().contains(EntityCategory.PLAYER)
        }
    }
    
    override fun getEntityAttributes(): List<StatAttribute> {
        return attributes.values.filter { attribute ->
            val categories = attribute.getApplicableCategories()
            categories.any { category ->
                category != EntityCategory.PLAYER
            }
        }
    }
    
    override fun unregister(id: ResourceLocation): Boolean {
        return attributes.remove(id) != null
    }
    
    override fun clear() {
        attributes.clear()
    }
    
    /**
     * 获取注册的属性数量
     */
    fun size(): Int {
        return attributes.size
    }
    
    /**
     * 检查注册表是否为空
     */
    fun isEmpty(): Boolean {
        return attributes.isEmpty()
    }
    
    /**
     * 获取所有注册的属性ID
     */
    fun getAttributeIds(): Set<ResourceLocation> {
        return attributes.keys.toSet()
    }
} 