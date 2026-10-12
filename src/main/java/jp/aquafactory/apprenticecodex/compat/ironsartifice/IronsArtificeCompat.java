package jp.aquafactory.apprenticecodex.compat.ironsartifice;

import jp.aquafactory.apprenticecodex.item.modifier.commencemagimodule.CommenceMagiModuleItem;
import jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule.ManaStabilizerModuleItem;
import jp.aquafactory.apprenticecodex.event.client.ManaStabilizerClientEvents;
import net.minecraft.server.level.ServerPlayer;
import jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule.SpellboundArmsModuleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import team.lodestar.lodestone.registry.common.tag.LodestoneDamageTypeTags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class IronsArtificeCompat {
    public static final String MOD_ID = "irons_artifice";

    private IronsArtificeCompat() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static Item createCommenceMagiModule() {
        // 保存済みアイテムのIDは未導入時も維持し、外部型は導入確認後にだけ解決する。
        return isAvailable() ? Loaded.createItem() : new Item(new Item.Properties().stacksTo(1));
    }

    public static Item createSpellboundArmsModule() {
        return isAvailable() ? Loaded.createSpellboundItem() : new Item(new Item.Properties().stacksTo(1));
    }

    public static Item createManaStabilizerModule() {
        return isAvailable() ? Loaded.createStabilizerItem() : new Item(new Item.Properties().stacksTo(1));
    }

    public static void manaStabilizerInput(ServerPlayer player, boolean aiming, boolean hook, long shot) {
        if (isAvailable()) Loaded.stabilizerInput(player, aiming, hook, shot);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener((FMLCommonSetupEvent event) -> {
            if (isAvailable()) {
                event.enqueueWork(Loaded::registerEvents);
            }
        });
    }

    static boolean defersSpellResistance(DamageSource source) {
        return ModList.get().isLoaded("lodestone") && LodestoneLoaded.recalculatesDamage(source);
    }

    private static final class LodestoneLoaded {
        private static boolean recalculatesDamage(DamageSource source) {
            return source.getEntity() instanceof LivingEntity && source.is(LodestoneDamageTypeTags.IS_MAGIC);
        }

        private static void registerEvents() {
            NeoForge.EVENT_BUS.register(SpellboundArmsLodestoneEvents.class);
        }
    }

    private static final class MalumLoaded {
        private static void registerEvents() {
            NeoForge.EVENT_BUS.register(SpellboundArmsMalumEvents.class);
        }
    }

    private static final class ClientLoaded {
        private static void registerEvents() {
            NeoForge.EVENT_BUS.register(IronsArtificeClientTooltips.class);
            ManaStabilizerClientEvents.register();
            NeoForge.EVENT_BUS.register(ManaStabilizerClientEvents.class);
        }
    }

    // 戻り値の共通型をJVMが検証する際もModifierItemの継承関係を解決しないよう隔離する。
    private static final class Loaded {
        private static Item createStabilizerItem() { return new ManaStabilizerModuleItem(); }

        private static void stabilizerInput(ServerPlayer player, boolean aiming, boolean hook, long shot) {
            ManaStabilizerModuleEvents.input(player, aiming, hook, shot);
        }

        private static Item createItem() {
            return new CommenceMagiModuleItem();
        }

        private static Item createSpellboundItem() {
            return new SpellboundArmsModuleItem();
        }

        private static void registerEvents() {
            NeoForge.EVENT_BUS.register(CommenceMagiModuleEvents.class);
            NeoForge.EVENT_BUS.register(SpellboundArmsModuleEvents.class);
            NeoForge.EVENT_BUS.register(IronsArtificeManaEvents.class);
            NeoForge.EVENT_BUS.register(ManaStabilizerModuleEvents.class);
            if (ModList.get().isLoaded("lodestone")) LodestoneLoaded.registerEvents();
            if (ModList.get().isLoaded("malum")) MalumLoaded.registerEvents();
            if (FMLEnvironment.dist == Dist.CLIENT) ClientLoaded.registerEvents();
        }
    }
}
