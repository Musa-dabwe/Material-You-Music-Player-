# Bug Report: Inconsistent Playback Behavior in Music Player

## 1. Bug Description

**Location:** `index.html`, in the `prevSong()` and `nextSong()` JavaScript functions (lines 251-283).

**The Bug:** There is an inconsistency in how the "previous" and "next" buttons function when the music player is paused.

*   **`nextSong()` behavior (original):** When the player is paused, clicking the "next" button correctly loads the next song and **automatically begins playback**.
*   **`prevSong()` behavior (original):** When the player is paused, clicking the "previous" button loads the previous song but **fails to start playback**. The player remains in a paused state.

**Impact:** This inconsistent behavior creates a confusing and unintuitive user experience. Users expect the "previous" and "next" controls to behave symmetrically.

## 2. The Fix

The fix addresses two issues: the inconsistent behavior and redundant code in the original `nextSong` function.

1.  **Consistent Behavior:** Both the `prevSong` and `nextSong` functions were refactored to ensure they always start playback, regardless of whether the player was previously playing or paused. This is the standard expected behavior for these controls in a music player.

2.  **Code Cleanup:** The original `nextSong` function contained a redundant `if/else` block where `audio.play()` was called in both branches. The refactored code removes this redundancy by calling `audio.play()` unconditionally at the end of the function and only updating the player's state (`isPlaying` and icons) if it was previously paused.

The new, corrected logic for both functions is:
```javascript
function func() { // Represents both nextSong and prevSong
    if (songs.length === 0) return;
    // ... logic to determine next/previous index ...
    loadSong(newIndex);
    if (!isPlaying) {
        isPlaying = true;
        playIcon.classList.add('hidden');
        pauseIcon.classList.remove('hidden');
    }
    audio.play();
}
```

## 3. How to Verify the Fix

A test file has been created at `tests/test-repro.html`. To verify the fix:

1.  Open `tests/test-repro.html` in a web browser.
2.  Open the developer console.
3.  The test will run automatically and log its results to the console.
4.  A successful fix will result in the message: "**✅ All tests passed!**" being logged.

This test specifically checks that both `prevSong()` and `nextSong()` correctly change the player's state to "playing" when it was previously paused.
