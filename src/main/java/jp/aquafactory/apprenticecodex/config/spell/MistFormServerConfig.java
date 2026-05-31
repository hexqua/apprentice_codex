package jp.aquafactory.apprenticecodex.config.spell;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MistFormServerConfig {
    private final ModConfigSpec.ConfigValue<List<? extends String>> passableBlockDenylist;
    private List<String> passableBlockDenylistOverride;
    private List<String> cachedDenylistEntries = List.of();
    private Set<ResourceLocation> cachedDeniedBlocks = Set.of();
    private List<TagKey<Block>> cachedDeniedTags = List.of();

    private MistFormServerConfig(ModConfigSpec.ConfigValue<List<? extends String>> passableBlockDenylist) {
        this.passableBlockDenylist = passableBlockDenylist;
    }

    public static MistFormServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("MistForm");
        var passableBlockDenylist = builder
                .comment(
                        "Block IDs or block tag IDs that Mist Form cannot pass through even when they are in apprenticecodex:mist_form_passable.",
                        "Entries use \"modid:path\" for blocks and \"#modid:path\" for block tags."
                )
                .defineListAllowEmpty("mistFormPassableBlockDenylist", List.<String>of(), MistFormServerConfig::isBlockOrTagId);
        builder.pop();
        return new MistFormServerConfig(passableBlockDenylist);
    }

    public boolean isPassableBlockDenied(BlockState state) {
        refreshDenylistCache();

        var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId != null && cachedDeniedBlocks.contains(blockId)) {
            return true;
        }

        for (var tag : cachedDeniedTags) {
            if (state.is(tag)) {
                return true;
            }
        }
        return false;
    }

    public List<String> passableBlockDenylist() {
        return Objects.requireNonNullElseGet(passableBlockDenylistOverride, () -> stringList(passableBlockDenylist));
    }

    public void setPassableBlockDenylistForGameTest(List<String> passableBlockDenylist) {
        this.passableBlockDenylistOverride = List.copyOf(passableBlockDenylist);
    }

    private void refreshDenylistCache() {
        var entries = passableBlockDenylist();
        if (entries.equals(cachedDenylistEntries)) {
            return;
        }

        cachedDenylistEntries = List.copyOf(entries);
        cachedDeniedBlocks = entries.stream()
                .filter(entry -> !entry.startsWith("#"))
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        cachedDeniedTags = entries.stream()
                .filter(entry -> entry.startsWith("#"))
                .map(entry -> ResourceLocation.tryParse(entry.substring(1)))
                .filter(Objects::nonNull)
                .map(id -> TagKey.create(Registries.BLOCK, id))
                .toList();
    }

    private static boolean isBlockOrTagId(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        var idText = text.startsWith("#") ? text.substring(1) : text;
        return idText.contains(":") && ResourceLocation.tryParse(idText) != null;
    }

    private static List<String> stringList(ModConfigSpec.ConfigValue<List<? extends String>> configValue) {
        return configValue.get().stream()
                .map(String::valueOf)
                .toList();
    }
}
