# StatCore 属性重构框架

## 概述

StatCore 是一个为 Minecraft 1.21.5 + NeoForge 21.5.75 设计的属性系统重构模组，提供了完全独立的属性计算框架，不依赖于原版属性系统。

## 核心特性

### 1. 独立属性系统
- **完全自主**：不依赖原版属性系统
- **精确控制**：细粒度的实体类型适用性控制
- **灵活计算**：统一的计算公式加上自定义计算支持
- **线程安全**：使用 ConcurrentHashMap 确保并发安全

### 2. 实体类型精确控制
```kotlin
// 仅对玩家有效
val playerAttribute = BaseStatAttribute.playerOnly(id, defaultValue, minValue, maxValue)

// 仅对战斗类实体有效（玩家+怪物）
val combatAttribute = BaseStatAttribute.combatOnly(id, defaultValue, minValue, maxValue)

// 自定义实体类型范围
val customAttribute = BaseStatAttribute.custom(
    id = id,
    allowedTypes = setOf(EntityCategory.ANIMAL, EntityCategory.NPC)
)
```

#### 支持的实体类型
- **PLAYER** - 玩家
- **MONSTER** - 怪物（能造成伤害的敌对生物）
- **ANIMAL** - 动物（和平生物）
- **NPC** - NPC（村民等）
- **OTHER** - 其他生物

### 3. 简化的修改器系统
**仅保留两种操作类型**：
- **ADDITION**：加法运算
- **MULTIPLY**：乘法运算

**统一计算公式**：
```
最终值 = (基础值 + 所有加法修改器) × (1 + 所有乘法修改器)
```

**示例计算**：
```kotlin
// 基础暴击伤害: 20
// 加法修改器: +10 (装备), +5 (药水)
// 乘法修改器: +50% (技能), +25% (buff)
// 最终值 = (20 + 10 + 5) × (1 + 0.5 + 0.25) = 35 × 1.75 = 61.25
```

### 4. 自定义计算支持
开发者可以重写 `customCalculate` 方法实现特殊逻辑：

```kotlin
val customAttribute = object : BaseStatAttribute(...) {
    override fun customCalculate(baseValue: Double, modifiers: List<AttributeModifier>): Double? {
        // 自定义计算逻辑
        // 返回 null 则使用默认计算
        return customResult
    }
}
```

### 5. 命令支持
- `/stat` - 查看自己的属性
- `/stat <target>` - 查看指定实体的属性（需要管理员权限）

## 架构组件

### API 层 (`api/`)
- `StatAttribute` - 属性定义接口
- `AttributeModifier` - 属性修改器（简化版）
- `AttributeInstance` - 属性实例
- `AttributeMap` - 实体属性映射
- `AttributeRegistry` - 属性注册接口

### 核心层 (`core/`)
- `BaseStatAttribute` - 基础属性实现
- `EntityCategory` - 实体类型枚举
- `AttributeManager` - 全局属性管理器
- `AttributeRegistryImpl` - 注册表实现

### 工具层 (`util/`)
- `AttributeUtils` - 属性操作工具类

### 命令层 (`commands/`)
- `StatCommand` - /stat 命令实现

## 使用示例

### 1. 定义不同类型的属性

```kotlin
// 仅对玩家有效的幸运属性
val PLAYER_LUCK = BaseStatAttribute.playerOnly(
    id = AttributeUtils.createStatCoreLocation("player_luck"),
    defaultValue = 0.0,
    minValue = -10.0,
    maxValue = 10.0
)

// 仅对战斗类实体有效的暴击伤害（玩家和怪物）
val CRIT_DAMAGE = BaseStatAttribute.combatOnly(
    id = AttributeUtils.createStatCoreLocation("crit_damage"),
    defaultValue = 0.0,
    minValue = 0.0,
    maxValue = 200.0
)

// 仅对动物有效的繁殖速度
val BREEDING_SPEED = BaseStatAttribute.custom(
    id = AttributeUtils.createStatCoreLocation("breeding_speed"),
    allowedTypes = setOf(EntityCategory.ANIMAL),
    defaultValue = 1.0
)
```

### 2. 自定义计算逻辑

```kotlin
val COMPOSITE_ARMOR = object : BaseStatAttribute(
    id = AttributeUtils.createStatCoreLocation("composite_armor"),
    allowedEntityTypes = setOf(EntityCategory.PLAYER, EntityCategory.MONSTER)
) {
    override fun customCalculate(baseValue: Double, modifiers: List<AttributeModifier>): Double? {
        // 特殊护甲公式实现
        val additionSum = modifiers.filter { it.operation == ADDITION }.sumOf { it.amount }
        val multiplySum = modifiers.filter { it.operation == MULTIPLY }.sumOf { it.amount }
        
        val totalArmor = baseValue + additionSum
        val penetration = multiplySum * 100
        
        return if (totalArmor + penetration > 0) {
            (totalArmor / (totalArmor + penetration)) * 100
        } else 0.0
    }
}
```

