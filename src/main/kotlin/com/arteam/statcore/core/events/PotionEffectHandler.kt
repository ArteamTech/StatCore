package com.arteam.statcore.core.events

import com.arteam.statcore.api.attributes.AttributeOperation
import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.attributes.DefenseAttributes
import com.arteam.statcore.core.attributes.AttributeManager
import com.arteam.statcore.core.sync.ImmediateSyncManager
import com.arteam.statcore.util.AttributeUtils
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.MobEffectEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import net.neoforged.neoforge.event.tick.EntityTickEvent
import org.slf4j.LoggerFactory

/**
 * 药水效果处理器
 * 适配原版药水效果到我们的属性系统
 */
@EventBusSubscriber(modid = "statcore")
@Suppress("unused")
object PotionEffectHandler {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.potion")
    
    // 修改器来源标识
    private val RESISTANCE_SOURCE = ResourceLocation.fromNamespaceAndPath("statcore", "resistance_potion")
    private val HEALTH_BOOST_SOURCE = ResourceLocation.fromNamespaceAndPath("statcore", "health_boost_potion")
    
    // 效果常量
    private const val RESISTANCE_DEFENSE_PER_LEVEL = 20.0  // 每级抗性提升+20真实防御
    private const val HEALTH_BOOST_PER_LEVEL = 20.0        // 每级生命提升+20血量
    
    /**
     * 药水效果添加事件
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMobEffectAdded(event: MobEffectEvent.Added) {
        val entity = event.entity
        if (entity.level().isClientSide) return
        
        val effectInstance = event.effectInstance ?: return
        val effect = effectInstance.effect
        val amplifier = effectInstance.amplifier
        
        when (effect) {
            MobEffects.RESISTANCE -> {
                applyResistanceEffect(entity, amplifier + 1) // amplifier从0开始，所以+1得到实际等级
                LOGGER.debug("为实体 {} 应用抗性提升效果，等级: {}", entity.uuid, amplifier + 1)
            }
            MobEffects.HEALTH_BOOST -> {
                // 立即拦截并阻止原版生命提升效果
                preemptivelyBlockVanillaHealthBoost(entity)
                
                // 应用我们的生命提升效果
                applyHealthBoostEffect(entity, amplifier + 1)
                LOGGER.debug("为实体 {} 应用生命提升效果，等级: {}", entity.uuid, amplifier + 1)
            }
        }
    }
    
    /**
     * 药水效果移除事件
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMobEffectRemoved(event: MobEffectEvent.Remove) {
        val entity = event.entity
        if (entity.level().isClientSide) return
        
        val effectInstance = event.effectInstance ?: return
        val effect = effectInstance.effect
        
        when (effect) {
            MobEffects.RESISTANCE -> {
                removeResistanceEffect(entity)
                LOGGER.debug("为实体 {} 移除抗性提升效果", entity.uuid)
            }
            MobEffects.HEALTH_BOOST -> {
                removeHealthBoostEffect(entity)
                LOGGER.debug("为实体 {} 移除生命提升效果", entity.uuid)
            }
        }
    }
    
    /**
     * 药水效果过期事件
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMobEffectExpired(event: MobEffectEvent.Expired) {
        val entity = event.entity
        if (entity.level().isClientSide) return
        
        val effectInstance = event.effectInstance ?: return
        val effect = effectInstance.effect
        
        when (effect) {
            MobEffects.RESISTANCE -> {
                removeResistanceEffect(entity)
                LOGGER.debug("实体 {} 的抗性提升效果已过期", entity.uuid)
            }
            MobEffects.HEALTH_BOOST -> {
                removeHealthBoostEffect(entity)
                LOGGER.debug("实体 {} 的生命提升效果已过期", entity.uuid)
            }
        }
    }
    
    /**
     * 应用抗性提升效果
     * 每级+20真实防御
     */
    private fun applyResistanceEffect(entity: LivingEntity, level: Int) {
        // 先移除现有的抗性修改器
        removeResistanceEffect(entity)
        
        // 计算真实防御值
        val defenseValue = RESISTANCE_DEFENSE_PER_LEVEL * level
        
        // 添加真实防御修改器
        AttributeUtils.addTemporaryModifier(
            entity = entity,
            attribute = DefenseAttributes.TRUE_DEFENSE,
            name = "抗性提升 $level",
            amount = defenseValue,
            operation = AttributeOperation.ADDITION,
            source = RESISTANCE_SOURCE
        )
        
        // 立即同步属性
        ImmediateSyncManager.asyncForceSyncAll(entity)
    }
    
    /**
     * 移除抗性提升效果
     */
    private fun removeResistanceEffect(entity: LivingEntity) {
        // 移除所有来自抗性提升的修改器
        AttributeUtils.removeModifiersBySource(entity, RESISTANCE_SOURCE)
        
        // 立即同步属性
        ImmediateSyncManager.asyncForceSyncAll(entity)
    }
    
