package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
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

public class IronSpellcasterGun extends AbstractSpellGunItem implements GeoItem {
    private static final SpellGunConfig SPELL_GUN_CONFIG = new SpellGunConfig(
            EnumSet.of(SpellGunCastType.INSTANT),
            ApprenticeCodexServerConfig::ironSpellgunMaxInstantImbueCooldownTicks,
            true,
            ApprenticeCodexServerConfig::ironSpellgunOverriddenSpellCooldownTicks,
            null,
            null,
            false,
            ApprenticeCodexServerConfig::ironSpellgunIgnoreMaxMana
    );
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public IronSpellcasterGun() {
        super(
                new Item.Properties().stacksTo(1).rarity(Rarity.COMMON),
                SPELL_GUN_CONFIG,
                SpellRegistry.MAGIC_MISSILE_SPELL,
                1,
                "IronSpellcasterGun"
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
        return ItemRegistry.RAPID_SPELLCASTER_ROUND.get();
    }
    
    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return 14;
    }
}
