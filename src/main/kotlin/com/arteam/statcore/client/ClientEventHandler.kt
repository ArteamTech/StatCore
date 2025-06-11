package com.arteam.statcore.client

import com.arteam.statcore.client.screen.AttributeScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ScreenEvent
import org.slf4j.LoggerFactory

/**
 * 客户端事件处理器
 * 处理GUI相关的客户端事件
 */
@EventBusSubscriber(modid = "statcore", bus = EventBusSubscriber.Bus.GAME, value = [Dist.CLIENT])
object ClientEventHandler {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.client")
    
    /**
     * 处理屏幕初始化事件
     * 用于确保GUI正确加载
     */
    @SubscribeEvent
    fun onScreenInit(event: ScreenEvent.Init.Post) {
        val screen = event.screen
        
        if (screen is InventoryScreen) {
            LOGGER.debug("物品栏界面已初始化，按钮应该已添加")
        } else if (screen is AttributeScreen) {
            LOGGER.debug("属性界面已初始化")
        }
    }
    
    /**
     * 处理屏幕关闭事件
     * 用于清理资源
     */
    @SubscribeEvent
    fun onScreenClose(event: ScreenEvent.Closing) {
        val screen = event.screen
        
        if (screen is AttributeScreen) {
            LOGGER.debug("属性界面已关闭")
        }
    }
} 