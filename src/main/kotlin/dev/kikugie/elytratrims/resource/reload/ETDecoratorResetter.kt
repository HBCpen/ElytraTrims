package dev.kikugie.elytratrims.resource.reload

import dev.kikugie.elytratrims.api.impl.ETRenderingAPIImpl
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.Unit
import net.minecraft.util.profiling.Profiler
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

object ETDecoratorResetter : PreparableReloadListener {
    override fun reload(
        sharedState: PreparableReloadListener.SharedState,
        executor: Executor,
        preparationBarrier: PreparableReloadListener.PreparationBarrier,
        executor2: Executor
    ): CompletableFuture<Void> = preparationBarrier.wait(Unit.INSTANCE).thenRun {
        with(Profiler.get()) {
            push("reset elytra decorators")
            for (it in ETRenderingAPIImpl.getDecorators())
                it.reset()
            pop()
        }
    }
}