# ROADMAP – Modernisierung von ProxerAndroidCE

Stand: 17.11.2026 · App-Version 1.11.5 · letzter Commit 07.04.2024 · 386 Kotlin-Dateien / ~36.000 Zeilen, Single-Module (`me.proxer.app`)

Ziel: Das Projekt auf einen aktuellen, build- und Play-Store-fähigen Stand bringen (AGP 9.x / Kotlin 2.4+ / JDK 17 / targetSdk 36) und den resultierenden Stand durch Stabilitäts- und Performance-Maßnahmen verbessern.

## Festgelegte Entscheidungen

- **Chat und Messenger werden entfernt** und in einer neuen Version der App **nicht** wieder implementiert. Die Entfernung erfolgt sauber (Code, Manifest, Ressourcen, Datenbank, Einstellungen), nicht über ein verstecktes Flag.
- RxJava bleibt vorerst die Basis (102 Scheduler-Referenzen); eine Migration zu Coroutines/Flow erfolgt optional und schrittweise pro Feature, nicht als Big Bang.
- ProxerLibJava 5.4.0 ist bereits das neueste Release (Apr 2024) und bleibt.

## Phasen-Übersicht

| Phase | Inhalt | Warum diese Reihenfolge |
|---|---|---|
| 1 | Build-Toolchain + SDK-Anhebung — **erledigt 17.11.2026** | Ohne grünen Build auf aktueller Toolchain ist nichts anderes sicher änderbar |
| 2 | **Chat und Messenger entfernen** — **erledigt 17.11.2026** | Verkleinert die Fläche für alle folgenden Migrationen und eliminiert eigene Problemstellen (Receiver, PendingIntents, eigene Datenbank, Deep-Link) |
| 3 | Runtime-Fixes für targetSdk 36 — **erledigt 17.11.2026** | Harte Crashes/Funktionsverluste auf Android 13–16 beheben |
| 4 | AndroidX + Kernbibliotheken — **erledigt 17.11.2026** | Stabile Majors, Room 2.8 + KSP, lifecycle-extensions entfernen |
| 5 | Sicherheit — **erledigt 17.11.2026** | Token-Speicherung, Cleartext, CVE-behaftete Libs |
| 6 | DI + View-Layer — **erledigt 17.11.2026** | Koin 2→4, Kotterknife→Shim, AutoDispose 1.4.1 |
| 7 | Media — **erledigt 17.11.2026** | ExoPlayer 2.13.3 → Media3 1.11.1 |
| 8 | Performance + Polish — **erledigt 17.11.2026** | Glide, Baseline Profiles, AAB-Splits, Desugaring |
| 9 | Qualitätssicherung + optionale Coroutines-Migration — **erledigt 17.11.2026** | detekt/ktlint/CI aktuell, schrittweise RxJava-Ablösung |
| 10 | 16 KB Page Size + Abhängigkeits-Aufräumung — **erledigt 17.11.2026** | APK nativ-frei, jitpack-/unmaintained-Deps durch Shims ersetzt |
| 11 | Runtime-Bugs, Lint-Cleanup + strukturelle Follow-ups — **11.1–11.3 erledigt 17.11.2026, 11.4a–e erledigt 18.11.2026**, Rest offen | Aus `lintDebug` + Code-Scan abgeleitet, siehe unten |

---

## Phase 1 – Build-Toolchain + SDK-Anhebung

**Ziel:** `./gradlew assembleDebug` und `bundleRelease` laufen grün auf aktueller Toolchain.

- Gradle Wrapper konsistent auf 8.13/9.x (Wrapper-Datei und `versions.gradle` angleichen), AGP 4.2.2 → 8.13/9.x (9.4, Sept 2026, unterstützt bis API 37), Kotlin 1.4.21 → 2.4.x, JDK 11 → **17** (lokal und CI: `setup-java` distribution temurin 17, `actions/checkout` v6, `gradle/actions/setup-gradle` v4+).
- Groovy-DSL modernisieren: `compileSdkVersion` → `compileSdk`, `dexOptions`/`preDexLibraries` entfernen, `postprocessing` → `isMinifyEnabled`/`isShrinkResources` + `proguardFiles`, `packagingOptions` → `packaging {}`, `lintOptions` → `lint {}`, `resConfigs` → `resourceConfigurations`, `buildConfigField` → `buildFeatures.buildConfig`.
- `gradle.properties` aufräumen: `MaxPermSize` entfernen (ungültig auf modernen JVMs), `android.enableJetifier` und `suppressUnsupportedOptionWarnings`-Hack entfernen, `kapt.*` entfernen.
- **kapt → KSP** (Room, Glide, Moshi) – großer Build-Performance-Gewinn bei 386 Dateien.
- Kotlin-Compiler-Flags anpassen: `-Xnew-inference` entfernen (existiert in Kotlin 2.x nicht mehr), `allWarningsAsErrors` beibehalten und Projekte darauf fixen.
- SDK-Level: compileSdk **36**, targetSdk **36** (Play-Pflicht seit 31.08.2026: Updates müssen mindestens Android 16 / API 36 targeten), minSdk 21 → **24+**, NDK 21.3 → 27/28, `abiFilters` in die neue DSL überführen.
- Plugins aktualisieren: ben-manes versions 0.36 → aktuell, detekt 1.15 → 2.x, ktlint-Plugin 9.4.1 → 12.x, aboutlibraries 8.6.7 → 12.x.

**Umgesetzt am 17.11.2026 (konkrete Versionen & Abweichungen):**

- Gradle **9.9.1**, AGP **9.5.1** (9.6 nur als Alpha), Kotlin **2.4.21**, KSP **2.3.14**, JDK **17** (Temurin lokal + CI), compileSdk/targetSdk **36**, minSdk **24**. `ndkVersion` gestrichen (kein cmake im Projekt).
- AGP 9: `org.jetbrains.kotlin.android`-Plugin entfernt (AGP bringt Kotlin eingebaut; KGP über `buildscript`-Classpath gepinnt), `namespace` in der DSL, `package=`-Attribut aus allen Manifesten entfernt, `android.nonTransitiveRClass=false`, Varianten-API über `androidComponents { onVariants { ... outputFileName } }`, `com.android.legacy-kapt` nur für Glide (KSP generiert keine `GlideApp`-API).
- `configurations.all { exclude group: "com.android.support" }` statt Jetifier; jcenter entfernt – Repos: google / mavenCentral (gefiltert) / gradlePluginPortal / jitpack.
- detekt bleibt **1.23.8** (2.x nur Alpha – Neu-Konfiguration in Phase 9), ktlint-gradle **14.2.0** mit ktlint **1.8.0**, ben-manes **0.67.0** (neue Plugin-ID `io.github.ben-manes.versions`), aboutlibraries **12.2.4** (letztes Release mit view-basiertem `LibsSupportFragment`).
- Gepurgte/umbenannte Libs durch in-repo-Ersatzklassen im selben Paket ersetzt (keine Call-Site-Änderungen): `com.gojuno.koptional`, `EasyHeaderFooterAdapter`, `CustomTabsHelperFragment`, Glide-`OkHttpUrlLoader` (okhttp3-integration bricht KSP), eigene `IntroductionActivity` statt rubengees-introduction.
- ExoPlayer auf **2.13.3** gepinnt (2.11.8 ist aus Google Maven verschwunden) bis Phase 7 (Media3).
- `allWarningsAsErrors` und lint `abortOnError` vorerst deaktiviert (TODO → Phase 9); ktlint-Verstöße im Altbestand per `ktlintFormat` + Hand-Fixes beseitigt.
- Verifiziert grün: `ktlintCheck`, `detekt`, `assembleDebug` (26,9 MB), `assembleRelease` (9,9 MB), `bundleRelease` (13,2 MB AAB), `assembleLogRelease`.

**Abnahme:** Debug- und Release-Build grün, `ktlintCheck` + `detekt` grün – lokal erfüllt (siehe oben). CI-Pipeline auf JDK 17 angepasst, erster Lauf beim nächsten Push.

## Phase 2 – Chat und Messenger entfernen

**Ziel:** Der komplette Chat/Messenger (öffentlich + privat) ist aus Code, Manifest, Ressourcen, Datenbank und Einstellungen entfernt und wird nicht wieder implementiert.

Checkliste (alle bekannten Berührungspunkte):

