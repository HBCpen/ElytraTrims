package dev.kikugie.elytratrims.api.impl

import com.google.common.collect.ImmutableMap
import dev.kikugie.elytratrims.api.ETDecorator
import dev.kikugie.elytratrims.api.ETRenderingAPI
import dev.kikugie.elytratrims.api.ETRenderMethod
import dev.kikugie.elytratrims.api.ETRenderParameters
import dev.kikugie.elytratrims.api.ETRendererType
import org.jetbrains.annotations.ApiStatus
import java.util.function.Function

private typealias Callback = Function<ETRenderParameters, ETRenderParameters>

@ApiStatus.Internal
object ETRenderingAPIImpl {
    private val renderersByType: MutableMap<ETRendererType, ETDecorator> = mutableMapOf()
    private val parameterWrappers: MutableMap<ETRendererType, Callback> = mutableMapOf()
    private val renderWrappers: MutableMap<ETRendererType, ETRenderingAPI.Callback> = mutableMapOf()

    @JvmStatic fun getDecorators(): Map<ETRendererType, ETDecorator> =
        ImmutableMap.copyOf(renderersByType)

    @JvmStatic fun registerDecorator(type: ETRendererType, renderer: ETDecorator) : ETRendererType = type.also {
        renderersByType.compute(type) { _, current ->
            require(current == null) { "Decorator ${type.identifier} is already registered as ${current!!::class.qualifiedName}" }
            renderer
        }
    }

    @JvmStatic fun wrapRenderParameters(type: ETRendererType, parameters: Callback) {
        parameterWrappers.compute(type) { _, it ->
            if (it == null) parameters else it.andThen(parameters)
        }
    }

    @JvmStatic fun wrapRenderCall(type: ETRendererType, callback: ETRenderingAPI.Callback) {
        renderWrappers.compute(type) { _, current: ETRenderingAPI.Callback? ->
            if (current == null) callback else ETRenderingAPI.Callback { parameters, original ->
                callback.render(parameters) { current.render(it, original) }
            }
        }
    }

    @JvmStatic fun render(parameters: ETRenderParameters, method: ETRenderMethod): Boolean {
        var aggregate = false
        for ((type, renderer) in renderersByType) if (type.method == method)
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