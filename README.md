# Haru Beta — a private, gentle-start planner

Haru means a fresh beginning. The original sunrise-and-path icon and soft anime-inspired scene reflect the idea that starting again is always allowed.

Haru stores your name, tasks, goals, routines, habit check-ins, chat, and saved memories locally on your phone. Android cloud backup is disabled for the app. Haru has no account, analytics, chat sync, or cloud AI. Haru downloads only the public on-device model you select. If you explicitly approve a web search, Haru opens your browser with the reviewed search phrase; your original message is not sent automatically. The AI model itself runs on the phone's CPU or GPU. Dedicated NPU acceleration is not assumed.

## Features

- First launch asks what Haru should call you. It greets you by name and shows one original quote per day. The 30-quote set is never repeated; when it's used up, Haru stops showing quotes.
- Add tasks with an optional first tiny step, then tap **Start** for a five-minute focus timer.
- Add one-time task reminders or daily routine alarms. A plain phrase such as “remind me of waking up every day at 7 am” becomes a daily 7:00 AM routine with the chosen title. With Android's **Alarms & reminders** access, Haru schedules a precise alarm and plays the phone's alarm tone continuously until you stop or snooze it. Use **More → Hear a test alarm** to check the actual alarm sound immediately. Full-screen alarm controls require Android's full-screen notification access; without precise alarm access, Android may delay the fallback reminder, which cannot guarantee sustained ringing.
- Record a thought through on-device speech recognition when the phone provides it, or type it.
- Ask Haru for task, habit, routine, or goal suggestions. The optional AI makes suggestions from the thought; review, edit, and select what to save. With AI off, a lightweight local rule-based suggestion is available.
- Talk with Haru in a private conversation. Recent turns and preferences you explicitly save under **Teach Haru** stay in the local database and help future replies. This is local context memory, not model retraining. Clear chat without deleting saved memories. If Haru is unsure, it can offer a web search; review or edit the query, then tap **Search the web** to open it in your browser. No search runs automatically.
- Choose AI off, a 143 MB compact model intended for 4 GB phones, or a 345 MB standard INT4 model suggested for 8 GB phones. Haru reports the phone's RAM as a guide. Model downloads are user-started, Wi-Fi only by default, cancellable, SHA-256 checked, and never contain your private notes. Mobile data must be enabled explicitly.
- Create a portable JSON backup and restore it on a new installation. Backups include chat and saved memories; a restore asks before replacing existing entries. Save backups to a private local folder such as Downloads, then move them yourself. Haru does not upload them.

## Install

Open this folder in Android Studio and build the debug APK, or run `./gradlew assembleDebug` on macOS/Linux (`gradlew.bat assembleDebug` on Windows). The APK is created at `app/build/outputs/apk/debug/app-debug.apk`. Transfer it to your Android phone and open it to install. Installing over Haru preserves its data. Allow notifications so reminders can appear.

## Credit

Haru Beta is a collaboration by **para9oid × GPT**. Personal tasks, routines, goals, habit check-ins, and AI prompts stay on the phone.

## Privacy and limits

Haru does not call a cloud AI API or transmit your notes. The optional model download contacts Hugging Face only to fetch the model weights you selected; inference remains local. Web search is a separate opt-in action: Haru shows the proposed search phrase, lets you edit it, and opens your browser only when you tap Search. The search provider then receives that phrase. The standard download uses a smaller INT4 model file, about 345 MB instead of the previous 614 MB file. Downloads are Wi-Fi only by default and can be cancelled. Dictation uses Android's on-device recognition service only when available. If it is unavailable, Haru asks you to type instead of using an online speech service.

Backups contain personal data in readable JSON. Store them somewhere private. Restore replaces the current Haru lists and check-ins with the backup contents. Model files are not in the backup; you can download them again after moving phones.

Android 12 and later require the user to grant **Alarms & reminders** special access before Haru can schedule precise ringing alarms. Android 14 and later may also require **Full-screen notifications** access to show alarm controls over the lock screen. Android 13 and later require notification permission for the ongoing alarm controls. Alarm volume, sound selection, notification access, and battery restrictions can affect delivery. Open Haru → **More** to see the alarm access status. Without precise alarm access, Haru uses an inexact fallback that may be late and cannot guarantee sustained ringing.

Haru is a personal organizer, not medical care or addiction treatment. It does not provide crisis support, medication guidance, or recovery services.
