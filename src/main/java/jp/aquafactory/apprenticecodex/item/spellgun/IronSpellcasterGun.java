package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.IronSpellcasterGunRenderer;
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
import java.util.function.Consumer;

public class IronSpellcasterGun extends AbstractSpellGunItem implements GeoItem {
    private static final SpellGunConfig SPELL_GUN_CONFIG = new SpellGunConfig(
            EnumSet.of(SpellGunCastType.INSTANT),
            ApprenticeCodexServerConfig::ironSpellgunMaxInstantImbueCooldownTicks,
            true,
            ApprenticeCodexServerConfig::ironSpellgunOverriddenSpellCooldownTicks,
            null
    );
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public IronSpellcasterGun() {
        super(
                new Item.Properties().stacksTo(1).rarity(Rarity.COMMON),
                SPELL_GUN_CONFIG,
                SpellRegistry.MAGIC_MISSILE_SPELL,
                1,
                "IronSpellcasterGun",
                bonus(AttributeRegistry.SPELL_POWER, 0.05, AttributeModifier.Operation.MULTIPLY_BASE)
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private IronSpellcasterGunRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new IronSpellcasterGunRenderer();
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
        return ItemRegistry.RAPID_SPELLCASTER_ROUND.get();
    }
    
    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 14;
    }
}
