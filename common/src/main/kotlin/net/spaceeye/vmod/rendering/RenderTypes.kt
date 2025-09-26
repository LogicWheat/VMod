package net.spaceeye.vmod.rendering

import com.google.common.collect.ImmutableMap
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance

object RenderTypes {
    val VERTEX_FORMAT = VertexFormat(ImmutableMap.copyOf(mapOf(
        "Position" to DefaultVertexFormat.ELEMENT_POSITION,
        "Color"    to DefaultVertexFormat.ELEMENT_COLOR,
        "UV0"      to DefaultVertexFormat.ELEMENT_UV0,
        "UV1"      to DefaultVertexFormat.ELEMENT_UV1,
        "UV2"      to DefaultVertexFormat.ELEMENT_UV2,
        "Normal"   to DefaultVertexFormat.ELEMENT_NORMAL

    )))

    @JvmStatic lateinit var textureArrayShader: ShaderInstance
    @JvmStatic private var RENDERTYPE_TEXTURE_ARRAY_SHADER = RenderStateShard.ShaderStateShard {textureArrayShader}

    @JvmStatic private fun textureArrayShader(): RenderType {
        val state = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_TEXTURE_ARRAY_SHADER)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setOverlayState(RenderStateShard.OVERLAY)
            .createCompositeState(false)

        return RenderType.create("texture_array_full", VERTEX_FORMAT, VertexFormat.Mode.QUADS, 256, state)
    }

    @JvmStatic var TEXTURE_ARRAY_FULL = textureArrayShader()
}