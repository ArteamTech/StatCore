package com.arteam.statcore.core.events

import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.core.attributes.AttributeManager
import com.arteam.statcore.core.sync.AttributeSyncManager
import com.arteam.statcore.core.sync.ImmediateSyncManager
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import org.slf4j.LoggerFactory

/**
 * 装备变化处理器
 * 监听装备变化并实时更新防御值
 */
@EventBusSubscriber(modid = "statcore")
object EquipmentChangeHandler {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.equipment")
    
    /**
     * 监听装备变化事件
     * 当实体装备发生变化时，立即重新计算防御值
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onLivingEquipmentChange(event: LivingEquipmentChangeEvent) {
        val entity = event.entity
        if (entity.level().isClientSide) return
        
        // 立即执行，提高响应性
        ImmediateSyncManager.forceSyncDefense(entity)
        
        // 也安排一个延迟执行作为保险
        ImmediateSyncManager.asyncForceSyncAll(entity)
    }
    
    /**
     * 玩家Tick事件 - 定期检查装备变化
     * 作为装备变化监听的补充，确保不会遗漏
     */
    @SubscribeEvent
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity
        if (player.level().isClientSide) return
        
        // 每20 ticks（1秒）检查一次，提高检测频率
        if (player.tickCount % 20 == 0) {
            updateEntityDefense(player)
        }
    }
    
    /**
     * 更新实体的防御属性
     * 根据当前装备重新计算所有防御值
     */
    private fun updateEntityDefense(entity: LivingEntity) {
        try {
            // 获取当前护甲值
            val currentArmor = entity.armorValue.toDouble()
            
            // 计算物理防御值（护甲点数 × 5）
            val physicalDefense = currentArmor * 5.0
            
            // 获取当前物理防御值
            val currentPhysicalDefense = AttributeManager.getAttributeValue(entity, CoreAttributes.PHYSICAL_DEFENSE)
            
            // 只在防御值发生变化时更新（降低阈值提高精度）
            if (Math.abs(currentPhysicalDefense - physicalDefense) > 0.01) {
                // 更新物理防御值
                AttributeManager.setAttributeBaseValue(entity, CoreAttributes.PHYSICAL_DEFENSE, physicalDefense)
                
                // 同步属性到原版系统
                AttributeSyncManager.syncEntityAttributes(entity)
                
                LOGGER.debug("实体 {} 防御值已更新: 护甲={} -> 物理防御={} (原值={})", 
                    entity.uuid, currentArmor, physicalDefense, currentPhysicalDefense)
            }
        } catch (e: Exception) {
            LOGGER.error("更新实体防御属性时发生错误: {}", e.message, e)
        }
    }
} 