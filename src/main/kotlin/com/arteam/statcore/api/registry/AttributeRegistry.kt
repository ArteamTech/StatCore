package com.arteam.statcore.api.registry

import com.arteam.statcore.api.attributes.StatAttribute
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

/**
 * 属性注册接口
 * 管理所有属性的注册和查询
 */
interface AttributeRegistry {
    /**
     * 注册一个新属性
     * @param attribute 要注册的属性
     * @return 注册后的属性实例
     * @throws IllegalArgumentException 如果属性ID已存在
     */
    fun register(attribute: StatAttribute): StatAttribute
    
    /**
     * 获取所有已注册的属性
     * @return 所有属性的集合
     */
    fun getAttributes(): Collection<StatAttribute>
    
    /**
     * 根据ID获取属性
     * @param id 属性的资源位置ID
     * @return 对应的属性，如果不存在则返回null
     */
    fun getAttribute(id: ResourceLocation): StatAttribute?
    
    /**
     * 根据ID获取属性
     * @param namespace 命名空间
     * @param path 路径
     * @return 对应的属性，如果不存在则返回null
     */
    fun getAttribute(namespace: String, path: String): StatAttribute? {
        return getAttribute(ResourceLocation.fromNamespaceAndPath(namespace, path))
    }
    
    /**
     * 检查属性是否已注册
     * @param id 属性的资源位置ID
     * @return 如果属性已注册返回true，否则返回false
     */
    fun hasAttribute(id: ResourceLocation): Boolean {
        return getAttribute(id) != null
    }
    
    /**
     * 获取对特定实体类型有效的所有属性
     * @param entity 实体实例
     * @return 对该实体有效的属性列表
     */
    fun getApplicableAttributes(entity: LivingEntity): List<StatAttribute>
    
    /**
     * 获取对玩家有效的所有属性
     * @return 对玩家有效的属性列表
     */
    fun getPlayerAttributes(): List<StatAttribute>
    
    /**
     * 获取对非玩家实体有效的所有属性
     * @return 对非玩家实体有效的属性列表
     */
    fun getEntityAttributes(): List<StatAttribute>
    
    /**
     * 注销属性（主要用于测试和动态管理）
     * @param id 要注销的属性ID
     * @return 如果成功注销返回true，如果属性不存在返回false
     */
    fun unregister(id: ResourceLocation): Boolean
    
    /**
     * 清空所有注册的属性（主要用于测试）
     */
    fun clear()
} 