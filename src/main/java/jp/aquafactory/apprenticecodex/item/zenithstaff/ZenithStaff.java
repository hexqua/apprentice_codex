package jp.aquafactory.apprenticecodex.item.zenithstaff;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import io.redspace.ironsspellbooks.item.weapons.StaffTier;
import io.redspace.ironsspellbooks.render.ClientStaffItemExtensions;
import jp.aquafactory.apprenticecodex.item.StaffEnchantmentTargeting;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.renderer.item.ZenithStaffRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ZenithStaff extends StaffItem implements GeoItem, UniqueItem, WisdomPolicy {
    private static final String MAIN_CONTROLLER = "main";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final StaffTier ZENITH_STAFF_TIER = new StaffTier(
            3.0F,
            -3.0F,
            new AttributeContainer(
                    AttributeRegistry.SPELL_POWER,
                    0.10D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            )
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ZenithStaff() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), ZENITH_STAFF_TIER);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public boolean hasCustomRendering() {
        return true;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new ClientStaffItemExtensions() {
            private ZenithStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new ZenithStaffRenderer();
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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return StaffEnchantmentTargeting.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantments(book);
        return enchantments.isEmpty() || enchantments.keySet().stream()
                .allMatch(enchantment -> canApplyAtEnchantingTable(stack, enchantment));
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        // Pastel Staff と同じく金ツール相当。
        return 22;
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack itemStack,
            @Nullable Level level,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(itemStack, level, lines, flag);

        var tooltipLines = resolveClientTooltipLines();
        tooltipLines.ifPresent(lines::addAll);
    }

    public static boolean isZenithStaff(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ZenithStaff;
    }

    private static Optional<List<Component>> resolveClientTooltipLines() {
        var result = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> ZenithStaffClientTooltip::createLines);
        return Optional.ofNullable(result);
    }
}
