package com.arteam.statcore.attributes

import com.arteam.statcore.StatCore
import com.arteam.statcore.core.attributes.BaseStatAttribute
import com.arteam.statcore.util.AttributeUtils

/**
 * 生命值相关属性定义
 */
object HealthAttributes {
    
    /**
     * 最大生命值属性
     * 玩家默认100，其他生物为原版生命值的5倍
     * 无最大值限制，可以无限增长
     */
    val MAX_HEALTH = BaseStatAttribute.universal(
        id = AttributeUtils.createStatCoreLocation("max_health"),
        defaultValue = StatCore.PLAYER_DEFAULT_MAX_HEALTH,  // 玩家默认100
        minValue = 1.0,
        maxValue = Double.POSITIVE_INFINITY  // 无最大值限制
    )
    
    /**
     * 当前生命值属性
     * 实体的当前生命值，用于与原版系统同步
     * 无最大值限制，可以无限增长
     */
    val CURRENT_HEALTH = BaseStatAttribute.universal(
        id = AttributeUtils.createStatCoreLocation("current_health"),
        defaultValue = StatCore.PLAYER_DEFAULT_MAX_HEALTH, // 默认满血
        minValue = 0.0,
        maxValue = Double.POSITIVE_INFINITY  // 无最大值限制
    )
    
    /**
     * 生命恢复速度属性
     * 每秒恢复的生命值
     */
    val HEALTH_REGENERATION = BaseStatAttribute.universal(
        id = AttributeUtils.createStatCoreLocation("health_regeneration"),
        defaultValue = 0.0,
        minValue = 0.0,
        maxValue = 100.0  // 恢复速度保持限制，避免过快恢复
    )
} 