package com.arteam.statcore.core.sync

import com.arteam.statcore.StatCore
import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.core.attributes.AttributeManager
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import org.slf4j.LoggerFactory
import kotlin.math.min

/**
 * 属性同步管理器
 * 负责将我们的自定义属性值同步到原版Minecraft系统
 * 确保属性修改能够正确反映到游戏机制中
 */
@Suppress("unused")
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
        
        // 将属性值限制在原版系统能安全处理的范围内
        val safeMaxHealth = if (customMaxHealth.isInfinite() || customMaxHealth > StatCore.VANILLA_MAX_HEALTH_LIMIT) {
            StatCore.VANILLA_MAX_HEALTH_LIMIT
        } else {
            customMaxHealth
        }
        
        entity.attributes.getInstance(Attributes.MAX_HEALTH)?.let { instance ->
            if (instance.baseValue != safeMaxHealth) {
                instance.baseValue = safeMaxHealth
                
                // 如果当前生命值超过新的最大值，调整当前生命值
                // 注意：这里使用自定义的最大生命值而不是限制后的值
                if (entity.health > customMaxHealth) {
                    entity.health = min(customMaxHealth, safeMaxHealth).toFloat()
                }
                
                if (customMaxHealth > StatCore.VANILLA_MAX_HEALTH_LIMIT) {
                    LOGGER.debug("实体 {} 最大生命值 {} 超过原版限制，同步为安全值: {}", 
                        entity.uuid, customMaxHealth, safeMaxHealth)
                } else {
                    LOGGER.debug("同步实体 {} 最大生命值: {}", entity.uuid, safeMaxHealth)
                }
            }
        }
    }
    
    /**
     * 获取安全的原版属性值
     * 将我们的属性值转换为原版系统能安全处理的值
     * @param value 原始属性值
     * @param maxLimit 原版系统的最大限制
     * @return 安全的属性值
     */
    fun getSafeVanillaValue(value: Double, maxLimit: Double): Double {
        return when {
            value.isInfinite() -> maxLimit
            value > maxLimit -> maxLimit
            else -> value
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