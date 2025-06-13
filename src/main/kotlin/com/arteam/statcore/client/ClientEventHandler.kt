package com.arteam.statcore.client

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
@Suppress("unused")
object ClientEventHandler {
    
    private val LOGGER = LoggerFactory.getLogger("statcore.client")
    
    /**
     * 处理屏幕初始化事件
     * 用于确保GUI正确加载
     */
    @SubscribeEvent
    fun onScreenInit(event: ScreenEvent.Init.Post) {
        // TODO: 在这里添加GUI初始化逻辑
    }
    
    /**
     * 处理屏幕关闭事件
     * 用于清理资源
     */
    @SubscribeEvent
    fun onScreenClose(event: ScreenEvent.Closing) {
        // TODO: 在这里添加GUI关闭逻辑
    }
} 