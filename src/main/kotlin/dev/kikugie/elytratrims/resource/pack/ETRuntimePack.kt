package dev.kikugie.elytratrims.resource.pack

import dev.kikugie.elytratrims.Identifier
import dev.kikugie.elytratrims.Text
import dev.kikugie.elytratrims.resource.pack.ETRuntimePack.Companion.LOGGER
import dev.kikugie.elytratrims.resource.provider.ETAtlasGenerator
import dev.kikugie.elytratrims.resource.provider.ETItemModelGenerator
import dev.kikugie.elytratrims.resource.provider.ETTagGenerator
import dev.kikugie.elytratrims.resource.provider.ETTextureGenerator
import dev.kikugie.elytratrims.then
import net.minecraft.FileUtil
import net.minecraft.server.packs.AbstractPackResources
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.metadata.MetadataSectionType
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.resources.IoSupplier
import net.minecraft.server.packs.resources.ResourceManager
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.streams.asSequence
import kotlin.time.measureTime

typealias InputSupplier = IoSupplier<InputStream>
typealias ResourceEntry = Pair<PackIdentifier, InputSupplier>

private val CLIENT_GENERATORS = listOf(
    ::ETAtlasGenerator,
    ::ETItemModelGenerator,
    ::ETTextureGenerator,
)
private val SERVER_GENERATORS = listOf(
    ::ETTagGenerator
)

private fun generatorsFor(type: PackType) = when(type) {
    PackType.CLIENT_RESOURCES -> CLIENT_GENERATORS
    PackType.SERVER_DATA -> SERVER_GENERATORS
}

private fun collectResources(type: PackType, manager: ResourceManager): Map<PackIdentifier, InputSupplier> {
    LOGGER.info("Generating $type")
    val data = ConcurrentHashMap<PackIdentifier, InputSupplier>()
    val time = measureTime {
        for (generator in generatorsFor(type))
            data.putAll(generator(manager).assemble())

//        val futures = generatorsFor(type).map {
//            val generator = it(manager)
//            LOGGER.info("Running ${generator::class.simpleName}")
//            generator.run().thenAccept(data::putAll)
//        }
//
//        CompletableFuture.allOf(*futures.toTypedArray()).join()
    }

    LOGGER.info("Collected ${data.size} ${type.directory} resources in $time")
    return data
}

class ETRuntimePack(type: PackType, val manager: ResourceManager) : AbstractPackResources(ET_PACK_INFO) {
    private val packs get() = manager.listPacks().asSequence()
    private val resources: Map<PackIdentifier, InputSupplier> by lazy { collectResources(type, manager) }
    private val namespaces: Map<PackType, Set<String>> by lazy {
        buildMap<PackType, MutableSet<String>> {
            for (pack in packs) for (type in PackType.entries)
                getOrPut(type) { mutableSetOf() } += pack.getNamespaces(type)
        }
    }
    private val base: PackResources by lazy {
        requireNotNull(packs.find { "elytratrims" in it.packId() }) {
            "Missing 'elytratrims' pack, present packs:\n" + packs.joinToString("\n") { "  - ${it.packId()}" }
        }
    }

    override fun getNamespaces(type: PackType): Set<String> = namespaces[type] ?: emptySet()

    override fun getRootResource(vararg strings: String?): InputSupplier? =
        FileUtil.validatePath(*strings) then open(strings.joinToString("/"))

    override fun getResource(packType: PackType, identifier: Identifier): InputSupplier? =
        open(getFilename(packType, identifier))

    override fun <T> getMetadataSection(reader: MetadataSectionType<T?>): T? =
        open("pack.mcmeta")?.get()?.use { getMetadataFromStream(reader, it, ET_PACK_INFO) }

    override fun listResources(type: PackType, namespace: String, path: String, visitor: PackResources.ResourceOutput): Unit =
        locate(PackIdentifier.of(type, namespace, path)).forEach { (id, supplier) -> visitor.accept(id.toIdentifier(), supplier) }

    override fun close() {
    }

    private fun open(file: String): InputSupplier? =
        if ('/' !in file) base.getRootResource(file)
        else resources[PackIdentifier.of(file)]

    private fun locate(path: PackIdentifier): Sequence<Map.Entry<PackIdentifier, InputSupplier>> = resources.entries.asSequence()
        .filter { path.type == it.key.type && path.namespace == it.key.namespace && it.key.path.startsWith(path.path) }

    private fun getFilename(type: PackType, identifier: Identifier) =
        "${type.directory}/${identifier.namespace}/${identifier.path}"

    companion object {
        @JvmField val LOGGER = LoggerFactory.getLogger("ETRuntimePack")

        @JvmField val ET_PACK_INFO: PackLocationInfo =
            PackLocationInfo("elytratrims-generated", Text.literal("Elytra Trims Generated"), PackSource.BUILT_IN, Optional.empty())

        @Suppress("RedundantNullableReturnType")
        @JvmStatic fun of(unchecked: PackType, manager: ResourceManager): ETRuntimePack? {
            // Stupid neoforge loading client side stuff on the server
            //? if fabric {
            return ETRuntimePack(unchecked, manager)
            //?} else {
            /*return if (unchecked == PackType.CLIENT_RESOURCES && ModData.isServer) null
            else ETRuntimePack(unchecked, manager)
            *///?}
        }
    }
}