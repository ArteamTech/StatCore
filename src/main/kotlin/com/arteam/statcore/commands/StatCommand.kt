package com.arteam.statcore.commands

import com.arteam.statcore.attributes.CoreAttributes
import com.arteam.statcore.core.attributes.AttributeManager
import com.arteam.statcore.core.sync.ImmediateSyncManager
import com.arteam.statcore.debug.AttributeDebugTool
import com.arteam.statcore.client.HealthDisplayManager
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player

/**
 * /stat 命令实现
 * 用于查看玩家或其他生物的属性信息
 */
object StatCommand {
    
    /**
     * 注册命令
     */
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("stat")
                .requires { source -> source.hasPermission(0) } // 任何玩家都可以使用
                .executes { context -> 
                    // /stat - 显示执行者自己的属性
                    showOwnStats(context)
                }
                .then(
                    Commands.argument("target", EntityArgument.entity())
                        .requires { source -> source.hasPermission(2) } // 需要管理员权限查看其他实体
                        .executes { context ->
                            // /stat <target> - 显示指定实体的属性
                            showTargetStats(context)
                        }
                )
                .then(
                    Commands.literal("debug")
                        .requires { source -> source.hasPermission(2) } // 需要管理员权限
                        .executes { context ->
                            // /stat debug - 显示详细调试信息
                            showOwnDebugStats(context)
                        }
                        .then(
                            Commands.argument("target", EntityArgument.entity())
                                .executes { context ->
                                    // /stat debug <target> - 显示目标的详细调试信息
                                    showTargetDebugStats(context)
                                }
                        )
                )
                .then(
                    Commands.literal("sync")
                        .requires { source -> source.hasPermission(2) } // 需要管理员权限
                        .executes { context ->
                            // /stat sync - 强制同步执行者的属性
                            forceSyncOwnAttributes(context)
                        }
                        .then(
                            Commands.argument("target", EntityArgument.entity())
                                .executes { context ->
                                    // /stat sync <target> - 强制同步指定实体的属性
                                    forceSyncTargetAttributes(context)
                                }
                        )
                )
                .then(
                    Commands.literal("test")
                        .requires { source -> source.hasPermission(2) } // 需要管理员权限
                        .then(
                            Commands.argument("target", EntityArgument.entity())
                                .executes { context ->
                                    // /stat test <target> - 显示指定实体的详细测试信息
                                    showTestInfo(context)
                                }
                        )
                )
        )
    }
    
    /**
     * 显示执行者自己的属性
     */
    private fun showOwnStats(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val entity = source.entity
        
        if (entity !is LivingEntity) {
            source.sendFailure(Component.translatable("command.statcore.stat.not_living_entity"))
            return 0
        }
        
        return showEntityStats(source, entity)
    }
    
    /**
     * 显示指定目标的属性
     */
    private fun showTargetStats(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val target = EntityArgument.getEntity(context, "target")
        
        if (target !is LivingEntity) {
            source.sendFailure(Component.translatable("command.statcore.stat.target_not_living"))
            return 0
        }
        
        return showEntityStats(source, target)
    }
    
    /**
     * 显示实体的属性信息
     */
    private fun showEntityStats(source: CommandSourceStack, entity: LivingEntity): Int {
        val attributeMap = AttributeManager.getAttributeMap(entity)
        val applicableAttributes = AttributeManager.registry.getApplicableAttributes(entity)
        
        // 发送标题信息
        val entityName = if (entity is Player) {
            entity.scoreboardName
        } else {
            entity.displayName?.string
        }
        
        source.sendSuccess(
            {
                Component.translatable(
                    "command.statcore.stat.header",
                    entityName
                )
            },
            false
        )
        
        if (applicableAttributes.isEmpty()) {
            source.sendSuccess(
                {
                    Component.translatable("command.statcore.stat.no_attributes")
                },
                false
            )
            return 1
        }
        
        // 按命名空间分组显示属性
        val attributesByNamespace = applicableAttributes.groupBy { it.id.namespace }
        
        for ((namespace, attributes) in attributesByNamespace.toSortedMap()) {
            // 命名空间标题
            source.sendSuccess(
                {
                    Component.translatable(
                        "command.statcore.stat.namespace_header",
                        namespace
                    )
                },
                false
            )
            
            // 显示该命名空间下的所有属性
            for (attribute in attributes.sortedBy { it.id.path }) {
                val instance = attributeMap.getInstance(attribute)
                val baseValue = instance?.baseValue ?: attribute.defaultValue
                val finalValue = instance?.getValue() ?: attribute.defaultValue
                val modifierCount = instance?.getModifiers()?.size ?: 0
                
                // 格式化数值显示
                val baseStr = formatAttributeValue(baseValue)
                val finalStr = formatAttributeValue(finalValue)
                
                val messageKey = if (modifierCount > 0) {
                    "command.statcore.stat.attribute_with_modifiers"
                } else {
                    "command.statcore.stat.attribute_simple"
                }
                
                source.sendSuccess(
                    {
                        if (modifierCount > 0) {
                            Component.translatable(
                                messageKey,
                                Component.translatable(attribute.getTranslationKey()),
                                baseStr,
                                finalStr,
                                modifierCount
                            )
                        } else {
                            Component.translatable(
                                messageKey,
                                Component.translatable(attribute.getTranslationKey()),
                                finalStr
                            )
                        }
                    },
                    false
                )
            }
        }
        
        // 显示统计信息
        source.sendSuccess(
            {
                Component.translatable(
                    "command.statcore.stat.summary",
                    applicableAttributes.size,
                    AttributeManager.getManagedEntityCount()
                )
            },
            false
        )
        
        return 1
    }
    
    /**
     * 显示执行者自己的调试属性
     */
    private fun showOwnDebugStats(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val entity = source.entity
        
        if (entity !is LivingEntity) {
            source.sendFailure(Component.translatable("command.statcore.stat.not_living_entity"))
            return 0
        }
        
        return showEntityDebugStats(source, entity)
    }
    
    /**
     * 显示指定目标的调试属性
     */
    private fun showTargetDebugStats(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val target = EntityArgument.getEntity(context, "target")
        
        if (target !is LivingEntity) {
            source.sendFailure(Component.translatable("command.statcore.stat.target_not_living"))
            return 0
        }
        
        return showEntityDebugStats(source, target)
    }
    
    /**
     * 显示实体的详细调试信息
     */
    private fun showEntityDebugStats(source: CommandSourceStack, entity: LivingEntity): Int {
        val debugReport = AttributeDebugTool.generateDetailedReport(entity)
        
        // 发送所有调试信息
        debugReport.forEach { component ->
            source.sendSuccess({ component }, false)
        }
        
        // 如果是玩家，添加血量显示信息
        if (entity is Player) {
            val healthDisplayInfo = HealthDisplayManager.getHealthDisplayInfo(entity)
            source.sendSuccess({ Component.literal("血量显示: $healthDisplayInfo")
                .withStyle(net.minecraft.ChatFormatting.AQUA) }, false)
        }
        
        return 1
    }
    
    /**
     * 强制同步执行者自己的属性
     */
    private fun forceSyncOwnAttributes(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val entity = source.entity
        
        if (entity !is LivingEntity) {
            source.sendFailure(Component.translatable("command.statcore.stat.not_living_entity"))
            return 0
        }
        
        return forceSyncEntityAttributes(source, entity)
    }
    
    /**
     * 强制同步指定目标的属性
     */
    private fun forceSyncTargetAttributes(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val target = EntityArgument.getEntity(context, "target")
        
        if (target !is LivingEntity) {
            source.sendFailure(Component.translatable("command.statcore.stat.target_not_living"))
            return 0
        }
        
        return forceSyncEntityAttributes(source, target)
    }
    
    /**
     * 强制同步实体属性
     */
    private fun forceSyncEntityAttributes(source: CommandSourceStack, entity: LivingEntity): Int {
        val entityName = if (entity is Player) {
            entity.scoreboardName
        } else {
            entity.displayName?.string
        }
        
        try {
            // 执行强制同步
            ImmediateSyncManager.forceSyncAll(entity)
            
            source.sendSuccess({
                Component.literal("已强制同步实体 $entityName 的所有属性")
                    .withStyle(net.minecraft.ChatFormatting.GREEN)
            }, false)
            
            return 1
        } catch (e: Exception) {
            source.sendFailure(
                Component.literal("同步属性时发生错误: ${e.message}")
                    .withStyle(net.minecraft.ChatFormatting.RED)
            )
            return 0
        }
    }
    
    /**
     * 显示实体的测试信息
     */
    private fun showTestInfo(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val target = EntityArgument.getEntity(context, "target")
        
        if (target !is LivingEntity) {
            source.sendFailure(Component.translatable("command.statcore.stat.target_not_living"))
            return 0
        }
        
        val entityName = if (target is Player) {
            target.scoreboardName
        } else {
            target.displayName?.string ?: target.type.description.string
        }
        
        // 获取血量信息
        val vanillaCurrentHealth = target.health.toDouble()
        val vanillaMaxHealth = target.maxHealth.toDouble()
        val statCoreCurrentHealth = AttributeManager.getAttributeValue(target, CoreAttributes.CURRENT_HEALTH)
        val statCoreMaxHealth = AttributeManager.getAttributeValue(target, CoreAttributes.MAX_HEALTH)
        
        // 获取防御信息
        val vanillaArmor = target.armorValue.toDouble()
        val statCoreDefense = AttributeManager.getAttributeValue(target, CoreAttributes.PHYSICAL_DEFENSE)
        
        source.sendSuccess({
            Component.literal("=== 实体测试信息: $entityName ===")
                .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD)
        }, false)
        
        source.sendSuccess({
            Component.literal("实体类型: ${target.type.description.string}")
                .withStyle(net.minecraft.ChatFormatting.GRAY)
        }, false)
        
        source.sendSuccess({
            Component.literal("UUID: ${target.uuid}")
                .withStyle(net.minecraft.ChatFormatting.GRAY)
        }, false)
        
        // 血量对比
        source.sendSuccess({
            Component.literal("--- 血量对比 ---")
                .withStyle(net.minecraft.ChatFormatting.RED)
        }, false)
        
        source.sendSuccess({
            Component.literal(String.format("原版血量: %.1f / %.1f", vanillaCurrentHealth, vanillaMaxHealth))
                .withStyle(net.minecraft.ChatFormatting.WHITE)
        }, false)
        
        source.sendSuccess({
            Component.literal(String.format("StatCore血量: %.1f / %.1f", statCoreCurrentHealth, statCoreMaxHealth))
                .withStyle(net.minecraft.ChatFormatting.AQUA)
        }, false)
        
        // 缩放比例检查
        val expectedMaxHealth = if (target is Player) {
            com.arteam.statcore.StatCore.PLAYER_DEFAULT_MAX_HEALTH
        } else {
            vanillaMaxHealth * com.arteam.statcore.StatCore.VANILLA_TO_STATCORE_SCALE_FACTOR
        }
        
        val isCorrectlyScaled = kotlin.math.abs(statCoreMaxHealth - expectedMaxHealth) < 0.1
        val scaleStatus = if (isCorrectlyScaled) "✓ 正确" else "✗ 错误"
        val scaleColor = if (isCorrectlyScaled) net.minecraft.ChatFormatting.GREEN else net.minecraft.ChatFormatting.RED
        
        source.sendSuccess({
            Component.literal(String.format("预期最大血量: %.1f (%s)", expectedMaxHealth, scaleStatus))
                .withStyle(scaleColor)
        }, false)
        
        // 防御对比
        source.sendSuccess({
            Component.literal("--- 防御对比 ---")
                .withStyle(net.minecraft.ChatFormatting.BLUE)
        }, false)
        
        source.sendSuccess({
            Component.literal(String.format("原版护甲: %.1f", vanillaArmor))
                .withStyle(net.minecraft.ChatFormatting.WHITE)
        }, false)
        
        source.sendSuccess({
            Component.literal(String.format("StatCore物理防御: %.1f", statCoreDefense))
                .withStyle(net.minecraft.ChatFormatting.AQUA)
        }, false)
        
        val expectedDefense = vanillaArmor * com.arteam.statcore.StatCore.VANILLA_TO_STATCORE_SCALE_FACTOR
        val isDefenseCorrect = kotlin.math.abs(statCoreDefense - expectedDefense) < 0.1
        val defenseStatus = if (isDefenseCorrect) "✓ 正确" else "✗ 错误"
        val defenseColor = if (isDefenseCorrect) net.minecraft.ChatFormatting.GREEN else net.minecraft.ChatFormatting.RED
        
        source.sendSuccess({
            Component.literal(String.format("预期物理防御: %.1f (%s)", expectedDefense, defenseStatus))
                .withStyle(defenseColor)
        }, false)
        
        return 1
    }
    
    /**
     * 格式化属性值显示
     */
    private fun formatAttributeValue(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            // 整数值
            value.toLong().toString()
        } else {
            // 小数值，保留2位小数
            String.format("%.2f", value)
        }
    }
}