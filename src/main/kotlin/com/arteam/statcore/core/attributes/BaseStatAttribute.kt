package com.arteam.statcore.core.attributes

import com.arteam.statcore.api.attributes.AttributeModifier
import com.arteam.statcore.api.attributes.AttributeOperation
import com.arteam.statcore.api.attributes.StatAttribute
import com.arteam.statcore.api.attributes.EntityCategory
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.npc.Npc
import net.minecraft.world.entity.player.Player

/**
 * 基础属性实现
 * 提供默认的属性计算逻辑和实体类型检查
 */
@Suppress("unused")
open class BaseStatAttribute(
    override val id: ResourceLocation,
    override val defaultValue: Double = 0.0,
    override val minValue: Double = Double.NEGATIVE_INFINITY,
    override val maxValue: Double = Double.POSITIVE_INFINITY,
    override val isStackable: Boolean = true,
    /**
     * 允许的实体类型集合
     */
    val allowedEntityTypes: Set<EntityCategory> = setOf(EntityCategory.PLAYER, EntityCategory.MONSTER, EntityCategory.ANIMAL, EntityCategory.NPC, EntityCategory.OTHER)
) : StatAttribute {
    
    override fun getApplicableCategories(): Set<EntityCategory> {
        return allowedEntityTypes
    }
    
    override fun isApplicableTo(entity: LivingEntity): Boolean {
        val entityCategory = getEntityCategory(entity)
        return allowedEntityTypes.contains(entityCategory)
    }
    
    /**
     * 获取实体的类型分类
     */
    private fun getEntityCategory(entity: LivingEntity): EntityCategory {
        return when (entity) {
            is Player -> EntityCategory.PLAYER
            is Monster -> EntityCategory.MONSTER
            is Animal -> EntityCategory.ANIMAL
            is Npc -> EntityCategory.NPC
            else -> EntityCategory.OTHER
        }
    }
    
    /**
     * 计算应用修改器后的最终属性值
     * 公式: (基础值 + 所有加法修改值) * (1 + 所有乘法修改值)
     */
    override fun calculate(baseValue: Double, modifiers: List<AttributeModifier>): Double {
        if (modifiers.isEmpty()) {
            return clampValue(baseValue)
        }
        
        // 首先检查是否有自定义计算
        val customResult = customCalculate(baseValue, modifiers)
        if (customResult != null) {
            return clampValue(customResult)
        }
        
        // 按优先级排序修改器（主要用于显示顺序）
        val sortedModifiers = modifiers.sortedBy { it.priority }
        
        // 计算所有加法修改器的总和
        val additionSum = sortedModifiers
            .filter { it.operation == AttributeOperation.ADDITION }
            .sumOf { it.amount }
        
        // 计算所有乘法修改器的总和
        val multiplySum = sortedModifiers
            .filter { it.operation == AttributeOperation.MULTIPLY }
            .sumOf { it.amount }
        
        // 应用公式: (基础值 + 加法总和) * (1 + 乘法总和)
        val result = (baseValue + additionSum) * (1.0 + multiplySum)
        
        return clampValue(result)
    }
    
    companion object {
        /**
         * 创建一个仅对玩家有效的属性
         */
        fun playerOnly(
            id: ResourceLocation,
            defaultValue: Double = 0.0,
            minValue: Double = Double.NEGATIVE_INFINITY,
            maxValue: Double = Double.POSITIVE_INFINITY,
            isStackable: Boolean = true
        ): BaseStatAttribute {
            return BaseStatAttribute(
                id = id,
                defaultValue = defaultValue,
                minValue = minValue,
                maxValue = maxValue,
                isStackable = isStackable,
                allowedEntityTypes = setOf(EntityCategory.PLAYER)
            )
        }
        
        /**
         * 创建一个仅对战斗类实体有效的属性（玩家和怪物）
         */
        fun combatOnly(
            id: ResourceLocation,
            defaultValue: Double = 0.0,
            minValue: Double = Double.NEGATIVE_INFINITY,
            maxValue: Double = Double.POSITIVE_INFINITY,
            isStackable: Boolean = true
        ): BaseStatAttribute {
            return BaseStatAttribute(
                id = id,
                defaultValue = defaultValue,
                minValue = minValue,
                maxValue = maxValue,
                isStackable = isStackable,
                allowedEntityTypes = setOf(EntityCategory.PLAYER, EntityCategory.MONSTER)
            )
        }
        
        /**
         * 创建一个仅对非玩家实体有效的属性
         */
        fun entityOnly(
            id: ResourceLocation,
            defaultValue: Double = 0.0,
            minValue: Double = Double.NEGATIVE_INFINITY,
            maxValue: Double = Double.POSITIVE_INFINITY,
            isStackable: Boolean = true
        ): BaseStatAttribute {
            return BaseStatAttribute(
                id = id,
                defaultValue = defaultValue,
                minValue = minValue,
                maxValue = maxValue,
                isStackable = isStackable,
                allowedEntityTypes = setOf(EntityCategory.MONSTER, EntityCategory.ANIMAL, EntityCategory.NPC, EntityCategory.OTHER)
            )
        }
        
        /**
         * 创建一个对所有生物有效的属性
         */
        fun universal(
            id: ResourceLocation,
            defaultValue: Double = 0.0,
            minValue: Double = Double.NEGATIVE_INFINITY,
            maxValue: Double = Double.POSITIVE_INFINITY,
            isStackable: Boolean = true
        ): BaseStatAttribute {
            return BaseStatAttribute(
                id = id,
                defaultValue = defaultValue,
                minValue = minValue,
                maxValue = maxValue,
                isStackable = isStackable,
                allowedEntityTypes = setOf(EntityCategory.PLAYER, EntityCategory.MONSTER, EntityCategory.ANIMAL, EntityCategory.NPC, EntityCategory.OTHER)
            )
        }
        
        /**
         * 创建一个自定义实体类型范围的属性
         */
        fun custom(
            id: ResourceLocation,
            allowedTypes: Set<EntityCategory>,
            defaultValue: Double = 0.0,
            minValue: Double = Double.NEGATIVE_INFINITY,
            maxValue: Double = Double.POSITIVE_INFINITY,
            isStackable: Boolean = true
        ): BaseStatAttribute {
            return BaseStatAttribute(
                id = id,
                defaultValue = defaultValue,
                minValue = minValue,
                maxValue = maxValue,
                isStackable = isStackable,
                allowedEntityTypes = allowedTypes
            )
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StatAttribute) return false
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "BaseStatAttribute(id=$id, default=$defaultValue, min=$minValue, max=$maxValue, allowedTypes=$allowedEntityTypes)"
    }
} 