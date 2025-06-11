package com.arteam.statcore.core.events

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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent
import net.neoforged.neoforge.event.entity.living.LivingHealEvent
import org.slf4j.LoggerFactory

/**
 * 属性事件处理器
 * 处理生命值迁移、治疗放大、伤害重构等核心逻辑
 */
@EventBusSubscriber(modid = "statcore")
object AttributeEventHandler {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.events")
    
    /**
     * 实体加入世界时初始化属性
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onEntityJoinLevel(event: EntityJoinLevelEvent) {
        val entity = event.entity
        if (entity is LivingEntity && !event.level.isClientSide) {
            // 异步初始化属性，避免阻塞主线程
            entity.level().server?.execute {
                try {
                    initializeEntityAttributes(entity)
                    LOGGER.debug("已初始化实体 {} 的属性", entity.uuid)
                } catch (e: Exception) {
                    LOGGER.error("初始化实体属性时发生错误: {}", e.message, e)
                }
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
        
        // 放大治疗量5倍
        val scaledAmount = originalAmount * 5.0f
        event.amount = scaledAmount
        
        LOGGER.debug("实体 {} 治疗量从 {} 放大到 {}", 
            entity.uuid, originalAmount, scaledAmount)
        
        // 治疗后立即同步血量
        ImmediateSyncManager.asyncForceSyncAll(entity)
    }
    
    /**
     * 处理实体伤害事件
     * 放大伤害5倍，然后应用我们的防御系统
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onLivingDamage(event: LivingDamageEvent.Pre) {
        if (event.entity.level().isClientSide) return
        
        val entity = event.entity
        val damageSource = event.source
        val originalDamage = event.originalDamage
        
        // 第一步：放大伤害5倍
        val scaledDamage = originalDamage * 5.0f
        
        // 第二步：应用我们的防御系统
        val defenseType = mapDamageSourceToDefenseType(damageSource)
        val damageReduction = DefenseAttributes.calculateDamageReduction(entity, defenseType)
        
        // 计算最终伤害
        val finalDamage = scaledDamage * (1.0f - damageReduction.toFloat())
        
        // 更新事件
        event.newDamage = finalDamage
        
        LOGGER.debug("实体 {} 受到伤害: 原始={}, 放大={}, 防御类型={}, 减免={:.2f}%, 最终={}", 
            entity.uuid, originalDamage, scaledDamage, defenseType.id, 
            damageReduction * 100, finalDamage)
        
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
        
        // 迁移最大生命值（原版 × 5）
        migrateMaxHealth(entity)
        
        // 迁移护甲值到物理防御（原版护甲 × 5）
        migrateArmorToPhysicalDefense(entity)
        
        // 同步所有属性到原版系统
        AttributeSyncManager.syncEntityAttributes(entity)
    }
    
    /**
     * 迁移最大生命值
     * 将原版最大生命值放大5倍设置到我们的系统
     */
    private fun migrateMaxHealth(entity: LivingEntity) {
        val vanillaMaxHealth = entity.maxHealth.toDouble()
        val scaledMaxHealth = if (entity is Player) {
            // 玩家默认是100（原版20×5）
            100.0
        } else {
            // 其他实体是原版的5倍
            vanillaMaxHealth * 5.0
        }
        
        // 设置到我们的属性系统
        AttributeManager.setAttributeBaseValue(entity, CoreAttributes.MAX_HEALTH, scaledMaxHealth)
        
        // 同步到原版系统（保持兼容性）
        entity.attributes.getInstance(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)?.let { instance ->
            instance.baseValue = scaledMaxHealth
        }
        
        // 设置当前生命值到我们的系统
        val currentHealth = Math.min(entity.health.toDouble(), scaledMaxHealth)
        AttributeManager.setAttributeValue(entity, CoreAttributes.CURRENT_HEALTH, currentHealth)
        
        // 如果当前生命值超过新的最大值，调整当前生命值
        if (entity.health > scaledMaxHealth) {
            entity.health = scaledMaxHealth.toFloat()
        }
        
        LOGGER.debug("实体 {} 生命值迁移: 原版={}, 新值={}", entity.uuid, vanillaMaxHealth, scaledMaxHealth)
    }
    
    /**
     * 迁移护甲值到物理防御
     * 将原版护甲点数转换为我们的物理防御值
     */
    private fun migrateArmorToPhysicalDefense(entity: LivingEntity) {
        val vanillaArmor = entity.armorValue.toDouble()
        val physicalDefense = vanillaArmor * 5.0
        
        // 总是设置物理防御值，即使是0也要设置（用于初始化）
        AttributeManager.setAttributeBaseValue(entity, CoreAttributes.PHYSICAL_DEFENSE, physicalDefense)
        LOGGER.debug("实体 {} 护甲迁移: 原版护甲={}, 物理防御={}", entity.uuid, vanillaArmor, physicalDefense)
    }
    
    /**
     * 将伤害源映射到防御类型
     * @param damageSource 伤害源
     * @return 对应的防御类型
     */
    private fun mapDamageSourceToDefenseType(damageSource: DamageSource): DefenseType {
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