package com.arteam.statcore.core.events

import com.arteam.statcore.core.attributes.AttributeManager
import com.arteam.statcore.core.sync.AttributeSyncManager
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import org.slf4j.LoggerFactory

/**
 * 属性更新处理器
 * 处理装备变化、周期性属性更新等
 */
@EventBusSubscriber(modid = "statcore")
object AttributeUpdateHandler {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.update")
    private var tickCounter = 0
    private const val UPDATE_INTERVAL = 100 // 每5秒更新一次（20 ticks/秒 × 5秒）
    
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
     * 服务器tick事件，周期性更新属性
     */
    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Pre) {
        tickCounter++
        
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0
            performPeriodicUpdate(event)
        }
    }
    
    /**
     * 执行周期性属性更新
     */
    private fun performPeriodicUpdate(event: ServerTickEvent.Pre) {
        val server = event.server
        
        // 获取所有在线玩家
        val players = server.playerList.players
        if (players.isEmpty()) return
        
        try {
            // 批量更新玩家属性
            val entitiesToUpdate = mutableListOf<LivingEntity>()
            
            for (player in players) {
                // 检查玩家装备是否发生变化
                if (hasEquipmentChanged(player)) {
                    entitiesToUpdate.add(player)
                    updatePlayerEquipmentAttributes(player)
                }
            }
            
            // 批量同步属性
            if (entitiesToUpdate.isNotEmpty()) {
                AttributeSyncManager.syncMultipleEntities(entitiesToUpdate)
                LOGGER.debug("周期性更新了 {} 个实体的属性", entitiesToUpdate.size)
            }
            
        } catch (e: Exception) {
            LOGGER.error("周期性属性更新时发生错误: {}", e.message, e)
        }
    }
    
    /**
     * 检查玩家装备是否发生变化
     * 这里简化处理，实际可以缓存装备hash值进行比较
     */
    private fun hasEquipmentChanged(player: Player): Boolean {
        // 简化版本：检查是否有护甲装备
        return player.armorValue > 0
    }
    
    /**
     * 更新玩家装备相关的属性
     */
    private fun updatePlayerEquipmentAttributes(player: Player) {
        // 重新计算护甲相关的物理防御
        val currentArmor = player.armorValue.toDouble()
        val physicalDefense = currentArmor * 5.0
        
        // 更新物理防御属性
        AttributeManager.setAttributeBaseValue(
            player, 
            com.arteam.statcore.attributes.CoreAttributes.PHYSICAL_DEFENSE, 
            physicalDefense
        )
        
        LOGGER.debug("更新玩家 {} 的装备属性，护甲={}, 物理防御={}", 
            player.name.string, currentArmor, physicalDefense)
    }
} 