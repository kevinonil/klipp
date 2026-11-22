# Multitrack editor expansion

## Goal

Expand Klipp's current three-video-track workflow to support up to eight independently editable video layers while preserving the existing audio, caption, transition, preview, and export behavior.

The editor must support two composition layouts:

1. **Stacked layers** — clips on V1–V8 composite from bottom to top and can play simultaneously.
2. **Split-screen layout** — eight independent video clips can be arranged in a grid or custom positions in the program frame.

Each clip keeps its own trim points, timeline position, crop/transform, visibility, and selection state.

## Implementation status

The feature/multitrack-editor branch now contains the first end-to-end eight-track pass:

- The timeline model exposes V8–V1 plus the existing A1–A3 lanes, with target and linked-track index mapping updated.
- Existing sequences are expanded in the FilmCraft engine to eight video tracks once per sequence; this is not repeated on every refresh.
- Timeline placement, track flags, clip conversion, transition mapping, title placement, preview planning, live still preview, render-cache transition windows, and the playback rebuild key now account for all eight video tracks.
- Tests cover the eight-track model, V1/A1 source targets, linked-track bounds, and title placement up to V8.

Still outstanding before calling the feature complete:

- A dedicated stacked/split-screen layout control and preset workflow.
- Full integration coverage for eight simultaneous clips through preview, save/reopen, and MP4 export.
- Device-level decoder/performance validation and safeguards for phones that cannot decode eight streams at once.
- A successful Android CI build and unit-test run. No workflow run/check result is currently available for the branch, so build success is not yet verified.

## User-facing requirements

- Import and place up to eight video clips on independent video tracks.
- Move, trim, split, duplicate, and delete each clip independently.
- Scroll horizontally and vertically through the timeline and zoom the time scale.
- Crop, scale, and position each clip independently.
- Support simultaneous playback of overlapping clips.
- Provide stacked-layer and split-screen composition modes.
- Preserve undo/redo and MP4 export.

## Implementation plan

### Phase 1 — Track model and project compatibility
- Replace fixed three-track assumptions with a bounded, ordered video-track collection (V1–V8).
- Keep existing projects readable: projects with V1–V3 must load unchanged.
- Add migration/default behavior for V4–V8 without changing existing track IDs or serialized meanings.
- Add model and serialization tests for 1–8 video tracks.

### Phase 2 — Timeline editing
- Render V1–V8 in the timeline with independent clip selection and editing.
- Keep track headers, lock/visibility controls, vertical scrolling, and zoom usable on small screens.
- Verify move, trim, razor/split, duplicate, delete, and undo/redo across all eight tracks.

### Phase 3 — Preview composition
- Generalize preview planning and upper-layer playback from three tracks to eight.
- Define deterministic z-order (V8 above V7 … V1).
- Keep playback clocks synchronized and avoid creating unnecessary decoder instances.
- Add stacked and split-screen layout controls; persist per-clip transform/crop values.

### Phase 4 — Export and performance
- Generalize the Media3 compositor/export plan to all enabled video tracks.
- Validate alpha/opacity, crop, transform, transitions, and audio synchronization.
- Add device/performance safeguards for eight simultaneous sources (decoder limits, proxy/quality fallback, and clear unsupported-format errors).

### Phase 5 — Verification
- Add unit tests for track planning, overlap, z-order, split-screen placement, and project migration.
- Add instrumentation tests for preview and MP4 export with multiple simultaneous videos.
- Run the Android build and test suite before merging.

## Architectural constraints discovered

- The current timeline and preview code contain fixed three-video-track assumptions.
- UpperLayers already implements independent preview players for upper video layers.
- engine/Layers.kt builds a multi-layer export plan and currently plans only the existing video-track collection.
- This must be expanded end-to-end; changing only the visible timeline would create clips that appear editable but do not preview or export correctly.

## Acceptance criteria

A project with eight overlapping videos can be edited independently, previewed in sync, saved/reopened, and exported to MP4. The original three-track projects continue to behave as before.
