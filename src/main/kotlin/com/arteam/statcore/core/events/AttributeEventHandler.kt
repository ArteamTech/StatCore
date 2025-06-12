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
        // 使用固定的原版基础血量值，避免重复放大
        val vanillaBaseMaxHealth = getVanillaBaseMaxHealth(entity)
        val vanillaCurrentHealth = entity.health.toDouble()
        val vanillaMaxHealth = entity.maxHealth.toDouble()
        
        val scaledMaxHealth = if (entity is Player) {
            // 玩家使用默认值
            StatCore.PLAYER_DEFAULT_MAX_HEALTH
        } else {
            // 其他实体使用固定的原版基础值乘以缩放因子
            vanillaBaseMaxHealth * StatCore.VANILLA_TO_STATCORE_SCALE_FACTOR
        }
        
        // 计算当前血量的缩放值
        val scaledCurrentHealth = if (entity is Player) {
            // 玩家保持当前血量比例
            val healthRatio = vanillaCurrentHealth / vanillaMaxHealth
            StatCore.PLAYER_DEFAULT_MAX_HEALTH * healthRatio
        } else {
            // 其他实体：如果当前血量已经被放大过，就按比例处理；否则直接放大
            if (vanillaMaxHealth > vanillaBaseMaxHealth * 1.5) {
                // 血量已经被放大过，按比例计算
                val healthRatio = vanillaCurrentHealth / vanillaMaxHealth
                scaledMaxHealth * healthRatio
            } else {
                // 血量还没被放大过，直接放大
                vanillaCurrentHealth * StatCore.VANILLA_TO_STATCORE_SCALE_FACTOR
            }
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
    }
    
    /**
     * 获取实体的原版基础最大生命值
     * 这些是固定的原版血量值，不会被其他模组或系统修改
     */
    private fun getVanillaBaseMaxHealth(entity: LivingEntity): Double {
        // 使用实体类型的注册名来获取固定的原版血量值
        val entityTypeName = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.type).toString()
        return when (entityTypeName) {
            // 玩家
            "minecraft:player" -> 20.0
            
            // 被动生物
            "minecraft:allay" -> 20.0
            "minecraft:axolotl" -> 14.0
            "minecraft:armadillo" -> 12.0
            "minecraft:bat" -> 6.0
            "minecraft:camel" -> 32.0
            "minecraft:cat" -> 10.0
            "minecraft:chicken" -> 4.0
            "minecraft:cod" -> 3.0
            "minecraft:cow" -> 10.0
            "minecraft:donkey" -> 15.0  // 到 30.0 (随机)
            "minecraft:fox" -> 10.0     // Java版，基岩版是20
            "minecraft:frog" -> 10.0
            "minecraft:glow_squid" -> 10.0
            "minecraft:horse" -> 15.0   // 到 30.0 (随机)
            "minecraft:mooshroom" -> 10.0
            "minecraft:mule" -> 15.0    // 到 30.0 (随机)
            "minecraft:ocelot" -> 10.0
            "minecraft:parrot" -> 6.0
            "minecraft:pig" -> 10.0
            "minecraft:pufferfish" -> 3.0
            "minecraft:rabbit" -> 3.0
            "minecraft:salmon" -> 3.0
            "minecraft:sheep" -> 8.0
            "minecraft:skeleton_horse" -> 15.0
            "minecraft:snow_golem" -> 4.0
            "minecraft:sniffer" -> 14.0
            "minecraft:squid" -> 10.0
            "minecraft:strider" -> 20.0
            "minecraft:tadpole" -> 6.0
            "minecraft:tropical_fish" -> 3.0
            "minecraft:turtle" -> 30.0
            "minecraft:villager" -> 20.0
            "minecraft:wandering_trader" -> 20.0
            
            // 中性生物
            "minecraft:bee" -> 10.0
            "minecraft:cave_spider" -> 12.0
            "minecraft:dolphin" -> 10.0
            "minecraft:enderman" -> 40.0
            "minecraft:goat" -> 10.0
            "minecraft:iron_golem" -> 100.0
            "minecraft:llama" -> 15.0   // 到 30.0 (随机)
            "minecraft:panda" -> 20.0   // 虚弱熊猫是10
            "minecraft:piglin" -> 16.0
            "minecraft:polar_bear" -> 30.0
            "minecraft:spider" -> 16.0
            "minecraft:trader_llama" -> 15.0 // 到 30.0 (随机)
            "minecraft:wolf" -> 8.0     // 野生状态，驯服后是20
            "minecraft:zombified_piglin" -> 20.0
            
            // 敌对生物
            "minecraft:blaze" -> 20.0
            "minecraft:bogged" -> 16.0
            "minecraft:breeze" -> 30.0
            "minecraft:creaking" -> 1.0
            "minecraft:creeper" -> 20.0
            "minecraft:drowned" -> 20.0
            "minecraft:elder_guardian" -> 80.0
            "minecraft:endermite" -> 8.0
            "minecraft:evoker" -> 24.0
            "minecraft:ghast" -> 10.0
            "minecraft:guardian" -> 30.0
            "minecraft:hoglin" -> 40.0
            "minecraft:husk" -> 20.0
            "minecraft:magma_cube" -> 16.0  // 大型，中型4，小型1
            "minecraft:phantom" -> 20.0
            "minecraft:piglin_brute" -> 50.0
            "minecraft:pillager" -> 24.0
            "minecraft:ravager" -> 100.0
            "minecraft:shulker" -> 30.0
            "minecraft:silverfish" -> 8.0
            "minecraft:skeleton" -> 20.0
            "minecraft:slime" -> 16.0   // 大型，中型4，小型1
            "minecraft:stray" -> 20.0
            "minecraft:vex" -> 14.0
            "minecraft:vindicator" -> 24.0
            "minecraft:warden" -> 500.0
            "minecraft:witch" -> 26.0
            "minecraft:wither_skeleton" -> 20.0
            "minecraft:zoglin" -> 40.0
            "minecraft:zombie" -> 20.0
            "minecraft:zombie_villager" -> 20.0
            
            // Boss
            "minecraft:ender_dragon" -> 200.0
            "minecraft:wither" -> 300.0
            
            // 默认值：如果是未知实体，尝试从当前血量推断原版基础值
            else -> {
                val currentMaxHealth = entity.maxHealth.toDouble()
                // 如果当前血量是5的倍数且大于原版范围，可能已经被放大了
                if (currentMaxHealth > 50.0 && currentMaxHealth % 5.0 == 0.0) {
                    currentMaxHealth / StatCore.VANILLA_TO_STATCORE_SCALE_FACTOR
                } else {
                    currentMaxHealth
                }
            }
        }
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