package com.example.sonora.player

import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource.MediaPeriodId
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator

/**
 * Wraps Media3's load control and caps loading at a buffer target that can change
 * while playback is active.
 *
 * Important: Java interface default methods are not reliably forwarded by Kotlin's
 * `by delegate` for this Media3 API. Every LoadControl callback used by ExoPlayer
 * is therefore explicitly forwarded to DefaultLoadControl. Without these methods,
 * Media3 can call its default implementation and throw "not implemented".
 */
@OptIn(UnstableApi::class)
class AdaptiveRadioLoadControl(
    private val delegate: DefaultLoadControl,
    private val bufferController: AdaptiveBufferController
) : LoadControl by delegate {

    override fun onPrepared(playerId: PlayerId) {
        delegate.onPrepared(playerId)
    }

    override fun onTracksSelected(
        parameters: LoadControl.Parameters,
        trackGroups: TrackGroupArray,
        trackSelections: Array<out ExoTrackSelection?>
    ) {
        delegate.onTracksSelected(parameters, trackGroups, trackSelections)
    }

    override fun onStopped(playerId: PlayerId) {
        delegate.onStopped(playerId)
    }

    override fun onReleased(playerId: PlayerId) {
        delegate.onReleased(playerId)
    }

    override fun getAllocator(playerId: PlayerId): Allocator =
        delegate.getAllocator(playerId)

    override fun getBackBufferDurationUs(playerId: PlayerId): Long =
        delegate.getBackBufferDurationUs(playerId)

    override fun retainBackBufferFromKeyframe(playerId: PlayerId): Boolean =
        delegate.retainBackBufferFromKeyframe(playerId)

    override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean {
        val targetBufferUs = bufferController.targetBufferMs().toLong() * 1_000L
        if (parameters.bufferedDurationUs >= targetBufferUs) return false
        return delegate.shouldContinueLoading(parameters)
    }

    override fun shouldContinuePreloading(
        playerId: PlayerId,
        timeline: Timeline,
        mediaPeriodId: MediaPeriodId,
        bufferedDurationUs: Long
    ): Boolean = delegate.shouldContinuePreloading(
        playerId,
        timeline,
        mediaPeriodId,
        bufferedDurationUs
    )

    override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean =
        delegate.shouldStartPlayback(parameters)
}
