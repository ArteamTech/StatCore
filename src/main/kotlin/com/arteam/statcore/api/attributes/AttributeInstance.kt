package com.arteam.statcore.api.attributes

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 属性实例
 * 存储特定实体的属性值和修改器
 */
@Suppress("unused")
class AttributeInstance(
    /**
     * 关联的属性定义
     */
    val attribute: StatAttribute,
    
    /**
     * 拥有此属性实例的实体
     */
    val owner: LivingEntity
) {
    /**
     * 基础属性值（不包含修改器）
     */
    var baseValue: Double = attribute.defaultValue
        set(value) {
            field = attribute.clampValue(value)
            markDirty()
        }
    
    /**
     * 所有修改器的映射表 (UUID -> AttributeModifier)
     */
    private val modifiers = ConcurrentHashMap<UUID, AttributeModifier>()
    
    /**
     * 缓存的计算结果
     */
    private var cachedValue: Double? = null
    
    /**
     * 标记是否需要重新计算
     */
    private var isDirty = true
    
    /**
     * 添加修改器
     * @param modifier 要添加的修改器
     */
    fun addModifier(modifier: AttributeModifier) {
        modifiers[modifier.id] = modifier
        markDirty()
    }
    
    /**
     * 移除修改器
     * @param modifierId 要移除的修改器ID
     * @return 如果成功移除返回true
     */
    fun removeModifier(modifierId: UUID): Boolean {
        val removed = modifiers.remove(modifierId) != null
        if (removed) {
            markDirty()
        }
        return removed
    }
    
    /**
     * 根据来源移除所有修改器
     * @param source 修改器来源
     * @return 移除的修改器数量
     */
    fun removeModifiersBySource(source: ResourceLocation): Int {
        val toRemove = modifiers.values.filter { it.source == source }
        var removed = 0
        for (modifier in toRemove) {
            if (removeModifier(modifier.id)) {
                removed++
            }
        }
        return removed
    }
    
    /**
     * 获取指定ID的修改器
     * @param modifierId 修改器ID
     * @return 对应的修改器，如果不存在则返回null
     */
    fun getModifier(modifierId: UUID): AttributeModifier? {
        return modifiers[modifierId]
    }
    
    /**
     * 获取所有修改器
     * @return 所有修改器的集合
     */
    fun getModifiers(): Collection<AttributeModifier> {
        return modifiers.values.toList()
    }
    
    /**
     * 获取指定来源的所有修改器
     * @param source 修改器来源
     * @return 来自指定来源的修改器列表
     */
    fun getModifiersBySource(source: ResourceLocation): List<AttributeModifier> {
        return modifiers.values.filter { it.source == source }
    }
    
    /**
     * 检查是否存在指定ID的修改器
     * @param modifierId 修改器ID
     * @return 如果存在返回true
     */
    fun hasModifier(modifierId: UUID): Boolean {
        return modifiers.containsKey(modifierId)
    }
    
    /**
     * 清空所有修改器
     */
    fun clearModifiers() {
        modifiers.clear()
        markDirty()
    }
    
    /**
     * 获取计算后的最终属性值
     * @return 应用所有修改器后的属性值
     */
    fun getValue(): Double {
        if (isDirty || cachedValue == null) {
            cachedValue = calculateValue()
            isDirty = false
        }
        return cachedValue!!
    }
    
    /**
     * 设置目标属性值
     * 通过反向计算调整基础值来达到目标值
     * 会考虑当前存在的所有修改器
     * @param targetValue 目标属性值
     */
    fun setValue(targetValue: Double) {
        val modifiers = getModifiers().toList()
        if (modifiers.isEmpty()) {
            // 没有修改器，直接设置基础值
            baseValue = targetValue
            return
        }
        
        // 计算加法和乘法修改器的总和
        val additionSum = modifiers
            .filter { it.operation == AttributeOperation.ADDITION }
            .sumOf { it.amount }
        val multiplySum = modifiers
            .filter { it.operation == AttributeOperation.MULTIPLY }
            .sumOf { it.amount }
        
        // 反向计算基础值：base = (target / (1 + mult)) - add
        val requiredBaseValue = (targetValue / (1.0 + multiplySum)) - additionSum
        baseValue = requiredBaseValue
    }
    
    /**
     * 强制设置属性值（通过基础值）
     * 忽略所有修改器，直接通过调整基础值来强制设定属性的最终值
     * 警告：此方法会完全忽略已存在的修改器
     * @param value 要强制设置的值
     */
    fun forceSetValueByBase(value: Double) {
        baseValue = value
    }
    
    /**
     * 计算属性值（不使用缓存）
     * @return 计算后的属性值
     */
    private fun calculateValue(): Double {
        return attribute.calculate(baseValue, getModifiers().toList())
    }
    
    /**
     * 标记为需要重新计算
     */
    private fun markDirty() {
        isDirty = true
        cachedValue = null
    }
    
    /**
     * 重置到默认状态
     */
    fun reset() {
        baseValue = attribute.defaultValue
        clearModifiers()
    }
    
    /**
     * 获取属性的本地化名称
     */
    fun getDisplayName(): String {
        return attribute.getTranslationKey()
    }
    
    override fun toString(): String {
        return "AttributeInstance(attribute=${attribute.id}, baseValue=$baseValue, finalValue=${getValue()}, modifiers=${modifiers.size})"
    }
} 