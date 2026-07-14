package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.PhotonSiphonRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class PhotonSiphon extends AbstractOffhandMagicItem implements GeoItem {
    private static final String MAIN_CONTROLLER = "main";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ResourceLocation textureLocation;

    public PhotonSiphon() {
        super(
                SpellRegistry.MANA_CHARGE,
                1,
                Rarity.RARE,
                "photon_siphon",
                true,
                bonus(AttributeRegistry.MANA_REGEN, 1.0, AttributeModifier.Operation.MULTIPLY_BASE)
        );
        this.textureLocation = ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                "textures/geo/photon_siphon.png"
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    public boolean repairWorkbenchLegacyLockedSpellIfNeeded(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ISpellContainer.isSpellContainer(stack)) {
            return false;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getMaxSpellCount() <= 0) {
            return false;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        if (spellData == SpellData.EMPTY
                || !spellData.isLocked()
                || spellData.getSpell() == SpellRegistry.MANA_CHARGE.get()) {
            return false;
        }

        var normalized = ISpellContainer.create(1, true, false).mutableCopy();
        if (!normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false)) {
            return false;
        }

        // Workbench 操作時だけ旧 locked 差し替え魔法を抽出可能な状態へ救済する。
        ISpellContainer.set(stack, normalized.toImmutable());
        return true;
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private PhotonSiphonRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new PhotonSiphonRenderer();
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
}
