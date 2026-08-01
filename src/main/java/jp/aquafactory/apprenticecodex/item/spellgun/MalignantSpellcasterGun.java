package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.network.chat.Component;
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

public class MalignantSpellcasterGun extends AbstractSpellGunItem
        implements GeoItem, ForcedSpellPowerSpellgun {
    private static final SpellGunConfig SPELL_GUN_CONFIG = new SpellGunConfig(
            EnumSet.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG),
            null,
            false,
            null,
            null,
            null,
            true,
            () -> true
    );
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MalignantSpellcasterGun() {
        super(
                new Properties().stacksTo(1).rarity(Rarity.COMMON).fireResistant(),
                SPELL_GUN_CONFIG,
                "MalignantSpellcasterGun"
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        // Malumのhiddenは登録されている他のアイテムを巻き込んで非表示側に倒れるため、このアイテム自体のJEI説明を除外.
        return null;
    }

    @Override
    public String getJeiInfoGroupId() {
        // Malumのhiddenは登録されている他のアイテムを巻き込んで非表示側に倒れるため、このアイテム自体のJEI説明を除外.
        return null;
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
        return ItemRegistry.SPELL_DOMINATOR_ROUND.get();
    }

    @Override
    protected List<AmmoTooltipEntry> getAmmoTooltipEntries(ItemStack stack) {
        return List.of(new AmmoTooltipEntry(ItemRegistry.SPELL_DOMINATOR_ROUND.get(), null));
    }

    @Override
    protected void appendAdditionalSpellGunAbilityTooltipLines(List<Component> translatedLines) {
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_force_spell_power"
        ));
    }

    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return 10;
    }

    @Override
    public double forcedSpellPower() {
        return ApprenticeCodexServerConfig.malignantSpellgunForcedSpellPower();
    }

    @Override
    public double forcedSchoolSpellPower() {
        return ApprenticeCodexServerConfig.malignantSpellgunForcedSchoolSpellPower();
    }

    @Override
    public double forcedSummonDamage() {
        return ApprenticeCodexServerConfig.malignantSpellgunForcedSummonDamage();
    }
}