    /**
     * 应用生命提升效果
     * 每级+20血量，并确保血量条保持在10颗心的范围内显示
     */
    private fun applyHealthBoostEffect(entity: LivingEntity, level: Int) {
        // 先移除现有的生命提升修改器
        removeHealthBoostEffect(entity)
        
        // 计算额外生命值
        val healthBoost = HEALTH_BOOST_PER_LEVEL * level
        
        // 添加最大生命值修改器
        AttributeUtils.addTemporaryModifier(
            entity = entity,
            attribute = CoreAttributes.MAX_HEALTH,
            name = "生命提升 $level",
            amount = healthBoost,
            operation = AttributeOperation.ADDITION,
            source = HEALTH_BOOST_SOURCE
        )
        
        // 根据记忆，当扩展最大血量时，当前血量也必须按相同比例扩展
        val currentHealth = AttributeManager.getAttributeValue(entity, CoreAttributes.CURRENT_HEALTH)
        val currentMaxHealth = AttributeManager.getAttributeValue(entity, CoreAttributes.MAX_HEALTH)
        val newMaxHealth = currentMaxHealth + healthBoost
        
        // 按比例增加当前血量，确保比例保持一致
        val healthRatio = currentHealth / currentMaxHealth
        val newCurrentHealth = newMaxHealth * healthRatio
        
        AttributeManager.setAttributeValue(entity, CoreAttributes.CURRENT_HEALTH, newCurrentHealth)
        
        // 立即同步属性
        ImmediateSyncManager.asyncForceSyncAll(entity)
    }
    
    /**
     * 移除生命提升效果
     */
    private fun removeHealthBoostEffect(entity: LivingEntity) {
        // 获取当前血量信息
        val currentHealth = AttributeManager.getAttributeValue(entity, CoreAttributes.CURRENT_HEALTH)
        val currentMaxHealth = AttributeManager.getAttributeValue(entity, CoreAttributes.MAX_HEALTH)
        val healthRatio = currentHealth / currentMaxHealth
        
        // 移除所有来自生命提升的修改器
        AttributeUtils.removeModifiersBySource(entity, HEALTH_BOOST_SOURCE)
        
        // 重新计算血量，保持血量比例
        val newMaxHealth = AttributeManager.getAttributeValue(entity, CoreAttributes.MAX_HEALTH)
        val newCurrentHealth = (newMaxHealth * healthRatio).coerceAtMost(newMaxHealth)
        
        AttributeManager.setAttributeValue(entity, CoreAttributes.CURRENT_HEALTH, newCurrentHealth)
        
        // 立即同步属性
        ImmediateSyncManager.asyncForceSyncAll(entity)
    }

    /**
     * 玩家Tick事件 - 拦截原版生命提升对MAX_HEALTH的修改
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity
        if (player.level().isClientSide) return
        
        // 每10 ticks检查一次，避免过于频繁
        if (player.tickCount % 10 == 0) {
            interceptVanillaHealthBoost(player)
        }
    }
    
    /**
     * 抢先阻止原版生命提升效果
     * 在原版效果应用前就移除其修改器
     */
    private fun preemptivelyBlockVanillaHealthBoost(entity: LivingEntity) {
        try {
            // 立即检查并移除任何原版生命提升修改器
            val vanillaHealthInstance = entity.attributes.getInstance(Attributes.MAX_HEALTH)
            if (vanillaHealthInstance != null) {
                // 查找并移除所有可能的原版生命提升修改器
                val modifiersToRemove = vanillaHealthInstance.modifiers.filter { modifier ->
                    // 原版生命提升修改器通常是4的倍数（每级+4血量）
                    modifier.amount > 0 && (modifier.amount % 4.0 == 0.0)
                }
                
                modifiersToRemove.forEach { modifier ->
                    vanillaHealthInstance.removeModifier(modifier.id)
                    LOGGER.debug("抢先移除原版生命提升修改器: {} ({})", modifier.id, modifier.amount)
                }
            }
        } catch (e: Exception) {
            LOGGER.error("抢先阻止原版生命提升时出错", e)
        }
    }
    
    /**
     * 生物Tick事件 - 更频繁地拦截所有生物的原版生命提升对MAX_HEALTH的修改
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onEntityTick(event: EntityTickEvent.Post) {
        val entity = event.entity
        if (entity !is LivingEntity || entity.level().isClientSide) return
        
        // 更频繁检查（每5 ticks而不是10 ticks）
        if (entity.tickCount % 5 == 0) {
            interceptVanillaHealthBoost(entity)
        }
    }
    
    /**
     * 拦截原版生命提升效果对MAX_HEALTH的修改
     */
    private fun interceptVanillaHealthBoost(player: Player) {
        interceptVanillaHealthBoost(player as LivingEntity)
    }
    
    /**
     * 拦截原版生命提升效果对MAX_HEALTH的修改（适用于所有生物）
     */
    private fun interceptVanillaHealthBoost(entity: LivingEntity) {
        val healthBoostEffect = entity.getEffect(MobEffects.HEALTH_BOOST)
        if (healthBoostEffect != null) {
            val level = healthBoostEffect.amplifier + 1
            
            // 获取原版最大血量属性实例
            val vanillaMaxHealthInstance = entity.attributes.getInstance(Attributes.MAX_HEALTH)
            if (vanillaMaxHealthInstance != null) {
                // 查找原版生命提升修改器
                val vanillaBoostModifiers = vanillaMaxHealthInstance.modifiers
                    .filter { modifier -> 
                        // 检查修改器来源是否为原版生命提升效果
                        modifier.amount > 0 && (modifier.amount % 4.0 == 0.0) // 原版每级+4血量
                    }
                
                if (vanillaBoostModifiers.isNotEmpty()) {
                    // 移除原版生命提升修改器
                    vanillaBoostModifiers.forEach { modifier ->
                        vanillaMaxHealthInstance.removeModifier(modifier.id)
                    }
                    
                    LOGGER.debug("拦截并移除了实体 {} 的原版生命提升修改器", entity.uuid)
                }
            }
        }
    }
} 