package dev.kikugie.elytratrims.render.api

import net.minecraft.client.renderer.SubmitNodeCollector
import java.util.concurrent.atomic.AtomicInteger

interface ETDecorator {
    fun render(parameters: ETParameters, collector: SubmitNodeCollector, i: AtomicInteger): Boolean
}