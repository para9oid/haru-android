<p align="center">
  <img src="docs/mitrio-logo.svg" width="104" alt="Mitrio logo" />
</p>
<h1 align="center">Mitrio</h1>
<p align="center">A calmer way to remember what matters, one small step at a time.</p>
<p align="center">
  <img alt="Private by default" src="https://img.shields.io/badge/Private-by%20default-188A78" />
  <img alt="AI is optional" src="https://img.shields.io/badge/On--device%20AI-optional-D78A5B" />
  <img alt="Beta version 2.1 beta 9" src="https://img.shields.io/badge/Beta-2.1--beta9-8B76AE" />
</p>

![Mitrio's illustrated fresh-start home screen](app/src/main/res/drawable-nodpi/mitrio_hero.png)


**Mitrio helps you remember what matters, one small step at a time.** Add reminders, make a plan for today, work on goals and habits, and check in with your progress.

Your plans and chats stay on your phone. You do not need an account. The optional AI also runs on your phone; you can use Mitrio without AI.

## Try Mitrio on your phone

1. Download the APK file from the [latest beta release](https://github.com/para9oid/mitrio/releases).
2. Open the downloaded file on your Android phone and tap **Install**. If Android asks, allow your browser or file app to install this file.
3. Open Mitrio. Allow notifications if you want reminders to appear.

**Updating Mitrio?** Install the new APK over the old app. Do not uninstall first, because uninstalling may erase your saved information. The update uses the same app identity as earlier betas.

## What Mitrio can do

- Help you plan your day and remember tasks.
- Remind you about tasks and routines. Android may ask for alarm and notification access so reminders can ring and appear on time.
- Help you track goals, habits, and small wins.
- Let you talk or type freely, then choose which suggestions to save.
- Let you choose English or Hinglish, a gentle or direct assistant style, and a simple or detailed screen.
- Keep AI turned off, or optionally download a small AI model that works on your phone. AI may briefly make your phone feel slower while it thinks.
- Make a backup file you can keep and restore on another phone. Keep that file private because it contains your saved information.

## What’s in this beta

- Set a name or nickname on the first screen; it stays on this phone. On first open, Mitrio explains and asks for precise alarm access, then asks for notification permission.
- Choose **Easy** for a three-tab home with one next step, simple progress, and optional shortcuts. Choose **Geek** for a six-tab dashboard with task/habit/routine counts, streak details, routines, goals, and full AI controls.
- Find options under the **Settings** tab, grouped by Profile & interface, Reminders & alarms, Language & tone, Assistant & AI, Recovery support, and Backups & data. Geek mode keeps its detailed controls inside the matching section.
- Start a goal from a practical suggestion. For example, “Build a running habit” adds the goal and a daily “Move for 10 minutes” check-in; you can also write your own goal.
- Pick **English** or **Hinglish** for the app and AI. Choose a **Friendly**, **Moderate**, or **Brutally honest** reply style. Blunt mode can use strong language, but does not insult or shame you.
- Use **Plan today together** to talk through what you want to do, your available time and energy, and the tasks and goals you already saved.
- Get an honest **Did you do it?** check-in for timed tasks and routines. “Not yet” keeps the task without guilt and offers a smaller next step or another reminder.
- Choose focused alarms or **Low-key mode**, which only puts a quiet notification in your shade. Set an optional daily goal check-in time in **Settings → Reminders & alarms**.
- Add tasks with an optional first tiny step, then tap **5 min** for a five-minute focus timer.
- On first open, get a plain-language prompt for Android's **Alarms & reminders** access, followed by notification permission. Android 14+ can also offer optional full-screen alarm access. Add one-time task reminders or daily routine alarms; without the needed access, Android may delay or hide reminders.
- Talk by typing or on-device dictation. With **personal context** enabled, Mitrio can use relevant prior chat, saved notes, open tasks, goals, routines, and recent habit check-ins as context. Turn this off in **Settings → Assistant & AI** any time. Context helps replies feel personal; the model's downloaded weights are not retrained.
- **Quiet local learning** is optional and off until you enable it in **Settings → Assistant & AI**. If enabled, Android may run the model after Mitrio is minimized, at most once a day, only while charging and idle. It runs at low priority and can be delayed. It makes a short, tentative preference note from recent chats and routine or habit patterns; it does not read recovery check-ins, retrain model weights, or upload anything. The phone may warm up or feel briefly slower. Turn the option off to cancel future runs and erase the note.
- Choose AI off, a 143 MB compact model intended for 4 GB phones, or a 345 MB standard INT4 model suggested for 8 GB phones. Downloads are user-started, Wi-Fi only by default, cancellable, and SHA-256 checked. Your notes are never included.
- Create a portable JSON backup and restore it on a new installation. Backups now include your assistant name, language, tone, reminder style, chats, goals, habits, and saved notes. Backups from earlier app versions are still accepted.

## Update or install

This beta uses the same Android application ID and signing key as the previous beta, so install it over the existing app; do **not** uninstall first. Android should update the app and keep its saved data. Keep the same signing key when building future update APKs.

Open this folder in Android Studio and build the debug APK, or run `./gradlew assembleDebug` on macOS/Linux (`gradlew.bat assembleDebug` on Windows). The APK is created at `app/build/outputs/apk/debug/app-debug.apk`. Transfer it to your Android phone and open it to install. Allow notifications so reminders can appear.

## Privacy and limits

On-device AI uses your phone's CPU/GPU and may make it feel slower for a few seconds. Optional background reflection uses low-priority CPU work while charging and idle, at most once a day; Android may postpone it. Smaller models can still be wrong. Mitrio uses instructions and local context to guide them; it does not fine-tune or retrain the downloaded model. Web search is a separate choice: review or edit the proposed phrase and tap **Search the web** before it opens your browser. The search provider receives that phrase.

Backups are readable JSON containing personal information. Store them somewhere private. Model files are not included in backups and must be downloaded again on a new phone. Dictation uses Android's on-device speech service only when available; otherwise type your thought.

Android 12 and later require **Alarms & reminders** special access for precise alarms. Android 14 and later may also require **Full-screen notifications** access. Android 13 and later require notification permission. Alarm volume, app battery restrictions, and notification settings can still affect delivery.

Mitrio is a personal organizer, not medical care or addiction treatment. It does not provide crisis support, medication guidance, or recovery services.

## Credit

Mitrio Beta is a collaboration by **para9oid × GPT**.
