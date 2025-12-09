package net.spaceeye.vmod.compat.patchouli

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import net.spaceeye.vmod.rendering.textures.GIFManager
import net.spaceeye.vmod.rendering.textures.GIFTexture
import vazkii.patchouli.client.book.gui.GuiBook
import vazkii.patchouli.client.book.gui.GuiBookEntry
import vazkii.patchouli.client.book.page.abstr.PageWithText

class PageGIF: PageWithText() {
    var title: String? = null
    var border = false
    var images = ArrayList<ResourceLocation>()

    @Transient private var currentFrame: Int = 0
    @Transient private var time: Float = 0f

    override fun getTextHeight(): Int = 120

    override fun onDisplayed(parent: GuiBookEntry?, left: Int, top: Int) {
        super.onDisplayed(parent, left, top)
        currentFrame = 0
        time = 0f
    }

    private fun advanceTime(delta: Float, gif: GIFTexture) {
        if (!gif.loadedSuccessfully.isDone || !gif.loadedSuccessfully.get()) return
        // gif uses 1/100 th of a second and not milliseconds
        time += delta / 10f
        var delay = gif.sprites[currentFrame / gif.framesPerSprite].delays[currentFrame % gif.framesPerSprite]
        if (time <= delay) return

        //if delay is over a second then just reset
        if (time > 100f) { time = 0f }

        while (true) {
            time -= delay
            currentFrame++
            if (currentFrame >= gif.totalFrames) { currentFrame = 0 }

            delay = gif.sprites[currentFrame / gif.framesPerSprite].delays[currentFrame % gif.framesPerSprite]
            if (time <= delay) {break}
        }

        return
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, pticks: Float) {
        //creates new object each frame but PageWithText doesn't have onClose so i have to do this otherwise i can't free texture after player closes page
        if (images.isEmpty()) { return }
        val ref = GIFManager.getTextureFromLocation(images[0])

        var x = GuiBook.PAGE_WIDTH / 2 - 53
        var y = 7

        graphics.setColor(1f, 1f, 1f, 1f)
        RenderSystem.enableBlend()
        graphics.pose().scale(.5f, .5f, .5f)

        //ptick = delta / ms_per_tick
        //ms_per_tick = 50
        advanceTime(Minecraft.getInstance().deltaFrameTime * 50, ref.it)
        ref.it.blit(graphics.pose(), currentFrame, x*2+6, y*2+6, 200, 200)

        graphics.pose().scale(2f, 2f, 2f)

        if (border) {
            GuiBook.drawFromTexture(graphics, book, x, y, 405, 149, 106, 106)
        }
        if (title != null && title!!.isNotEmpty()) {
            parent.drawCenteredStringNoShadow(graphics, i18n(title), GuiBook.PAGE_WIDTH / 2, -3, book.headerColor)
        }

        super.render(graphics, mouseX, mouseY, pticks)

        ref.close()
    }
}