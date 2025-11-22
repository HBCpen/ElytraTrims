package dev.kikugie.elytratrims

typealias Identifier = net.minecraft.resources.ResourceLocation
typealias ResourcePack = net.minecraft.server.packs.PackResources
typealias Text = net.minecraft.network.chat.Component
typealias MutableText = net.minecraft.network.chat.MutableComponent

typealias RenderConsumer =
    /*? if >=1.21.9 {*/net.minecraft.client.renderer.SubmitNodeCollector
    /*?} else*///net.minecraft.client.renderer.MultiBufferSource