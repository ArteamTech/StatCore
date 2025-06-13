package com.arteam.statcore.network

import com.arteam.statcore.StatCore
import com.arteam.statcore.core.attributes.AttributeManager
import com.arteam.statcore.network.payload.RequestAttributesPayload
import com.arteam.statcore.network.payload.SyncAttributesPayload
import net.minecraft.world.entity.player.Player
import net.minecraft.client.Minecraft
import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.registration.PayloadRegistrar
import org.slf4j.LoggerFactory

/**
 * 处理自定义网络包的注册与处理逻辑
 */
@EventBusSubscriber(modid = StatCore.ID, bus = EventBusSubscriber.Bus.MOD)
@Suppress("unused")
object NetworkHandler {

    private val LOGGER = LoggerFactory.getLogger("statcore.network")

    @SubscribeEvent
    fun onRegisterPayloads(event: RegisterPayloadHandlersEvent) {
        // 获取 registrar，使用模组ID作为命名空间，并设置版本号 1
        val registrar: PayloadRegistrar = event.registrar(StatCore.ID).versioned("1")

        // 客户端 -> 服务器：请求同步
        registrar.playToServer(
            RequestAttributesPayload.TYPE,
            RequestAttributesPayload.STREAM_CODEC
        ) { payload, context ->
            handleRequestAttributes(payload, context.player())
        }

        // 服务器 -> 客户端：同步属性
        registrar.playToClient(
            SyncAttributesPayload.TYPE,
            SyncAttributesPayload.STREAM_CODEC
        ) { payload, _ ->
            handleSyncAttributes(payload)
        }

        LOGGER.info("已注册 StatCore 网络数据包")
    }

    /**
     * 服务器端：处理来自客户端的属性同步请求
     */
    private fun handleRequestAttributes(@Suppress("UNUSED_PARAMETER") payload: RequestAttributesPayload, player: Player?) {
        if (player == null || player.level().isClientSide) return

        try {
            // 收集玩家的属性数据
            val attributeMap = AttributeManager.getAttributeMap(player)
            val entries = attributeMap.getAllInstances().map { instance ->
                val modifierDataList = instance.getModifiers().map { SyncAttributesPayload.ModifierData.from(it) }
                SyncAttributesPayload.AttributeEntry(instance.attribute.id, instance.baseValue, modifierDataList)
            }
            val syncPayload = SyncAttributesPayload(entries)

            // 发送到客户端
            PacketDistributor.sendToPlayer(player as ServerPlayer, syncPayload)

        } catch (e: Exception) {
            LOGGER.error("同步玩家属性时发生错误: {}", e.message, e)
        }
    }

    /**
     * 客户端：接收服务器发送的属性数据并更新本地缓存
     */
    @Suppress("MemberVisibilityCanBePrivate")
    private fun handleSyncAttributes(payload: SyncAttributesPayload) {
        // 仅在客户端执行
        val player = Minecraft.getInstance().player ?: return
        val attributeMap = AttributeManager.getAttributeMap(player)

        // 清空旧数据，重新写入
        attributeMap.clear()

        for (entry in payload.entries) {
            val attribute = AttributeManager.registry.getAttribute(entry.id)
            if (attribute == null) {
                LOGGER.warn("未知属性 {}，跳过", entry.id)
                continue
            }
            val instance = attributeMap.getOrCreateInstance(attribute)
            instance.baseValue = entry.baseValue
            // 清空并写入修改器
            instance.clearModifiers()
            entry.modifiers.forEach { instance.addModifier(it.toModifier()) }
        }
    }
} 