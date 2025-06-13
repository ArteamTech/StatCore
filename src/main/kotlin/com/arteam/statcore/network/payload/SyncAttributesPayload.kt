package com.arteam.statcore.network.payload

import com.arteam.statcore.StatCore
import com.arteam.statcore.api.attributes.AttributeOperation
import com.arteam.statcore.api.attributes.AttributeModifier
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import java.util.*

/**
 * 服务器 -> 客户端
 * 同步实体全部属性数据（仅当前玩家使用）
 */
@Suppress("unused")
class SyncAttributesPayload(
    val entries: List<AttributeEntry>
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    /** 单个属性的数据 */
    data class AttributeEntry(
        val id: ResourceLocation,
        val baseValue: Double,
        val modifiers: List<ModifierData>
    )

    /** 修改器序列化数据 */
    data class ModifierData(
        val id: UUID,
        val name: String,
        val amount: Double,
        val operation: Byte,
        val priority: Int,
        val source: ResourceLocation?
    ) {
        fun toModifier(): AttributeModifier {
            val op = if (operation.toInt() == 0) AttributeOperation.ADDITION else AttributeOperation.MULTIPLY
            return AttributeModifier(id, name, amount, op, priority, source)
        }

        companion object {
            fun from(mod: AttributeModifier): ModifierData {
                return ModifierData(
                    mod.id,
                    mod.name,
                    mod.amount,
                    mod.operation.ordinal.toByte(),
                    mod.priority,
                    mod.source
                )
            }
        }
    }

    companion object {
        /** 包唯一标识符 */
        val TYPE: CustomPacketPayload.Type<SyncAttributesPayload> = CustomPacketPayload.Type(
            ResourceLocation.fromNamespaceAndPath(StatCore.ID, "sync_attributes")
        )

        /** StreamCodec 实现 */
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SyncAttributesPayload> = object : StreamCodec<RegistryFriendlyByteBuf, SyncAttributesPayload> {
            override fun encode(buffer: RegistryFriendlyByteBuf, value: SyncAttributesPayload) {
                buffer.writeVarInt(value.entries.size)
                for (entry in value.entries) {
                    buffer.writeResourceLocation(entry.id)
                    buffer.writeDouble(entry.baseValue)
                    buffer.writeVarInt(entry.modifiers.size)
                    for (mod in entry.modifiers) {
                        buffer.writeUUID(mod.id)
                        buffer.writeUtf(mod.name)
                        buffer.writeDouble(mod.amount)
                        buffer.writeByte(mod.operation.toInt())
                        buffer.writeVarInt(mod.priority)
                        buffer.writeBoolean(mod.source != null)
                        mod.source?.let { buffer.writeResourceLocation(it) }
                    }
                }
            }

            override fun decode(buffer: RegistryFriendlyByteBuf): SyncAttributesPayload {
                val entryCount = buffer.readVarInt()
                val entries = ArrayList<AttributeEntry>(entryCount)
                repeat(entryCount) {
                    val id = buffer.readResourceLocation()
                    val base = buffer.readDouble()
                    val modCount = buffer.readVarInt()
                    val mods = ArrayList<ModifierData>(modCount)
                    repeat(modCount) {
                        val uuid = buffer.readUUID()
                        val name = buffer.readUtf()
                        val amount = buffer.readDouble()
                        val op = buffer.readByte()
                        val prio = buffer.readVarInt()
                        val hasSource = buffer.readBoolean()
                        val src = if (hasSource) buffer.readResourceLocation() else null
                        mods.add(ModifierData(uuid, name, amount, op, prio, src))
                    }
                    entries.add(AttributeEntry(id, base, mods))
                }
                return SyncAttributesPayload(entries)
            }
        }
    }
} 