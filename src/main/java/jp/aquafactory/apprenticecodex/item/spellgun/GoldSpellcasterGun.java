package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

public class GoldSpellcasterGun extends AbstractSpellGunItem implements GeoItem {
    private static final SpellGunConfig SPELL_GUN_CONFIG = new SpellGunConfig(
            EnumSet.of(SpellGunCastType.INSTANT),
            20 * 20,
            false,
            40,
            null
    );
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GoldSpellcasterGun() {
        super(
                new Properties().stacksTo(1).rarity(Rarity.COMMON),
                SPELL_GUN_CONFIG,
                "GoldSpellcasterGun",
                bonus(AttributeRegistry.SPELL_POWER, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    public boolean hasCustomRendering() {
        return true;
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
        if (spellData == null) {
            return ItemRegistry.BASIC_SPELLCASTER_ROUND.get();
        }

        return spellData.getRarity().compareRarity(SpellRarity.EPIC) > 0
                ? ItemRegistry.ARCANE_SPELLCASTER_ROUND.get()
                : ItemRegistry.BASIC_SPELLCASTER_ROUND.get();
    }

    @Override
    protected List<AmmoTooltipEntry> getAmmoTooltipEntries(ItemStack stack) {
        return List.of(
                new AmmoTooltipEntry(
                        ItemRegistry.BASIC_SPELLCASTER_ROUND.get(),
                        "item.apprenticecodex.spellgun.tooltip.ammo_condition_below_rare"
                ),
                new AmmoTooltipEntry(
                        ItemRegistry.ARCANE_SPELLCASTER_ROUND.get(),
                        "item.apprenticecodex.spellgun.tooltip.ammo_condition_above_epic"
                )
        );
    }
    
    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return 22;
    }
}
