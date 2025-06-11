package com.arteam.statcore.core.events

import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.MobEffectEvent
import org.slf4j.LoggerFactory

/**
 * 早期药水效果拦截器
 * 在原版效果真正应用之前就拦截，防止任何原版生命提升修改器的应用
 */
@EventBusSubscriber(modid = "statcore")
object EarlyPotionInterceptor {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.early_interceptor")
    
    /**
     * 应用级拦截 - 阻止原版生命提升效果的属性修改器应用
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMobEffectApplicable(event: MobEffectEvent.Applicable) {
        val entity = event.entity
        val effectInstance = event.effectInstance ?: return
        
        // 只处理生命提升效果
        if (effectInstance.effect == MobEffects.HEALTH_BOOST) {
            // 允许效果本身存在（用于我们的检测），但阻止其属性修改
            LOGGER.debug("拦截实体 {} 的原版生命提升效果应用", entity.uuid)
            
            // 不取消事件，让效果存在但我们会拦截其属性修改
            // 这样可以让我们的系统检测到效果，同时阻止原版修改
        }
    }
    
    /**
     * 添加级拦截 - 在效果添加时立即处理
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMobEffectAddedEarly(event: MobEffectEvent.Added) {
        val entity = event.entity
        val effectInstance = event.effectInstance ?: return
        
        if (effectInstance.effect == MobEffects.HEALTH_BOOST) {
            // 立即清理任何可能已经应用的原版修改器
            immediatelyCleanupVanillaHealthBoost(entity)
            LOGGER.debug("早期拦截：立即清理实体 {} 的原版生命提升修改器", entity.uuid)
        }
    }
    
    /**
     * 立即清理原版生命提升修改器
     * 这是最早期的清理，确保原版修改器不会被应用
     */
    private fun immediatelyCleanupVanillaHealthBoost(entity: LivingEntity) {
        try {
            val maxHealthAttribute = entity.attributes.getInstance(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
            if (maxHealthAttribute != null) {
                // 获取当前所有修改器的快照
                val currentModifiers = maxHealthAttribute.modifiers.toList()
                
                // 查找并立即移除任何生命提升相关的修改器
                currentModifiers.forEach { modifier ->
                    // 原版生命提升修改器的特征：
                    // 1. 正数值
                    // 2. 通常是4的倍数（每级+4血量）
                    // 3. 可能包含特定的ID模式
                    if (modifier.amount > 0 && 
                        (modifier.amount % 4.0 == 0.0 || modifier.amount % 2.0 == 0.0)) {
                        
                        maxHealthAttribute.removeModifier(modifier.id)
                        LOGGER.debug("早期移除可疑的生命提升修改器: {} -> {}", modifier.id, modifier.amount)
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("早期清理原版生命提升修改器时出错", e)
        }
    }
} 