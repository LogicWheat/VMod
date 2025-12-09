package net.spaceeye.vmod.rendering.textures

import com.mojang.blaze3d.platform.NativeImage
import net.spaceeye.vmod.GLMaxArrayTextureLayers
import net.spaceeye.vmod.gif.GIFImageReader
import net.spaceeye.vmod.gif.GIFImageReaderSpi
import net.spaceeye.vmod.mixin.NativeImageInvoker
import org.lwjgl.system.MemoryUtil
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.InputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.ImageInputStreamImpl
import kotlin.math.max

class WrappedByteArrayInputStream(val array: ByteArray): ImageInputStreamImpl() {
    override fun read(): Int {
        if (streamPos >= array.size) return -1
        val ret = array[streamPos.toInt()].toUByte().toInt()
        streamPos++
        return ret
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        if (b == null) throw NullPointerException("b == null!")
        if (off < 0 || len < 0 || off + len > b.size || off + len < 0) {
            throw IndexOutOfBoundsException("off < 0 || len < 0 || off+len > b.length || off+len < 0!")
        }

        bitOffset = 0

        if (len == 0) return 0
        if (streamPos + len > array.size) return -1

        var i = 0
        while (i < len) {
            b[off + i] = array[streamPos.toInt() + i]
            i++
        }
        streamPos += len

        return len
    }

    override fun length(): Long = array.size.toLong()

    override fun isCached(): Boolean = true
    override fun isCachedFile(): Boolean = true
    override fun isCachedMemory(): Boolean = true
}

//https://stackoverflow.com/a/18425922
object GIFReader {
    val reader = ImageIO.getImageReadersByFormatName("gif").next()

    data class ImageFrame(val image: BufferedImage, val delay: Int, val disposal: String)
    @JvmStatic fun readGIF(stream: InputStream): MutableList<ImageFrame> {
        reader.setInput(ImageIO.createImageInputStream(stream))

        val frames = mutableListOf<ImageFrame>()

        var width = -1
        var height = -1

        val metadata = reader.streamMetadata
        if (metadata != null) {
            val globalRoot = metadata.getAsTree(metadata.getNativeMetadataFormatName()) as IIOMetadataNode

            val globalScreenDescriptor = globalRoot.getElementsByTagName("LogicalScreenDescriptor")

            if (globalScreenDescriptor != null && globalScreenDescriptor.length > 0) {
                val screenDescriptor = globalScreenDescriptor.item(0) as IIOMetadataNode?

                if (screenDescriptor != null) {
                    width = screenDescriptor.getAttribute("logicalScreenWidth").toInt()
                    height = screenDescriptor.getAttribute("logicalScreenHeight").toInt()
                }
            }
        }

        var master: BufferedImage? = null
        var masterGraphics: Graphics2D? = null

        var frameIndex = 0
        while (true) {
            val image = try { reader.read(frameIndex) } catch (_: IndexOutOfBoundsException) { break }

            if (width == -1 || height == -1) {
                width = image.width
                height = image.height
            }

            val root = reader.getImageMetadata(frameIndex).getAsTree("javax_imageio_gif_image_1.0") as IIOMetadataNode
            val gce = root.getElementsByTagName("GraphicControlExtension").item(0) as IIOMetadataNode
            val delay = gce.getAttribute("delayTime").toInt()
            val disposal = gce.getAttribute("disposalMethod")

            var x = 0
            var y = 0

            if (master == null) {
                master = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                masterGraphics = master.createGraphics()
                masterGraphics.background = Color(0, 0, 0, 0)
            } else {
                val children = root.childNodes
                for (nodeIndex in 0 until children.length) {
                    val nodeItem = children.item(nodeIndex)
                    if (nodeItem.nodeName == "ImageDescriptor") {
                        val map = nodeItem.attributes
                        x = map.getNamedItem("imageLeftPosition").nodeValue.toInt()
                        y = map.getNamedItem("imageTopPosition").nodeValue.toInt()
                    }
                }
            }
            masterGraphics!!.drawImage(image, x, y, null)

            val copy = BufferedImage(master.colorModel, master.copyData(null), master.isAlphaPremultiplied, null)
            frames.add(ImageFrame(copy, delay, disposal))

            if (disposal == "restoreToPrevious") {
                var from: BufferedImage? = null
                for (i in frameIndex - 1 downTo 0) {
                    if (frames[i].disposal != "restoreToPrevious" || frameIndex == 0) {
                        from = frames[i].image
                        break
                    }
                }

                master = BufferedImage(from!!.colorModel, from.copyData(null), from.isAlphaPremultiplied, null)
                masterGraphics = master.createGraphics()
                masterGraphics.background = Color(0, 0, 0, 0)
            } else if (disposal == "restoreToBackgroundColor") {
                masterGraphics.clearRect(x, y, image.width, image.height)
            }
            frameIndex++
        }
        reader.dispose()

        return frames
    }

