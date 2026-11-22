package com.example.sonora.player

/**
 * Chooses a buffer target from measured download throughput and the station's
 * advertised bitrate. The target is intentionally conservative after a rebuffer
 * and only shrinks after several consecutive good measurements.
 */
class AdaptiveBufferController {
    @Volatile
    private var targetMs: Int = DEFAULT_BUFFER_MS

    private var consecutiveGoodSamples: Int = 0

    fun targetBufferMs(): Int = targetMs

    @Synchronized
    fun updateBandwidthEstimate(bandwidthEstimateBps: Long, stationBitrateKbps: Int) {
        if (bandwidthEstimateBps <= 0L) return

        val expectedBitrateBps = (stationBitrateKbps.takeIf { it > 0 } ?: DEFAULT_STATION_BITRATE_KBPS)
            .toLong() * 1_000L
        val headroom = bandwidthEstimateBps.toDouble() / expectedBitrateBps.toDouble()

        when {
            headroom < POOR_NETWORK_HEADROOM -> {
                targetMs = POOR_NETWORK_BUFFER_MS
                consecutiveGoodSamples = 0
            }
            headroom < GOOD_NETWORK_HEADROOM -> {
                targetMs = MEDIUM_NETWORK_BUFFER_MS
                consecutiveGoodSamples = 0
            }
            else -> {
                consecutiveGoodSamples++
                targetMs = if (consecutiveGoodSamples >= GOOD_SAMPLES_TO_REDUCE_BUFFER) {
                    GOOD_NETWORK_BUFFER_MS
                } else {
                    minOf(targetMs, MEDIUM_NETWORK_BUFFER_MS)
                }
            }
        }
    }

    @Synchronized
    fun onRebuffer() {
        targetMs = POOR_NETWORK_BUFFER_MS
        consecutiveGoodSamples = 0
    }

    @Synchronized
    fun resetForNewStation() {
        targetMs = DEFAULT_BUFFER_MS
        consecutiveGoodSamples = 0
    }

    private companion object {
        const val DEFAULT_STATION_BITRATE_KBPS = 128
        const val POOR_NETWORK_HEADROOM = 1.25
        const val GOOD_NETWORK_HEADROOM = 2.5
        const val GOOD_SAMPLES_TO_REDUCE_BUFFER = 3

        const val GOOD_NETWORK_BUFFER_MS = 15_000
        const val MEDIUM_NETWORK_BUFFER_MS = 35_000
        const val DEFAULT_BUFFER_MS = 30_000
        const val POOR_NETWORK_BUFFER_MS = 60_000
    }
}
