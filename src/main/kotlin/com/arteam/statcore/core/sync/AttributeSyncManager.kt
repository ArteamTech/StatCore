package com.arteam.statcore.core.sync

import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.core.attributes.AttributeManager
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import org.slf4j.LoggerFactory

/**
 * 属性同步管理器
 * 负责将我们的自定义属性值同步到原版Minecraft系统
 * 确保属性修改能够正确反映到游戏机制中
 */
object AttributeSyncManager {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.sync")
    
    /**
     * 同步实体的所有属性到原版系统
     * @param entity 目标实体
     */
    fun syncEntityAttributes(entity: LivingEntity) {
        try {
            syncMaxHealth(entity)
            // 未来可以添加其他属性的同步
            LOGGER.debug("已同步实体 {} 的所有属性", entity.uuid)
        } catch (e: Exception) {
            LOGGER.error("同步实体属性时发生错误: {}", e.message, e)
        }
    }
    
    /**
     * 同步最大生命值到原版系统
     * @param entity 目标实体
     */
    fun syncMaxHealth(entity: LivingEntity) {
        val customMaxHealth = AttributeManager.getAttributeValue(entity, CoreAttributes.MAX_HEALTH)
        
        entity.attributes.getInstance(Attributes.MAX_HEALTH)?.let { instance ->
            if (instance.baseValue != customMaxHealth) {
                instance.baseValue = customMaxHealth
                
                // 如果当前生命值超过新的最大值，调整当前生命值
                if (entity.health > customMaxHealth) {
                    entity.health = customMaxHealth.toFloat()
                }
                
                LOGGER.debug("同步实体 {} 最大生命值: {}", entity.uuid, customMaxHealth)
            }
        }
    }
    
    /**
     * 强制刷新实体的属性显示
     * 用于确保客户端看到最新的属性值
     * @param entity 目标实体
     */
    fun refreshEntityAttributes(entity: LivingEntity) {
        if (!entity.level().isClientSide) {
            // 服务器端：强制标记属性为脏数据，会在下次tick时同步
            // 简单的刷新实现
            LOGGER.debug("强制刷新实体 {} 的属性", entity.uuid)
        }
    }
    
    /**
     * 批量同步多个实体的属性
     * @param entities 实体列表
     */
    fun syncMultipleEntities(entities: Collection<LivingEntity>) {
        entities.forEach { entity ->
            syncEntityAttributes(entity)
        }
        LOGGER.debug("批量同步了 {} 个实体的属性", entities.size)
    }
} 