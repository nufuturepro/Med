# Draft comment for upstream issue #19
# (https://github.com/FeDeveloper95/Med/issues/19)
#
# Review, then paste into the issue. Nothing is posted automatically.

---

Found the root cause. It's not the button itself — it's one line of Kotlin
that only works on Android 15+.

**What's happening**

The minus button on "Times per day" removes the last entry of a time-slot
list here:

```kotlin
while (newTimes.size > count) newTimes.removeLast()
```

(`MedicineBottomSheet.kt`, inside the `LaunchedEffect(frequencyType, timesPerDay)` sync.)

`MutableList.removeLast()` is compiled against the JDK 21
`SequencedCollection` interface, which the Android runtime only ships since
**API 35 (Android 15)**. On any device running Android 14 or older the call
throws `java.lang.NoSuchMethodError` and the app crashes. That's why it
reproduces on a Galaxy M34 (Android 14) but nobody could reproduce it on
newer phones — and why "+" works fine: only the reduce path calls
`removeLast()`.

It crashes on the *Save* sync, not on the tap itself, which is why the
video shows the crash landing a moment after the press.

**The fix** is one line — use the index-based removal that exists on every
API level:

```kotlin
while (newTimes.size > count) newTimes.removeAt(newTimes.lastIndex)
```

**How to verify** without an Android 14 device: run the debug build on an
emulator with API 34 or lower (minSdk 26), add a medicine with 2+ times per
day, press minus, and save — the current code crashes, the patched code
saves normally.

Happy to open a PR with this if that's easier to review.

Side note: I'm maintaining a fork with this and a few other fixes
(https://github.com/nufuturepro/MedRX) — no pressure to merge anything there,
this comment is just so the bug gets documented for upstream users either
way.
