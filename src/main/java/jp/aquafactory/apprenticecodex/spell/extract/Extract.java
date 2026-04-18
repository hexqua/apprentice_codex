package jp.aquafactory.apprenticecodex.spell.extract;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class Extract extends AbstractSpell {
    private static final HolderLookup.Provider SERIALIZATION_LOOKUP =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static final String MESSAGE_NO_FLASK = "ui.apprenticecodex.extract.no_flask";
    private static final String MESSAGE_EMPTY_FLASK = "ui.apprenticecodex.extract.empty_flask";
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "extract");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(0.5)
            .setAllowCrafting(false)
            .build();

    public Extract() {
        baseSpellPower = 100;
        spellPowerPerLevel = 100;
        baseManaCost = 30;
        manaCostPerLevel = 90;
        castTime = 10;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.apprenticecodex.extract.amplify_level", getAmplify(spellLevel, caster)));
    }

    private int getAmplify(int spellLevel, LivingEntity entity) {
        var basePower = getSpellPower(spellLevel, entity) - 100;
        if (basePower <= 0) {
            return 0;
        }

        return (int) Math.floor(basePower / 100);
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return config;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BREWING_STAND_BREW);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.SPLASH_POTION_THROW);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_SPIT_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.THROW_SINGLE_ITEM;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new ExtractCastData();
    }

    @Override
    public final boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var resolution = resolveFilledFlask(entity);
        if (resolution == null) {
            sendErrorMessage(entity, hasAnyAlchemistsFlask(entity) ? MESSAGE_EMPTY_FLASK : MESSAGE_NO_FLASK);
            return false;
        }

        var castData = new ExtractCastData();
        castData.hand = resolution.hand();
        castData.storedItem = resolution.storedItem().copy();
        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);

        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var castData = playerMagicData != null && playerMagicData.getAdditionalCastData() instanceof ExtractCastData data
                ? data
                : null;
        var storedItem = castData != null && !castData.storedItem.isEmpty()
                ? castData.storedItem
                : Optional.ofNullable(resolveFilledFlask(entity)).map(FlaskResolution::storedItem).orElse(ItemStack.EMPTY);
        if (storedItem.isEmpty()) {
            return;
        }

        serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                Component.translatable("ui.apprenticecodex.extract.effect_use", storedItem.getHoverName())
        ));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            var castData = playerMagicData.getAdditionalCastData() instanceof ExtractCastData data ? data : null;
            var resolution = resolveFlaskForCast(entity, castData);
            if (resolution != null) {
                var potionStack = AbstractPotionFlaskItem.createExtractedPotionForThrow(
                        resolution.flaskStack(),
                        resolution.storedItem(),
                        getAmplify(spellLevel, entity)
                );
                if (!potionStack.isEmpty()) {
                    var projectile = new ExtractPotionProjectileEntity(EntityRegistry.EXTRACT_POTION_PROJECTILE.get(), serverLevel, entity);
                    projectile.setItem(potionStack);
                    projectile.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
                    projectile.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), -20.0F, 0.5F, 1.0F);
                    if (serverLevel.addFreshEntity(projectile)) {
                        consumeResolvedDose(entity, resolution.hand(), resolution.flaskStack());
                    }
                }
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static @Nullable FlaskResolution resolveFilledFlask(LivingEntity entity) {
        for (var hand : List.of(InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND)) {
            var stack = entity.getItemInHand(hand);
            if (!(stack.getItem() instanceof AlchemistsFlask)) {
                continue;
            }

            if (AbstractPotionFlaskItem.canExtractOneDose(stack)) {
                var storedItem = AbstractPotionFlaskItem.getStoredItem(stack);
                if (!storedItem.isEmpty()) {
                    return new FlaskResolution(hand, stack, storedItem);
                }
            }
        }

        return null;
    }

    private static boolean hasAnyAlchemistsFlask(LivingEntity entity) {
        return entity.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof AlchemistsFlask
                || entity.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof AlchemistsFlask;
    }

    private static @Nullable FlaskResolution resolveFlaskForCast(LivingEntity entity, @Nullable ExtractCastData castData) {
        if (castData != null && castData.hand != null) {
            var heldStack = entity.getItemInHand(castData.hand);
            if (heldStack.getItem() instanceof AlchemistsFlask
                    && AbstractPotionFlaskItem.canExtractOneDose(heldStack)
                    && ItemStack.isSameItemSameComponents(AbstractPotionFlaskItem.getStoredItem(heldStack), castData.storedItem)) {
                return new FlaskResolution(castData.hand, heldStack, castData.storedItem);
            }
        }

        return resolveFilledFlask(entity);
    }

    private static void consumeResolvedDose(LivingEntity entity, InteractionHand hand, ItemStack expectedFlask) {
        var heldStack = entity.getItemInHand(hand);
        if (heldStack != expectedFlask || !(heldStack.getItem() instanceof AlchemistsFlask)) {
            return;
        }

        var afterUse = AbstractPotionFlaskItem.copyAfterExtractingOneDose(heldStack);
        if (!afterUse.isEmpty()) {
            entity.setItemInHand(hand, afterUse);
        }
    }

    private static void sendErrorMessage(LivingEntity entity, String messageKey) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable(messageKey).withStyle(ChatFormatting.RED)
            ));
        }
    }

    public static class ExtractCastData implements ICastDataSerializable {
        private InteractionHand hand;
        private ItemStack storedItem = ItemStack.EMPTY;

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeBoolean(hand != null && !storedItem.isEmpty());
            if (hand == null || storedItem.isEmpty()) {
                return;
            }

            friendlyByteBuf.writeEnum(hand);
            friendlyByteBuf.writeNbt((CompoundTag) storedItem.saveOptional(SERIALIZATION_LOOKUP));
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            if (!friendlyByteBuf.readBoolean()) {
                reset();
                return;
            }

            hand = friendlyByteBuf.readEnum(InteractionHand.class);
            storedItem = ItemStack.parseOptional(
                    SERIALIZATION_LOOKUP,
                    friendlyByteBuf.readNbt() instanceof CompoundTag tag ? tag : new CompoundTag()
            );
        }

        @Override
        public void reset() {
            hand = null;
            storedItem = ItemStack.EMPTY;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            var tag = new CompoundTag();
            if (hand == null || storedItem.isEmpty()) {
                return tag;
            }

            tag.putString("Hand", hand.name());
            tag.put("StoredItem", storedItem.saveOptional(provider));
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            if (!nbt.contains("Hand") || !nbt.contains("StoredItem")) {
                reset();
                return;
            }

            hand = InteractionHand.valueOf(nbt.getString("Hand"));
            storedItem = ItemStack.parseOptional(provider, nbt.getCompound("StoredItem"));
        }

        public InteractionHand hand() {
            return hand;
        }

        public ItemStack storedItem() {
            return storedItem;
        }
    }

    private record FlaskResolution(InteractionHand hand, ItemStack flaskStack, ItemStack storedItem) {
    }
}
