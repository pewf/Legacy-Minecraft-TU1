package wily.legacy.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyTip;

public class HelpOptionsScreen extends RenderableVListScreen {
    public HelpOptionsScreen(Screen parent) {
        super(parent,Component.translatable("options.title"), r-> {});
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.how_to_play"),(b)-> minecraft.getToasts().addToast(new LegacyTip(Component.literal("Work in Progress!!"), 80, 40).disappearTime(960))).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("controls.title"),()-> new RenderableVListScreen(this,Component.translatable("controls.title"),r->r.addRenderables(Button.builder(Component.translatable("options.mouse_settings.title"), button -> this.minecraft.setScreen(new PanelVListScreen(r.screen,250,110,Component.translatable("options.mouse_settings.title"), minecraft.options.invertYMouse(), minecraft.options.mouseWheelSensitivity(), minecraft.options.discreteMouseScroll(), minecraft.options.touchscreen()))).build(),Button.builder(Component.translatable("controls.keybinds.title"), button -> this.minecraft.setScreen(new LegacyKeyBindsScreen(r.screen,minecraft.options))).build(),Button.builder(Component.translatable("legacy.controls.controller"), button -> this.minecraft.setScreen(new ControllerMappingScreen(r.screen,minecraft.options))).build()))).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.settings"),()->new SettingsScreen(this)).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("credits_and_attribution.button.credits"),()->new RenderableVListScreen(this,Component.translatable("credits_and_attribution.screen.title"),r-> r.addRenderables(openScreenButton(Component.translatable("credits_and_attribution.button.credits"),()->new WinScreen(false, () -> this.minecraft.setScreen(r.screen))).build(),Button.builder(Component.translatable("credits_and_attribution.button.attribution"), ConfirmLinkScreen.confirmLink(this,"https://aka.ms/MinecraftJavaAttribution")).build(),Button.builder(Component.translatable("credits_and_attribution.button.licenses"), ConfirmLinkScreen.confirmLink(this,"https://aka.ms/MinecraftJavaLicenses")).build()))).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.reset_defaults"),()->new ConfirmationScreen(this,Component.translatable("legacy.menu.reset_settings"),Component.translatable("legacy.menu.reset_message"), b1->{
            Legacy4JClient.resetVanillaOptions(minecraft);
            minecraft.setScreen(this);
        })).build());
        if (parent instanceof TitleScreen) {
            renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.reinstall_content"),(b)-> {
            /* in the future add a list with a bunch of disabled buttons that just does nothing lol
            akin to how it would be if u didn't have any of the packs purchased on a 360 at that time */
            }).build());
        }
        this.parent = parent;
    }
}
