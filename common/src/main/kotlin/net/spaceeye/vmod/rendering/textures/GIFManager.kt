package net.spaceeye.vmod.rendering.textures

import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.spaceeye.valkyrien_ship_schematics.ELOG
import net.spaceeye.vmod.OnFinalize
import net.spaceeye.vmod.utils.MPair
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

object GIFManager {
    private val toLoad = LinkedBlockingQueue<Pair<ResourceLocation, GIFTexture>>()
    init {
        thread(isDaemon = true, name = "VMod GIF Textures loader") {
            while (true) {
                val (location, texture) = toLoad.take()

                val bytes = try {
                    resourceManager.getResourceOrThrow(location).open().readAllBytes()
                } catch (e: FileNotFoundException) {
                    ELOG("Failed to load location $location because it doesn't exist")
                    texture.loadedSuccessfully.complete(false)
                    continue
                }

                try {
                    texture.loadFromBytes(bytes)
                } catch (e: Exception) {
                    ELOG("Failed to load location $location with exception\n${e.stackTraceToString()}")
                    texture.loadedSuccessfully.complete(false)
                    continue
                }
            }
        }
    }

    /**
     * On being collected by GC will decrease ref counter of texture. Uses finalize which is probably not great but whatever
     */
    data class TextureReference(val id: String, var gif: AnimatedGIFTexture, private val wasFinalized: () -> Unit): OnFinalize() {
        override fun onFinalize() = wasFinalized()
    }

    private val storage = ConcurrentHashMap<String, MPair<Int, GIFTexture>>()
    private val resourceManager = Minecraft.getInstance().resourceManager

    private fun makeRef(id: String, pair: MPair<Int, GIFTexture>): TextureReference {
        pair.first++
        return TextureReference(id, pair.second.animated()) {
            pair.first--
            if (pair.first <= 0) {
                pair.second.close()
                storage.remove(id)
            }
        }
    }

    /**
     * SAVE REFERENCE, DO NOT SAVE TEXTURE ITSELF. Will unload location on GC if nothing references it.
     */
    fun getTextureFromLocation(location: ResourceLocation): TextureReference = synchronized(storage) {
        val strId = location.toString()
        storage[strId]?.also { return makeRef(strId, it) }

        val texture = GIFTexture().also { toLoad.add(Pair(location, it)) }
        return makeRef(strId, MPair(0, texture).also { storage[strId] = it })
    }
}