package se.gendut.klipp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import se.gendut.klipp.engine.FrameSpan
import se.gendut.klipp.engine.TitlePlace
import se.gendut.klipp.engine.TitlePlaceProblem
import se.gendut.klipp.engine.titlePlace

/** A new title goes on a visible track above the picture, never over a clip. */
class TitlePlaceTest {
    private fun s(a: Long, b: Long) = FrameSpan(a, b)
    private val none = emptyList<Boolean>()
    // Keep the legacy three-track edge cases explicit; the app's default is now eight.
    private fun place(tracks: List<List<FrameSpan>>, locked: List<Boolean>, at: Long, len: Long) =
        titlePlace(tracks, locked, at, len, shown = 3)

    @Test fun defaultSupportsEightVideoTracks() {
        // V1 through V7 are occupied at the playhead, so the title can use V8.
        val tracks = List(7) { listOf(s(0, 250)) } + listOf(emptyList())
        assertEquals(TitlePlace(7, false, null), titlePlace(tracks, none, 50, 125))
        // Occupying V8 too means a title cannot be placed on a hidden ninth track.
        val allBusy = tracks.dropLast(1) + listOf(listOf(s(0, 250)))
        assertEquals(TitlePlaceProblem.COVERED, titlePlace(allBusy, none, 50, 125).problem)
    }

    @Test fun aboveThePictureOnTheFirstFreeTrack() {
        // only V1, a clip under the playhead: the engine makes V2
        assertEquals(TitlePlace(1, true, null), place(listOf(listOf(s(0, 250))), none, 50, 125))
        // V1 and V2 busy at the playhead, V3 free: V3
        assertEquals(TitlePlace(2, false, null), place(listOf(listOf(s(0, 250)), listOf(s(25, 100)), emptyList()), none, 50, 125))
        // nothing under the playhead: V1 (as the engine does), right after the clip that ends there
        assertEquals(TitlePlace(0, false, null), place(listOf(listOf(s(0, 50)), emptyList(), emptyList()), none, 50, 125))
    }

    @Test fun freeForTheWholeTitleNotJustAtThePlayhead() {
        // V2 free at the playhead but a clip starts there 2 s later: V3
        val t = listOf(listOf(s(0, 500)), listOf(s(100, 200)), emptyList())
        assertEquals(TitlePlace(2, false, null), place(t, none, 50, 125))
        // V3 busy later too: no room – and nothing is made
        val full = listOf(listOf(s(0, 500)), listOf(s(100, 200)), listOf(s(150, 160)))
        assertEquals(TitlePlace(null, false, TitlePlaceProblem.NO_ROOM), place(full, none, 50, 125))
        // a title short enough fits before the clip on V2
        assertEquals(TitlePlace(1, false, null), place(t, none, 50, 50))
    }

    @Test fun v3InUseOrAHiddenTrackAbove() {
        // a clip on V3 at the playhead: a title over it would need V4, which the timeline doesn't show
        assertEquals(TitlePlaceProblem.COVERED, place(listOf(listOf(s(0, 250)), emptyList(), listOf(s(0, 100))), none, 50, 125).problem)
        // a V4 left over from before (made by the engine) with a clip at the playhead
        assertEquals(TitlePlaceProblem.COVERED, place(listOf(emptyList(), emptyList(), emptyList(), listOf(s(40, 60))), none, 50, 125).problem)
        // … but elsewhere V4's clip doesn't matter
        assertEquals(TitlePlace(0, false, null), place(listOf(emptyList(), emptyList(), emptyList(), listOf(s(400, 600))), none, 50, 125))
    }

    @Test fun aLockedTrackIsNotUsed() {
        val t = listOf(listOf(s(0, 250)), listOf(s(0, 250)), emptyList())
        assertEquals(TitlePlace(null, false, TitlePlaceProblem.LOCKED), place(t, listOf(false, false, true), 50, 125))
        // V2 free but locked, V3 free: V3
        assertEquals(TitlePlace(2, false, null), place(listOf(listOf(s(0, 250)), emptyList(), emptyList()), listOf(false, true, false), 50, 125))
    }

    @Test fun neverOverAClipAndAlwaysShown() {
        val r = java.util.Random(7)
        repeat(4000) {
            val n = 1 + r.nextInt(4)
            val tracks = List(n) { List(r.nextInt(3)) { val a = r.nextInt(300).toLong(); s(a, a + 1 + r.nextInt(120)) }.sortedBy { it.start } }
            val locked = List(n) { r.nextInt(5) == 0 }
            val at = r.nextInt(300).toLong(); val len = 1L + r.nextInt(150)
            val p = place(tracks, locked, at, len)
            if (p.ok) {
                val i = p.track!!
                assertTrue("shown: $p", i in 0..2)
                if (p.create) assertEquals(tracks.size, i)
                else {
                    assertFalse("over a clip: $tracks $at $len $p", tracks[i].any { it.start < at + len && at < it.end })
                    assertFalse("locked: $p", locked[i])
                }
                assertFalse("under the picture at the playhead: $p", tracks.drop(i).any { t -> t.any { at >= it.start && at < it.end } })
            }
        }
    }
}