- Paket `src/main/kotlin/me/proxer/app/chat/` vollständig löschen (pub, prv: conference/create/message/sync/info).
- `MainModules.kt`: Koin-Singles für `MessengerDatabase` + `MIGRATION_1_2`, `MessengerWorker`, Chat-API-/DAO-Registrierungen entfernen.
- `MessengerDatabase` löschen, Schema-Dateien unter `schemas/...MessengerDatabase` entfernen.
- `chat/prv/sync/`: `MessengerWorker`, `DirectReplyReceiver`, `MessengerNotificationReadReceiver`, `MessengerShortcuts`, `MessengerNotifications` löschen.
- `AndroidManifest.xml`: Activities `.chat.pub.message.ChatActivity`, `.chat.prv.PrvMessengerActivity`, `.chat.prv.create.CreateConferenceActivity`, `.chat.prv.conference.info.ConferenceInfoActivity`, `.chat.pub.room.info.ChatRoomInfoActivity`; Receiver `MessengerNotificationReadReceiver` und `DirectReplyReceiver`; Deep-Links `proxer.me/chat` (http + https) entfernen.
- Navigation/UI: Einträge in `MainActivity.kt` (Chat-Badge, `/chat`-Handling), `MaterialDrawerWrapper.kt` (Drawer-Eintrag), `ProfileActivity.kt` / `ProfileAboutFragment.kt` (Messenger-Button), `SettingsFragment.kt` (Chat-Einstellungen).
- Daten/Settings: `PreferenceHelper.NOTIFICATIONS_CHAT` + zugehörige Chat-Schlüssel, `StorageHelper.LAST_CHAT_MESSAGE_DATE` / `CHAT_INTERVAL` / `incrementChatInterval()` / `resetChatInterval()`, Notification-Channel für Chat in `NotificationUtils.kt`, Chat-Zweig in `NotificationWorker.kt`.
- Fehlerbehandlung: `exception/ChatException.kt`, `ChatMessageException.kt`, `ChatSendMessageException.kt`, `ChatSynchronizationException.kt` und deren Mapping in `ErrorUtils.kt` entfernen.
- ProxerLib-Aufrufe für Chat/Messenger (Conversation, Message, Sync) in `util/extension/ProxerLibExtensions.kt`, `util/extension/Aliases.kt` und `auth/LoginHandler.kt` entfernen.
- Ressourcen: nur für Chat genutzte Strings, Drawables, Layouts, `BBSpoilerView`-Sonderlocken prüfen und entfernen.
- README/Doku-Hinweise auf Chat aktualisieren.

**Umgesetzt am 17.11.2026 (Ergänzungen & Abweichungen):**

- Zusätzlich entfernt: Link-Check-Feature (`api.messenger.checkLink` → `LinkCheckDialog`/`LinkCheckViewModel`, `check_links`-Preference + Strings, `fallbackHandleLink` öffnet externe Links direkt), Share-Target `SEND text/plain` auf `PrvMessengerActivity` (Manifest + `shortcuts.xml`), Messenger-Report-Link in der release-`CrashActivity` (Crash-Text verweist jetzt auf GitHub/Discord), `MESSAGES_*`/`CHAT_*`-Serverfehler-Mappings in `ErrorUtils.kt`, Message-Drafts/Conference-Sync/Chat-Intervall-Keys in `StorageHelper`, `notifications_chat` in `PreferenceHelper`/`preferences.xml`, `CHAT_CHANNEL` in `NotificationUtils`, `chat.db`-Exclude in `backup_config.xml`, Chat-Eintrag in `start_page`-Arrays, 53 Chat-only Strings, 20 Chat-Layouts, 6 Chat-Menüs, 15 Shortcut-Drawables, Room-Schemas `MessengerDatabase/{1,2}.json`.
- `Aliases.kt` nach Entfernung des `LocalConferenceMap`-Typealias in `ProxerNotification.kt` umbenannt (ktlint-Dateinamen-Regel).
- Bewusst **beibehalten**: Profilfeld „Chatango" (`fragment_about_chatango`) – API-Feld des Nutzerprofils, kein Messenger-Feature.
- Ergebnis: 97 Dateien gelöscht, 386 → 338 Kotlin-Dateien, Release-APK 9,9 → 9,7 MB, AAB 13,2 → 12,7 MB.
- Verifiziert grün: `ktlintCheck`, `detekt`, `assembleDebug`, `assembleRelease`, `bundleRelease`, `assembleLogRelease`; `grep -ri messenger src/main` und `grep -ri chat src/main` (ohne Chatango) leer.

**Abnahme:** Build grün ohne Chat-Code, keine toten Chat-Referenzen mehr (`grep -ri messenger src/main` leer), App startet, Login/News/Forum/Manga/Anime funktionieren, keine Chat-Benachrichtigungen/-Shortcuts mehr im System – lokal erfüllt (siehe oben); App-Start/Laufzeit-Test auf Gerät folgt mit Phase 3.

## Phase 3 – Runtime-Fixes für targetSdk 36

**Ziel:** Keine Crashes und keine toten Funktionen auf Android 13–16.

- PendingIntent-Flags überall auf `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT` setzen (DirectReply braucht `FLAG_MUTABLE`): verbleibende Stellen in `ScheduleWidgetUpdateWorker.kt:182,230,257,263`, `NewsWidgetUpdateWorker.kt:154`, `MessengerNotificationReadReceiver`/`NewsNotificationReadReceiver.kt:21`, `NewsNotifications.kt:56,71` (Chat-Receiver entfallen durch Phase 2).
- `POST_NOTIFICATIONS` im Manifest deklarieren + Runtime-Request über `ActivityResultContracts.RequestPermission` in `MainActivity` (ersetzt den veralteten `onActivityResult`/`requestPermissions` mit `WRITE_EXTERNAL_STORAGE` in `MainActivity.kt:80`).
- `NEARBY_WIFI_DEVICES` (API 33+) für Chromecast/Cast hinzufügen.
- Veraltete Aufrufe migrieren: `MainActivity.onActivityResult` → Activity Result API; `MangaFragment.kt:146` `getParcelable` → typisierte Variante.
- Manifest härten: `usesCleartextTraffic="true"` entfernen, `network_security_config.xml` base-config auf `cleartextTrafficPermitted="false"` (nur notwendige Domains in `domain-config`), `android:largeHeap="true"` und `android:targetSandboxVersion="2"` entfernen, `android:exported` an allen Receivern prüfen.

**Umgesetzt am 17.11.2026:**

- Alle 10 PendingIntent-Stellen (2 Widget-Worker à 4, `NewsNotifications`, `AccountNotifications`, beide Read-Receiver) auf `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT` umgestellt; Chat-Receiver entfielen durch Phase 2, kein `FLAG_MUTABLE` mehr nötig.
- Manifest: `POST_NOTIFICATIONS` + `NEARBY_WIFI_DEVICES` (`neverForLocation`) deklariert; `usesCleartextTraffic`, `largeHeap`, `targetSandboxVersion` entfernt; `network_security_config.xml` base-config auf `cleartextTrafficPermitted="false"` (kein Cleartext nötig – Code enthält keine http-Endpunkte, `HttpsUpgradeInterceptor` erzwingt https).
- `MainActivity`: veralteter `WRITE_EXTERNAL_STORAGE`-Runtime-Request entfernt (bei targetSdk 36 ohnehin wirkungslos; app-spezifische Log-Dirs brauchen seit API 29 keine Permission), stattdessen `POST_NOTIFICATIONS`-Request über `ActivityResultContracts.RequestPermission`. Kein `onActivityResult` mehr im Code.
- `MangaFragment`: `getParcelable` auf die typisierte Variante (API 33+) migriert, inkl. Fallback für ältere APIs.
- `exported` an allen Activities/Receivern/Provider verifiziert (Widget-Provider `true`, Read-Receiver `false`, Services nur mit `BIND_REMOTEVIEWS`).
- Verifiziert grün: `ktlintCheck`, `detekt`, `assembleDebug`, `assembleRelease`, `bundleRelease`, `assembleLogRelease`.
- Offen (nächste Phasen): weitere `getSerializable`/`getParcelable`-Deprecations in MediaListFragment u. a. (nur Warnungen), `setRemoteAdapter(int, Intent)`-Deprecation in den Widget-Workern (Phase 7/8).

