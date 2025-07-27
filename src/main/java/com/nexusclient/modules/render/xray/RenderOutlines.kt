package com.amberclient.modules.render.xray

import net.minecraft.client.render.BuiltBuffer;
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.render.Tessellator
import net.minecraft.client.gl.GlUsage
import net.minecraft.client.render.*
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.gl.VertexBuffer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.BufferAllocator
import net.minecraft.util.math.Vec3d
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import java.util.concurrent.atomic.AtomicBoolean

object RenderOutlines {
    val requestedRefresh = AtomicBoolean(false)
    var vertexBuffer: VertexBuffer? = null
    private val LOGGER = LogManager.getLogger("amberclient-xray")

    fun render(context: WorldRenderContext) {
        if (ScanTask.renderQueue.isEmpty() || !SettingsStore.getInstance().get().isActive) {
            return
        }
        renderFallback(context)
    }

    private fun renderWithVertexConsumer(context: WorldRenderContext, immediate: VertexConsumerProvider.Immediate) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableCull()
        RenderSystem.lineWidth(4.0f)

        val vertexConsumer = immediate.getBuffer(RenderLayer.getTranslucent())
        val cameraPos = context.camera().pos
        val matrixStack = context.matrixStack()

        // Handle nullable MatrixStack
        if (matrixStack != null) {
            matrixStack.push()
            matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z)

            for (blockProps in ScanTask.renderQueue) {
                if (blockProps == null) continue

                val size = 1.0f
                val x = blockProps.pos.x.toFloat()
                val y = blockProps.pos.y.toFloat()
                val z = blockProps.pos.z.toFloat()
                val red = blockProps.color.red() / 255f
                val green = blockProps.color.green() / 255f
                val blue = blockProps.color.blue() / 255f
                val alpha = 1.0f

                renderLineBoxWithConsumer(vertexConsumer, matrixStack,
                    x, y, z, x + size, y + size, z + size,
                    red, green, blue, alpha)
            }

            matrixStack.pop()
        }

        immediate.draw()

        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.enableCull()
        RenderSystem.lineWidth(1.0f)
    }

    private fun renderFallback(context: WorldRenderContext) {
        if (!SettingsStore.getInstance().get().isActive) return

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableCull()
        RenderSystem.lineWidth(4.0f)

        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)

        val cameraPos = context.camera().pos
        val matrixStack = context.matrixStack()

        // Handle nullable MatrixStack
        if (matrixStack != null) {
            matrixStack.push()
            matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z)

            for (blockProps in ScanTask.renderQueue) {
                if (blockProps == null) continue

                val size = 1.0f
                val x = blockProps.pos.x.toFloat()
                val y = blockProps.pos.y.toFloat()
                val z = blockProps.pos.z.toFloat()
                val red = blockProps.color.red() / 255f
                val green = blockProps.color.green() / 255f
                val blue = blockProps.color.blue() / 255f
                val alpha = 1.0f

                val matrix = matrixStack.peek().positionMatrix

                // Bottom face
                bufferBuilder.vertex(matrix, x, y, z).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x + size, y, z).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x + size, y, z).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x + size, y, z + size).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x + size, y, z + size).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x, y, z + size).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x, y, z + size).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x, y, z).color(red, green, blue, alpha)

                // Top face
                bufferBuilder.vertex(matrix, x, y + size, z).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x + size, y + size, z).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x + size, y + size, z).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x + size, y + size, z + size).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x + size, y + size, z + size).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x, y + size, z + size).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x, y + size, z + size).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x, y + size, z).color(red, green, blue, alpha)

                // Vertical edges
                bufferBuilder.vertex(matrix, x, y, z).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x, y + size, z).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x + size, y, z).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x + size, y + size, z).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x + size, y, z + size).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x + size, y + size, z + size).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x, y, z + size).color(red, green, blue, alpha)
                bufferBuilder.vertex(matrix, x, y + size, z + size).color(red, green, blue, alpha)
            }

            matrixStack.pop()
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())

        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.enableCull()
        RenderSystem.lineWidth(1.0f)
    }

    private fun rebuildVertexBuffer(context: WorldRenderContext) {
        requestedRefresh.set(false)
        vertexBuffer?.close()
        vertexBuffer = null

        val allocator = BufferAllocator(1024 * 1024)
        val bufferBuilder = BufferBuilder(allocator, VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR)

        val cameraPos = context.camera().pos
        val matrixStack = context.matrixStack()

        // Handle nullable MatrixStack
        if (matrixStack != null) {
            for (blockProps in ScanTask.renderQueue) {
                if (blockProps == null) continue

                val size = 1.0f
                val x = (blockProps.pos.x - cameraPos.x).toFloat()
                val y = (blockProps.pos.y - cameraPos.y).toFloat()
                val z = (blockProps.pos.z - cameraPos.z).toFloat()
                val red = blockProps.color.red() / 255f
                val green = blockProps.color.green() / 255f
                val blue = blockProps.color.blue() / 255f

                renderLineBox(bufferBuilder, matrixStack,
                    x, y, z, x + size, y + size, z + size,
                    red, green, blue, 0.8f)
            }
        }

        vertexBuffer = VertexBuffer(GlUsage.STATIC_WRITE)
        val builtBuffer = bufferBuilder.end()
        vertexBuffer!!.bind()
        vertexBuffer!!.upload(builtBuffer)

        // Fixed: Access indexCount through drawParameters
        val vertexCount = try {
            builtBuffer.drawParameters.vertexCount
        } catch (e: Exception) {
            LOGGER.warn("Failed to get vertex count from drawParameters", e)
            0
        }

        val indexCount = try {
            builtBuffer.drawParameters.indexCount
        } catch (e: Exception) {
            LOGGER.warn("Failed to get index count from drawParameters", e)
            0
        }

        builtBuffer.close()
        VertexBuffer.unbind()
        allocator.close()
        LOGGER.info("Vertex buffer rebuilt, vertexCount: $vertexCount, indexCount: $indexCount")
    }

    private fun renderLineBoxWithConsumer(consumer: VertexConsumer, matrixStack: MatrixStack,
                                          x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
                                          red: Float, green: Float, blue: Float, alpha: Float) {
        val matrix = matrixStack.peek().positionMatrix

        // Bottom face
        consumer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha)
        consumer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha)
        consumer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha)
        consumer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha)
        consumer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha)
        consumer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha)
        consumer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha)
        consumer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha)

        // Top face
        consumer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha)
        consumer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha)
        consumer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha)
        consumer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha)
        consumer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha)
        consumer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha)
        consumer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha)
        consumer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha)

        // Vertical edges
        consumer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha)
        consumer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha)
        consumer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha)
        consumer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha)
        consumer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha)
        consumer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha)
        consumer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha)
        consumer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha)
    }

    private fun renderLineBox(buffer: BufferBuilder, matrixStack: MatrixStack,
                              x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
                              red: Float, green: Float, blue: Float, alpha: Float) {
        val matrix = matrixStack.peek().positionMatrix

        val r = (red * 255).toInt()
        val g = (green * 255).toInt()
        val b = (blue * 255).toInt()
        val a = (alpha * 255).toInt()

        // Bottom face
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0f, 0f, 1f)

        // Top face
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0f, 0f, 1f)

        // Vertical edges
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0f, 0f, 1f)
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).normal(0f, 0f, 1f)
    }
}