package com.example.sonora.player

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveBufferControllerTest {
    @Test
    fun poorBandwidthRaisesBufferTarget() {
        val controller = AdaptiveBufferController()
        controller.updateBandwidthEstimate(100_000L, 128)
        assertEquals(60_000, controller.targetBufferMs())
    }

    @Test
    fun mediumBandwidthUsesMediumBufferTarget() {
        val controller = AdaptiveBufferController()
        controller.updateBandwidthEstimate(256_000L, 128)
        assertEquals(35_000, controller.targetBufferMs())
    }

    @Test
    fun highBandwidthNeedsSeveralGoodSamplesBeforeReducingBuffer() {
        val controller = AdaptiveBufferController()
        controller.onRebuffer()
        controller.updateBandwidthEstimate(512_000L, 128)
        assertEquals(35_000, controller.targetBufferMs())
        controller.updateBandwidthEstimate(512_000L, 128)
        assertEquals(35_000, controller.targetBufferMs())
        controller.updateBandwidthEstimate(512_000L, 128)
        assertEquals(15_000, controller.targetBufferMs())
    }

    @Test
    fun rebufferImmediatelyRaisesBufferTarget() {
        val controller = AdaptiveBufferController()
        controller.updateBandwidthEstimate(512_000L, 128)
        controller.updateBandwidthEstimate(512_000L, 128)
        controller.updateBandwidthEstimate(512_000L, 128)
        assertEquals(15_000, controller.targetBufferMs())
        controller.onRebuffer()
        assertEquals(60_000, controller.targetBufferMs())
    }
}
