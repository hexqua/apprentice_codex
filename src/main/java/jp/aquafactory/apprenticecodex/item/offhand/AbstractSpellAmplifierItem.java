package jp.aquafactory.apprenticecodex.item.offhand;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Supplier;

public abstract class AbstractSpellAmplifierItem extends AbstractOffhandMagicItem implements GeoItem {
    private static final String MAIN_CONTROLLER = "main";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ResourceLocation textureLocation;

    protected AbstractSpellAmplifierItem(
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            Rarity rarity,
            String itemKey,
            AttributeBonus... attributeBonuses
    ) {
        super(configuredSpell, configuredSpellLevel, rarity, itemKey, attributeBonuses);
        this.textureLocation = ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                "textures/geo/" + itemKey + ".png"
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    protected AbstractSpellAmplifierItem(Rarity rarity, String itemKey, AttributeBonus... attributeBonuses) {
        super(rarity, itemKey, attributeBonuses);
        this.textureLocation = ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                "textures/geo/" + itemKey + ".png"
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    protected AbstractSpellAmplifierItem(
            Rarity rarity,
            String itemKey,
            boolean fireResistant,
            AttributeBonus... attributeBonuses
    ) {
        super(rarity, itemKey, fireResistant, attributeBonuses);
        this.textureLocation = ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                "textures/geo/" + itemKey + ".png"
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
                new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
                    state.setAnimation(ANIM_IDLE);
                    return PlayState.CONTINUE;
                })
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
