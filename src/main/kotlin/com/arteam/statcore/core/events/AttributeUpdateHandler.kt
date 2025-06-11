package com.arteam.statcore.core.events

import com.arteam.statcore.core.sync.AttributeSyncManager
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import org.slf4j.LoggerFactory

/**
 * 属性更新处理器
 * 处理玩家登录初始化、维度切换同步等通用属性更新任务
 * 装备变化相关的更新由 EquipmentChangeHandler 处理
 */
@EventBusSubscriber(modid = "statcore")
@Suppress("unused")
object AttributeUpdateHandler {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.update")
    private var tickCounter = 0
    private const val REFRESH_INTERVAL = 600 // 每30秒进行一次属性刷新（20 ticks/秒 × 30秒）
    
    /**
     * 玩家登录时初始化属性
     */
    @SubscribeEvent
    fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity
        LOGGER.info("玩家 {} 登录，初始化属性", player.name.string)
        
        // 强制刷新属性
        AttributeSyncManager.syncEntityAttributes(player)
        AttributeSyncManager.refreshEntityAttributes(player)
    }
    
    /**
     * 玩家换维度时重新同步属性
     */
    @SubscribeEvent
    fun onPlayerChangeDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        val player = event.entity
        LOGGER.debug("玩家 {} 换维度，重新同步属性", player.name.string)
        
        // 延迟1 tick后同步，确保维度切换完成
        player.level().server?.execute {
            AttributeSyncManager.syncEntityAttributes(player)
        }
    }
    
    /**
     * 服务器tick事件，周期性刷新属性
     * 主要用于处理那些无法通过事件监听、且不需要高频更新的属性
     */
    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Pre) {
        tickCounter++
        
        if (tickCounter >= REFRESH_INTERVAL) {
            tickCounter = 0
            performPeriodicRefresh(event)
        }
    }
    
    /**
     * 执行周期性属性刷新
     * 对所有在线玩家进行一次全面的属性同步，确保不遗漏任何变化
     */
    private fun performPeriodicRefresh(event: ServerTickEvent.Pre) {
        val server = event.server
        
        // 获取所有在线玩家
        val players = server.playerList.players
        if (players.isEmpty()) return
        
        try {
            val refreshedPlayers = mutableListOf<LivingEntity>()
            
            for (player in players) {
                // 对每个玩家进行全面的属性刷新
                AttributeSyncManager.syncEntityAttributes(player)
                refreshedPlayers.add(player)
            }
            
            LOGGER.debug("周期性刷新了 {} 个玩家的属性", refreshedPlayers.size)
            
        } catch (e: Exception) {
            LOGGER.error("周期性属性刷新时发生错误: {}", e.message, e)
        }
    }
} 