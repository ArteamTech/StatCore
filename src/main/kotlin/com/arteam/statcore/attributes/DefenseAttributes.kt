package com.arteam.statcore.attributes

import com.arteam.statcore.core.attributes.BaseStatAttribute
import com.arteam.statcore.util.AttributeUtils
import net.minecraft.world.entity.LivingEntity

/**
 * 防御类型枚举
 * 定义各种防御类型，支持扩展和继承
 */
@Suppress("unused")
enum class DefenseType(
    /**
     * 防御类型的ID
     */
    val id: String,
    
    /**
     * 防御类型的显示名称本地化键
     */
    val translationKey: String,
    
    /**
     * 父级防御类型（用于继承关系）
     */
    val parent: DefenseType? = null,
    
    /**
     * 是否为真实防御（真实防御对所有类型伤害都有效）
     */
    val isTrueDefense: Boolean = false
) {
    /**
     * 物理防御（基础防御类型）
     */
    PHYSICAL("physical", "defense.statcore.physical"),
    
    /**
     * 弹射物防御（继承物理防御）
     */
    PROJECTILE("projectile", "defense.statcore.projectile", parent = PHYSICAL),
    
    /**
     * 爆炸防御（独立防御类型，不继承物理防御）
     */
    EXPLOSION("explosion", "defense.statcore.explosion"),
    
    /**
     * 火焰防御（独立防御类型，不继承物理防御）
     */
    FIRE("fire", "defense.statcore.fire"),
    
    /**
     * 真实防御（防御任何类型的伤害）
     */
    TRUE_DEFENSE("true_defense", "defense.statcore.true_defense", isTrueDefense = true);
    
    companion object {
        /**
         * 根据ID获取防御类型
         */
        fun fromId(id: String): DefenseType? {
            return DefenseType.entries.find { it.id == id }
        }
        
        /**
         * 获取指定防御类型的有效防御类型列表
         * 基于继承关系，弹射物防御 = 弹射物防御 + 物理防御 + 真实防御
         * 火焰防御 = 火焰防御 + 真实防御（不继承物理防御）
         */
        fun getEffectiveDefenseTypes(defenseType: DefenseType): List<DefenseType> {
            val types = mutableSetOf<DefenseType>()
            
            // 递归添加当前类型及其所有父类型
            var current: DefenseType? = defenseType
            while (current != null) {
                types.add(current)
                current = current.parent
            }
            
            // 始终添加真实防御（对所有伤害类型都有效）
            types.add(TRUE_DEFENSE)
            
            return types.toList()
        }
        
        /**
         * 获取所有独立的防御类型（没有父类型的）
         */
        fun getRootDefenseTypes(): List<DefenseType> {
            return DefenseType.entries.filter { it.parent == null && !it.isTrueDefense }
        }
        
        /**
         * 获取所有派生的防御类型（有父类型的）
         */
        fun getDerivedDefenseTypes(): List<DefenseType> {
            return DefenseType.entries.filter { it.parent != null }
        }
    }
}

/**
 * 防御相关属性定义
 */
object DefenseAttributes {
    
    /**
     * 物理防御
     * 无最大值限制，可以无限增长
     */
    val PHYSICAL_DEFENSE = BaseStatAttribute.universal(
        id = AttributeUtils.createStatCoreLocation("physical_defense"),
        defaultValue = 0.0,
        minValue = 0.0,
        maxValue = Double.POSITIVE_INFINITY  // 无最大值限制
    )
    
    /**
     * 弹射物防御
     * 无最大值限制，可以无限增长
     */
    val PROJECTILE_DEFENSE = BaseStatAttribute.universal(
        id = AttributeUtils.createStatCoreLocation("projectile_defense"),
        defaultValue = 0.0,
        minValue = 0.0,
        maxValue = Double.POSITIVE_INFINITY  // 无最大值限制
    )
    
    /**
     * 爆炸防御
     * 无最大值限制，可以无限增长
     */
    val EXPLOSION_DEFENSE = BaseStatAttribute.universal(
        id = AttributeUtils.createStatCoreLocation("explosion_defense"),
        defaultValue = 0.0,
        minValue = 0.0,
        maxValue = Double.POSITIVE_INFINITY  // 无最大值限制
    )
    
    /**
     * 火焰防御
     * 无最大值限制，可以无限增长
     */
    val FIRE_DEFENSE = BaseStatAttribute.universal(
        id = AttributeUtils.createStatCoreLocation("fire_defense"),
        defaultValue = 0.0,
        minValue = 0.0,
        maxValue = Double.POSITIVE_INFINITY  // 无最大值限制
    )
    
    /**
     * 真实防御
     * 无最大值限制，可以无限增长
     */
    val TRUE_DEFENSE = BaseStatAttribute.universal(
        id = AttributeUtils.createStatCoreLocation("true_defense"),
        defaultValue = 0.0,
        minValue = 0.0,
        maxValue = Double.POSITIVE_INFINITY  // 无最大值限制
    )
    
    /**
     * 防御类型到属性的映射
     */
    private val defenseTypeToAttribute = mapOf(
        DefenseType.PHYSICAL to PHYSICAL_DEFENSE,
        DefenseType.PROJECTILE to PROJECTILE_DEFENSE,
        DefenseType.EXPLOSION to EXPLOSION_DEFENSE,
        DefenseType.FIRE to FIRE_DEFENSE,
        DefenseType.TRUE_DEFENSE to TRUE_DEFENSE
    )
    
    /**
     * 根据防御类型获取对应的属性
     */
    fun getDefenseAttribute(defenseType: DefenseType) = defenseTypeToAttribute[defenseType]
    
    /**
     * 获取所有防御属性
     */
    @Suppress("unused")
    fun getAllDefenseAttributes() = defenseTypeToAttribute.values.toList()
    
    /**
     * 计算特定防御类型的有效防御值
     * @param entity 目标实体
     * @param defenseType 防御类型
     * @return 有效防御值（累加了基础防御类型）
     */
    fun calculateEffectiveDefense(entity: LivingEntity, defenseType: DefenseType): Double {
        val effectiveTypes = DefenseType.getEffectiveDefenseTypes(defenseType)
        var totalDefense = 0.0
        
        for (type in effectiveTypes) {
            val attribute = getDefenseAttribute(type)
            if (attribute != null) {
                totalDefense += AttributeUtils.getAttributeValue(entity, attribute)
            }
        }
        
        return totalDefense
    }
    
    /**
     * 使用防御公式计算伤害减免
     * 公式：防御值 / (防御值 + 100)
     * @param defenseValue 防御值
     * @return 伤害减免比例 (0.0 到 1.0)
     */
    fun calculateDamageReduction(defenseValue: Double): Double {
        if (defenseValue <= 0) return 0.0
        return defenseValue / (defenseValue + 100.0)
    }
    
    /**
     * 计算特定防御类型的伤害减免
     * @param entity 目标实体
     * @param defenseType 防御类型
     * @return 伤害减免比例 (0.0 到 1.0)
     */
    fun calculateDamageReduction(entity: LivingEntity, defenseType: DefenseType): Double {
        val effectiveDefense = calculateEffectiveDefense(entity, defenseType)
        return calculateDamageReduction(effectiveDefense)
    }
} 