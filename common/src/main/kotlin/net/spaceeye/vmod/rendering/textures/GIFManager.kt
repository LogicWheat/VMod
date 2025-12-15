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
import kotlin.math.max

object GIFManager {
    private val toLoad = LinkedBlockingQueue<Pair<ResourceLocation, GIFTexture>>()
    private val toUnload = ConcurrentHashMap<String, Long>()
    private val processing = ConcurrentHashMap.newKeySet<ResourceLocation>()
    init {
        thread(isDaemon = true, name = "VMod GIF Textures loader", priority = Thread.MAX_PRIORITY, block = ::loadThread)
        thread(isDaemon = true, name = "VMod GIF Textures unloader", block = ::unloadThread)
    }

    //TODO i don't like it
    @JvmStatic private fun loadThread() {
        while (true) {
            val (location, texture) = toLoad.take()
            if (processing.contains(location) || storage[location.toString()]?.texture?.loadedSuccessfully?.isDone == true) continue
            processing.add(location)
            texture.loadedSuccessfully.thenAccept { processing.remove(location) }

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
            }
        }
    }

    //for debug purposes
    private var timeUnused = 1000L
    private var doUnload = true

    //TODO I don't entirely like it
    @JvmStatic private fun unloadThread() {
        while (true) {
            Thread.sleep(1000L)
            //add conditional variables here maybe
            if (toUnload.isEmpty()) continue
            synchronized(storage) {
            synchronized(toUnload) {
                if (!doUnload) return@synchronized
                val now = getNow_ms()
                val toRemoveKeys = mutableListOf<String>()
                toUnload.forEach { (k, v) ->
                    val (usages, lastUsed, texture) = storage[k] ?: return@forEach
                    if (usages != 0) { return@forEach }
                    if (now - v < timeUnused || now - lastUsed < timeUnused) { return@forEach }
                    toRemoveKeys.add(k)
                    storage.remove(k)

                    texture.close()
                }
                toRemoveKeys.forEach { toUnload.remove(it) }
            }}
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
                synchronized(toUnload) {
                    val now = getNow_ms()
                    toUnload[id] = max(toUnload[id]?:0L, now)
                }
            }
        }
    }

    private fun makeAnimatedRef(id: String, item: GIFUnit): WeakReference<AnimatedGIFTexture> {
        item.numReferences++
        item.lastTimeUsed = getNow_ms()
        return WeakReference(id, item.texture.animated()) {
            item.numReferences--
            if (item.numReferences <= 0) {
                synchronized(toUnload) {
                    val now = getNow_ms()
                    toUnload[id] = max(toUnload[id]?:0L, now)
                }
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