    data class NativeTextureWithData(
        var image: NativeImage,
        var buffer: ByteBuffer,
        var ptr: Long,
        var delays: IntArray,
        var frameWidth: Int,
        var frameHeight: Int,
        var spriteWidth: Int,
        var spriteHeight: Int,
        var widthTiles: Int,
        var heightTiles: Int,
        var numFrames: Int,
    )

    class NativeTextureBuilder(
        val frameWidth: Int,
        val frameHeight: Int,
        var maxWidth: Int,
        var maxHeight: Int
    ) {
        data class Data(val spriteWidth: Int, val spriteHeight: Int, val widthTiles: Int, val heightTiles: Int, val numFrames: Int)
        fun calculateDimensions(requiredFrames: Int): Data {
            val maxWidthTiles = maxWidth / frameWidth
            val maxHeightTiles = maxHeight / frameHeight

            var remainderTiles = 0
            val widthTiles = if (maxWidthTiles >= requiredFrames) { requiredFrames } else { maxWidthTiles }
            val heightTiles = if (maxWidthTiles >= requiredFrames) { 1 } else {
                if (maxWidthTiles * maxHeightTiles >= requiredFrames) {
                    remainderTiles = requiredFrames % maxWidthTiles
                    requiredFrames / maxWidthTiles + if (remainderTiles == 0) 0 else 1
                } else {
                    maxHeightTiles
                }
            }

            val spriteWidth = widthTiles * frameWidth
            val spriteHeight = heightTiles * frameHeight

            val numFrames = if (remainderTiles == 0) {
                widthTiles * heightTiles
            } else {
                widthTiles * (heightTiles - 1) + remainderTiles
            }

            return Data(spriteWidth, spriteHeight, widthTiles, heightTiles, numFrames)
        }

        fun makeEmpty(requiredFrames: Int): NativeTextureWithData {
            val (spriteWidth, spriteHeight, widthTiles, heightTiles, numFrames) = calculateDimensions(requiredFrames)

            val size = spriteWidth * spriteHeight * 4
            val ptr = MemoryUtil.nmemAlloc(size.toLong())
            val buf = MemoryUtil.memByteBuffer(ptr, size)
            val img = NativeImageInvoker.theConstructor(NativeImage.Format.RGBA, spriteWidth, spriteHeight, false, ptr)

            return NativeTextureWithData(
                img, buf, ptr,
                IntArray(requiredFrames),
                frameWidth,
                frameHeight,
                spriteWidth,
                spriteHeight,
                widthTiles,
                heightTiles,
                numFrames
            )
        }
    }

    @JvmStatic fun abgr2rgba(it: Int): Int {
                                                  //   a b g r       a r g b
        return  ((it and -16777216 ))  or         // 0xff000000 -> 0xff000000
                ((it and 0x00ff0000) ushr 2*8) or // 0x000000ff -> 0x00ff0000
                ((it and 0x0000ff00))  or         // 0x0000ff00 -> 0x0000ff00
                ((it and 0x000000ff)  shl 2*8)    // 0x00ff0000 -> 0x000000ff
    }

    val fastReader = GIFImageReader(GIFImageReaderSpi())

