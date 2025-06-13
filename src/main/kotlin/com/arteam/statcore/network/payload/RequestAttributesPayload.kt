package com.arteam.statcore.network.payload

import com.arteam.statcore.StatCore
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.network.codec.StreamCodec

/**
 * 客户端 -> 服务器
 * 请求同步当前玩家的全部属性数据
 */
@Suppress("unused")
class RequestAttributesPayload() : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        /** 包唯一标识符 */
        val TYPE: CustomPacketPayload.Type<RequestAttributesPayload> = CustomPacketPayload.Type(
            ResourceLocation.fromNamespaceAndPath(StatCore.ID, "request_attributes")
        )

        /**
         * 该包没有任何内容，因此 StreamCodec 仅用于创建空实例
         */
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, RequestAttributesPayload> = object : StreamCodec<RegistryFriendlyByteBuf, RequestAttributesPayload> {
            override fun encode(buffer: RegistryFriendlyByteBuf, value: RequestAttributesPayload) {
                // 无数据需要写入
            }

            override fun decode(buffer: RegistryFriendlyByteBuf): RequestAttributesPayload {
                return RequestAttributesPayload()
            }
        }
    }
} 