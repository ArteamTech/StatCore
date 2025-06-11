package com.arteam.statcore.core.events

import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.core.attributes.AttributeManager
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.min

/**
 * 血量同步处理器
 * 确保原版血量系统与我们的血量系统保持同步
 */
@EventBusSubscriber(modid = "statcore")
@Suppress("unused")
object HealthSyncHandler {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.health_sync")
    private var entitySyncTicker = 0
    private const val ENTITY_SYNC_INTERVAL = 10 // 每0.5秒同步一次非玩家实体（20 ticks/秒 × 0.5秒）
    
    /**
     * 玩家Tick事件 - 同步血量
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity
        if (player.level().isClientSide) return
        
        // 每2 ticks（0.1秒）同步一次血量，提高响应性
        if (player.tickCount % 2 == 0) {
            syncEntityHealth(player)
        }
    }
    
    /**
     * 服务器Tick事件 - 同步所有实体血量
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    fun onServerTick(event: ServerTickEvent.Pre) {
        entitySyncTicker++
        
        if (entitySyncTicker >= ENTITY_SYNC_INTERVAL) {
            entitySyncTicker = 0
            syncAllEntitiesHealth(event)
        }
    }
    
    /**
     * 同步所有实体的血量
     */
    private fun syncAllEntitiesHealth(event: ServerTickEvent.Pre) {
        val server = event.server
        
        try {
            // 获取所有已加载的实体
            for (level in server.allLevels) {
                for (entity in level.allEntities) {
                    if (entity is LivingEntity && entity !is Player) {
                        // 对非玩家实体进行血量同步（玩家已在PlayerTick中处理）
                        syncEntityHealth(entity)
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("批量同步实体血量时发生错误: {}", e.message, e)
        }
    }
    
    /**
     * 同步实体血量
     * @param entity 实体
     */
    private fun syncEntityHealth(entity: LivingEntity) {
        try {
            // 获取原版当前血量
            val vanillaHealth = entity.health.toDouble()
            
            // 获取我们系统的当前血量
            val ourCurrentHealth = AttributeManager.getAttributeValue(entity, CoreAttributes.CURRENT_HEALTH)
            val ourMaxHealth = AttributeManager.getAttributeValue(entity, CoreAttributes.MAX_HEALTH)
            
            // 如果两者差异过大，需要同步（降低阈值提高精度）
            if (abs(vanillaHealth - ourCurrentHealth) > 0.5) {
                // 以原版血量为准，更新我们的系统
                val syncedHealth = min(vanillaHealth, ourMaxHealth)
                AttributeManager.setAttributeValue(entity, CoreAttributes.CURRENT_HEALTH, syncedHealth)
            }
        } catch (e: Exception) {
            LOGGER.error("同步血量时发生错误: {}", e.message, e)
        }
    }
} 