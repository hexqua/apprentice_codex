package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.gui.EldritchResearchScreen;
import jp.aquafactory.apprenticecodex.spell.HiddenFromEldritchResearch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(EldritchResearchScreen.class)
public abstract class EldritchResearchScreenMixin {
    // 上流は学習の要否によらず一覧を作るため、ノード配置前に表示対象だけを絞る。
    // 表示のみの補正なので、上流変更で注入先がなくなった場合は非表示機能を無効にする。
    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/registry/SpellRegistry;getEnabledSpells()Ljava/util/List;",
                    remap = false
            ),
            require = 0,
            expect = 0
    )
    private List<AbstractSpell> apprenticecodex$filterResearchSpells() {
        return SpellRegistry.getEnabledSpells().stream()
                .filter(spell -> !(spell instanceof HiddenFromEldritchResearch))
                .toList();
    }
}
