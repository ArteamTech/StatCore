package com.arteam.statcore.client

import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.core.attributes.AttributeManager
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.resources.ResourceLocation
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent
import net.neoforged.neoforge.client.gui.VanillaGuiLayers
import org.slf4j.LoggerFactory

/**
 * 客户端血量条处理器
 * 确保生命提升效果不会在血量条上显示额外的心
 */
@EventBusSubscriber(modid = "statcore", value = [Dist.CLIENT])
@Suppress("unused")
object HealthBarHandler {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.health_bar")
    
    // 原版生命提升效果的修改器来源标识
    private val VANILLA_HEALTH_BOOST_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "effect.health_boost")
    
    // 用于跟踪血量条状态
    private var lastKnownMaxHealth = 20.0
    private var lastEffectLevel = 0
    
    /**
     * 客户端Tick事件 - 持续监控和调整血量显示
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onClientTick(event: ClientTickEvent.Post) {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        
        // 每tick都检查并调整
        handleHealthDisplay(player)
    }
    
    /**
     * 渲染血量条之前的事件
     * 确保血量条显示正确
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderHealthBar(event: RenderGuiLayerEvent.Pre) {
        // 只处理血量条层
        if (event.name != VanillaGuiLayers.PLAYER_HEALTH) return
        
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        
        // 在渲染前最后一次确保显示正确
        handleHealthDisplay(player)
    }
    
    /**
     * 处理血量显示逻辑
     * 统一的血量显示处理方法
     */
    private fun handleHealthDisplay(player: LocalPlayer) {
        try {
            val healthBoostEffect = player.getEffect(MobEffects.HEALTH_BOOST)
            val currentEffectLevel = if (healthBoostEffect != null) healthBoostEffect.amplifier + 1 else 0
            
            // 如果效果状态发生变化，立即处理
            if (currentEffectLevel != lastEffectLevel) {
                lastEffectLevel = currentEffectLevel
                LOGGER.debug("检测到生命提升效果变化: 等级 {}", currentEffectLevel)
            }
            
            if (healthBoostEffect != null) {
                // 有生命提升效果时，调整显示
                adjustHealthDisplayForBoost(player)
            } else {
                // 没有生命提升效果时，确保显示正常
                ensureNormalHealthDisplay(player)
            }
            
        } catch (e: Exception) {
            LOGGER.error("处理血量显示时出错", e)
        }
    }
    
    /**
     * 为生命提升效果调整血量显示
     */
    private fun adjustHealthDisplayForBoost(player: LocalPlayer) {
        try {
            // 获取我们系统的实际血量值
            val actualCurrentHealth = AttributeManager.getAttributeValue(player, CoreAttributes.CURRENT_HEALTH)
            val actualMaxHealth = AttributeManager.getAttributeValue(player, CoreAttributes.MAX_HEALTH)
            
            // 目标显示血量（10颗心）
            val maxDisplayHealth = 20.0
            
            // 如果实际最大血量大于20，进行比例缩放
            if (actualMaxHealth > maxDisplayHealth) {
                val displayRatio = maxDisplayHealth / actualMaxHealth
                val displayCurrentHealth = actualCurrentHealth * displayRatio
                
                // 设置原版属性用于显示
                val vanillaHealthInstance = player.attributes.getInstance(Attributes.MAX_HEALTH)
                
                // 立即强制设置显示值
                if (vanillaHealthInstance != null) {
                    vanillaHealthInstance.baseValue = maxDisplayHealth
                    player.health = displayCurrentHealth.toFloat()
                    
                    // 强制移除任何可能的原版生命提升修改器
                    removeVanillaHealthBoostModifiers(player)
                    
                    lastKnownMaxHealth = maxDisplayHealth
                }
                
                LOGGER.debug("调整血量条显示: 实际({}/{}) -> 显示({}/{})", 
                    actualCurrentHealth, actualMaxHealth, 
                    displayCurrentHealth, maxDisplayHealth)
            }
        } catch (e: Exception) {
            LOGGER.error("调整生命提升血量显示时出错", e)
        }
    }
    
    /**
     * 确保正常血量显示（无生命提升时）
     */
    private fun ensureNormalHealthDisplay(player: LocalPlayer) {
        try {
            // 获取我们系统的实际血量值
            val actualCurrentHealth = AttributeManager.getAttributeValue(player, CoreAttributes.CURRENT_HEALTH)
            val actualMaxHealth = AttributeManager.getAttributeValue(player, CoreAttributes.MAX_HEALTH)
            
            // 恢复正常显示
            val vanillaHealthInstance = player.attributes.getInstance(Attributes.MAX_HEALTH)
            if (vanillaHealthInstance != null) {
                vanillaHealthInstance.baseValue = actualMaxHealth
                player.health = actualCurrentHealth.toFloat()
                lastKnownMaxHealth = actualMaxHealth
            }
            
        } catch (e: Exception) {
            LOGGER.error("恢复正常血量显示时出错", e)
        }
    }
    
    /**
     * 移除客户端上的原版生命提升修改器（使用精确的ResourceLocation标识）
     */
    private fun removeVanillaHealthBoostModifiers(player: LocalPlayer) {
        try {
            val vanillaHealthInstance = player.attributes.getInstance(Attributes.MAX_HEALTH)
            if (vanillaHealthInstance != null) {
                // 通过精确的ResourceLocation来移除原版生命提升修改器
                val modifierRemoved = vanillaHealthInstance.removeModifier(VANILLA_HEALTH_BOOST_ID)
                
                if (modifierRemoved) {
                    LOGGER.debug("客户端精确移除原版生命提升修改器: {}", VANILLA_HEALTH_BOOST_ID)
                } else {
                    // 如果通过ID移除失败，尝试通过特征移除（作为备用方案）
                    val modifiersToRemove = vanillaHealthInstance.modifiers.filter { modifier ->
                        // 原版生命提升修改器特征：正数且为4的倍数
                        modifier.amount > 0 && (modifier.amount % 4.0 == 0.0)
                    }
                    
                    modifiersToRemove.forEach { modifier ->
                        vanillaHealthInstance.removeModifier(modifier.id)
                        LOGGER.debug("客户端通过特征移除原版生命提升修改器: {} ({})", modifier.id, modifier.amount)
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("客户端移除原版生命提升修改器时出错", e)
        }
    }
} 