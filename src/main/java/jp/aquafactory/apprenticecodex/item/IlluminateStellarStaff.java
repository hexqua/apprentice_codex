package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class IlluminateStellarStaff extends AbstractSwingMagicItem implements GeoItem, UniqueItem {
    private static final String MAIN_CONTROLLER = "main";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final double ATTACK_DAMAGE = 5.0D;
    private static final double ATTACK_SPEED = -2.4D;
    private static final double ENTITY_REACH_BONUS = 0.25D;
    private static final double SPELL_POWER_BONUS = 0.05D;
    private static final double HOLY_SPELL_POWER_BONUS = 0.10D;
    private static final int ENCHANTMENT_VALUE = 14;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public IlluminateStellarStaff() {
        super(
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                SpellRegistry.ILLUMINATE_STELLAR,
                1,
                ENCHANTMENT_VALUE,
                "illuminate_stellar_staff",
                ATTACK_DAMAGE,
                ATTACK_SPEED,
                bonus(Attributes.ENTITY_INTERACTION_RANGE, ENTITY_REACH_BONUS, AttributeModifier.Operation.ADD_VALUE, "entity_reach"),
                bonus((Holder<Attribute>) AttributeRegistry.SPELL_POWER, SPELL_POWER_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "spell_power"),
                bonus((Holder<Attribute>) AttributeRegistry.HOLY_SPELL_POWER, HOLY_SPELL_POWER_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "holy_spell_power")
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    public boolean hasCustomRendering() {
        return true;
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

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
