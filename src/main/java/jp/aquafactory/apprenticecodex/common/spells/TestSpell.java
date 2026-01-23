package jp.aquafactory.apprenticecodex.common.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class TestSpell extends AbstractSpell {
    // 現時点で1.20.1専用アドオンのため、deprecatedを無視する.
    @SuppressWarnings("removal") private final ResourceLocation spellId = new ResourceLocation(ApprenticeCodex.MODID, "test_spell");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(1)
            .build();

    public TestSpell() {
        // スペルパワー100 = 1ダメージ.
        baseSpellPower = 600;
        spellPowerPerLevel = 50;
        manaCostPerLevel = 10;
        baseManaCost = 5;
        castTime = 0;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        // スペルパワーはintのため、設定値をそもそも100倍として考える.
        return getSpellPower(spellLevel, entity) / 100.0f;
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
        return CastType.INSTANT;
    }
    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return super.getCastStartSound();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return super.getCastFinishSound();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            // お試しに金の剣を飛ばす.
            var item = new ItemStack(Items.GOLDEN_SWORD);
            for(var count = 0; count < 20; count++){
                var projectile = new TestBoltProjectileEntity(EntityRegistry.TEST_BOLT.get(), level, entity, item);

                // テストで頭上から少しランダムにずらす.
                var spawnPosition = entity.position().add(0, 2, 0).add(getRandomRange(level.random, 4), 0, getRandomRange(level.random, 4));

                // 視線先の対象を狙うようにする.
                var result = RaycastTools.raycastFromEye(entity, 48);
                var velocity = result.hitPosition().subtract(spawnPosition).normalize();
                var delay = Math.round(level.random.nextFloat() * 5) + 20;
                projectile.setDamage(getDamage(spellLevel, entity));
                projectile.setPos(spawnPosition);
                projectile.setProjectileVelocity(velocity, 1.8f);
                projectile.setStandbyTicks(delay);
                level.addFreshEntity(projectile);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private double getRandomRange(RandomSource random, double range){
        return (random.nextDouble() * 2 - 1) * range;
    }
}
