package com.arteam.statcore.mixin;

import com.arteam.statcore.client.gui.StatCoreTabButton;
import com.arteam.statcore.client.screen.AttributeScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 物品栏界面Mixin
 * 在物品栏界面添加背包和属性切换按钮
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    
    @Unique
    private static final ResourceLocation CREATIVE_INVENTORY_TABS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/creative_inventory/tabs.png");
    
    @Unique
    private StatCoreTabButton statcore$inventoryButton;
    
    @Unique
    private StatCoreTabButton statcore$attributesButton;
    
    /**
     * 在界面初始化时添加我们的按钮
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void statcore$addButtons(CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen) (Object) this;
        Player player = self.getMinecraft().player;
        if (player == null) return;
        
        // 计算按钮位置（与物品栏左侧对齐）
        int guiLeft = (self.width - 176) / 2;
        int guiTop = (self.height - 166) / 2;
        
        // 创建按钮纹理精灵（使用创造模式标签页纹理）
        WidgetSprites inventorySprites = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath("minecraft", "container/creative_inventory/tab_top_selected_1"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "container/creative_inventory/tab_top_unselected_1")
        );
        
        WidgetSprites attributesSprites = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath("minecraft", "container/creative_inventory/tab_top_unselected_2"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "container/creative_inventory/tab_top_selected_2")
        );
        
        // 背包按钮（左侧第一个，激活状态）
        statcore$inventoryButton = new StatCoreTabButton(
            guiLeft, guiTop - 28,       // 位置：与物品栏左侧对齐，在物品栏上方
            28, 32,                      // 大小
            inventorySprites,            // 纹理精灵
            button -> {
                // 点击背包按钮，不做任何操作（已经在物品栏界面）
            },
            Component.translatable("gui.statcore.inventory.tooltip"),
            true,                        // 选中状态
            new ItemStack(Items.CHEST)   // 箱子图标
        );
        
        // 属性按钮（左侧第二个，未激活状态）
        statcore$attributesButton = new StatCoreTabButton(
            guiLeft + 28, guiTop - 28,  // 位置：紧贴背包按钮右侧
            28, 32,                      // 大小
            attributesSprites,           // 纹理精灵
            button -> {
                // 点击属性按钮，切换到属性界面
                self.getMinecraft().setScreen(new AttributeScreen(player));
            },
            Component.translatable("gui.statcore.attributes.tooltip"),
            false,                       // 未选中状态
            new ItemStack(Items.IRON_SWORD) // 铁剑图标
        );
        
        // 添加按钮到界面系统以处理点击事件（使用反射调用protected方法）
        try {
            java.lang.reflect.Method addWidgetMethod = net.minecraft.client.gui.screens.Screen.class.getDeclaredMethod("addRenderableWidget", 
                net.minecraft.client.gui.components.events.GuiEventListener.class);
            addWidgetMethod.setAccessible(true);
            addWidgetMethod.invoke(self, statcore$inventoryButton);
            addWidgetMethod.invoke(self, statcore$attributesButton);
        } catch (Exception e) {
            // 如果反射失败，记录错误但不崩溃
            System.err.println("StatCore: Failed to add GUI buttons: " + e.getMessage());
        }
    }
    
    /**
     * 在物品栏背景渲染之前渲染未选中的按钮
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void statcore$renderButtonsBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // 渲染未选中状态的按钮（在物品栏背景之前）
        if (statcore$attributesButton != null) {
            statcore$attributesButton.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
    
    /**
     * 在物品栏完整渲染之后渲染选中的按钮
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void statcore$renderButtonsForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // 渲染选中状态的按钮（在物品栏渲染之后）
        if (statcore$inventoryButton != null) {
            statcore$inventoryButton.renderForeground(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
} 