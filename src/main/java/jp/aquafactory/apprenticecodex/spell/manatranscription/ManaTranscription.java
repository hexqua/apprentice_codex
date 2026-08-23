package jp.aquafactory.apprenticecodex.spell.manatranscription;

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
import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ManaTranscription extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mana_transcription");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(4)
            .build();

    public ManaTranscription() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 500;
        manaCostPerLevel = 0;
        castTime = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        if (!(caster instanceof Player player)) {
            return List.of(Component.translatable(
                    "ui.apprenticecodex.mana_transcription.no_caster_help"
            ));
        }

        var resolution = ManaTranscriptionLogic.resolve(player);
        var requiredExperience = resolution.success()
                ? Component.literal(Integer.toString(resolution.requiredExperience()))
                : Component.literal("-");
        MutableComponent targetOperation = Component.literal("-");
        if (resolution.success() && resolution.mode() == ManaTranscriptionLogic.Mode.RESET) {
            targetOperation = Component.translatable(
                    "ui.apprenticecodex.mana_transcription.target_operation.work_count"
            );
        } else if (resolution.success() && resolution.candidates().size() == 1) {
            var candidate = resolution.candidates().get(0);
            targetOperation = candidate.enchantment().getFullname(candidate.level()).copy();
        } else if (resolution.success()) {
            targetOperation = Component.translatable(
                    "ui.apprenticecodex.mana_transcription.target_operation.random",
                    Component.literal(Integer.toString(resolution.candidates().size())).withStyle(ChatFormatting.AQUA)
            );
        }

        return List.of(
                Component.translatable(
                        "ui.apprenticecodex.mana_transcription.current_experience",
                        ManaTranscriptionLogic.currentExperience(player)
                ),
                Component.translatable(
                        "ui.apprenticecodex.mana_transcription.required_experience",
                        requiredExperience
                ),
                Component.translatable(
                        "ui.apprenticecodex.mana_transcription.target_operation",
                        targetOperation
                )
        );
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
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        return getCastTime(spellLevel);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.VANILLA_POWER_TUNING.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VANILLA_INSCRIBE_MANA.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.BOW_CHARGE_ANIMATION;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new ManaTranscriptionCastData();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (!super.checkPreCastConditions(level, spellLevel, entity, playerMagicData)
                || !(entity instanceof ServerPlayer player)) {
            return false;
        }

        var resolution = ManaTranscriptionLogic.resolve(player);
        if (!resolution.success()) {
            sendFailure(player, resolution.failure());
            return false;
        }
        if (ManaTranscriptionLogic.currentExperience(player) < resolution.requiredExperience()) {
            sendActionBar(player, Component.translatable(
                    "ui.apprenticecodex.mana_transcription.insufficient_experience"
            ).withStyle(ChatFormatting.RED));
            return false;
        }

        ManaTranscriptionLogic.Candidate selected = null;
        if (resolution.mode() == ManaTranscriptionLogic.Mode.EXTRACTION) {
            selected = resolution.candidates().get(player.getRandom().nextInt(resolution.candidates().size()));
        }
        var castData = new ManaTranscriptionCastData();
        castData.lock(player, resolution, selected);
        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
        if (!(entity instanceof ServerPlayer player)
                || playerMagicData == null
                || !(playerMagicData.getAdditionalCastData() instanceof ManaTranscriptionCastData castData)) {
            return;
        }

        if (castData.mode == ManaTranscriptionLogic.Mode.EXTRACTION && castData.selectedEnchantment != null) {
            sendActionBar(player, Component.translatable(
                    "ui.apprenticecodex.mana_transcription.operation_help.enchantment",
                    castData.selectedEnchantment.getFullname(castData.selectedLevel),
                    castData.requiredExperience
            ));
        } else {
            sendActionBar(player, Component.translatable(
                    "ui.apprenticecodex.mana_transcription.operation_help.work_count",
                    castData.requiredExperience
            ));
        }
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (!(entity instanceof ServerPlayer player)
                || playerMagicData == null
                || !(playerMagicData.getAdditionalCastData() instanceof ManaTranscriptionCastData castData)
                || !castData.matchesHands(player)) {
            sendActionBar(entity, Component.translatable(
                    "ui.apprenticecodex.mana_transcription.canceled_by_swap"
            ).withStyle(ChatFormatting.RED));
            MagicTools.cancelCasting(entity, true);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
        if (!(entity instanceof ServerPlayer player)
                || !(playerMagicData.getAdditionalCastData() instanceof ManaTranscriptionCastData castData)
                || !castData.matchesHands(player)) {
            sendCastError(entity);
            return;
        }

        var resolution = ManaTranscriptionLogic.resolve(player);
        if (!castData.matchesResolution(resolution)) {
            sendCastError(player);
            return;
        }
        if (ManaTranscriptionLogic.currentExperience(player) < resolution.requiredExperience()) {
            sendActionBar(player, Component.translatable(
                    "ui.apprenticecodex.mana_transcription.insufficient_experience"
            ).withStyle(ChatFormatting.RED));
            return;
        }

        if (resolution.requiredExperience() > 0) {
            player.giveExperiencePoints(-resolution.requiredExperience());
        }
        if (resolution.mode() == ManaTranscriptionLogic.Mode.EXTRACTION) {
            finishExtraction(player, castData);
        } else {
            finishRepairCostReset(level, player);
        }
    }

    private static void finishExtraction(ServerPlayer player, ManaTranscriptionCastData castData) {
        var target = player.getMainHandItem();
        var operationItem = ManaTranscriptionLogic.physicalOffhandItem(player);
        var selected = castData.selectedEnchantment;
        if (selected == null) {
            sendCastError(player);
            return;
        }

        var enchantments = EnchantmentHelper.getEnchantments(target);
        enchantments.remove(selected);
        if (target.is(Items.ENCHANTED_BOOK)) {
            target.removeTagKey("StoredEnchantments");
        }
        EnchantmentHelper.setEnchantments(enchantments, target);
        target.setRepairCost(AnvilMenu.calculateIncreasedRepairCost(target.getBaseRepairCost()));

        var result = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(selected, castData.selectedLevel));
        player.getInventory().offhand.set(
                0,
                ItemUtils.createFilledResult(operationItem, player, result)
        );
        sendActionBar(player, Component.translatable(
                "ui.apprenticecodex.mana_transcription.finished_enchantment",
                selected.getFullname(castData.selectedLevel)
        ).withStyle(ChatFormatting.GREEN));
    }

    private static void finishRepairCostReset(Level level, ServerPlayer player) {
        var target = player.getMainHandItem();
        var operationItem = ManaTranscriptionLogic.physicalOffhandItem(player);
        var consumedItemName = operationItem.getHoverName();
        target.setRepairCost(0);

        if (player.isCreative()) {
            sendActionBar(player, Component.translatable(
                    "ui.apprenticecodex.mana_transcription.finished_work_count_creative"
            ).withStyle(ChatFormatting.GREEN));
            return;
        }

        operationItem.shrink(1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        sendActionBar(player, Component.translatable(
                "ui.apprenticecodex.mana_transcription.finished_work_count",
                consumedItemName
        ).withStyle(ChatFormatting.GREEN));
    }

    private static void sendCastError(LivingEntity entity) {
        sendActionBar(entity, Component.translatable(
                "ui.apprenticecodex.mana_transcription.error_on_cast"
        ).withStyle(ChatFormatting.RED));
    }

    private static void sendFailure(ServerPlayer player, ManaTranscriptionLogic.Failure failure) {
        MutableComponent message = switch (failure) {
            case INVALID_TARGET -> Component.translatable(
                    "ui.apprenticecodex.mana_transcription.invalid_target"
            );
            case TARGET_MUST_BE_SINGLE -> Component.translatable(
                    "ui.apprenticecodex.mana_transcription.target_must_be_single",
                    player.getMainHandItem().getHoverName()
            );
            case REQUIRED_OPERATION_ITEM -> requiredOperationItemMessage();
            case REJECT_SINGLE_ENCHANTMENT_BOOK -> Component.translatable(
                    "ui.apprenticecodex.mana_transcription.reject_single_enchantment_book"
            );
            case NO_ENCHANTMENT -> Component.translatable(
                    "ui.apprenticecodex.mana_transcription.no_enchantment"
            );
            case REQUIRED_EMPTY_BOOK -> Component.translatable(
                    "ui.apprenticecodex.mana_transcription.required_empty_book",
                    ManaTranscriptionLogic.physicalOffhandItem(player).getHoverName()
            );
            case NO_WORK_COUNT -> Component.translatable(
                    "ui.apprenticecodex.mana_transcription.no_work_count"
            );
            case NONE -> Component.empty();
        };
        if (failure != ManaTranscriptionLogic.Failure.NONE) {
            sendActionBar(player, message.withStyle(ChatFormatting.RED));
        }
    }

    private static MutableComponent requiredOperationItemMessage() {
        var resetItems = ManaTranscriptionLogic.effectiveResetItems();
        if (resetItems.isEmpty()) {
            return Component.translatable(
                    "ui.apprenticecodex.mana_transcription.required_operation_item_no_tag",
                    Items.WRITABLE_BOOK.getDescription()
            );
        }
        return Component.translatable(
                "ui.apprenticecodex.mana_transcription.required_operation_item_tag",
                Items.WRITABLE_BOOK.getDescription()
        );
    }

    private static void sendActionBar(LivingEntity entity, Component message) {
        if (entity instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
        }
    }

    public static class ManaTranscriptionCastData implements ICastDataSerializable {
        private boolean locked;
        private ManaTranscriptionLogic.Mode mode = ManaTranscriptionLogic.Mode.EXTRACTION;
        private ResourceLocation selectedEnchantmentId;
        private int selectedLevel;
        private int requiredExperience;
        private transient Enchantment selectedEnchantment;
        private transient ItemStack targetReference = ItemStack.EMPTY;
        private transient ItemStack operationItemReference = ItemStack.EMPTY;
        private transient ItemStack targetSnapshot = ItemStack.EMPTY;
        private transient ItemStack operationItemSnapshot = ItemStack.EMPTY;

        private void lock(
                ServerPlayer player,
                ManaTranscriptionLogic.Resolution resolution,
                @Nullable ManaTranscriptionLogic.Candidate selected
        ) {
            locked = true;
            mode = resolution.mode();
            requiredExperience = resolution.requiredExperience();
            targetReference = player.getMainHandItem();
            operationItemReference = ManaTranscriptionLogic.physicalOffhandItem(player);
            targetSnapshot = targetReference.copy();
            operationItemSnapshot = operationItemReference.copy();
            if (selected != null) {
                selectedEnchantment = selected.enchantment();
                selectedEnchantmentId = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS
                        .getKey(selected.enchantment());
                selectedLevel = selected.level();
            }
        }

        private boolean matchesHands(Player player) {
            var target = player.getMainHandItem();
            var operationItem = ManaTranscriptionLogic.physicalOffhandItem(player);
            var targetMatches = targetReference == target && ItemStack.matches(targetSnapshot, target);
            if (!targetMatches) {
                // SpellOnCastEvent が転写完了前に剣のチャージ状態を更新して copy を再装備するため、その内部更新だけを許容する。
                targetMatches = SpellchargedGreatsword.matchesIgnoringChargeRuntimeState(targetSnapshot, target);
            }
            return locked
                    && targetMatches
                    && operationItemReference == operationItem
                    && ItemStack.matches(operationItemSnapshot, operationItem);
        }

        private boolean matchesResolution(ManaTranscriptionLogic.Resolution resolution) {
            if (!resolution.success()
                    || resolution.mode() != mode
                    || resolution.requiredExperience() != requiredExperience) {
                return false;
            }
            if (mode == ManaTranscriptionLogic.Mode.RESET) {
                return true;
            }
            return selectedEnchantment != null
                    && resolution.candidates().stream().anyMatch(candidate ->
                    candidate.enchantment().equals(selectedEnchantment) && candidate.level() == selectedLevel);
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf buffer) {
            buffer.writeBoolean(locked);
            buffer.writeEnum(mode);
            buffer.writeBoolean(selectedEnchantmentId != null);
            if (selectedEnchantmentId != null) {
                buffer.writeResourceLocation(selectedEnchantmentId);
            }
            buffer.writeVarInt(selectedLevel);
            buffer.writeVarInt(requiredExperience);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf buffer) {
            locked = buffer.readBoolean();
            mode = buffer.readEnum(ManaTranscriptionLogic.Mode.class);
            selectedEnchantmentId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
            selectedLevel = buffer.readVarInt();
            requiredExperience = buffer.readVarInt();
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            tag.putBoolean("Locked", locked);
            tag.putString("Mode", mode.name());
            if (selectedEnchantmentId != null) {
                tag.putString("SelectedEnchantment", selectedEnchantmentId.toString());
            }
            tag.putInt("SelectedLevel", selectedLevel);
            tag.putInt("RequiredExperience", requiredExperience);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            locked = tag.getBoolean("Locked");
            try {
                mode = tag.contains("Mode")
                        ? ManaTranscriptionLogic.Mode.valueOf(tag.getString("Mode"))
                        : ManaTranscriptionLogic.Mode.EXTRACTION;
            } catch (IllegalArgumentException ignored) {
                mode = ManaTranscriptionLogic.Mode.EXTRACTION;
            }
            selectedEnchantmentId = tag.contains("SelectedEnchantment")
                    ? ResourceLocation.tryParse(tag.getString("SelectedEnchantment"))
                    : null;
            selectedLevel = tag.getInt("SelectedLevel");
            requiredExperience = tag.getInt("RequiredExperience");
        }

        @Override
        public void reset() {
            locked = false;
            mode = ManaTranscriptionLogic.Mode.EXTRACTION;
            selectedEnchantmentId = null;
            selectedLevel = 0;
            requiredExperience = 0;
            selectedEnchantment = null;
            targetReference = ItemStack.EMPTY;
            operationItemReference = ItemStack.EMPTY;
            targetSnapshot = ItemStack.EMPTY;
            operationItemSnapshot = ItemStack.EMPTY;
        }
    }
}
