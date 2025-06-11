package com.arteam.statcore.attributes

/**
 * StatCore 核心属性统一入口
 * 集合所有属性定义的访问点
 */
object CoreAttributes {
    
    // 生命值相关属性
    val MAX_HEALTH = HealthAttributes.MAX_HEALTH
    val CURRENT_HEALTH = HealthAttributes.CURRENT_HEALTH
    val HEALTH_REGENERATION = HealthAttributes.HEALTH_REGENERATION
    
    // 防御相关属性
    val PHYSICAL_DEFENSE = DefenseAttributes.PHYSICAL_DEFENSE
    val PROJECTILE_DEFENSE = DefenseAttributes.PROJECTILE_DEFENSE
    val EXPLOSION_DEFENSE = DefenseAttributes.EXPLOSION_DEFENSE
    val FIRE_DEFENSE = DefenseAttributes.FIRE_DEFENSE
    val TRUE_DEFENSE = DefenseAttributes.TRUE_DEFENSE
    
    /**
     * 获取所有核心属性列表
     */
    fun getAllCoreAttributes() = listOf(
        // 生命值属性
        MAX_HEALTH,
        CURRENT_HEALTH,
        HEALTH_REGENERATION,
        
        // 防御属性
        PHYSICAL_DEFENSE,
        PROJECTILE_DEFENSE,
        EXPLOSION_DEFENSE,
        FIRE_DEFENSE,
        TRUE_DEFENSE
    )
} 