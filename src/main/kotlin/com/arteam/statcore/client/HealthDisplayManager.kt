package com.arteam.statcore.client

import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.core.attributes.AttributeManager
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent
import net.neoforged.api.distmarker.Dist
import org.slf4j.LoggerFactory

/**
 * 血量显示管理器
 * 负责将我们的血量系统按比例显示在血条上
 * 将最大血量缩放到10颗心的范围内显示
 */
@EventBusSubscriber(modid = "statcore", value = [Dist.CLIENT])
object HealthDisplayManager {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.display")
    private const val MAX_HEARTS_DISPLAY = 10 // 最多显示10颗心
    private const val VANILLA_MAX_HEALTH = 20.0 // 原版满血血量
    
    // 用于恢复原始值的缓存
    private var originalHealth: Float? = null
    private var originalMaxHealth: Double? = null
    
    /**
     * 拦截血量渲染事件，修改显示逻辑
     */
    @SubscribeEvent
    fun onRenderGuiLayer(event: RenderGuiLayerEvent.Pre) {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        
        // 只处理血量层的渲染
        if (event.name.toString() == "minecraft:player_health") {
            // 修改血量显示值，让原版渲染系统使用我们的值
            handleHealthDisplay(player)
        }
    }
    
    /**
     * 渲染后恢复原始值
     */
    @SubscribeEvent
    fun onRenderGuiLayerPost(event: RenderGuiLayerEvent.Post) {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        
        // 只处理血量层的渲染
        if (event.name.toString() == "minecraft:player_health") {
            // 恢复原始血量值
            restoreOriginalValues(player)
        }
    }
    
    /**
     * 处理血量显示逻辑
     * @param player 玩家
     * @return 是否成功处理了血量显示
     */
    private fun handleHealthDisplay(player: Player){
        try {
            // 保存原始值
            originalHealth = player.health
            originalMaxHealth = player.attributes.getInstance(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)?.baseValue
            
            // 获取我们系统的血量
            val currentHealth = AttributeManager.getAttributeValue(player, CoreAttributes.CURRENT_HEALTH)
            val maxHealth = AttributeManager.getAttributeValue(player, CoreAttributes.MAX_HEALTH)
            
            // 计算血量比例
            val healthRatio = (currentHealth / maxHealth).coerceIn(0.0, 1.0)
            
            // 计算要显示的血量（按比例缩放到20点血量内）
            val displayHealth = (healthRatio * VANILLA_MAX_HEALTH).toFloat()
            val displayMaxHealth = VANILLA_MAX_HEALTH.toFloat()
            
            // 临时修改玩家的显示血量
            player.health = displayHealth
            player.attributes.getInstance(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)?.let { instance ->
                instance.baseValue = displayMaxHealth.toDouble()
            }
        } catch (e: Exception) {
            LOGGER.error("处理血量显示时发生错误: {}", e.message, e)
        }
    }
    
    /**
     * 恢复原始血量值
     */
    private fun restoreOriginalValues(player: Player) {
        try {
            originalHealth?.let { health ->
                player.health = health
            }
            originalMaxHealth?.let { maxHealth ->
                player.attributes.getInstance(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)?.let { instance ->
                    instance.baseValue = maxHealth
                }
            }
            // 清除缓存
            originalHealth = null
            originalMaxHealth = null
        } catch (e: Exception) {
            LOGGER.error("恢复原始血量值时发生错误: {}", e.message, e)
        }
    }
    
    /**
     * 获取血量显示信息（用于调试）
     */
    fun getHealthDisplayInfo(player: Player): String {
        val currentHealth = AttributeManager.getAttributeValue(player, CoreAttributes.CURRENT_HEALTH)
        val maxHealth = AttributeManager.getAttributeValue(player, CoreAttributes.MAX_HEALTH)
        val healthRatio = if (maxHealth > 0) currentHealth / maxHealth else 0.0
        val heartsCount = (healthRatio * MAX_HEARTS_DISPLAY)
        
        return "血量: %.1f/%.1f (%.1f颗心)".format(currentHealth, maxHealth, heartsCount)
    }
} 