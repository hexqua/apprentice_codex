package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileDefinition;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileList;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MulticastEchoStaffAttackProfileDataGenerator
        extends JsonCodecProvider<MulticastEchoStaffAttackProfileList> {
    public MulticastEchoStaffAttackProfileDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                MulticastEchoStaffAttackProfileManager.DIRECTORY,
                MulticastEchoStaffAttackProfileList.CODEC,
                Map.of(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "profiles"),
                        new MulticastEchoStaffAttackProfileList(List.of(
                                defaultDefinition(SpellRegistry.BLOOD_SLASH_SPELL),
                                defaultDefinition(SpellRegistry.DEVOUR_SPELL),
                                defaultDefinition(SpellRegistry.WITHER_SKULL_SPELL),
                                defaultDefinition(SpellRegistry.MAGIC_ARROW_SPELL),
                                defaultDefinition(SpellRegistry.MAGIC_MISSILE_SPELL),
                                defaultDefinition(SpellRegistry.FIRE_ARROW_SPELL),
                                defaultDefinition(SpellRegistry.FIREBALL_SPELL),
                                defaultDefinition(SpellRegistry.FIREBOLT_SPELL),
                                defaultDefinition(SpellRegistry.GUIDING_BOLT_SPELL),
                                defaultDefinition(SpellRegistry.SUNBEAM_SPELL),
                                defaultDefinition(SpellRegistry.ICICLE_SPELL),
                                defaultDefinition(SpellRegistry.BALL_LIGHTNING_SPELL),
                                defaultDefinition(SpellRegistry.CHAIN_LIGHTNING_SPELL),
                                defaultDefinition(SpellRegistry.LIGHTNING_LANCE_SPELL),
                                defaultDefinition(SpellRegistry.SHOCKWAVE_SPELL),

                                // ポイズンスプラッシュは後発のCloudがhurt直のためマルチキャストのi-Frame無視を処理できないが、
                                // 設置AoEにそれが効くとバランスが壊れるため、直撃の救済のみとして設定を意図する.
                                defaultDefinition(SpellRegistry.POISON_SPLASH_SPELL),

                                directDefinition(SpellRegistry.SHADOW_SLASH),
                                directDefinition(SpellRegistry.SONIC_BOOM_SPELL),
                                directDefinition(SpellRegistry.FIRECRACKER_SPELL),
                                directDefinition(SpellRegistry.FLAMING_STRIKE_SPELL),
                                directDefinition(SpellRegistry.DIVINE_SMITE_SPELL),
                                directDefinition(SpellRegistry.RAY_OF_FROST_SPELL),
                                directDefinition(SpellRegistry.LIGHTNING_BOLT_SPELL),

                                directDefinition(jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARCANE_BLAST),
                                directDefinition(jp.aquafactory.apprenticecodex.registry.SpellRegistry.SHOCK),

                                defaultDefinition(jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH),
                                directDefinitionWithLifeTime(jp.aquafactory.apprenticecodex.registry.SpellRegistry.UNITE_LUNA, 200)
                        ))
                )
        );
    }

    private static MulticastEchoStaffAttackProfileDefinition defaultDefinition(
            RegistryObject<AbstractSpell> spellRegistryObject
    ) {
        return new MulticastEchoStaffAttackProfileDefinition(
                getResourceLocationRegistry(spellRegistryObject),
                MulticastEchoStaffAttackProfile.DEFAULT
        );
    }

    private static MulticastEchoStaffAttackProfileDefinition directDefinitionWithLifeTime(
            RegistryObject<AbstractSpell> spellRegistryObject, int lifeTime
    ) {
        return new MulticastEchoStaffAttackProfileDefinition(
                getResourceLocationRegistry(spellRegistryObject),
                MulticastEchoStaffAttackProfile.GenerateDefaultWithLifeTime(lifeTime)
        );
    }

    private static MulticastEchoStaffAttackProfileDefinition directDefinition(
            RegistryObject<AbstractSpell> spellRegistryObject
    ) {
        return new MulticastEchoStaffAttackProfileDefinition(
                getResourceLocationRegistry(spellRegistryObject),
                MulticastEchoStaffAttackProfile.WITHOUT_PROJECTILE_TRACKING
        );
    }

    private static ResourceLocation getResourceLocationRegistry(RegistryObject<AbstractSpell> spellRegistryObject) {
        return ResourceLocation.fromNamespaceAndPath(
                Objects.requireNonNull(spellRegistryObject.getId()).getNamespace(),
                Objects.requireNonNull(spellRegistryObject.getId()).getPath()
        );
    }
}
