package com.arteam.statcore.core.events

import com.arteam.statcore.StatCore
import com.arteam.statcore.api.events.MapDamageSourceEvent
import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.attributes.DefenseAttributes
import com.arteam.statcore.attributes.DefenseType
import com.arteam.statcore.core.attributes.AttributeManager
import com.arteam.statcore.core.sync.AttributeSyncManager
import com.arteam.statcore.core.sync.ImmediateSyncManager
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent
import net.neoforged.neoforge.event.entity.living.LivingHealEvent
import org.slf4j.LoggerFactory

/**
 * 属性事件处理器
 * 处理生命值迁移、治疗放大、伤害重构等核心逻辑
 */
@EventBusSubscriber(modid = "statcore")
@Suppress("unused")
object AttributeEventHandler {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.events")
    
    /**
     * 实体加入世界时初始化属性
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onEntityJoinLevel(event: EntityJoinLevelEvent) {
        val entity = event.entity
        if (entity is LivingEntity && !event.level.isClientSide) {
            // 立即初始化属性，确保所有生物都被处理
            try {
                initializeEntityAttributes(entity)
                val entityType = if (entity is Player) "玩家" else entity.type.description.string
                LOGGER.info("已初始化实体 {} ({}) 的属性", entityType, entity.uuid)
            } catch (e: Exception) {
                LOGGER.error("初始化实体属性时发生错误: {}", e.message, e)
            }
        }
    }
    
    /**
     * 处理实体治疗事件
     * 将治疗量放大5倍，保持与生命值的比例一致
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onLivingHeal(event: LivingHealEvent) {
        if (event.entity.level().isClientSide) return
        
        val entity = event.entity
        val originalAmount = event.amount
        
        // 放大治疗量
        val scaledAmount = originalAmount * StatCore.VANILLA_TO_STATCORE_SCALE_FACTOR.toFloat()
        event.amount = scaledAmount
        
        LOGGER.debug("实体 {} 治疗量从 {} 放大到 {}", 
            entity.uuid, originalAmount, scaledAmount)
        
        // 治疗后立即同步血量
        ImmediateSyncManager.asyncForceSyncAll(entity)
    }
    
    /**
     * 处理实体伤害事件
     * 放大伤害，然后应用我们的防御系统
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onLivingDamage(event: LivingDamageEvent.Pre) {
        if (event.entity.level().isClientSide) return
        
        val entity = event.entity
        val damageSource = event.source
        val originalDamage = event.originalDamage
        
        // 第一步：放大伤害
        val scaledDamage = originalDamage * StatCore.VANILLA_TO_STATCORE_SCALE_FACTOR.toFloat()
        
        // 第二步：应用我们的防御系统
        val defenseType = mapDamageSourceToDefenseType(damageSource)
        val damageReduction = DefenseAttributes.calculateDamageReduction(entity, defenseType)
        
        // 计算最终伤害
        val finalDamage = scaledDamage * (1.0f - damageReduction.toFloat())
        
        // 更新事件
        event.newDamage = finalDamage
        
        LOGGER.debug("实体 {} 受到伤害: 原始={}, 放大={}, 防御类型={}, 减免={}%, 最终={}", 
            entity.uuid, originalDamage, scaledDamage, defenseType.id, 
            String.format("%.2f", damageReduction * 100), finalDamage)
        
        // 受伤后立即同步血量
        ImmediateSyncManager.asyncForceSyncAll(entity)
    }
    
    /**
     * 初始化实体属性
     * 包括生命值迁移和护甲迁移
     */
    private fun initializeEntityAttributes(entity: LivingEntity) {
        // 初始化属性映射
        AttributeManager.initializeEntityAttributes(entity)
        
        // 迁移最大生命值
        migrateMaxHealth(entity)
        
        // 迁移护甲值到物理防御
        migrateArmorToPhysicalDefense(entity)
        
        // 同步所有属性到原版系统
        AttributeSyncManager.syncEntityAttributes(entity)
    }
    
