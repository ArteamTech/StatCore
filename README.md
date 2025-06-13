# StatCore

A flexible and powerful attribute system overhaul for Minecraft 1.21.5+, built on NeoForge.

---

## 📖 Overview

StatCore is a foundational mod designed to completely replace Minecraft's vanilla attribute system. It provides a more robust, intuitive, and extensible framework for managing entity stats like health, defense, and more. With a clean API and clear calculation logic, it serves as a powerful dependency for other mods and modpacks that require a sophisticated stats system.

## ✨ Core Features

-   **Complete Attribute Overhaul**: Say goodbye to the limitations of the vanilla system. StatCore implements its own logic from the ground up.
-   **Advanced Calculation Formula**: All attributes are calculated with a clear and predictable formula: `Final Value = (Base Value + Additive Modifiers) * (1 + Multiplicative Modifiers)`.
-   **Comprehensive Defense System**: Move beyond simple armor points. The system includes dedicated defenses for various damage types:
    -   Physical Defense
    -   Projectile Defense
    -   Explosion Defense
    -   Fire Defense
    -   True Defense (reduces all incoming damage)
-   **Scalable Health System**: Entity health is scaled up (default x5) to provide greater granularity for damage and healing calculations, with player health defaulting to 100.
-   **Clean Developer API**: A well-documented Kotlin API makes it easy for other mods to register their own attributes, add modifiers, and interact with the system.
-   **In-Game GUI**: A dedicated screen (accessible from the inventory) allows players to view their detailed stats and understand how different effects and equipment contribute to their attributes.
-   **Built-in Potion & Equipment Integration**: Seamlessly adapts vanilla effects (like Resistance, Health Boost) and equipment stats (like Armor) into its own system.

## 📦 Installation

1.  Ensure you have [NeoForge](https://neoforged.net/) for Minecraft 1.21.5 installed.
2.  Download the latest version of StatCore.
3.  Place the downloaded `.jar` file into your `mods` folder.
4.  Launch the game!

## 👨‍💻 For Developers (API Usage)

Integrating with StatCore is straightforward.

### Registering a new Attribute

```kotlin
// In your mod's initialization
val LIFESTEAL = BaseStatAttribute.universal(
    id = ResourceLocation.fromNamespaceAndPath("yourmodid", "lifesteal"),
    defaultValue = 0.0,
    minValue = 0.0,
    maxValue = 100.0 // Capped at 100%
)

// Register it
AttributeManager.registry.register(LIFESTEAL)
```

### Reading an Attribute Value

```kotlin
val entity: LivingEntity = ...
val lifestealChance = AttributeManager.getAttributeValue(entity, YourAttributes.LIFESTEAL)
```

### Adding/Removing Modifiers

```kotlin
// Add a temporary modifier from an item or effect
val modifierId = AttributeUtils.addTemporaryModifier(
    entity = player,
    attribute = CoreAttributes.MAX_HEALTH,
    name = "Powerful Ring",
    amount = 20.0,
    operation = AttributeOperation.ADDITION,
    source = ResourceLocation.fromNamespaceAndPath("yourmodid", "powerful_ring")
)

// Remove it when the item is unequipped or effect expires
AttributeUtils.removeModifiersBySource(player, ResourceLocation.fromNamespaceAndPath("yourmodid", "powerful_ring"))
```

## 📜 License

This project is licensed under the terms of the license specified in the `LICENSE` file. 