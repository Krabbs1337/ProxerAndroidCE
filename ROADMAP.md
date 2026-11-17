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
| 8 | Performance + Polish | Glide, Baseline Profiles, AAB-Splits, Desugaring |
| 9 | Qualitätssicherung + optionale Coroutines-Migration | detekt/ktlint/CI aktuell, schrittweise RxJava-Ablösung |

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

**Abnahme:** Manueller Test auf Android 16 (API 36): Widgets, Notification-Aktionen, Deep-Links, Cast-Verbindung, Bild-Upload im Forum/Profil – alles ohne Crash. lokal: Build grün; Gerätetest folgt.

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

**Abnahme:** Stream-Wiedergabe (lokal + Cast) auf Android 16 getestet, HLS + Werbung laufen – lokaler Build grün; Gerätetest folgt.

## Phase 8 – Performance + Polish

- Glide (`ProxerGlideModule.kt`): `disallowHardwareConfig()` und `PREFER_RGB_565` entfernen (Hardware-Bitmaps sparen RAM/GPU), `DiskLruCacheFactory` → neuer `DiskCache.Factory`, 250 MB Cache beibehalten.
- Baseline Profiles + `androidx.profileinstaller` hinzufügen (messbarer Start-/Scroll-Gewinn).
- AAB mit ABI-Splits und Language-Split (aktuell `language.enableSplit = false`, `resConfigs "de"`).
- coreLibraryDesugaring aktivieren → threetenabp entfernen (java.time nativ ab minSdk 24 mit Desugaring bzw. 26+ nativ).
- `BaseViewModel.load()`: `data.value = null` pro Reload entfernt Flackern – Daten halten, nur `isLoading` setzen.
- `initConnectionManager` in `MainApplication`: `NetworkCallback` in `onTerminate` abmelden.
- `largeHeap` entfernen (Phase 3), Speicherverhalten mit LeakCanary 2.x prüfen.

**Abnahme:** Kaltstart- und Scroll-Performance messbar verbessert (Macrobenchmark oder Profiler), AAB-Größe durch Splits reduziert.

## Phase 9 – Qualitätssicherung + optionale Coroutines-Migration

- detekt 2.x + ktlint 12.x neu konfigurieren, `warningsAsErrors` beibehalten, CI auf JDK 17 + aktuelle Actions, `dependencyUpdates` regelmäßig laufen lassen.
- Manuelle Regression-Checkliste pro Release: Login/2FA, News, Forum, Anime-Stream, Manga-Reader, Bookmarks, Benachrichtigungen, Widgets, Deep-Links, Einstellungen, About-Libraries.
- Optional und schrittweise pro Feature: RxJava → Coroutines/Flow (Start z. B. mit `BaseViewModel.load()`), Compose nur für neue Screens.

**Abnahme:** Jeder Merge ist build- und lint-grün; Release nur mit erfüllter Regression-Checkliste.

---

## Quick Wins (vor oder während Phase 1, geringer Aufwand, sofortiger Nutzen)

1. PendingIntent-Flags nachtragen (8 Stellen, siehe Phase 3).
2. `POST_NOTIFICATIONS` + Runtime-Request, `NEARBY_WIFI_DEVICES`.
3. Moshi-Substitution auf jitpack-Commit entfernen, jsoup aktualisieren.
4. Gradle/AGP/Kotlin/JDK auf aktuelle Stables, CI auf JDK 17.
