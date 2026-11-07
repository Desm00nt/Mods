package org.sprautestudio.spraute_engine.platform.forge;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.registries.DeferredRegister;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.platform.Hooks;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;

final class ForgeVersion {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "spraute_engine");

    private ForgeVersion() {
    }

    static Attribute blockReach() {
        return net.minecraftforge.common.ForgeMod.BLOCK_REACH.get();
    }

    static Attribute entityReach() {
        return net.minecraftforge.common.ForgeMod.ENTITY_REACH.get();
    }

    static boolean accepted(String version, String protocol) {
        return version.equals(protocol) || version.equals(NetworkRegistry.ABSENT.version()) || version.equals(NetworkRegistry.ACCEPTVANILLA);
    }

    static void registerTabs(IEventBus modBus) {
        TABS.register(modBus);
    }

    static Object ownTab(String id, Supplier<ItemStack> icon, Supplier<List<ItemStack>> items) {
        TABS.register(
            id,
            () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.spraute_engine." + id))
                .icon(icon)
                .displayItems((parameters, output) -> items.get().forEach(output::accept))
                .build()
        );
        return null;
    }

    static void creativeTab(Supplier<Item> item, String key) {
        ResourceKey<CreativeModeTab> tab = Mc.tabKey(key);
        FMLJavaModLoadingContext.get().getModEventBus().addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == tab) {
                e.accept(item.get());
            }
        });
    }

    static boolean startsBreaking(LeftClickBlock f) {
        return f.getAction() == Action.START;
    }

    static void registerTrade() {
        MinecraftForge.EVENT_BUS.addListener((TradeWithVillagerEvent f) -> Hooks.TRADE.fire(new Hooks.Trade(f.getEntity(), f.getAbstractVillager(), f.getMerchantOffer())));
    }
}
