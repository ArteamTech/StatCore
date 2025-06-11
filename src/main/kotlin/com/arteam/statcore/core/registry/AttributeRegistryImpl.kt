package com.arteam.statcore.core.registry

import com.arteam.statcore.api.attributes.StatAttribute
import com.arteam.statcore.api.registry.AttributeRegistry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import java.util.concurrent.ConcurrentHashMap

/**
 * 属性注册的具体实现
 * 使用线程安全的ConcurrentHashMap来存储属性
 * 单例对象，确保全局唯一
 */
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
        // 创建一个虚拟玩家实例用于检查（这是一个简化方案）
        // 在实际实现中，可能需要更复杂的逻辑
        return attributes.values.filter { attribute ->
            // 检查属性是否适用于玩家类型
            // 这里我们假设如果属性允许玩家，那么它就是玩家属性
            try {
                // 我们无法创建虚拟Player实例，所以使用类型检查
                val allowedTypes = if (attribute is com.arteam.statcore.core.attributes.BaseStatAttribute) {
                    attribute.allowedEntityTypes
                } else {
                    // 对于其他实现，我们假设它们都支持玩家
                    setOf(com.arteam.statcore.core.attributes.EntityCategory.PLAYER)
                }
                allowedTypes.contains(com.arteam.statcore.core.attributes.EntityCategory.PLAYER)
            } catch (e: Exception) {
                false
            }
        }
    }
    
    override fun getEntityAttributes(): List<StatAttribute> {
        return attributes.values.filter { attribute ->
            // 检查属性是否适用于非玩家实体
            try {
                val allowedTypes = if (attribute is com.arteam.statcore.core.attributes.BaseStatAttribute) {
                    attribute.allowedEntityTypes
                } else {
                    // 对于其他实现，我们假设它们都支持实体
                    setOf(
                        com.arteam.statcore.core.attributes.EntityCategory.MONSTER,
                        com.arteam.statcore.core.attributes.EntityCategory.ANIMAL,
                        com.arteam.statcore.core.attributes.EntityCategory.NPC,
                        com.arteam.statcore.core.attributes.EntityCategory.OTHER
                    )
                }
                allowedTypes.any { 
                    it != com.arteam.statcore.core.attributes.EntityCategory.PLAYER 
                }
            } catch (e: Exception) {
                false
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