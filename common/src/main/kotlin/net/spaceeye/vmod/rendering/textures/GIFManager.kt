package net.spaceeye.vmod.rendering.textures

import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.spaceeye.valkyrien_ship_schematics.ELOG
import net.spaceeye.vmod.OnFinalize
import net.spaceeye.vmod.utils.getNow_ms
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

object GIFManager {
    private val toLoad = LinkedBlockingQueue<Pair<ResourceLocation, GIFTexture>>()
    private val toUnload = LinkedBlockingQueue<Pair<String, Long>>()
    init {
        thread(isDaemon = true, name = "VMod GIF Textures loader", priority = Thread.MAX_PRIORITY) {
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

        //TODO i don't like it
        thread(isDaemon = true, name = "VMod GIF Textures unloader") {
            val temp = ArrayDeque<Pair<String, Long>>()
            val timeUnused = 1000L
            while (true) {
                Thread.sleep(1000L)
                temp.add(toUnload.take()) //will await until toUnload is not empty
                toUnload.drainTo(temp) //will drain the rest
                synchronized(storage) {
                    val now = getNow_ms()
                    temp.forEach {
                        val (usages, lastUsed, texture) = storage[it.first] ?: return@forEach
                        if (usages != 0) { return@forEach }
                        if (now - it.second < timeUnused || now - lastUsed < timeUnused) {
                            toUnload.add(it)
                            return@forEach
                        }
                        storage.remove(it.first)
                        texture.close()
                    }
                }
                temp.clear()
            }
        }
    }

    /**
     * On being collected by GC will decrease ref counter of texture. Uses finalize which is probably not great but whatever
     */
    data class WeakReference<T>(val id: String, var it: T, private val wasFinalized: () -> Unit): OnFinalize(), AutoCloseable {
        private var closed = false
        override fun onFinalize() {
            if (!closed) wasFinalized()
            closed = true
        }

        override fun close() {
            if (closed) return
            closed = true
            wasFinalized()
        }
    }

    private data class GIFUnit(var numReferences: Int, var lastTimeUsed: Long, var texture: GIFTexture)

    private val storage = ConcurrentHashMap<String, GIFUnit>()
    private val resourceManager = Minecraft.getInstance().resourceManager

    private fun makeRef(id: String, item: GIFUnit): WeakReference<GIFTexture> {
        item.numReferences++
        item.lastTimeUsed = getNow_ms()
        return WeakReference(id, item.texture) {
            item.numReferences--
            if (item.numReferences <= 0) {
                toUnload.add(Pair(id, getNow_ms()))
            }
        }
    }

    private fun makeAnimatedRef(id: String, item: GIFUnit): WeakReference<AnimatedGIFTexture> {
        item.numReferences++
        item.lastTimeUsed = getNow_ms()
        return WeakReference(id, item.texture.animated()) {
            item.numReferences--
            if (item.numReferences <= 0) {
                toUnload.add(Pair(id, getNow_ms()))
            }
        }
    }

    /**
     * SAVE REFERENCE, DO NOT SAVE TEXTURE ITSELF. Will unload location on GC if nothing references it.
     */
    fun getTextureFromLocation(location: ResourceLocation): WeakReference<GIFTexture> = synchronized(storage) {
        val strId = location.toString()
        storage[strId]?.also { return makeRef(strId, it) }

        val texture = GIFTexture().also { toLoad.add(Pair(location, it)) }
        return makeRef(strId, GIFUnit(0, getNow_ms(), texture).also { storage[strId] = it })
    }

    /**
     * SAVE REFERENCE, DO NOT SAVE TEXTURE ITSELF. Will unload location on GC if nothing references it.
     */
    fun getAnimatedTextureFromLocation(location: ResourceLocation): WeakReference<AnimatedGIFTexture> = synchronized(storage) {
        val strId = location.toString()
        storage[strId]?.also { return makeAnimatedRef(strId, it) }

        val texture = GIFTexture().also { toLoad.add(Pair(location, it)) }
        return makeAnimatedRef(strId, GIFUnit(0, getNow_ms(), texture).also { storage[strId] = it })
    }
}