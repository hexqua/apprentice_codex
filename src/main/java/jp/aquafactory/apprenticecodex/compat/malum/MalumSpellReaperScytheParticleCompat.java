package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.visual_effects.networked.MalumNetworkedWeaponParticleEffectType.MalumWeaponParticleEffectBuilder;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.world.item.ItemStack;
import team.lodestar.lodestone.systems.network.WeaponParticleEffectType;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;

import java.awt.Color;

public final class MalumSpellReaperScytheParticleCompat {
    private MalumSpellReaperScytheParticleCompat() {
    }

    public static <T extends WeaponParticleEffectType.WeaponParticleEffectData>
    MalumWeaponParticleEffectBuilder<T> applyImbueSchoolColor(
            MalumWeaponParticleEffectBuilder<T> particle,
            ItemStack stack
    ) {
        var school = MagicTools.getImbuedSpellSchool(stack);
        var schoolColor = new Color(MagicTools.resolveSchoolTintColor(school));
        return particle.color(ColorParticleData.create(schoolColor, Color.WHITE).build());
    }
}
