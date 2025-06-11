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
class StatCore {
    
    companion object {
        const val ID = "statcore"
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
