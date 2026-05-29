package jp.aquafactory.apprenticecodex.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class ApprenticeRenderTypes extends RenderStateShard {
    private static final ResourceLocation BOUND_SPELL_WEAPON_GLINT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/bound_spell_weapon_glint.png");
    private static final RenderType BOUND_SPELL_WEAPON_GLINT = createBoundSpellWeaponGlint(
            "bound_spell_weapon_glint",
            RenderStateShard.RENDERTYPE_GLINT_SHADER,
            RenderStateShard.GLINT_TEXTURING
    );
    private static final RenderType BOUND_SPELL_WEAPON_GLINT_DIRECT = createBoundSpellWeaponGlint(
            "bound_spell_weapon_glint_direct",
            RenderStateShard.RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER,
            RenderStateShard.ENTITY_GLINT_TEXTURING
    );

    public ApprenticeRenderTypes(String pName, Runnable pSetupState, Runnable pClearState) {
        super(pName, pSetupState, pClearState);
    }

    public static RenderType boundSpellWeaponGlint() {
        return BOUND_SPELL_WEAPON_GLINT;
    }

    public static RenderType boundSpellWeaponGlintDirect() {
        return BOUND_SPELL_WEAPON_GLINT_DIRECT;
    }

    private static RenderType createBoundSpellWeaponGlint(String renderTypeName,
                                                          RenderStateShard.ShaderStateShard shaderState,
                                                          RenderStateShard.TexturingStateShard texturingState) {
        return RenderType.create(
                renderTypeName,
                DefaultVertexFormat.POSITION_TEX,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(shaderState)
                        .setTextureState(new RenderStateShard.TextureStateShard(BOUND_SPELL_WEAPON_GLINT_TEXTURE, true, false))
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                        .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                        .setTexturingState(texturingState)
                        .createCompositeState(false)
        );
    }

    public static RenderType beamNoCull(ResourceLocation tex) {
        return RenderType.create(
                "beam_no_cull",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_BEACON_BEAM_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .createCompositeState(true)
        );
    }

    // 加算合成の glow 用 RenderType。発光自体は呼び出し側で FULL_BRIGHT を渡して成立させる。
    public static RenderType entityAdditiveGlowNoCull(String renderTypeName, ResourceLocation tex) {
        return RenderType.create(
                renderTypeName,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                        .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .createCompositeState(true)
        );
    }

    // 同一エフェクト内の加算レイヤー同士が depth write で潰し合わないよう、Zテストだけ残して色だけ書く。
    public static RenderType entityAdditiveGlowNoCullColorOnly(String renderTypeName, ResourceLocation tex) {
        return RenderType.create(
                renderTypeName,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                        .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .createCompositeState(true)
        );
    }

    public static RenderType entityTranslucentNoCull(String renderTypeName, ResourceLocation tex) {
        return RenderType.create(
                renderTypeName,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .createCompositeState(true)
        );
    }

    // 奥行き無視の加算 glow 用 RenderType。発光自体は呼び出し側で FULL_BRIGHT を渡して成立させる。
    public static RenderType entityAdditiveGlowNoCullNoDepth(String renderTypeName, ResourceLocation tex) {
        return RenderType.create(
                renderTypeName,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                        .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .createCompositeState(true)
        );
    }

    public static RenderType color(String renderTypeName) {
        return RenderType.create(
                renderTypeName,
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                        .setTextureState(RenderStateShard.NO_TEXTURE)
                        .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                        .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                        .setCullState(RenderStateShard.CULL)
                        .createCompositeState(false)
        );
    }

    public static RenderType colorNoCull(String renderTypeName) {
        return RenderType.create(
                renderTypeName,
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                        .setTextureState(RenderStateShard.NO_TEXTURE)
                        .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                        .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                        .setCullState(RenderStateShard.NO_CULL)
                        .createCompositeState(false)
        );
    }

    public static RenderType translucentColorNoCull(String renderTypeName) {
        return RenderType.create(
                renderTypeName,
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                        .setTextureState(RenderStateShard.NO_TEXTURE)
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                        .setCullState(RenderStateShard.NO_CULL)
                        .createCompositeState(true)
        );
    }

    // テクスチャなしの加算合成。POSITION_COLOR は lightmap を持たないため常に明るい色として描く。
    public static RenderType additiveColorNoCull(String renderTypeName) {
        return RenderType.create(
                renderTypeName,
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                        .setTextureState(RenderStateShard.NO_TEXTURE)
                        .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                        .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                        .setCullState(RenderStateShard.NO_CULL)
                        .createCompositeState(true)
        );
    }

}
