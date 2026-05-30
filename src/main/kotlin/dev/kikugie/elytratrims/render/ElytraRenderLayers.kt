package dev.kikugie.elytratrims.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.resources.Identifier
import dev.kikugie.elytratrims.Memoizer
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.memoize
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.RenderPipelines.*
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType

private typealias Topology =
    /*? if >=26.2 {*//*com.mojang.blaze3d.PrimitiveTopology
    *//*?} else*/com.mojang.blaze3d.vertex.VertexFormat.Mode

//? if >=26.2
//private typealias BindGroups = net.minecraft.client.renderer.BindGroupLayouts

object ElytraRenderLayers {
    //? if >=26.2 {
    /*@JvmField val GATEWAY_MASKED_SNIPPET: RenderPipeline.Snippet = RenderPipeline
        .builder(MATRICES_FOG_SNIPPET)
        .withVertexShader(elytratrims("core/elytratrims_gateway"))
        .withFragmentShader(elytratrims("core/elytratrims_gateway"))
        .withBindGroupLayout(BindGroups.SAMPLER0_SAMPLER1_SAMPLER2)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
        .withPrimitiveTopology(Topology.QUADS)
        .withDepthStencilState(com.mojang.blaze3d.pipeline.DepthStencilState.DEFAULT)
        .withCull(false)
        .buildSnippet()
    *///?} elif >=26.1 {
    @JvmField val GATEWAY_MASKED_SNIPPET: RenderPipeline.Snippet = RenderPipeline
        .builder(MATRICES_PROJECTION_SNIPPET, ENTITY_SNIPPET, FOG_SNIPPET, GLOBALS_SNIPPET)
        .withVertexShader(elytratrims("core/elytratrims_gateway"))
        .withFragmentShader(elytratrims("core/elytratrims_gateway"))
        .withSampler("Sampler0")
        .withSampler("Sampler1")
        .withSampler("Sampler2")
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX, Topology.QUADS)
        .withCull(false)
        .buildSnippet()
    //?} elif >=1.21.11 {
    /*@JvmField val GATEWAY_MASKED_SNIPPET: RenderPipeline.Snippet = RenderPipeline
        .builder(MATRICES_PROJECTION_SNIPPET, ENTITY_SNIPPET, FOG_SNIPPET, GLOBALS_SNIPPET)
        .withVertexShader(elytratrims("core/elytratrims_gateway"))
        .withFragmentShader(elytratrims("core/elytratrims_gateway"))
        .withSampler("Sampler0")
        .withSampler("Sampler1")
        .withSampler("Sampler2")
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX, Topology.QUADS)
        .withCull(false)
        .buildSnippet()
    *///?}

    @JvmField val GATEWAY_MASKED: RenderPipeline = RenderPipeline.builder(GATEWAY_MASKED_SNIPPET)
        .withLocation(elytratrims("pipeline/elytratrims_gateway"))
        .withShaderDefine("PORTAL_LAYERS", 16)
        .build().let(RenderPipelines::register)

    @JvmField val GATEWAY: Memoizer<Identifier, RenderType> = memoize {
        val builder = RenderSetup.builder(GATEWAY_MASKED)
            .withTexture("Sampler0", TheEndPortalRenderer.END_SKY_LOCATION)
            .withTexture("Sampler1", TheEndPortalRenderer.END_PORTAL_LOCATION)
            .withTexture("Sampler2", it)
            .createRenderSetup()
        RenderType.create("elytra_gateway", builder)
    }
}