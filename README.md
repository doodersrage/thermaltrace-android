# ThermalTrace for Android

Native Android client for [thermaltrace.dev](https://thermaltrace.dev). The phone **does not** collect sensor data — it signs in with your ThermalTrace account and loads live readings from the existing HTTPS API.

## Stack

- Kotlin, Jetpack Compose, Material 3
- Supabase Auth (same project as the web app)
- OkHttp cookie jar → `sb-access-token` / `sb-refresh-token` for dashboard APIs
- Retrofit + kotlinx.serialization → `GET /api/home/readings`

## Setup

1. Open this folder in **Android Studio** (Ladybug / 2024.2+ recommended).
2. Copy `local.properties.example` → `local.properties` and set:

```properties
sdk.dir=/path/to/Android/Sdk
supabase.url=https://YOUR_PROJECT.supabase.co
supabase.anonKey=YOUR_PUBLISHABLE_OR_ANON_KEY
thermaltrace.baseUrl=https://thermaltrace.dev
```

Use the **anon / publishable** key only — never the service role key.

3. **Gradle JDK:** Android Studio’s bundled JBR may be Java 25, which is too new for Gradle 8.11. In **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**, pick **17** (or `/usr/lib/jvm/java-17-openjdk` on Linux). Then Sync.
4. Run on an emulator or device.

From the CLI (with Android SDK installed and JDK 17):

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk   # Linux; use a JDK 17–23 install
./gradlew :app:assembleDebug
```

## How auth works

1. Email/password via Supabase (`signInWithPassword`).
2. Access + refresh tokens are stored locally and attached as cookies on `thermaltrace.dev` requests (same contract as the web dashboard).
3. Home: `GET /api/home/readings?save=0`
4. Settings load: `GET /api/user/export` (preferences + alert settings)
5. Settings save: form `POST` to `/api/user/preferences`, `/api/user/alert-settings`, `/api/user/alert-snooze`

## Project layout

```
app/src/main/java/dev/thermaltrace/android/
  data/auth/     Session store + Supabase sign-in
  data/api/      Cookie jar, Retrofit, readings + settings
  data/model/    JSON models
  ui/login/      Sign-in
  ui/home/       Live readings
  ui/devices/    Device list, rename, space
  ui/alerts/     Alert thresholds, channels, snooze
  ui/household/  Members, rename, invite, switch
  ui/account/    Display prefs (°C, theme), push, sign out
```

## Next ideas

- SSE live stream (`/api/home/readings/stream`)
- History charts (needs a JSON history API on the server)
- Alert event list / acknowledge

## Push notifications (FCM)

Pro accounts can receive the same **Push** channel on browser (VAPID) and this app (FCM).

1. Create a Firebase project and add Android apps for `dev.thermaltrace.android` and `dev.thermaltrace.android.debug`.
2. Download `google-services.json` into `app/` (gitignored). You can start from `app/google-services.json.example`.
3. Optionally also set `firebase.*` keys in `local.properties` if you are not using the Google Services plugin.
4. On the server (`garage-temp` / thermaltrace.dev), set `FCM_SERVICE_ACCOUNT_JSON` (or `FCM_*` split fields), run the `fcm_device_tokens` migration, and `pnpm secrets:push`.
5. Sign in on the app → **Account → Enable push**, and on the website enable **Push (browser + Android)** under Alerts.

## License

MIT (aligned with ThermalTrace).
