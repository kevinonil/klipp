package se.gendut.klipp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What deleting transcript words cuts, said before (TranscriptCut.kt) – the engine's rule: targeted or sync-locked, unlocked tracks. */
class TranscriptCutTest {
    private val f = 0.04f
    // tracks by index: 5 V3, 6 V2, 7 V1, 8 A1, 9 A2, 10 A3
    private fun clip(id: Long, track: Int, start: Float, len: Float) = Clip(id, "c$id", if (track < VIDEO_TRACK_COUNT) ClipKind.VIDEO else ClipKind.AUDIO, track, start, len, 0f, len, 0)
    private val clips = listOf(clip(1, 7, 0f, 10f), clip(2, 8, 0f, 10f), clip(3, 6, 2f, 3f), clip(4, 9, 12f, 2f))

    @Test fun targetedAndSyncLockedTracksAreCutAndTheRestMovesEarlier() {
        // V1 and A1 targeted, everything sync-locked (the default)
        val c = transcriptCut(3.01f, 4.49f, f, clips, List(TRACKS.size) { TrackState() }, 7, 8, emptyList())
        assertEquals(3.0f, c.start, 1e-4f)
        assertEquals(4.52f, c.end, 1e-4f)
        assertEquals(listOf("V2", "V1", "A1", "A2"), c.tracks)
        // V2's clip (2–5 s), V1's and A1's: three clips touched; A2's clip after it moves earlier
        assertEquals(3, c.clips)
        assertTrue(c.laterMoves)
        assertTrue(c.untouched.isEmpty())
    }

    @Test fun tracksNeitherTargetedNorSyncLockedOrLockedStay() {
        val states = List(TRACKS.size) { i -> when (i) { 6 -> TrackState(syncLock = false); 9 -> TrackState(locked = true); else -> TrackState() } }
        val c = transcriptCut(3f, 4f, f, clips, states, 7, 8, emptyList())
        assertEquals(listOf("V1", "A1"), c.tracks)
        assertEquals(listOf("V2", "A2"), c.untouched)
        assertEquals(2, c.clips)
    }

    @Test fun nothingTargetedNorSyncLockedCutsNothing() {
        val c = transcriptCut(3f, 4f, f, clips, List(TRACKS.size) { TrackState(syncLock = false) }, -1, -1, emptyList())
        assertTrue(c.tracks.isEmpty())
        assertEquals(0, c.clips)
        assertFalse(c.laterMoves)
    }

    @Test fun captionsInTheRangeAreCountedNotPromised() {
        val caps = listOf(UiCaption(1, 2.5f, 3.5f, "a"), UiCaption(2, 5f, 6f, "b"), UiCaption(3, 3.2f, 3.4f, "hidden", hidden = true))
        assertEquals(1, transcriptCut(3f, 4f, f, clips, List(TRACKS.size) { TrackState() }, 7, 8, caps).captions)
    }
}
