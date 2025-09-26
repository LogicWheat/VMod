package net.spaceeye.vmod.rendering.textures

import com.mojang.blaze3d.platform.TextureUtil
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.spaceeye.vmod.OnFinalize
import net.spaceeye.vmod.rendering.RenderTypes
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.utils.Vector3d
import org.lwjgl.opengl.GL46
import org.lwjgl.system.MemoryUtil
import java.util.concurrent.CompletableFuture

open class ArrayTexture(ptr: Long, val width: Int, val height: Int, val numLayers: Int): OnFinalize() {
    var ptr: Long = ptr
        private set

    var id: Int = 0
        private set

    private var uploaded = false

    protected inline fun onRenderThread(crossinline fn: () -> Unit) {
        if (RenderSystem.isOnRenderThread()) {
            fn()
            return
        } else {
            RenderSystem.recordRenderCall { fn() }
        }
    }

    fun upload(): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        onRenderThread {
            if (uploaded) throw AssertionError("uploaded twice")

            id = TextureUtil.generateTextureId()
            bind()

            GL46.glTexStorage3D(GL46.GL_TEXTURE_2D_ARRAY, 1, GL46.GL_RGBA8, width, height, numLayers)
            GL46.glTexSubImage3D(GL46.GL_TEXTURE_2D_ARRAY, 0, 0, 0, 0, width, height, numLayers, GL46.GL_RGBA, GL46.GL_UNSIGNED_BYTE, ptr)

            GL46.glTexParameteri(GL46.GL_TEXTURE_2D_ARRAY, GL46.GL_TEXTURE_WRAP_S, GL46.GL_REPEAT)
            GL46.glTexParameteri(GL46.GL_TEXTURE_2D_ARRAY, GL46.GL_TEXTURE_WRAP_T, GL46.GL_REPEAT)
            GL46.glTexParameteri(GL46.GL_TEXTURE_2D_ARRAY, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_NEAREST)
            GL46.glTexParameteri(GL46.GL_TEXTURE_2D_ARRAY, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_NEAREST)

            future.complete(true)
            uploaded = true
        }
        return future
    }

    private fun bind() = onRenderThread {
        GL46.glBindTexture(GL46.GL_TEXTURE_2D_ARRAY, id)
    }

    fun setAsShaderTexture(i: Int = 0) = onRenderThread {
        GL46.glActiveTexture(GL46.GL_TEXTURE0 + i)
        bind()
    }

    fun close() = onRenderThread {
        TextureUtil.releaseTextureId(id)
        freeRAM()
    }

    fun freeRAM() {
        if (ptr == -1L) return
        MemoryUtil.nmemFree(ptr)
        ptr = -1
    }

    override fun onFinalize() {
        close()
    }

    fun blit(pose: PoseStack, frameNum: Int, x: Int, y: Int, uWidth: Int, vHeight: Int, u0: Float = 0f, u1: Float = 1f, v0: Float = 0f, v1: Float = 1f) {
        setAsShaderTexture(0)
        innerDraw(
            pose, numLayers, frameNum,
            Vector3d(x, y, 0),
            Vector3d(x, y + vHeight, 0),
            Vector3d(x + uWidth, y + vHeight, 0),
            Vector3d(x + uWidth, y, 0),
            u0, u1, v0, v1
        )
    }

    fun draw(pose: PoseStack, frameNum: Int, lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d, u0: Float = 0f, u1: Float = 1f, v0: Float = 0f, v1: Float = 1f) {
        setAsShaderTexture(0)
        innerDraw(pose, numLayers, frameNum, lu, ld, rd, ru, u0, u1, v0, v1)
    }

    fun innerDraw(pose: PoseStack, maxLayers: Int, currentLayer: Int, lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d, minU: Float, maxU: Float, minV: Float, maxV: Float) {
        RenderTypes.TEXTURE_ARRAY_FULL.setupRenderState()
        RenderTypes.textureArrayShader.getUniform("MaxLayers")!!.set(maxLayers)
        RenderTypes.textureArrayShader.getUniform("CurrentLayer")!!.set(currentLayer)
        RenderTypes.textureArrayShader.setSampler("texture_array", -1)

        val matrix4f = pose.last().pose()
        val tesselator = Tesselator.getInstance()
        val bufferBuilder = tesselator.builder
        bufferBuilder.begin(VertexFormat.Mode.QUADS, RenderTypes.VERTEX_FORMAT)
        RenderingUtils.Quad.drawQuad(bufferBuilder, matrix4f, 255, 255, 255, 255, 0, 0, lu, ld, rd, ru, minU, maxU, minV, maxV)
        tesselator.end()

        RenderTypes.TEXTURE_ARRAY_FULL.clearRenderState()
    }
}