    /**
     * 迁移最大生命值
     * 将原版最大生命值放大到我们的系统
     */
    private fun migrateMaxHealth(entity: LivingEntity) {
        val vanillaMaxHealth = entity.maxHealth.toDouble()
        val vanillaCurrentHealth = entity.health.toDouble()
        
        val scaledMaxHealth = if (entity is Player) {
            // 玩家使用默认值
            StatCore.PLAYER_DEFAULT_MAX_HEALTH
        } else {
            // 其他实体是原版的指定倍数
            vanillaMaxHealth * StatCore.VANILLA_TO_STATCORE_SCALE_FACTOR
        }
        
        // 计算当前血量的缩放值
        val scaledCurrentHealth = if (entity is Player) {
            // 玩家保持当前血量比例
            val healthRatio = vanillaCurrentHealth / vanillaMaxHealth
            StatCore.PLAYER_DEFAULT_MAX_HEALTH * healthRatio
        } else {
            // 其他实体按比例缩放当前血量
            vanillaCurrentHealth * StatCore.VANILLA_TO_STATCORE_SCALE_FACTOR
        }
        
        // 设置到我们的属性系统
        AttributeManager.setAttributeBaseValue(entity, CoreAttributes.MAX_HEALTH, scaledMaxHealth)
        AttributeManager.setAttributeValue(entity, CoreAttributes.CURRENT_HEALTH, scaledCurrentHealth)
        
        // 同步到原版系统（保持兼容性）
        entity.attributes.getInstance(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)?.let { instance ->
            instance.baseValue = scaledMaxHealth
        }
        
        // 设置原版当前血量
        entity.health = scaledCurrentHealth.toFloat()
        
        LOGGER.debug("实体 {} 生命值迁移: 原版={}/{}, 新值={}/{}", 
            entity.uuid, vanillaCurrentHealth, vanillaMaxHealth, scaledCurrentHealth, scaledMaxHealth)
    }
    
    /**
     * 迁移护甲值到物理防御
     * 将原版护甲点数转换为我们的物理防御值
     */
    private fun migrateArmorToPhysicalDefense(entity: LivingEntity) {
        val vanillaArmor = entity.armorValue.toDouble()
        val physicalDefense = vanillaArmor * StatCore.VANILLA_TO_STATCORE_SCALE_FACTOR
        
        // 总是设置物理防御值，即使是0也要设置（用于初始化）
        AttributeManager.setAttributeBaseValue(entity, CoreAttributes.PHYSICAL_DEFENSE, physicalDefense)
        LOGGER.debug("实体 {} 护甲迁移: 原版护甲={}, 物理防御={}", entity.uuid, vanillaArmor, physicalDefense)
    }
    
    /**
     * 将伤害源映射到防御类型
     * 支持通过事件扩展映射逻辑，允许其他模组自定义
     * @param damageSource 伤害源
     * @return 对应的防御类型
     */
    private fun mapDamageSourceToDefenseType(damageSource: DamageSource): DefenseType {
        // 首先发布映射事件，允许其他模组自定义映射
        val event = MapDamageSourceEvent(damageSource, DefenseType.PHYSICAL) // 默认为物理防御
        NeoForge.EVENT_BUS.post(event)
        
        // 如果事件被处理，使用事件中的防御类型
        if (event.isHandled) {
            return event.defenseType
        }
        
        // 如果事件没有被处理，使用默认映射逻辑
        return when {
            // 爆炸伤害
            damageSource.`is`(net.minecraft.world.damagesource.DamageTypes.EXPLOSION) ||
            damageSource.`is`(net.minecraft.world.damagesource.DamageTypes.PLAYER_EXPLOSION) -> {
                DefenseType.EXPLOSION
            }
            
            // 火焰伤害
            damageSource.`is`(net.minecraft.world.damagesource.DamageTypes.IN_FIRE) ||
            damageSource.`is`(net.minecraft.world.damagesource.DamageTypes.ON_FIRE) ||
            damageSource.`is`(net.minecraft.world.damagesource.DamageTypes.LAVA) ||
            damageSource.`is`(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR) -> {
                DefenseType.FIRE
            }
            
            // 弹射物伤害
            damageSource.`is`(net.minecraft.world.damagesource.DamageTypes.ARROW) ||
            damageSource.`is`(net.minecraft.world.damagesource.DamageTypes.TRIDENT) -> {
                DefenseType.PROJECTILE
            }
            
            // 默认物理伤害
            else -> DefenseType.PHYSICAL
        }
    }
} 