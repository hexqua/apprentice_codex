package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.DiamondSpellcasterGunRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

public class DiamondSpellcasterGun extends AbstractSpellGunItem implements GeoItem {
    private static final SpellGunConfig SPELL_GUN_CONFIG = new SpellGunConfig(
            EnumSet.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG),
            ApprenticeCodexServerConfig::diamondSpellgunMaxInstantImbueCooldownTicks,
            false,
            ApprenticeCodexServerConfig::diamondSpellgunOverriddenSpellCooldownTicks,
            0
    );
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DiamondSpellcasterGun() {
        super(
                new Properties().stacksTo(1).rarity(Rarity.COMMON).fireResistant(),
                SPELL_GUN_CONFIG,
                "DiamondSpellcasterGun",
                bonus(AttributeRegistry.SPELL_POWER, 0.10, AttributeModifier.Operation.MULTIPLY_BASE)
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private DiamondSpellcasterGunRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new DiamondSpellcasterGunRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public Item getAmmoItem(ItemStack stack, @Nullable SpellData spellData) {
        if (spellData == null){
            return ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get();
        }

        return spellData.getRarity().compareRarity(SpellRarity.EPIC) > 0 ? ItemRegistry.SPELL_DOMINATOR_ROUND.get() : ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get();
    }

    @Override
    protected List<AmmoTooltipEntry> getAmmoTooltipEntries(ItemStack stack) {
        return List.of(
                new AmmoTooltipEntry(
                        ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get(),
                        "item.apprenticecodex.spellgun.tooltip.ammo_condition_below_rare"
                ),
                new AmmoTooltipEntry(
                        ItemRegistry.SPELL_DOMINATOR_ROUND.get(),
                        "item.apprenticecodex.spellgun.tooltip.ammo_condition_above_epic"
                )
        );
    }
    
    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 10;
    }
}