    /**
     * Not thread safe as it uses global reader instance (creating reader is surprisingly slow)
     */
    fun readGifToTexturesFaster(bytes: ByteArray): MutableList<NativeTextureWithData> {
        var stream = WrappedByteArrayInputStream(bytes)
        fastReader.reset()
        fastReader.setInput(stream)

        fastReader.streamMetadata
        val imgStartPos = stream.streamPosition
        val numFrames = fastReader.getNumImages(true)

        fastReader.resetStreamSettingsWithoutMetadata()
        stream.seek(imgStartPos)

        val metadata = fastReader.streamMetadata!!
        val width = metadata.logicalScreenWidth
        val height = metadata.logicalScreenHeight

        //TODO
        if (width == -1 || height == -1) {
            throw RuntimeException("Cannot read texture as width or height are not defined in stream metadata")
        }

        val textureBuilder = NativeTextureBuilder(width, height, width, height * GLMaxArrayTextureLayers)
        val textures = mutableListOf<NativeTextureWithData>()
        val (_, _, _, _, framesPerTexture) = textureBuilder.calculateDimensions(numFrames)

        var disposals = mutableListOf<String>()

        var master: BufferedImage? = null
        var masterGraphics: Graphics2D? = null
        var frameIndex = 0
        var remainingFrames = numFrames
        while (true) {
            if (frameIndex == numFrames) {break}
            val texture = textures.getOrNull(frameIndex / framesPerTexture) ?: run {
                val texture = textureBuilder.makeEmpty(remainingFrames)
                textures.add(texture)
                remainingFrames -= texture.numFrames
                texture
            }
            val inTexturePos = frameIndex % framesPerTexture
            val inFrameStart = inTexturePos * width * height

            val prevTexture = textures[max(frameIndex-1, 0) / framesPerTexture]
            val prevInTexturePos = (frameIndex-1) % framesPerTexture
            val prevInFrameStart = prevInTexturePos * width * height

            val image = fastReader.readNext(width, height, inFrameStart * 4, texture.buffer, prevInFrameStart * 4, prevTexture.buffer)

            val metadata = fastReader.getImageMetadata(frameIndex)
            val delay = metadata.delayTime
            val disposal = metadata.disposalMethodString

            texture.delays[inTexturePos] = delay

            if (image == null) {
                disposals.add(disposal)
                frameIndex++
                continue
            }

            var x = 0
            var y = 0

            if (master == null) {
                master = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                masterGraphics = master.createGraphics()
                masterGraphics.background = Color(0, 0, 0, 0)
            } else {
                x = metadata.imageLeftPosition
                y = metadata.imageTopPosition
            }

            if (frameIndex != 0) {
                val inTexturePos = (frameIndex - 1) % framesPerTexture
                val inFrameStart = inTexturePos * width * height

                val src = textures[(frameIndex - 1) / framesPerTexture].buffer
                val pixel = intArrayOf(0, 0, 0, 0)
                var rgba: Int
                // inefficient, but do i care?
                for (i in 0 until width * height) {
                    rgba = src.getInt((inFrameStart + i) * 4)
                    pixel[0] = rgba and 0x000000ff
                    pixel[1] = rgba and 0x0000ff00 ushr 1*8
                    pixel[2] = rgba and 0x00ff0000 ushr 2*8
                    pixel[3] = rgba and -16777216  ushr 3*8
                    master.raster.setPixel(i % width, i / width, pixel)
                }
            }

            masterGraphics!!.drawImage(image, x, y, null)

            val src = master.data.dataBuffer
            val dst = texture.buffer
            for (i in 0 until width * height) {
                dst.putInt((inFrameStart + i) * 4, abgr2rgba(src.getElem(i)))
            }

            if (disposal == "restoreToPrevious") {
                var from: Int = -1
                for (i in frameIndex - 1 downTo 0) {
                    if (disposals[i] != "restoreToPrevious" || frameIndex == 0) {
                        from = i
                        break
                    }
                }

                val inTexturePos = from % framesPerTexture
                val inFrameStart = inTexturePos * width * height

                val src = textures[from / framesPerTexture].buffer
                val pixel = intArrayOf(0, 0, 0, 0)
                var rgba: Int
                for (i in 0 until width * height) {
                    rgba = src.getInt((inFrameStart + i) * 4)
                    pixel[0] = rgba and 0x000000ff
                    pixel[1] = rgba and 0x0000ff00 ushr 1*8
                    pixel[2] = rgba and 0x00ff0000 ushr 2*8
                    pixel[3] = rgba and -16777216  ushr 3*8
                    master.raster.setPixel(i % width, i / width, pixel)
                }
            } else if (disposal == "restoreToBackgroundColor") {
                masterGraphics.clearRect(x, y, image.width, image.height)
            }

            disposals.add(disposal)
            frameIndex++
        }

        return textures
    }
}
