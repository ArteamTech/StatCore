package com.arteam.statcore.mixin;

import com.arteam.statcore.client.screen.AttributeScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 物品栏界面Mixin
 * 在原版物品栏界面添加自定义按钮
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    
    @Unique
    private Button statCore$inventoryButton;
    @Unique
    private Button statCore$attributesButton;
    
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen)(Object)this;
        
        // 计算按钮位置（在物品栏界面左上方）
        int leftPos = self.getGuiLeft();
        int topPos = self.getGuiTop();
        
        // 背包按钮
        statCore$inventoryButton = Button.builder(
            Component.translatable("gui.statcore.inventory"),
            button -> {
                // 点击背包按钮不做任何事，因为已经在背包界面了
            }
        ).bounds(leftPos - 80, topPos + 6, 75, 20).build();
        
        // 属性按钮
        statCore$attributesButton = Button.builder(
            Component.translatable("gui.statcore.attributes"),
            button -> {
                // 点击属性按钮打开属性界面
                if (self.getMinecraft() != null && self.getMinecraft().player != null) {
                    self.getMinecraft().setScreen(new AttributeScreen(self.getMinecraft().player));
                }
            }
        ).bounds(leftPos - 80, topPos + 28, 75, 20).build();
        
        // 使用反射添加按钮到界面
        try {
            java.lang.reflect.Method addMethod = net.minecraft.client.gui.screens.Screen.class
                .getDeclaredMethod("addRenderableWidget", 
                    net.minecraft.client.gui.components.events.GuiEventListener.class);
            addMethod.setAccessible(true);
            addMethod.invoke(self, statCore$inventoryButton);
            addMethod.invoke(self, statCore$attributesButton);
        } catch (Exception e) {
            // 如果反射失败，记录错误但不崩溃
            System.err.println("StatCore: Failed to add buttons to inventory screen: " + e.getMessage());
        }
    }
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen)(Object)this;
        
        // 渲染按钮hover提示
        if (statCore$inventoryButton != null && statCore$inventoryButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(self.getMinecraft().font, 
                Component.translatable("gui.statcore.inventory.tooltip"), 
                mouseX, mouseY);
        }
        
        if (statCore$attributesButton != null && statCore$attributesButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(self.getMinecraft().font, 
                Component.translatable("gui.statcore.attributes.tooltip"), 
                mouseX, mouseY);
        }
    }
}