### 3. 注册和使用属性

```kotlin
// 注册属性
AttributeManager.registerAttribute(PLAYER_LUCK)
AttributeManager.registerAttribute(CRIT_DAMAGE)

// 获取属性值
val luckValue = AttributeUtils.getAttributeValue(player, PLAYER_LUCK)

// 添加修改器
val modifierId = AttributeUtils.addTemporaryModifier(
    entity = player,
    attribute = CRIT_DAMAGE,
    name = "装备加成",
    amount = 15.0,
    operation = AttributeOperation.ADDITION,
    source = AttributeUtils.createStatCoreLocation("equipment")
)
```

## 实体适用性控制

### 智能类型检查
框架会自动检查属性是否适用于特定实体：

```kotlin
// 这个属性只会影响怪物，动物不会受到影响
val DAMAGE_BONUS = BaseStatAttribute.custom(
    allowedTypes = setOf(EntityCategory.MONSTER)
)

// 动物调用此属性会返回默认值，不会应用任何修改器
val animalDamage = AttributeUtils.getAttributeValue(cow, DAMAGE_BONUS) // 返回默认值
val zombieDamage = AttributeUtils.getAttributeValue(zombie, DAMAGE_BONUS) // 正常计算
```

### 便捷方法
```kotlin
// 预定义的类型组合
BaseStatAttribute.playerOnly(...)     // 仅玩家
BaseStatAttribute.combatOnly(...)     // 玩家+怪物
BaseStatAttribute.entityOnly(...)     // 除玩家外的所有实体
BaseStatAttribute.universal(...)      // 所有实体
BaseStatAttribute.custom(allowedTypes) // 自定义组合
```

## 修改器叠加示例

### 多个修改器正确叠加
```kotlin
// 基础暴击伤害: 50
val baseCrit = 50.0

// 添加多个加法修改器
addModifier(weapon, "剑术精通", 20.0, ADDITION)    // +20
addModifier(weapon, "力量药水", 15.0, ADDITION)    // +15
addModifier(weapon, "装备属性", 10.0, ADDITION)    // +10

// 添加多个乘法修改器  
addModifier(weapon, "狂暴技能", 0.5, MULTIPLY)     // +50%
addModifier(weapon, "临时buff", 0.3, MULTIPLY)     // +30%

// 最终计算: (50 + 20 + 15 + 10) × (1 + 0.5 + 0.3) = 95 × 1.8 = 171
```

## 性能优化

### 1. 缓存机制
- 属性值计算结果会被缓存
- 只有在修改器变化时才重新计算
- 线程安全的缓存实现

### 2. 异步初始化
- 实体属性在加入世界时异步初始化
- 避免阻塞主线程
- 错误处理和日志记录

### 3. 精确适用性检查
- 避免为不相关实体创建属性实例
- 内存占用优化
- 计算性能提升

## 错误处理

### 1. 资源位置修复
使用 `ResourceLocation.fromNamespaceAndPath()` 替代已弃用的构造函数：

```kotlin
// 正确方式
val id = ResourceLocation.fromNamespaceAndPath("mymod", "my_attribute")

// 工具方法
val id = AttributeUtils.createStatCoreLocation("my_attribute")
```

### 2. 实体类型验证
```kotlin
// 自动检查实体类型适用性
if (!attribute.isApplicableTo(entity)) {
    return attribute.defaultValue // 返回默认值，不应用修改器
}
```

## 兼容性

### 1. 模组集成
```kotlin
// 获取框架实例
val manager = StatCore.getAttributeManager()
val registry = StatCore.getAttributeRegistry()

// 注册自定义属性
registry.register(myCustomAttribute)

// 检查实体适用性
val applicableAttributes = registry.getApplicableAttributes(entity)
```

### 2. 向后兼容
- 保持API稳定性
- 渐进式迁移支持
- 详细的变更日志

## 最佳实践

### 1. 属性设计
- 明确定义适用的实体类型
- 设置合理的数值范围
- 使用描述性的ID和名称

### 2. 修改器管理
- 为修改器设置来源标识
- 及时清理临时修改器
- 使用优先级控制显示顺序

### 3. 性能考虑
- 避免频繁的属性值查询
- 批量操作修改器
- 合理使用自定义计算

## 未来扩展

1. **数据包支持** - JSON 定义属性
2. **网络同步** - 客户端/服务端同步
3. **配置系统** - 运行时配置
4. **GUI 界面** - 可视化属性管理
5. **API 扩展** - 更多便捷方法

---

**StatCore 属性重构框架** - 精确、高效、灵活的 Minecraft 属性系统解决方案。 