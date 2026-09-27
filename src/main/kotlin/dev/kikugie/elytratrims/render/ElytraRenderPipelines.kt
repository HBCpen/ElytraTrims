package dev.kikugie.elytratrims.render

import com.mojang.renderpearl.api.pipeline.BlendFunction
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.DepthStencilState
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import dev.kikugie.elytratrims.elytratrims
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.RenderPipelines.*

//? if >=26.3
import net.minecraft.client.renderer.oit.OitPipelineSet

private typealias Topology =
    /*? if >=26.2 {*/com.mojang.renderpearl.api.pipeline.PrimitiveTopology
    /*?} else*///com.mojang.blaze3d.vertex.VertexFormat.Mode

//? if >=26.2
private typealias BindGroups = net.minecraft.client.renderer.BindGroupLayouts

object ElytraRenderPipelines {
    @JvmField val COSMIC_SNIPPET: RenderPipeline.Snippet = RenderPipeline
        //? if >=26.2 {
        .builder(MATRICES_FOG_SNIPPET)
        .withVertexShader(elytratrims("core/elytratrims_gateway"))
        .withFragmentShader(elytratrims("core/elytratrims_gateway"))
        .withBindGroupLayout(BindGroups.SAMPLER0_SAMPLER1_SAMPLER2)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
        .withPrimitiveTopology(Topology.QUADS)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withColorTargetState(ColorTargetState.DEFAULT)
        .withCull(false)
        .buildSnippet()
        //?} elif >=26.1 {
        /*.builder(MATRICES_PROJECTION_SNIPPET, ENTITY_SNIPPET, FOG_SNIPPET, GLOBALS_SNIPPET)
        .withVertexShader(elytratrims("core/elytratrims_gateway"))
        .withFragmentShader(elytratrims("core/elytratrims_gateway"))
        .withSampler("Sampler0")
        .withSampler("Sampler1")
        .withSampler("Sampler2")
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX, Topology.QUADS)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withColorTargetState(ColorTargetState.DEFAULT)
        .withCull(false)
        .buildSnippet()
        *///?} elif >=1.21.11 {
        /*.builder(MATRICES_PROJECTION_SNIPPET, ENTITY_SNIPPET, FOG_SNIPPET, GLOBALS_SNIPPET)
        .withVertexShader(elytratrims("core/elytratrims_gateway"))
        .withFragmentShader(elytratrims("core/elytratrims_gateway"))
        .withSampler("Sampler0")
        .withSampler("Sampler1")
        .withSampler("Sampler2")
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX, Topology.QUADS)
        .withCull(false)
        .buildSnippet()
        *///?}

    //? if >=26.3 {
    @JvmField val OIT_COSMIC: OitPipelineSet = OitPipelineSet.builder(
        "elytratrims_gateway",
        RenderPipeline.builder(COSMIC_SNIPPET).withShaderDefine("PORTAL_LAYERS", 16)
    )
    .build()
    .let(::register)
    //?}

    @JvmField val COSMIC: RenderPipeline = RenderPipeline.builder(COSMIC_SNIPPET)
        .withLocation(elytratrims("pipeline/elytratrims_gateway"))
        .withShaderDefine("PORTAL_LAYERS", 16)
        .register()

    @JvmField val ELYTRA_TRANSLUCENT: RenderPipeline = RenderPipeline.builder(ENTITY_SNIPPET)
        .withLocation(elytratrims("pipeline/elytra_translucent"))
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .withShaderDefine("PER_FACE_LIGHTING")
        .withShaderDefine("NO_OVERLAY")
        .withCull(false)
        .register()

    internal fun init() {
        COSMIC; ELYTRA_TRANSLUCENT
    }

    private fun RenderPipeline.Builder.register(): RenderPipeline =
        build().let(RenderPipelines::register)
}