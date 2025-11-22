package dev.kikugie.elytratrims

import java.nio.file.Path

interface ModDataSpec {
    val isDevEnv: Boolean
    val isClient: Boolean
    val isServer: Boolean
    val gameDir: Path

    fun isModLoaded(id: String): Boolean
}

//? if fabric {
object ModData : ModDataSpec {
    private val LOADER = net.fabricmc.loader.api.FabricLoader.getInstance()

    override val isDevEnv: Boolean get() = LOADER.isDevelopmentEnvironment
    override val isClient: Boolean get() = LOADER.environmentType == net.fabricmc.api.EnvType.CLIENT
    override val isServer: Boolean get() = LOADER.environmentType == net.fabricmc.api.EnvType.SERVER
    override val gameDir: Path get() = LOADER.gameDir

    override fun isModLoaded(id: String): Boolean = LOADER.isModLoaded(id)
}
//?} else {
/*object ModData : ModDataSpec {
    private val DIST =
        /*? if >=1.21.9 {*/net.neoforged.fml.loading.FMLEnvironment.getDist()
        /*?} else*///net.neoforged.fml.loading.FMLEnvironment.dist

    private val LIST =
        /*? if >=1.21.9 {*/net.neoforged.fml.loading.FMLLoader.getCurrent().loadingModList
        /*?} else*///net.neoforged.fml.loading.FMLLoader.getLoadingModList()

    override val isDevEnv: Boolean get() = false
    override val isClient: Boolean get() = DIST == net.neoforged.api.distmarker.Dist.CLIENT
    override val isServer: Boolean get() = DIST == net.neoforged.api.distmarker.Dist.DEDICATED_SERVER
    override val gameDir: Path get() = TODO("Not yet implemented")

    override fun isModLoaded(id: String): Boolean = LIST.getModFileById(id) != null
}
*///?}