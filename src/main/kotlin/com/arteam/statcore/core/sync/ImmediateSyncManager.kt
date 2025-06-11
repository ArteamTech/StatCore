package com.arteam.statcore.core.sync

import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.core.attributes.AttributeManager
import net.minecraft.world.entity.LivingEntity
import org.slf4j.LoggerFactory

/**
 * 即时同步管理器
 * 提供强制立即同步功能，用于关键操作后的属性同步
 */
object ImmediateSyncManager {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.immediate_sync")
    
    /**
     * 立即强制同步实体的所有属性
     * @param entity 目标实体
     */
    fun forceSyncAll(entity: LivingEntity) {
        if (entity.level().isClientSide) return
        
        try {
            forceSyncHealth(entity)
            forceSyncDefense(entity)
            
            LOGGER.debug("强制同步实体 {} 的所有属性", entity.uuid)
        } catch (e: Exception) {
            LOGGER.error("强制同步属性时发生错误: {}", e.message, e)
        }
    }
    
    /**
     * 立即强制同步血量
     * @param entity 目标实体
     */
    fun forceSyncHealth(entity: LivingEntity) {
        if (entity.level().isClientSide) return
        
        try {
            // 同步当前血量
            val vanillaHealth = entity.health.toDouble()
            val maxHealth = AttributeManager.getAttributeValue(entity, CoreAttributes.MAX_HEALTH)
            val syncedHealth = Math.min(vanillaHealth, maxHealth)
            
            AttributeManager.setAttributeValue(entity, CoreAttributes.CURRENT_HEALTH, syncedHealth)
            
            // 同步最大血量到原版系统
            AttributeSyncManager.syncEntityAttributes(entity)
            
            LOGGER.debug("强制同步实体 {} 血量: {} -> {}", entity.uuid, vanillaHealth, syncedHealth)
        } catch (e: Exception) {
            LOGGER.error("强制同步血量时发生错误: {}", e.message, e)
        }
    }
    
    /**
     * 立即强制同步防御属性
     * @param entity 目标实体
     */
    fun forceSyncDefense(entity: LivingEntity) {
        if (entity.level().isClientSide) return
        
        try {
            // 重新计算并同步防御值
            val currentArmor = entity.armorValue.toDouble()
            val physicalDefense = currentArmor * 5.0
            
            AttributeManager.setAttributeBaseValue(entity, CoreAttributes.PHYSICAL_DEFENSE, physicalDefense)
            AttributeSyncManager.syncEntityAttributes(entity)
            
            LOGGER.debug("强制同步实体 {} 防御: 护甲={} -> 物理防御={}", 
                entity.uuid, currentArmor, physicalDefense)
        } catch (e: Exception) {
            LOGGER.error("强制同步防御时发生错误: {}", e.message, e)
        }
    }
    
    /**
     * 异步强制同步（用于事件处理中）
     * @param entity 目标实体
     */
    fun asyncForceSyncAll(entity: LivingEntity) {
        if (entity.level().isClientSide) return
        
        entity.level().server?.execute {
            forceSyncAll(entity)
        }
    }
} 