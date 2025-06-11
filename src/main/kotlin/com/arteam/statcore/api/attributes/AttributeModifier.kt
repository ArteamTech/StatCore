package com.arteam.statcore.api.attributes

import net.minecraft.resources.ResourceLocation
import java.util.*

/**
 * 属性修改器操作类型
 */
enum class AttributeOperation {
    /**
     * 加法运算 (基础值 + 修改值)
     */
    ADDITION,
    
    /**
     * 乘法运算 (应用在最终计算中: (基础值 + 所有加法修改值) * (1 + 所有乘法修改值))
     */
    MULTIPLY
}

/**
 * 属性修改器
 * 用于修改实体的属性值
 * 计算公式: (基础值 + 所有加法修改值) * (1 + 所有乘法修改值)
 */
data class AttributeModifier(
    /**
     * 修改器的唯一标识符
     */
    val id: UUID,
    
    /**
     * 修改器的名称（用于调试和显示）
     */
    val name: String,
    
    /**
     * 修改器的值
     */
    val amount: Double,
    
    /**
     * 修改器的操作类型
     */
    val operation: AttributeOperation,
    
    /**
     * 修改器的优先级（数值越小优先级越高，主要用于排序显示）
     */
    val priority: Int = 0,
    
    /**
     * 修改器的来源（可选，用于调试和批量移除）
     */
    val source: ResourceLocation? = null
) {
    companion object {
        /**
         * 创建一个加法修改器
         */
        fun createAddition(name: String, amount: Double, priority: Int = 0, source: ResourceLocation? = null): AttributeModifier {
            return AttributeModifier(
                id = UUID.randomUUID(),
                name = name,
                amount = amount,
                operation = AttributeOperation.ADDITION,
                priority = priority,
                source = source
            )
        }
        
        /**
         * 创建一个乘法修改器
         */
        fun createMultiply(name: String, amount: Double, priority: Int = 0, source: ResourceLocation? = null): AttributeModifier {
            return AttributeModifier(
                id = UUID.randomUUID(),
                name = name,
                amount = amount,
                operation = AttributeOperation.MULTIPLY,
                priority = priority,
                source = source
            )
        }
    }
} 