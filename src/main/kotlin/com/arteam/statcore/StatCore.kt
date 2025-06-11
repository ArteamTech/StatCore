package com.arteam.statcore

import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.commands.StatCommand
import com.arteam.statcore.core.registry.AttributeRegistryImpl
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import org.slf4j.LoggerFactory

/**
 * StatCore 主模组类
 * 属性系统重构模组，提供独立的属性计算系统
 */
@Mod("statcore")
@Suppress("unused")
class StatCore {
    
    companion object {
        const val ID = "statcore"
        
        /**
         * 原版系统到StatCore系统的缩放因子
         * 用于将原版20点生命值扩展到100点，护甲值等比例缩放
         */
        const val VANILLA_TO_STATCORE_SCALE_FACTOR = 5.0
        
        /**
         * 玩家默认最大生命值
         */
        const val PLAYER_DEFAULT_MAX_HEALTH = 100.0
        
        /**
         * 原版系统支持的最大生命值限制
         * 防止设置过大的值导致原版系统异常
         */
        const val VANILLA_MAX_HEALTH_LIMIT = 1024.0
        
        /**
         * 推荐的属性最大值
         * 虽然属性可以无限大，但过大的值可能影响游戏体验
         */
        const val RECOMMENDED_ATTRIBUTE_MAX_VALUE = 10000.0
        
        /**
         * 安全的无限大替代值
         * 在某些情况下需要一个"安全"的大值作为无限大的替代
         */
        const val SAFE_INFINITY_VALUE = 1000000.0
        
        private val LOGGER = LoggerFactory.getLogger(ID)
    }
    
    constructor(modEventBus: IEventBus) {
        LOGGER.info("StatCore 模组正在初始化...")
        
        // 注册设置事件
        modEventBus.addListener(this::commonSetup)
        
        // 注册命令事件
        NeoForge.EVENT_BUS.addListener(this::registerCommands)
    }
    
    /**
     * 通用设置阶段
     * 注册所有自定义属性
     */
    private fun commonSetup(event: FMLCommonSetupEvent) {
        LOGGER.info("StatCore 正在进行通用设置...")
        
        // 注册所有核心属性
        val attributeRegistry = AttributeRegistryImpl
        CoreAttributes.getAllCoreAttributes().forEach { attribute ->
            attributeRegistry.register(attribute)
            LOGGER.debug("已注册属性: {}", attribute.id)
        }
        
        LOGGER.info("StatCore 核心属性注册完成，共注册了 ${CoreAttributes.getAllCoreAttributes().size} 个属性")
    }
    
    /**
     * 注册命令
     */
    private fun registerCommands(event: RegisterCommandsEvent) {
        LOGGER.info("StatCore 正在注册命令...")
        
        StatCommand.register(event.dispatcher)
        
        LOGGER.info("StatCore 命令注册完成")
    }
}
