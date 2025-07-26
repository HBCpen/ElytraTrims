package dev.kikugie.elytratrims.api.impl

import dev.kikugie.elytratrims.api.render.ETDecorator
import dev.kikugie.elytratrims.api.render.ETRenderMethod
import dev.kikugie.elytratrims.api.render.ETRenderParameters
import dev.kikugie.elytratrims.api.render.ETRendererID
import dev.kikugie.elytratrims.api.render.ETRenderingAPI
import dev.kikugie.elytratrims.render.RENDER_LOGGER
import org.jetbrains.annotations.ApiStatus
import java.util.function.BiConsumer
import java.util.function.Function

private typealias Callback = Function<ETRenderParameters, ETRenderParameters>

@ApiStatus.Internal
object ETRenderingAPIImpl {
    private val renderersByMethod: MutableMap<ETRenderMethod, MutableList<ETDecorator>> = mutableMapOf()
    private val parameterWrappers: MutableMap<ETRendererID, Callback> = mutableMapOf()
    private val renderWrappers: MutableMap<ETRendererID, ETRenderingAPI.Callback> = mutableMapOf()

    @JvmStatic fun getDecorators(): Sequence<ETDecorator> =
        renderersByMethod.values.asSequence().flatten()

    @JvmStatic fun registerDecorator(
        renderer: ETDecorator,
        adder: BiConsumer<MutableList<ETDecorator>, ETDecorator>
    ): ETRendererID = renderer.type.apply {
        val list = renderersByMethod.getOrPut(method) { mutableListOf() }
        if (list.isNotEmpty()) {
            val match = list.find { it.type.identifier == identifier }
            require(match == null) { "Decorator $identifier is already registered as ${match!!::class.qualifiedName}" }
        }
        RENDER_LOGGER.info("Registering decorator ${renderer.type}")
        adder.accept(list, renderer)
    }

    @JvmStatic fun wrapRenderParameters(type: ETRendererID, parameters: Callback) {
        parameterWrappers.compute(type) { _, it ->
            if (it == null) parameters else it.andThen(parameters)
        }
    }

    @JvmStatic fun wrapRenderCall(type: ETRendererID, callback: ETRenderingAPI.Callback) {
        renderWrappers.compute(type) { _, current: ETRenderingAPI.Callback? ->
            if (current == null) callback else ETRenderingAPI.Callback { parameters, original ->
                callback.render(parameters) { current.render(it, original) }
            }
        }
    }

    @JvmStatic fun render(parameters: ETRenderParameters, method: ETRenderMethod): Boolean {
        var aggregate = false
        for (renderer in renderersByMethod[method].orEmpty())
            aggregate = renderer.completeRender(parameters) || aggregate
        return aggregate
    }

    private fun ETDecorator.composeParameters(parameters: ETRenderParameters): ETRenderParameters =
        parameterWrappers[type]?.apply(parameters) ?: parameters

    private fun ETDecorator.composeRender(parameters: ETRenderParameters, operation: ETRenderParameters.() -> Boolean): Boolean =
        renderWrappers[type]?.render(parameters, operation) ?: operation(parameters)

    private fun ETDecorator.completeRender(parameters: ETRenderParameters): Boolean {
        val parameters = composeParameters(prepare(parameters))
        return composeRender(parameters, ::render)
    }
}