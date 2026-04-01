package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.IlluminateStellarStaffRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

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
                "IlluminateStellarStaff",
                ATTACK_DAMAGE,
                ATTACK_SPEED,
                bonus(net.minecraftforge.common.ForgeMod.ENTITY_REACH, ENTITY_REACH_BONUS, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION, "entity_reach"),
                bonus(AttributeRegistry.SPELL_POWER, SPELL_POWER_BONUS, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE, "spell_power"),
                bonus(AttributeRegistry.HOLY_SPELL_POWER, HOLY_SPELL_POWER_BONUS, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE, "holy_spell_power")
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private IlluminateStellarStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new IlluminateStellarStaffRenderer();
                }

                return renderer;
            }
        });
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
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        lines.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, lines, flag);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