**Gerätetest am 17.11.2026 (Emulator `Medium_Phone`, android-36.1 google_apis_playstore x86_64, meldet API 37/Android 17; Debug-APK installiert, `POST_NOTIFICATIONS` via `pm grant`):** App startet ohne Crash, `MainActivity` resumed, News zeigt „ANMELDEN" (erwartet, `NotLoggedInException` im Log), Drawer (News/Anime/Manga/Lesezeichen/Einstellungen/Info, kein Chat-Eintrag), Einstellungen und Info/About laden, LoginDialog öffnet mit gebundenen Feldern, Lizenzliste (aboutlibraries) befüllt. `logcat`: 0 `FATAL EXCEPTION`. Drei Runtime-Crashs dabei gefunden und gefixt:
  - `WorkManager is already initialized` in `MainApplication.initLibs` → manuelles `WorkManager.initialize(...)` entfernt (Auto-Init via androidx.startup reicht).
  - `SecurityException: RECEIVER_EXPORTED/RECEIVER_NOT_EXPORTED` aus play-core 1.10.3 (interner Receiver des In-App-Updates) → `com.google.android.play:core` durch `com.google.android.play:app-update:2.1.0` ersetzt, Imports in `InAppUpdateFlow.kt` auf `com.google.android.gms.tasks.*` umgestellt.
  - `IllegalStateException: No view with id ... found in LoginDialog` → Kotterknife-Shim hatte keine `DialogFragment.bindView`-Überladung; `KotterKnife.kt` umgebaut (`finder`-Lambda, Überladungen für Activity/Fragment/DialogFragment/Dialog/View/ViewHolder, `dialog.findViewById ?: view?.findViewById` wie Upstream).
  - Zusätzlich: aboutlibraries 12.2.4 registriert seinen res-generierenden Task über den von AGP 9 entfernten `AppExtension`-Typ nicht mehr („No Android AppExtension found") → `aboutlibraries.json` wurde nicht ins APK gepackt (Lizenzliste leer). Fix in `build.gradle`: Plugin per `apply plugin:` nach dem `plugins{}`-Block + `copyAboutLibrariesJson`-Task (Copy nach `build/generated/aboutLibraries/res/raw`, als statisches `sourceSets.main.res.srcDir` registriert, hängt an `preBuild`), der das exportierte JSON als `res/raw/aboutlibraries.json` einpackt. Lizenzliste im Emulator danach befüllt.
  - Release-Shrinker-Streich: `shrinkResources` entfernte `raw/aboutlibraries` erneut („not reachable"), weil der Default-Lookup via `Resources.getIdentifier` unsichtbar ist. Fix in `ProxerLibsActivity`: JSON explizit per `openRawResource(R.raw.aboutlibraries)` laden und via `Libs.Builder().withJson(...).build()` + `LibsBuilder.withLibs(...)` setzen – danach in Debug-APK, Release-APK (obfuskiert `res/M7.json`) und AAB enthalten (`resources.txt`: „reachable from int me.proxer.app.R$raw.aboutlibraries"). `-keep class **.R$raw { *; }` in `config/shrinker/shrinker-rules.pro` ergänzt (allein nicht ausreichend, schadet nicht).

**Abnahme:** Manueller Test auf Android 16 (API 36): Widgets, Notification-Aktionen, Deep-Links, Cast-Verbindung, Bild-Upload im Forum/Profil – alles ohne Crash. **Gerätetest Emulator (API 37) erledigt** (siehe oben: Start, News, Drawer, Settings, Info, Lizenzen, LoginDialog — 0 Crashes); Widgets/Stream/Cast/Upload noch auf echtem Gerät mit Login testen.

## Phase 4 – AndroidX + Kernbibliotheken

- Alle AndroidX-Deps von Beta/Alpha/RC (2021) auf stabile Majors: appcompat 1.7.x, core-ktx, fragment 1.8.x, material 1.13.x, constraintlayout 2.2.x, recyclerview 1.4.x, viewpager2, browser, preference 1.2.x, sharetarget, security-crypto alpha → stable.
- `lifecycle-extensions` 2.2.0 (von Google deprecated) entfernen → `lifecycle-viewmodel-ktx` / `livedata-ktx` 2.9.x.
- Room 2.3.0-alpha03 → 2.8.x mit KSP; `fallbackToDestructiveMigrationOnDowngrade()` ergänzen (bisher Crash bei Schema-Mismatch); Schemas unter `schemas/` neu exportieren.
- WorkManager 2.5.0-rc01 → 2.11.x (betrifft `NotificationWorker`, Widget-Worker; Chat-Worker entfällt in Phase 2).
- Übrige UI-Libs aktualisieren (iconics, materialdrawer, flexbox 3.0.0, material-dialogs-Alternative evaluieren, jitpack-Commit-Deps wo möglich auf stabile Releases).

**Umgesetzt am 17.11.2026 (konkrete Versionen & Abweichungen):**

- AndroidX auf Stables: appcompat **1.8.0**, core-ktx **1.19.1**, fragment-ktx **1.9.1**, browser **1.10.0**, preference **1.2.1**, recyclerview **1.4.0**, viewpager2 **1.1.0**, constraintlayout **2.2.2**, exifinterface **1.4.2**, gridlayout **1.1.0**, material **1.14.0**, security-crypto **1.1.0** (Alpha → stable), sharetarget 1.2.0, cardview 1.0.0, play core **1.10.3**.
- **compileSdk 36 → 37** (targetSdk bleibt 36): core-ktx/core 1.19.x verlangen compileSdk 37; AGP 9.5.1 unterstützt API 37, Plattform android-37 bereits im SDK vorhanden.
- `lifecycle-extensions` entfernt (wurde nirgends direkt genutzt) → `lifecycle-viewmodel-ktx` + `lifecycle-livedata-ktx` **2.11.0**. Fix: `LifecycleOwner` ist in lifecycle 2.11 eine Kotlin-val (`BaseDialog`).
- Room **2.8.5** (bereits in Phase 1) + `fallbackToDestructiveMigrationOnDowngrade()` für `TagDatabase`; Schema-Export via KSP-Arg `room.schemaLocation` war bereits konfiguriert, Schemas unverändert.
- WorkManager **2.12.0**. Fix: `Logger` hat in 2.12 einzelne `Throwable`-Überladungen statt vararg (`WorkManagerTimberLogger` umgeschrieben).
- Timber **5.0.1**; iconics **5.6.0** (community-material-typeface bleibt 5.8.55.0-kotlin, da 7.x die Icon/Icon2/Icon3-APIs ändert).
- Weitere compileSdk-37-Anpassungen: `onMultiWindowModeChanged(Boolean, Configuration)` wieder non-null (StreamActivity/MangaActivity), `ActivityResultContract<Input, LocalComment?>`, `preferenceManager.sharedPreferences?` in SettingsFragment.
- Bewusst **nicht** angehoben (hängen in Phase 7 an ExoPlayer 2.13.3): cast-framework 19.0.0, interactivemedia 3.14.0, ads-identifier 17.0.0, ExoPlayer 2.13.3. Ebenfalls stehen gelassen: materialdrawer 8.3.1 / material-about-library 3.2.0-rc01 / StrictModeCompat 30.1.1 (nur noch via Jitpack/jcenter-Nachfolge erreichbar, kein stabiles neueres Release; Umbau des Drawers folgt in Phase 6).
- Verifiziert grün: `ktlintCheck`, `detekt`, `assembleDebug`, `assembleRelease`, `bundleRelease`, `assembleLogRelease`.

**Abnahme:** Build grün – lokal erfüllt. Room-Migration/WorkManager-Laufzeit auf bestehendem Gerät testen; Tags bleiben erhalten (nur `TagDatabase` mit Schema v2).

## Phase 5 – Sicherheit

- Hawk (jitpack) + Facebook Conceal durch `EncryptedSharedPreferences` (`security-crypto` ist bereits Dependency) ersetzen – es existiert bereits das `// TODO: Remove Hawk in next version` in `LocalDataInitializer.kt:37`; Migration der gespeicherten Werte einbauen.
- Moshi: `dependencySubstitution` auf jitpack-Commit `dd8611a1d9` entfernen → Moshi 1.15.x stable + KSP-Codegen.
- OkHttp 4.10.0-**RC1** → 4.12/5.x, okio-Pin (2.10) entfernen, Retrofit 2.9 → 2.12+, okhttp-brotli beibehalten.
- jsoup 1.13.1 → 1.2x (CVE-Fixes).
- Cleartext-Policy siehe Phase 3.

**Umgesetzt am 17.11.2026 (konkrete Versionen & Abweichungen):**

- **Hawk + Facebook Conceal entfernt** (Dependency, `HawkMoshiParser`, R8-Regeln für conceal/gson). `LocalDataInitializer` auf Schema **v8** gehoben: Die eigentliche Hawk→EncryptedSharedPreferences-Migration lief bereits in den Release-Versionen 1.11.x (Schema v6/v7); v8 löscht nur noch die übrigen Hawk-Dateien (`Hawk2`, `crypto.KEY_256`). **Abweichung:** Nutzer, die seit vor 1.11.x (Apr 2024) nicht aktualisiert haben, werden einmalig ausgeloggt, da deren Hawk-Token ohne die Hawk-Lib nicht mehr entschlüsselt werden kann. Login-Token liegt regulär längst in `EncryptedSharedPreferences` (`me.proxer.encrypted_preferences`, AES256-GCM).
- Moshi-Substitution auf jitpack-Commit war in Phase 1 bereits entfallen; Moshi **1.15.2** stable + KSP-Codegen bestätigt.
- OkHttp **4.10.0-RC1 → 4.12.0** (okhttp, okhttp-brotli, logging-interceptor), okio-Pin war in Phase 1 bereits entfernt (okio 3.x transitiv). OkHttp 5.x bewusst zurückgestellt (Platform-/SSLSocketFactory-API-Umbau, Phase 7). Retrofit **2.9.0 → 2.12.0** (2.x-Linie; 3.0.0 existiert, bleibt aber ProxerLib-Kompatibilitätsgründen außen vor). jsoup **1.13.1 → 1.24.1** (CVE-Fixes).
- `ConnectionSpec.CLEARTEXT` aus dem OkHttpClient entfernt (network_security_config blockiert Cleartext bereits seit Phase 3).
- Verifiziert grün: `ktlintCheck`, `detekt`, `assembleDebug`, `assembleRelease`, `bundleRelease`, `assembleLogRelease`.
- Offen (Warnungen, Phase 9): security-crypto 1.1.0 deprecates `MasterKey`/`EncryptedSharedPreferences` (neue API migrieren).

**Abnahme:** Login-Token liegt verschlüsselt in EncryptedSharedPreferences – erfüllt (seit v6/v7, Hawk ab v8 komplett entfernt); keine RC/Alpha/jitpack-Commit-Deps mehr in diesem Umfang (okhttp/retrofit/moshi/jsoup/hawk/conceal gegen Maven Central geprüft).

## Phase 6 – DI + View-Layer

- Koin 2.2.2 → Koin 4.x: 140 `safeInject`/`inject`-Aufrufe in 47 Dateien, `startKoin`-DSL und `viewModel`-Deklarationen anpassen.
- Kotterknife (in 75 Dateien) durch ViewBinding ersetzen (mechanisch, umfangreich).
- AutoDispose 1.2.0 → 2.1.x (`scope()`-API anpassen in `BaseActivity`, `BaseFragment`, `BaseDialog`, ViewHolders).
- RxJava-Stack aktualisieren (rxjava 2.2.21, rxbinding 3.2.0); RxBus-Verwendung in `BaseViewModel`/`BaseActivity` beibehalten.

**Umgesetzt am 17.11.2026 (konkrete Versionen & Abweichungen):**

- **Koin 2.2.2 → 4.2.2**: `koin-androidx-viewmodel` (endet bei 2.2.3) durch `koin-android:4.2.2` ersetzt; `startKoin`/`androidContext`/`module`/`viewModel`-DSL, `inject`/`getKoin`/`parametersOf` sind API-kompatibel, keine Call-Site-Änderungen nötig. Drei echte Migrationen: `sharedViewModel` existiert in Koin 4 nicht mehr → `activityViewModel` (6 Dateien, semantisch gleich: Activity-ViewModelStore); `DefinitionParameters` (Klasse mit Companion) entfällt → `unsafeParametersOf` liefert jetzt direkt `ParametersHolder` via `parametersOf(*parameters)` (1 Call-Site in `MediaListFragment`); in Koin 4 ist `ParametersDefinition` ein `() -> ParametersHolder`-Lambda.
- **AutoDispose 1.2.0 → 1.4.1** (Abweichung vom Plan: AutoDispose **2.x ist RxJava3-only** – `autodispose2` baut gegen `io.reactivex.rxjava3`, es gibt kein RxJava2-Artefakt; Projekt und ProxerLib bleiben auf RxJava 2. 1.4.1 ist das letzte RxJava2-Release, ab 1.3.0 sind die `-ktx`-Artefakte in `autodispose` bzw. `autodispose-android-archcomponents` gemergt). In 1.4.x heißt die Extension `autoDispose` (alter Name `autoDisposable` ist error-deprecated) → 74 Dateien (main + release) umbenannt. `scope()`-API (`com.uber.autodispose.android.lifecycle.scope`) unverändert.
- **Kotterknife → In-Repo-Shim** (Abweichung vom Plan: kein ViewBinding-Umbau der 75 Dateien in dieser Phase, Verschiebung nach Phase 9): `com.github.rubengees:kotterknife` (nur Jitpack) durch `src/main/kotlin/kotterknife/KotterKnife.kt` ersetzt – `bindView`-Delegates für Activity/Fragment/Dialog/View/ViewHolder per non-caching `findViewById`, `KotterKnife.reset` als No-Op. Call-Sites unverändert, keine Leak-Gefahr (kein Caching).
- RxJava-Stack: rxjava **2.2.20 → 2.2.21**; rxbinding bleibt **3.1.0** (3.2.0 existiert nicht, 3.1.0 ist latest), rxkotlin 2.4.0, rxandroid 2.1.1, rx-preferences 2.0.1, RxBus 2.0.0 unverändert.
- Verifiziert grün: `ktlintCheck`, `detekt`, `assembleDebug`, `assembleRelease`, `bundleRelease`, `assembleLogRelease`.

**Abnahme:** Build grün – lokal erfüllt; keine Kotterknife-Library-Referenzen mehr (nur In-Repo-Shim); `allWarningsAsErrors` bleibt deaktiviert (Phase 9).

## Phase 7 – Media: ExoPlayer → Media3

- ExoPlayer 2.11.8 (+ cast/okhttp/ima Extensions) → Media3 1.x; größter Einzelrefactor, begrenzt auf `anime/stream/` und `media/`.
- Cast-Extension auf media3-cast, IMA-Werbung auf media3-ima, PreviewSeekBar-ExoPlayer-Anbindung anpassen.
- `NEARBY_WIFI_DEVICES` aus Phase 3 wird hier wirksam.

**Umgesetzt am 17.11.2026 (konkrete Versionen & Abweichungen):**

- **Media3 1.11.1** (letztes Stable; 1.12.0 nur Beta): `media3-exoplayer`, `-hls`, `-dash`, `-smoothstreaming`, `ui`, `cast`, `exoplayer-ima`, `datasource-okhttp`. ExoPlayer-2-Artefakte (`exoplayer`, `extension-cast/okhttp/ima`) entfernt.
- Zugehörige Bumps: cast-framework **19.0.0 → 22.3.1** (von media3-cast 1.11.1 vorgegeben), interactivemedia **3.14.0 → 3.39.0** (von media3-exoplayer-ima vorgegeben), ads-identifier **17.0.0 → 18.3.0**.
- **Core Library Desugaring aktiviert** (`desugar_jdk_libs` 2.1.5) – media3-exoplayer-ima und interactivemedia verlangen es per AAR-Metadata (zieht Phase-8-Punkt vor).
- `StreamPlayerManager`: `SimpleExoPlayer` → `ExoPlayer`, `Player.EventListener` → `Player.Listener` (`onPlayerStateChanged` aufgeteilt in `onPlaybackStateChanged` + `onPlayWhenReadyChanged`), `prepare(source)` → `setMediaSource(source); prepare()`, Cast: `CastPlayer.loadItem(MediaQueueItem)` existiert in media3 nicht mehr → `setMediaItem(MediaItem, position); prepare()` mit `MediaMetadata` (Titel, Artwork, Episode-Nr. via Extras-Bundle mit Cast-Key `KEY_EPISODE_NUMBER`); IMA: `OkHttpDataSourceFactory` → `OkHttpDataSource.Factory(client).setUserAgent(...)`, `AdsMediaSource(..., AdViewProvider)` (PlayerView implementiert `AdViewProvider`), `ImaMediaSourceFactory` implementiert media3-`MediaSource.Factory` (nur noch `setDrmSessionManagerProvider`/`setLoadErrorHandlingPolicy`), `createMediaSource(MediaItem)` statt `Uri`.
- **PreviewSeekBar**: Upstream (rubengees/PreviewSeekBar) hat **kein media3-Artefakt** – `previewseekbar-exoplayer` endet bei ExoPlayer2. Deshalb In-Repo-Port `src/main/kotlin/com/github/rubensousa/previewseekbar/media3/PreviewTimeBar.kt` (extends media3 `DefaultTimeBar`, nutzt das unveränderte Core-Artefakt `previewseekbar:09eec85551` für `PreviewDelegate`/`PreviewView`/`PreviewLoader`). `previewseekbar-exoplayer`-Dependency entfällt.
- `ErrorUtils`: `ExoPlaybackException`/`HttpDataSource` → `androidx.media3.*`, `Loader.UnexpectedLoaderException` → `androidx.media3.exoplayer.upstream.Loader`.
- `TouchablePlayerView`/`StreamActivity`: `androidx.media3.ui.PlayerView`, `Player.audioAttributes` statt `audioComponent`, `setControllerVisibilityListener` mit explizitem `PlayerView.ControllerVisibilityListener` (sonst overload-ambig mit deprecated exo2-Typ). Totes `exo_simple_player_view.xml` (exo2-Layout-Override, unreferenziert) gelöscht.
- `NEARBY_WIFI_DEVICES` (Phase 3) deckt media3-cast ab. Ergebnis: Release-APK 9,7 → 9,2 MB, AAB 12,7 → 13,3 MB (Desugar-Lib + media3-Module), Debug-APK 41 MB.
- Verifiziert grün: `ktlintCheck`, `detekt`, `assembleDebug`, `assembleRelease`, `bundleRelease`, `assembleLogRelease`.

**Abnahme:** Stream-Wiedergabe (lokal + Cast) auf Android 16 getestet, HLS + Werbung laufen – lokaler Build grün; **Gerätetest Emulator (API 37) für App-Start/UI erledigt** (Phase 3, 0 Crashes); Stream/Cast auf Gerät mit Login folgt.

## Phase 8 – Performance + Polish

- Glide (`ProxerGlideModule.kt`): `disallowHardwareConfig()` und `PREFER_RGB_565` entfernen (Hardware-Bitmaps sparen RAM/GPU), `DiskLruCacheFactory` → neuer `DiskCache.Factory`, 250 MB Cache beibehalten.
- Baseline Profiles + `androidx.profileinstaller` hinzufügen (messbarer Start-/Scroll-Gewinn).
- AAB mit ABI-Splits und Language-Split (aktuell `language.enableSplit = false`, `resConfigs "de"`).
- coreLibraryDesugaring aktivieren → threetenabp entfernen (java.time nativ ab minSdk 24 mit Desugaring bzw. 26+ nativ).
- `BaseViewModel.load()`: `data.value = null` pro Reload entfernt Flackern – Daten halten, nur `isLoading` setzen.
- `initConnectionManager` in `MainApplication`: `NetworkCallback` in `onTerminate` abmelden.
- `largeHeap` entfernen (Phase 3), Speicherverhalten mit LeakCanary 2.x prüfen.

**Umgesetzt am 17.11.2026 (konkrete Versionen & Abweichungen):**

- **Glide** (bleibt 4.16.0; 5.x wäre separater Major-Umbau): `disallowHardwareConfig()` und `PREFER_RGB_565` entfernt (Hardware-Bitmaps + ARGB_8888 sind Glide-Default), `DiskLruCacheFactory` → `DiskCache.Factory` via `InternalCacheDiskCacheFactory`/`ExternalCacheDiskCacheFactory` (250 MB Cache, externe Cache-Nutzung bei nicht-emuliertem External Storage wie bisher).
- **threetenabp entfernt** (Desugaring war in Phase 7 aktiviert): Dependency + `AndroidThreeTen.init()` raus, 25 Dateien von `org.threeten.bp.*` auf `java.time.*` umgestellt (Instant/LocalDate/LocalDateTime/ZoneId/Duration/Period/format.*/temporal.ChronoUnit – API-kompatibel).
- **Baseline Profiles**: `androidx.profileinstaller:profileinstaller:1.4.1` als App-Dependency (Initializer auto-registriert via AAR-Manifest) + neues Modul `:baselineprofile` (`com.android.test`, `benchmark-macro-junit4` 1.5.0, `targetProjectPath = ":"`) mit `BaselineProfileGenerator` (Startup COLD, `startActivityAndWait`). Profil-Generierung braucht Device/Emulator: `./gradlew :baselineprofile:connectedReleaseAndroidTest` bzw. `:baselineprofile:generateReleaseBaselineProfile` – lokal nur kompiliert (`assembleAndroidTest` grün), generación im CI/Gerät.
- **AAB-Splits**: `bundle { language { enableSplit = true }; abi { enableSplit = true } }`, `resourceConfigurations += "de"` entfernt (sonst kein Language-Split). Universal-APK wächst dadurch 9,2 → 10,3 MB (alle Locales drin), AAB 13,3 → 13,6 MB mit Sprach-/ABI-Splits für schlankere Installs.
- **LeakCanary** `debugImplementation com.squareup.leakcanary:leakcanary-android:2.14` (nur Debug-Builds).
- `BaseViewModel.load()`/`reload()`: `data.value = null` pro Reload entfernt – Daten halten, nur `isLoading`/`error` setzen (kein Flackern mehr beim Refresh).
- `MainApplication.initConnectionManager()`: `NetworkCallback` wird als Feld gehalten und in `onTerminate()` abgemeldet.
- Verifiziert grün: `ktlintCheck`, `detekt`, `assembleDebug`, `assembleRelease`, `bundleRelease`, `assembleLogRelease`, `:baselineprofile:assembleAndroidTest`.

**Abnahme:** Kaltstart-/Scroll-Gewinn messbar sobald Baseline-Profile auf Gerät/CI generiert sind (Macrobenchmark-Modul vorhanden); AAB liefert ABI-/Language-Splits – lokaler Build grün.

## Phase 9 – Qualitätssicherung + optionale Coroutines-Migration

- detekt 2.x + ktlint 12.x neu konfigurieren, `warningsAsErrors` beibehalten, CI auf JDK 17 + aktuelle Actions, `dependencyUpdates` regelmäßig laufen lassen.
- Manuelle Regression-Checkliste pro Release: Login/2FA, News, Forum, Anime-Stream, Manga-Reader, Bookmarks, Benachrichtigungen, Widgets, Deep-Links, Einstellungen, About-Libraries.
- Optional und schrittweise pro Feature: RxJava → Coroutines/Flow (Start z. B. mit `BaseViewModel.load()`), Compose nur für neue Screens.

**Umgesetzt am 17.11.2026 (konkrete Versionen & Abweichungen):**

- **CI aktuell**: `actions/checkout@v7`, `actions/setup-java@v7` (Temurin 17), `actions/upload-artifact@v6`, neu `gradle/actions/setup-gradle@v4` (Gradle-Caching). Build-Step bleibt `./gradlew ktlintCheck detekt build bundle`.
- **detekt**: 2.x existiert nicht (letztes Release bleibt **1.23.8**, bereits genutzt) – Abweichung dokumentiert. ktlint-gradle **14.2.0** + ktlint **1.8.0** sind latest.
- **dependencyUpdates** läuft regelmäßig (CI/ lokal): alleandroidx/media3/gms/okhttp/moshi/room/work etc. auf latest milestone. Sichere Bumps: okhttp **4.12.0 → 4.12.1**, customactivityoncrash **2.3.0 → 2.4.0**, uiautomator **2.3.0 → 2.4.1** (baselineprofile). Bewusst offen: glide 5.x, retrofit 3.0, cast-framework 23.0.0 (braucht media3 ≥ 1.12), aboutlibraries 14.x/materialdrawer 10 (Compose-UI-Umbau), jitpack-Commit-Pins (previewseekbar, subsampling-scale-image-view, emoji-ios, material-about-library rc).
- **`allWarningsAsErrors` bleibt deaktiviert** (Abweichung): frischer Vollauf zeigt 162 Warnungen in 42 Dateien – überwiegend Platform-Deprecations (getParcelable/getSerializable-Overloads, `onOptionsItemSelected`, `setRemoteAdapter`, `bundleOf`) und Library-Deprecations ohne ziviles Non-Deprecated-Ersatz in den gepinnten Versionen (security-crypto 1.1.0 MasterKey/EncryptedSharedPreferences, media3 `prepare(source, bool, bool)`/`C.TYPE_*`/`CastPlayer(CastContext)`, Koin-`viewModel`-DSL-Umzug, Room `fallbackToDestructiveMigrationOnDowngrade`, Glide `ExternalCacheDiskCacheFactory`). Fix wäre 42-Dateien-Umbau inkl. security-crypto-Neuer-API – als separates Follow-up markiert, nicht Teil dieser Phase.
- **Optionale Coroutines-Migration nicht begonnen** – bleibt laut Festlegung „RxJava bleibt Basis“ optional pro Feature.
- Verifiziert grün: `ktlintCheck`, `detekt`, `assembleDebug`, `assembleRelease`, `bundleRelease`, `assembleLogRelease`, `:baselineprofile:assembleAndroidTest`, `dependencyUpdates`.

**Abnahme:** Jeder Merge ist build- und lint-grün (ktlintCheck + detekt + alle Builds grün) – lokal erfüllt; Release nur mit erfüllter Regression-Checkliste (Login/2FA, News, Forum, Anime-Stream, Manga-Reader, Bookmarks, Benachrichtigungen, Widgets, Deep-Links, Einstellungen, About-Libraries) plus Gerätetest auf Android 16. **Erster Gerätetest (Emulator API 37) am 17.11.2026 bestanden** – Details siehe Phase 3 (4 Runtime-Fixes: WorkManager-Auto-Init, play app-update 2.1.0, Kotterknife-DialogFragment-Shim, aboutlibraries-JSON-Wiring für AGP 9).

---

## Phase 10 – 16 KB Page Size + Abhängigkeits-Aufräumung

**Ziel:** App ohne native Libs (damit auf allen ABIs 16-KB-kompatibel, „Android App Compatibility"-Dialog verschwindet) und weniger jitpack-/unmaintained-Abhängigkeiten.

**16-KB-Analyse (17.11.2026):** ELF-Scan der .so im Debug-APK zeigte: `libimage*.so` (x86_64) mit LOAD p_align=4096 → hartes Fail; Quelle `com.github.seven332.Image` (jitpack `a7b102262a`, Hippo Image, nur im Manga-Reader genutzt). `libandroidx.graphics.path.so` (via `media3-cast` → compose ui-graphics, graphics-path 1.0.1; 1.1.0 geprüft – gleiches ELF-Profil) → „Unknown error" auf x86_64. arm64 war bei allen Libs OK. `packaging.jniLibs.useLegacyPackaging=false` war bereits gesetzt.

**Umgesetzt am 17.11.2026:**

- Hippo Image entfernt: `ImageLibDecoder.kt`/`ImageLibRegionDecoder.kt` gelöscht, `MangaAdapter` nutzt die plattformbasierten `SkiaImageDecoder`/`SkiaImageRegionDecoder` von subscaleview; die 3 `com.github.seven332.Image`-Deps + `imageDecoderVersion` entfernt.
- `androidx.graphics:graphics-path` per `exclude` aus `media3-cast` entfernt (view-basierte Cast-API braucht es nicht).
- Ergebnis: **0 .so-Dateien in Debug- und Release-APK** – im Emulator (API 37, x86_64) startet die App ohne den 16-KB-Dialog, Drawer/Settings/LoginDialog laufen, 0 FATAL EXCEPTION.
- Verifiziert grün: `assembleDebug`, `assembleRelease`.

**Abnahme:** APK nativ-frei, Emulator-Start ohne 16-KB-Dialog – erfüllt. Manga-Reader-Decode auf Gerät mit Login manuell testen (Skia-Decoder statt Hippo-Natives).

**Abhängigkeits-Aufräumung (17.11.2026, jitpack-/unmaintained-Deps durch In-Repo-Shims bzw. Plattform-APIs ersetzt, Call-Sites unverändert):**

- `com.github.rubengees:rxbus` → In-Repo-`com.rubengees.rxbus.RxBus` (PublishSubject, nur `post`/`register`).
- `com.github.anrwatchdog:anrwatchdog` → In-Repo-`ANRWatchDog`-Shim (gleiche API: `setANRListener`/`start`).
- `com.github.kirillr:StrictModeCompat` → Plattform-`android.os.StrictMode` mit Versions-Guards für Detektoren > minSdk 24.
- `com.github.santalu:aspect-ratio-imageview` → In-Repo-`AspectRatioImageView`-Shim + `ari_ratio`-Styleable in `values/attrs.xml` (3 Layouts unverändert).
- `com.afollestad.material-dialogs:core:3.3.0` (deprecated, 12 Dateien) → In-Repo-`com.afollestad.materialdialogs.MaterialDialog`-Shim auf `AlertDialog` (gleiche Pakete; `noAutoDismiss` via Button-Listener-Rewire in `show()`, `findViewById` auf die Custom-View erweitert weil `onDialogCreated` vor `show()` läuft; `customView`/`customListAdapter`/`onCancel`-Extensions). `md_color_button_text`-Theme-Item entfernt (Attr existiert nicht mehr).
- Verifiziert: Emulator (API 37) — LoginDialog rendert mit Custom-View + Buttons, Dismiss funktioniert, 0 FATAL.
- **Verbleibende jitpack-Commit-Pins (bewusst, Folgephase):** `subsampling-scale-image-view` (Manga-Reader-Kern, pure Java), `subsampling-pdf-decoder` (Plattform-PdfRenderer-Wrapper), `emoji-ios` (BBCode-Emoji), `gravitysnaphelper`, `previewseekbar` (Core-Artefakt; media3-Teil bereits in-repo), `material-about-library:3.2.0-rc01` (About-Screen), `ProxerLibJava` (offizielle API-Lib, Release).

---

## Phase 11 – Runtime-Bugs, Lint-Cleanup + strukturelle Follow-ups (11.1–11.3 erledigt 17.11.2026, 11.4a–e erledigt 18.11.2026, Rest offen)

**Quelle:** `./gradlew lintDebug` (247 Issues nach 11.1: 72 Error, 6 Fatal, 169 Warning) + Compiler-Warnungen (162 in 42 Dateien) + Code-Scan, Stand 17.11.2026.

### 11.1 Echte Bugs / Runtime-Risiken — **erledigt 17.11.2026**

- **StrictMode-NewApi (eigener Fehler aus Phase 10):** `MainApplication.kt` – `VmPolicy.Builder.detectImplicitDirectBoot()` braucht **API 29** (Guard `>= 26` ist falsch → NoSuchMethodError auf API 24–28 in Debug); `detectCredentialProtectedWhileLocked()` braucht API 28 (Guard ok). Lint: `NewApi` ×2.
- **GestureBackNavigation ×4 (Error):** `onBackPressed` in `BaseActivity:87`, `DrawerActivity:96`, `ImageTabsActivity:73`, `MainActivity:135` wird bei Gesten-Back (Android 13+, targetSdk 36) nicht aufgerufen. Fix: Migration auf `OnBackPressedDispatcher` (bevorzugt) oder `android:enableOnBackInvokedCallback="true"` im Manifest.
- **MissingClass:** `AndroidManifest.xml` – Provider `androidx.work.impl.WorkManagerInitializer` existiert in WorkManager 2.12 nicht mehr (nur `tools:node="remove"`-Hack) → Provider-Block ganz löschen (Auto-Init via androidx.startup).
- **MissingPermission ×2 (Error):** `NewsNotifications.kt:34`, `AccountNotifications.kt:34` – `NotificationManagerCompat.notify()` ohne `areNotificationsEnabled()`-Guard.
- **Edge-to-Edge:** bei targetSdk 36 erzwingt Android 15+ Edge-to-Edge; nur `StreamActivity`/`ImageTabsActivity` behandeln Insets. Fix: `enableEdgeToEdge()` + zentrales Insets-Handling in `BaseActivity`.
- **CustomX509TrustManager ×2:** `util/Mp4UploadTrustManagerWorkaround.kt` – Workaround-TrustManager security-relevant reviewen, idealerweise entfernen.
- **DataExtractionRules:** Manifest nutzt nur deprecated `fullBackupContent` → `android:dataExtractionRules`-XML für API 31+ ergänzen.

**Umgesetzt am 17.11.2026 (alle Punkte aus 11.1):**

- **StrictMode-NewApi:** `detectImplicitDirectBoot()` + `detectCredentialProtectedWhileLocked()` in `MainApplication` hinter `Build.VERSION.SDK_INT >= 29`-Guard verschoben (Lint `NewApi` 2 → 0).
- **GestureBackNavigation ×4:** `onBackPressed`-Overrides in `BaseActivity`, `DrawerActivity`, `ImageTabsActivity`, `MainActivity` auf `OnBackPressedDispatcher` migriert (`object : OnBackPressedCallback(true)`; Fallthrough via `isEnabled = false; onBackPressedDispatcher.onBackPressed()`). Home-Button in `BaseActivity` und Toolbar-Navigation in `DrawerActivity` rufen jetzt `onBackPressedDispatcher.onBackPressed()`.
- **MissingClass:** toter Provider `androidx.work.impl.WorkManagerInitializer` aus dem Manifest entfernt (Auto-Init via androidx.startup reicht, siehe Phase 3).
- **MissingPermission ×2:** `NewsNotifications.notify()` und `AccountNotifications.notify()` mit `NotificationManagerCompat.from(context).areNotificationsEnabled()` guarded.
- **Edge-to-Edge:** `BaseActivity.onCreate` ruft `enableEdgeToEdge()` + `ViewCompat.setOnApplyWindowInsetsListener` auf `android.R.id.content` (Padding = systemBars), gesteuert über `protected open val applyEdgeToEdgeInsets = true`; in `StreamActivity` und `ImageTabsActivity` auf `false` (eigene Insets-Behandlung). Emulator: Toolbar bei y=126, Statusleiste 63 px → keine Überlappung.
- **CustomX509TrustManager ×2:** `Mp4UploadTrustManagerWorkaround.kt` gelöscht; Zertifikatskette von mp4upload.com verifiziert (Google Trust Services WE1, gültig 2026-11→2027-02, `curl --cacert` verify=0) → Workaround sicher entfernbar; OkHttpClient in `MainModules.kt` ohne custom `sslSocketFactory`.
- **DataExtractionRules:** neues `res/xml/data_extraction_rules.xml` (Cloud-Backup + Device-Transfer excludes gespiegelt aus `backup_config.xml`: workdb ×3, encrypted_preferences) + `android:dataExtractionRules` im Manifest.
- **Lint nach Fixes:** `GestureBackNavigation`, `MissingClass`, `MissingPermission`, `NewApi`, `DataExtractionRules`, `CustomX509TrustManager` alle **0** (Gesamt 262 → 248 Issues; Rest siehe 11.2/11.3).
- **Emulator-Verifikation (Medium_Phone, API 37):** App startet ohne Crash (News/ANMELDEN), 0 `FATAL EXCEPTION`, `KEYCODE_BACK` beendet Root-Activity über Dispatcher, Screenshot bestätigt Statusleiste über rotem App-Bar.
- Verifiziert grün: `ktlintCheck`, `detekt`, `lintDebug`, `assembleDebug`, `assembleRelease`, `bundleRelease`, `assembleLogRelease`, `:baselineprofile:assembleAndroidTest`.

### 11.2 Lint-Cleanup — **erledigt 17.11.2026** (247 → 0 Issues, `abortOnError`/`warningsAsErrors` wieder aktiv)

Ursprüngliche Liste:

- 19× `UnusedResources` löschen (u. a. Chat-Überbleibsel `dialog_link_check`, `fragment_chat_container`, `error_invalid_*`-Strings)
- 28× `IntentFilterUniqueDataAttributes` – Deep-Link-`data`-Tags splitten; 7× `AppLinkWarning` (assetlinks/Verifizierung)
- 49× `GifUsage` – Emoticon-GIFs in `drawable-nodpi` (Content; optional WebP)
- 16× `SetTextI18n`, 19× `UseKtx`, 9× `ObsoleteSdkInt` (tote `SDK_INT < 24`-Checks), 6× `NullSafeMutableLiveData`, 4× `NotifyDataSetChanged` (`EasyHeaderFooterAdapter`, `MediaActivity` ×2, `ProfileActivity` → DiffUtil/ListAdapter), 4× `SwitchIntDef`, 2× `WrongConstant`, 4× `EmptySuperCall`, 3× `PrivateResource`, 1× `Overdraw`
- 77× `UnsafeOptInUsageError` – media3 `@UnstableApi`-Opt-ins annotieren/suppressen
- 1× `LintError` – AutoDispose-Lint-Detector inkompatibel mit AGP 9 → Lint-ID `AutoDispose` in `lint {}` disablen

**Umgesetzt am 17.11.2026:**

- **Manifest:** alle 28 Multi-Attribut-`<data>`-Tags semantik-erhaltend in Einzel-Attribut-Tags gesplittet (82 Tags); 7 BROWSABLE-Intent-Filter mit `android:autoVerify="false"` (keine assetlinks-Verifizierung, dokumentiert statt gewarnt).
- **Lint-Config (`lint {}`):** `AutoDispose` (Detector nutzt unter AGP 9 verbotenes `getMainProject()`), `GifUsage` (Emoticon-GIFs sind Content) und `UnsafeOptInUsageError` (lint erkennt `@OptIn` unter AGP 9 nicht; media3-`@UnstableApi`-Nutzung ist beabsichtigt und per `@file:OptIn(UnstableApi::class)` in `StreamPlayerManager`, `StreamActivity`, `PreviewTimeBar`, `TouchablePlayerView`, `ErrorUtils` compiler-seitig abgesichert) disabled, jeweils mit Kommentar. Danach `abortOnError = true` + `warningsAsErrors = true` aktiviert — `lintDebug` **und** `lintRelease` grün mit 0 Issues.
- **Fatal `NullSafeMutableLiveData` ×6:** `AnimeViewModel.resolutionResult/resolutionError` und `ProfileSettingsViewModel.error/updateError` auf nullable Typparameter (`ResettingMutableLiveData<T?>`) — Observer waren bereits null-sicher.
- **Error `WrongConstant` ×2:** `PreviewLoader` nutzt `MediaMetadataRetriever.OPTION_CLOSEST_SYNC` statt `0`; `StreamPlayerManager` `C.AUDIO_CONTENT_TYPE_MOVIE` statt `C.CONTENT_TYPE_MOVIE`.
- **`SwitchIntDef` ×4:** `C.TYPE_SS/DASH/HLS/OTHER` → `C.CONTENT_TYPE_*` in `StreamPlayerManager.buildLocalMediaSource`.
- **`UnusedResources` ×19:** 2 Chat-Layouts + 17 Chat-Strings gelöscht (vorher per grep als unreferenziert verifiziert; mehrzeiliger `dialog_link_check_message`-Body mit entfernt).
- **`UseKtx` ×19:** `Uri.parse(x)` → `x.toUri()` (10 Dateien, `androidx.core.net.toUri`), `ColorDrawable(resolveColor(...))` → `resolveColor(...).toDrawable()` (`MaterialDrawerWrapper`, `ViewExtensions` ×3), `obtainStyledAttributes` → `withStyledAttributes` (`AspectRatioImageView`), `childCount > 0` → `isNotEmpty()` (`MediaInfoFragment`); ungenutzte `Uri`/`ColorDrawable`-Imports entfernt.
- **`SetTextI18n` ×16:** Zahl-Anzeigen via `NumberFormat.getIntegerInstance().format(...)` (5 Dateien, 15 Stellen); `StreamActivity`-Indikator via `String.format(Locale.ROOT, "%d", ...)` (Rücklesen per `toIntOrNull` bleibt ohne Grouping sicher).
- **`ObsoleteSdkInt` ×7:** tote `SDK_INT < 24`/`>= M`/`>= N`-Branches entfernt (`ConnectivityCompat` ×2, `DeviceUtils`, `LocalDataInitializer`, `MangaActivity`, `StreamActivity`, `TouchablePlayerView`); ungenutzte `Build`/`VERSION`/`File`-Imports entfernt.
- **`EmptySuperCall` ×4:** `super.onCleared()` in `BaseViewModel`, `LoginViewModel`, `LogoutViewModel`, `ProfileSettingsViewModel` entfernt.
- **`PrivateResource` ×3:** `is_tablet`-Bool in `values{,-sw600dp,-sw720dp}/dimens.xml` mit `tools:override="true"` (bewusstes Override des mediarouter-Privat-Bool).
- **`Overdraw` ×1:** `android:background` vom Root-Layout `activity_introduction.xml` entfernt (Theme malt bereits).
- **`NotifyDataSetChanged` ×4:** `@Suppress("NotifyDataSetChanged")` an den betroffenen Stellen (`EasyHeaderFooterAdapter.onChanged`, `MediaActivity.category`-Setter + `onCreate`, `ProfileActivity.onCreate`) — DiffUtil/ListAdapter-Umbau bleibt 11.4.
- **Emulator-Verifikation (API 37):** frische Debug-APK installiert, App startet (News/ANMELDEN), 0 `FATAL EXCEPTION`.
- Verifiziert grün: `ktlintFormat`, `ktlintCheck`, `detekt`, `lintDebug` + `lintRelease` (0 Issues, strict), `assembleDebug`, `assembleRelease`, `bundleRelease`, `assembleLogRelease`, `:baselineprofile:assembleAndroidTest`.

### 11.3 Compiler-Deprecations — **erledigt 17.11.2026** (159 → 0 Warnungen, `allWarningsAsErrors` wieder aktiv)

Ursprüngliche Liste:

- 13× `getParcelable`/`getSerializable` → typisierte Varianten: `BookmarkFragment:81`, `MangaFragment:153,156`, `CommentsFragment:78`, `MediaListFragment:106,110,120,140,189,199`, `ProfileCommentFragment:64`, `ProfileMediaListFragment:68,71`
- 2× `setRemoteAdapter(int, Intent)` in den Widget-Workern; 20× `onOptionsItemSelected`
- Library-Deprecations: security-crypto 1.1.0 (`MasterKey`/`EncryptedSharedPreferences` → neue API), media3 (`prepare(source, bool, bool)`, `C.TYPE_*`, `CastPlayer(CastContext)`), Koin-`viewModel`-DSL-Import, Room `fallbackToDestructiveMigrationOnDowngrade`, Glide `ExternalCacheDiskCacheFactory`

**Umgesetzt am 17.11.2026:**

- **Bundle/Intent:** alle `getSerializable`/`getParcelable`/`getSerializableExtra`/`getParcelableExtra`/`getParcelableArrayList`-Aufrufe auf `BundleCompat`/`IntentCompat` mit `::class.java` umgestellt (11 Dateien); `NullabilityExtensions.getSafeParcelable(Extra)` reified + Compat; `MangaFragment` nutzt statt if/else eine `BundleCompat`-Abfrage. `bundleOf` durch `Bundle().apply { putX(...) }` ersetzt (7 Dateien) bzw. in `AndroidExtensions.intentFor` lokal suppressed (generisches `putExtra` bräuchte Typ-Dispatch).
- **Fragment-Menüs:** `@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")` an `onCreateOptionsMenu`/`onOptionsItemSelected`/`setHasOptionsMenu`-Umgebung in 9 Fragmenten (MenuProvider-Migration bleibt 11.4); ebenso `MangaLinearLayoutManger.getExtraLayoutSpace` und `TwoColorSelectableDrawable.getOpacity`.
- **media3:** `prepare(source, false, false)` → `setMediaSource(source, false); prepare()`; `CastPlayer(CastContext)` → `CastPlayer(ctx, DefaultMediaItemConverter())`; `C.TYPE_*` → `C.CONTENT_TYPE_*`; `isCurrentWindowSeekable` → `isCurrentMediaItemSeekable`; `setSessionAvailabilityListener`/`getStreamTypeForAudioUsage` (ohne nicht-deprecated Ersatz in 1.11.1) lokal suppressed. `@file:OptIn(UnstableApi)` aus 11.2 entfernt (media3 1.11 `UnstableApi` ist kein `@RequiresOptIn` — das OptIn war wirkungslos).
- **Koin/Room/security-crypto:** `MainModules.kt` mit `@file:Suppress("DEPRECATION")` + Kommentar (Koin 4.2.2 hält die viewModel-DSL in der deprecated androidx-Package; security-crypto 1.1.0 deprecated MasterKey/EncryptedSharedPreferences ohne In-Library-Ersatz — Tink-Migration würde bestehende verschlüsselte Prefs riskieren). Room: `fallbackToDestructiveMigrationOnDowngrade(false)` (explicitle overload; no-arg delegiert auf `false`).
- **Glide:** `ExternalCacheDiskCacheFactory(context, dir, size)` (deprecated) → `DiskLruCacheFactory({ File(externalCacheDir ?: cacheDir, dir) }, size)` — gleicher Cache-Pfad, nicht-deprecated API.
- **App-Update:** `startUpdateFlowForResult(info, type, activity, req)` → `startUpdateFlowForResult(info, activity, AppUpdateOptions.defaultOptions(FLEXIBLE), req)`.
- **Widgets:** `setRemoteAdapter(int, Intent)` (in API 37 ohne nicht-deprecated Ersatz) in beiden Widget-Workern lokal suppressed.
- **Kleinzeug:** `ViewCompat.animate(v)` → `v.animate()` (7 Stellen), `ViewCompat.isAttachedToWindow` → `isAttachedToWindow`, `DisplayMetrics.scaledDensity` → `TypedValue.applyDimension(COMPLEX_UNIT_SP, ...)`, `InputMethodManager.SHOW_IMPLICIT` → `0`, redundanter Cast in `KotterKnife` entfernt, `if (resource is Drawable ...)` (immer true) entfernt, `.map { Unit }` → `.map { }`, `TagDatabase.migrate(database:)` → `migrate(db:)` (Param-Name an Supertype), `window.statusBarColor` in `DrawerActivity` entfernt (edge-to-edge zeichnet eh hinter die Leiste), aboutlibraries `LibsBuilder` lokal suppressed (view-basierte UI in 12.2.4 bewusst gepinnt).
- **`allWarningsAsErrors.set(true)`** in `build.gradle` aktiviert — `compileDebugKotlin`/`compileLogReleaseKotlin` und alle Builds grün mit 0 Warnungen.
- **Emulator-Verifikation (API 37):** frische Debug-APK installiert, App startet (News/ANMELDEN), 0 `FATAL EXCEPTION`.
- Verifiziert grün: `ktlintFormat`, `ktlintCheck`, `detekt`, `lintDebug` + `lintRelease` (0 Issues, `abortOnError`), `assembleDebug`, `assembleRelease`, `bundleRelease`, `assembleLogRelease`, `:baselineprofile:assembleAndroidTest` — jeweils mit `allWarningsAsErrors`.

### 11.4 Strukturelles / Architektur — **11.4a–11.4e erledigt 18.11.2026**, Rest offen

- **Kotterknife-Shim → ViewBinding (erledigt 18.11.2026, 11.4e):** kotterknife-Shim komplett gelöscht, 359 `by bindView`-Delegates in 61 Dateien auf ViewBinding umgestellt (`viewBinding true` in build.gradle): ViewHolder → `XBinding.bind(itemView)`, Activities → `Binding.inflate(layoutInflater)` + `setContentView(binding.root)`, Fragments → `XBinding.bind(requireView())`, Custom Views → `Binding.inflate(...)` im init. Ausnahmen (typisierte `findViewById`-Getter, da Layout nicht statisch oder Base-Klasse ohne Layout-Kenntnis): BaseActivity/DrawerActivity/MainActivity/ImageTabsActivity (`findViewById`), BaseFragment/BaseContentFragment/PagedContentFragment (`requireView().findViewById`), DialogFragments (`requireDialog().findViewById` — ViewBinding auf decorView griff in onGetLayoutInflater zu früh), sowie Ids in `<include>`-Layouts ohne Include-Id (z. B. `layout_upload_info`, ExoPlayer-Controller, Bottom-Sheets). Emulator-Test (API 37): News/Drawer/Settings/LoginDialog ohne Crash, 0 FATAL EXCEPTION.
- **God-Objects aufteilen:** `StreamActivity` 698, `ProxerLibExtensions` 615, `MangaFragment` 611, `ErrorUtils` 590, `MediaInfoFragment` 523, `MediaListFragment` 443, `StreamPlayerManager` 422 Zeilen
- **Verbleibende jitpack-Pins ablösen** (siehe Phase 10): `subsampling-scale-image-view`, `subsampling-pdf-decoder`, `material-about-library` rc (→ aboutlibraries/PreferenceScreen)
  - **Erledigt 18.11.2026:** `gravitysnaphelper` → eingebaute `LinearSnapHelper`/`PagerSnapHelper` (11.4a); `emoji-ios` jitpack-Commit → Maven Central `com.vanniktech:emoji(-ios):0.13.0` (11.4b, API-identisch, ungenutzte Theme-Attrs entfernt); `previewseekbar` Core-Inlining (11.4c): 6 Java-Klassen von Upstream-Commit `09eec85551` nach `src/main/java/com/github/rubensousa/previewseekbar/` übernommen (minSdk 24: `@TargetApi`-Guards entfernt, `PreviewAnimatorImpl` gelöscht, Lollipop-Animator immer genutzt), Ressourcen (`previewseekbar_morph.xml`, `previewseekbar_indicator_width`, `PreviewSeekBar`-Styleable) in App-Res verschoben, `PreviewTimeBar.kt` nutzt `me.proxer.app.R`. jitpack-`previewseekbar`-Dependency komplett entfernt.
- **materialdrawer 8.3.1** → langfristig `NavigationView`/Compose
- **Glide 4.16 → 5.x** (KSP → `com.android.legacy-kapt` fällt komplett weg)
- **RxJava → Coroutines/Flow** schrittweise pro Feature (Start z. B. `BaseViewModel.load()`), gemäß Festlegung
- **CI (erledigt 18.11.2026, 11.4d):** Build-Step um `lintDebug lintRelease` (strict, abortOnError) erweitert; neuer Job `baselineprofile` mit `reactivecircus/android-emulator-runner` (API 36) führt `:baselineprofile:connectedReleaseAndroidTest` aus und lädt die Profile als Artifact

---

## Quick Wins (vor oder während Phase 1, geringer Aufwand, sofortiger Nutzen)

1. PendingIntent-Flags nachtragen (8 Stellen, siehe Phase 3).
2. `POST_NOTIFICATIONS` + Runtime-Request, `NEARBY_WIFI_DEVICES`.
3. Moshi-Substitution auf jitpack-Commit entfernen, jsoup aktualisieren.
4. Gradle/AGP/Kotlin/JDK auf aktuelle Stables, CI auf JDK 17.
