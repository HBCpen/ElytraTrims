package dev.kikugie.elytratrims.resource.reload

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withContext
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier
import net.minecraft.server.packs.resources.PreparableReloadListener.SharedState
import net.minecraft.server.packs.resources.PreparableReloadListener.StateKey
import net.minecraft.server.packs.resources.ResourceManager
import org.jetbrains.annotations.ApiStatus
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * A mod-loader-independent implementation of a [PreparableReloadListener] inspired by
 * the Fabric [SimpleResourceReloader](https://github.com/FabricMC/fabric/blob/1.21.10/fabric-resource-loader-v1/src/main/java/net/fabricmc/fabric/api/resource/v1/reloader/SimpleResourceReloader.java),
 * with additional Kotlin features.
 */
interface CoroutineReloadListener<T> : PreparableReloadListener {
    suspend fun load(state: StateWrapper): T
    suspend fun apply(state: StateWrapper, data: T)
    fun prepare(state: StateWrapper) {
    }

    @ApiStatus.NonExtendable
    override fun prepareSharedState(state: SharedState) {
        prepare(StateWrapper(state))
    }

    @ApiStatus.NonExtendable
    @OptIn(DelicateCoroutinesApi::class)
    override fun reload(
        state: SharedState,
        preparator: Executor,
        sync: PreparationBarrier,
        applier: Executor
    ): CompletableFuture<Void> {
        val wrapper = StateWrapper(state)
        return GlobalScope
            .future(preparator.asCoroutineDispatcher()) { load(wrapper) }
            .thenCompose(sync::wait)
            .thenApply { GlobalScope.future(applier.asCoroutineDispatcher()) { apply(wrapper, it) }; null }
    }

    @JvmInline @Suppress("NOTHING_TO_INLINE")
    value class StateWrapper(val delegate: SharedState) {
        val manager: ResourceManager inline get() = delegate.resourceManager()

        inline operator fun <T> get(key: StateKey<T>): T = delegate.get(key)
        inline operator fun <T> set(key: StateKey<T>, value: T): Unit = delegate.set(key, value)
    }
}