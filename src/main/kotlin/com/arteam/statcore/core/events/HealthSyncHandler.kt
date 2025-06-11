package com.arteam.statcore.core.events

import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.core.attributes.AttributeManager
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import org.slf4j.LoggerFactory

/**
 * 血量同步处理器
 * 确保原版血量系统与我们的血量系统保持同步
 */
@EventBusSubscriber(modid = "statcore")
object HealthSyncHandler {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.health_sync")
    
    /**
     * 玩家Tick事件 - 同步血量
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity
        if (player.level().isClientSide) return
        
        // 每2 ticks（0.1秒）同步一次血量，提高响应性
        if (player.tickCount % 2 == 0) {
            syncPlayerHealth(player)
        }
    }
    
    /**
     * 同步玩家血量
     * @param entity 实体
     */
    private fun syncPlayerHealth(entity: LivingEntity) {
        try {
            // 获取原版当前血量
            val vanillaHealth = entity.health.toDouble()
            
            // 获取我们系统的当前血量
            val ourCurrentHealth = AttributeManager.getAttributeValue(entity, CoreAttributes.CURRENT_HEALTH)
            val ourMaxHealth = AttributeManager.getAttributeValue(entity, CoreAttributes.MAX_HEALTH)
            
            // 如果两者差异过大，需要同步（降低阈值提高精度）
            if (Math.abs(vanillaHealth - ourCurrentHealth) > 0.5) {
                // 以原版血量为准，更新我们的系统
                val syncedHealth = Math.min(vanillaHealth, ourMaxHealth)
                AttributeManager.setAttributeValue(entity, CoreAttributes.CURRENT_HEALTH, syncedHealth)
                
                LOGGER.debug("同步实体 {} 血量: 原版={}, 系统={} -> {}", 
                    entity.uuid, vanillaHealth, ourCurrentHealth, syncedHealth)
            }
        } catch (e: Exception) {
            LOGGER.error("同步血量时发生错误: {}", e.message, e)
        }
    }
} 