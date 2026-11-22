package se.gendut.klipp

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoTrackModelTest {
    @Test fun exposesEightVideoTracksAboveThreeAudioTracks() {
        assertEquals(8, VIDEO_TRACK_COUNT)
        assertEquals(3, AUDIO_TRACK_COUNT)
        assertEquals(11, TRACKS.size)
        assertEquals((8 downTo 1).map { "V$it" }, TRACKS.take(VIDEO_TRACK_COUNT).map { it.name })
        assertEquals(listOf("A1", "A2", "A3"), TRACKS.drop(VIDEO_TRACK_COUNT).map { it.name })
        assertEquals(List(VIDEO_TRACK_COUNT) { TrackKind.VIDEO }, TRACKS.take(VIDEO_TRACK_COUNT).map { it.kind })
        assertEquals(List(AUDIO_TRACK_COUNT) { TrackKind.AUDIO }, TRACKS.drop(VIDEO_TRACK_COUNT).map { it.kind })
    }

    @Test fun sourceTargetsStartOnV1AndA1() {
        val state = EditorState(emptyList())
        assertEquals(VIDEO_TRACK_COUNT - 1, state.targetVideo)
        assertEquals(VIDEO_TRACK_COUNT, state.targetAudio)
    }

    @Test fun linkedTracksKeepTheirRelativeLayer() {
        val state = EditorState(emptyList())
        val video = Clip(1, "video", ClipKind.VIDEO, VIDEO_TRACK_COUNT - 1, 0f, 2f, 0f, 2f, 0, link = 7)
        val audio = Clip(2, "audio", ClipKind.AUDIO, VIDEO_TRACK_COUNT, 0f, 2f, 0f, 2f, 0, link = 7)
        assertEquals(VIDEO_TRACK_COUNT, state.partnerTrack(video, 0, audio))
    }
}
