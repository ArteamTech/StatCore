package com.arteam.statcore.attributes

import com.arteam.statcore.core.attributes.BaseStatAttribute
import com.arteam.statcore.util.AttributeUtils
import net.minecraft.world.entity.LivingEntity

/**
 * 防御类型枚举
 * 定义各种防御类型，支持扩展
 */
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
     * 是否为基础防御类型（会被其他防御类型继承）
     */
    val isBase: Boolean = false
) {
    /**
     * 物理防御（基础防御类型）
     */
    PHYSICAL("physical", "defense.statcore.physical", true),
    
    /**
     * 弹射物防御
     */
    PROJECTILE("projectile", "defense.statcore.projectile"),
    
    /**
     * 爆炸防御
     */
    EXPLOSION("explosion", "defense.statcore.explosion"),
    
    /**
     * 火焰防御
     */
    FIRE("fire", "defense.statcore.fire"),
    
    /**
     * 真实防御（防御任何类型的伤害，基础防御类型）
     */
    TRUE_DEFENSE("true_defense", "defense.statcore.true_defense", true);
    
    companion object {
        /**
         * 根据ID获取防御类型
         */
        fun fromId(id: String): DefenseType? {
            return DefenseType.entries.find { it.id == id }
        }
        
        /**
         * 获取所有基础防御类型
         */
        fun getBaseTypes(): List<DefenseType> {
            return DefenseType.entries.filter { it.isBase }
        }
        
        /**
         * 获取指定防御类型的有效防御类型列表
         * 例如弹射物防御 = 弹射物防御 + 物理防御 + 真实防御
         */
        fun getEffectiveDefenseTypes(defenseType: DefenseType): List<DefenseType> {
            val types = mutableListOf<DefenseType>()
            
            // 添加自身
            types.add(defenseType)
            
            // 添加基础防御类型
            types.addAll(getBaseTypes())
            
            // 去重
            return types.distinct()
        }
    }
}

/**
 * 防御相关属性定义
 */
object DefenseAttributes {
    
    /**
     * 物理防御
     */
    val PHYSICAL_DEFENSE = BaseStatAttribute.universal(
        id = AttributeUtils.createStatCoreLocation("physical_defense"),
        defaultValue = 0.0,
        minValue = 0.0,
        maxValue = 1000.0
    )
    
    /**
     * 弹射物防御
     */
    val PROJECTILE_DEFENSE = BaseStatAttribute.universal(
        id = AttributeUtils.createStatCoreLocation("projectile_defense"),
        defaultValue = 0.0,
        minValue = 0.0,
        maxValue = 1000.0
    )
    
    /**
     * 爆炸防御
     */
    val EXPLOSION_DEFENSE = BaseStatAttribute.universal(
        id = AttributeUtils.createStatCoreLocation("explosion_defense"),
        defaultValue = 0.0,
        minValue = 0.0,
        maxValue = 1000.0
    )
    
    /**
     * 火焰防御
     */
    val FIRE_DEFENSE = BaseStatAttribute.universal(
        id = AttributeUtils.createStatCoreLocation("fire_defense"),
        defaultValue = 0.0,
        minValue = 0.0,
        maxValue = 1000.0
    )
    
    /**
     * 真实防御
     */
    val TRUE_DEFENSE = BaseStatAttribute.universal(
        id = AttributeUtils.createStatCoreLocation("true_defense"),
        defaultValue = 0.0,
        minValue = 0.0,
        maxValue = 1000.0
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