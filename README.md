# Rx

**Never miss your medicine ever again.**

> **Rx** (formerly **Med RX**) is a community fork of [Med by FeDeveloper95](https://github.com/FeDeveloper95/Med) (Apache 2.0), maintained by **nukirk** and not affiliated with or endorsed by the original author. It adds dose skipping with clinical reasons, pre-skips for future dates, a supply ledger with full traceability, idempotent dose logging so stock always matches the bottle, a doctor-friendly skipped-dose report, and the fix for upstream's crash when reducing doses on Android 14 and below ([upstream issue #19](https://github.com/FeDeveloper95/Med/issues/19)).
>
> Install it **alongside** the original Med (different app id, `com.nukirk.medrx`). Moving data is a two-step: export a backup/CSV from Med, import in Rx. Releases and the in-app updater point at this repository; bug reports go to [this repo's issues](https://github.com/nufuturepro/MedRX/issues).

## Highlights vs upstream

- **Skip doses with reasons** — eight clinical reason chips + optional note, from the alarm, notification, or the med card
- **Pre-skip future doses** — travel, fasting, procedures: plan the gap before it happens
- **Supply ledger** — every stock change (take, refund, refill, correction) recorded with its balance
- **Idempotent dose logging** — a dose confirmed twice (card + reminder/watch) counts once, so stock always matches the bottle
- **Doctor report** — CSV of every skipped dose with date, medication, reason, note
- **Stats** — skipped days are visually distinct from missed ones, per-reason breakdown

### Acknowledgements

Rx began as **Med RX**, a community fork of [Med by FeDeveloper95](https://github.com/FeDeveloper95/Med) (Apache 2.0). All credit for the original concept, design language, and core code belongs to FeDeveloper95 — please consider supporting the original project:

**➡ [github.com/FeDeveloper95/Med](https://github.com/FeDeveloper95/Med)**

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white) ![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white) ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white) ![Material 3](https://img.shields.io/badge/Material%203-Expressive-purple?style=for-the-badge)

### :sparkles: Features

Med helps you keep track of your medicines and medical history with ease. You can create fixed custom events via Quick Actions for immediate logging or save specific custom events when needed. Notifications aren't enough? Med can also set alarms to remind you it's time to take a medicine. The scheduling is highly flexible, allowing you to save medicines with recurrence set to daily, specific days of the week, or a fully custom schedule. The setup process is intuitive and fast.
Additionally, Med includes an integrated in-app update system to ensure you are always running the latest version.

### :art: Design

The UI is built strictly following the latest **Material 3 Expressive** guidelines using Jetpack Compose to keep the app clean, fluid, and modern. While it is designed with **Pixel and AOSP** aesthetics in priority, it functions seamlessly on any Android 8+ device. Med works and adapts perfectly on every device, whether you use a regular phone, a foldable, a tablet or a desktop.

### :light_blue_heart: Loved help

Special thanks to my awesome girlfriend, [Gaia](https://github.com/Gaia-Kapo), who helped me a lot with the design choices and suggested the base idea. I love you 🩵

### :warning: Feedback

Found a bug or have a feature request? **Create a new issue (preferred)** or reach me directly via [Telegram](https://t.me/fedeveloper95). 


### :bangbang: Disclaimer
No fork of this project will recieve support, if you use a fork, ask the forker to support you.

### :camera: Screenshots

<details>
  <summary><b>Phone screenshots</b></summary>
  <br>
<img width="2665" height="1270" alt="Phone" src="https://github.com/user-attachments/assets/5b2d6541-6ead-41cb-8d5e-92935493f48d" />
</details>
<details>
  <summary><b>Watch screenshots</b></summary>
  <br>
<img width="2666" height="1267" alt="Watch" src="https://github.com/user-attachments/assets/29c955e4-5601-4329-837f-f8762c9506c3" />
</details>
<details>
  <summary><b>Desktop screenshots</b></summary>
  <br>
<img width="2744" height="1771" alt="Desktop" src="https://github.com/user-attachments/assets/e23bb028-dd60-43d2-8f73-64958ce27465" />
</details>
