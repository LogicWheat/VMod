package net.spaceeye.vmod.rendering.textures

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.utils.Vector3d
import java.util.concurrent.CompletableFuture

class AnimatedGIFTexture(val gif: GIFTexture): AutoCloseable {
    var currentFrame: Int = 0

    /**
     * 1/100th of a second
     */
    var time = 0f

    //https://usage.imagemagick.org/anim_basics/
    /**
     * @param delta in milliseconds
     */
    fun advanceTime(delta: Float): Boolean {
        if (!gif.loadedSuccessfully.isDone || !gif.loadedSuccessfully.get()) return false

        // gif uses 1/100 th of a second and not milliseconds
        time += delta / 10f
        var delay = gif.sprites[currentFrame / gif.framesPerSprite].delays[currentFrame % gif.framesPerSprite]
        if (time <= delay) return false

        //if delay is over a second then just reset
        if (time > 100f) { time = 0f }

        while (true) {
            time -= delay
            currentFrame++
            if (currentFrame >= gif.totalFrames) { currentFrame = 0 }

            delay = gif.sprites[currentFrame / gif.framesPerSprite].delays[currentFrame % gif.framesPerSprite]
            if (time <= delay) {break}
        }

        return true
    }

    fun blit(pose: PoseStack, x: Int, y: Int, uWidth: Int, vHeight: Int) {
        gif.blit(pose, currentFrame, x, y, uWidth, vHeight)
    }

    fun draw(pose: PoseStack, lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d) {
        gif.draw(pose, currentFrame, lu, ld, rd, ru)
    }

    override fun close() {
        gif.close()
    }

    fun reset() {
        currentFrame = 0
        time = 0f
    }
}

//TODO Current impl is pretty wasteful and needs to be redone
// 1) GIFTexture shouldn't load long gifs completely, instead it should do double buffering (although i'm not sure how feasible it with current GIFReader)
// 2) I should probably figure out how to use Texture Array instead of current atlas approach
class GIFTexture(): AbstractTexture() {
    var sprites = mutableListOf<SlidingFrameTexture>()
    var width = 0
    var height = 0
    var totalFrames = 0
    var framesPerSprite = 0

    val loadedSuccessfully = CompletableFuture<Boolean>()

    fun loadFromBytes(bytes: ByteArray) {
        val textures = GIFReader.readGifToTexturesFaster(bytes)
        for (texture in textures) {
            sprites.add(SlidingFrameTexture(texture))
            totalFrames += texture.numFrames
        }
        width = textures[0].frameWidth
        height = textures[0].frameHeight
        framesPerSprite = sprites[0].numFrames

        loadedSuccessfully.complete(true)
    }

    override fun load(resourceManager: ResourceManager) {}

    override fun close() {
        if (!loadedSuccessfully.isDone) {
            loadedSuccessfully.thenAccept {
                sprites.forEach { it.close() }
            }
            return
        }
        sprites.forEach { it.close() }
    }

    fun blit(pose: PoseStack, frameNum: Int, x: Int, y: Int, uWidth: Int, vHeight: Int) {
        if (!loadedSuccessfully.isDone) {
            RenderSystem.setShaderTexture(0, tempTextureLocation)
            return tempBlit(pose, x, y, uWidth, vHeight)
        }
        if (!loadedSuccessfully.get()) {
            RenderSystem.setShaderTexture(0, dummyLocation)
            return tempBlit(pose, x, y, uWidth, vHeight)
        }

        sprites[frameNum / framesPerSprite].blit(pose, frameNum, x, y, uWidth, vHeight)
    }

    fun draw(pose: PoseStack, frameNum: Int, lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d) {
        if (!loadedSuccessfully.isDone) {
            RenderSystem.setShaderTexture(0, tempTextureLocation)
            return tempDraw(pose, lu, ld, rd, ru)
        }
        if (!loadedSuccessfully.get()) {
            RenderSystem.setShaderTexture(0, dummyLocation)
            return tempDraw(pose, lu, ld, rd, ru)
        }

        sprites[frameNum / framesPerSprite].draw(pose, frameNum, lu, ld, rd, ru)
    }

    fun animated(): AnimatedGIFTexture = AnimatedGIFTexture(this)

    companion object {
        //will cause missing texture
        private val dummyLocation = ResourceLocation(MOD_ID, "missing")
        private val tempTextureLocation = ResourceLocation(MOD_ID, "textures/misc/loading.png")

        private fun tempBlit(pose: PoseStack, x: Int, y: Int, uWidth: Int, vHeight: Int) {
            SlidingFrameTexture.innerDraw(
                pose,
                Vector3d(x,          y,           0),
                Vector3d(x,          y + vHeight, 0),
                Vector3d(x + uWidth, y + vHeight, 0),
                Vector3d(x + uWidth, y          , 0),
                0f, 1f, 0f, 1f
            )
        }

        private fun tempDraw(pose: PoseStack, lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d) {
            SlidingFrameTexture.innerDraw(pose, lu, ld, rd, ru, 0f, 1f, 0f, 1f)
        }
    }
}