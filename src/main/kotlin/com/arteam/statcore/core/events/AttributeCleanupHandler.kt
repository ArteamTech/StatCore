package com.arteam.statcore.core.events

import com.arteam.statcore.core.attributes.AttributeManager
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import org.slf4j.LoggerFactory

/**
 * 属性清理处理器
 * 负责在实体离开世界或服务器关闭时释放缓存，避免内存泄漏
 */
@EventBusSubscriber(modid = "statcore")
object AttributeCleanupHandler {

    private val LOGGER = LoggerFactory.getLogger("statcore.cleanup")

    /**
     * 实体离开世界时，移除其属性映射
     */
    @SubscribeEvent
    fun onEntityLeave(event: EntityLeaveLevelEvent) {
        val entity = event.entity
        if (entity is LivingEntity && !entity.level().isClientSide) {
            val removed = AttributeManager.removeAttributeMap(entity)
            if (removed) {
                LOGGER.debug("移除实体 {} 的属性映射", entity.uuid)
            }
        }
    }

    /**
     * 服务器停止时，清理所有缓存映射，防止单机多次进入世界导致内存累积
     */
    @SubscribeEvent
    fun onServerStopped(@Suppress("UNUSED_PARAMETER") event: ServerStoppedEvent) {
        AttributeManager.clearAllEntityMaps()
        LOGGER.info("服务器已停止，已清理全部实体属性映射")
    }
} 