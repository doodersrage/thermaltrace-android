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

3. Sync Gradle, then Run on an emulator or device.

From the CLI (with Android SDK installed):

```bash
./gradlew :app:assembleDebug
```

## How auth works

1. Email/password via Supabase (`signInWithPassword`).
2. Access + refresh tokens are stored locally and attached as cookies on `thermaltrace.dev` requests (same contract as the web dashboard).
3. Home screen calls `GET https://thermaltrace.dev/api/home/readings?save=0`.

## Project layout

```
app/src/main/java/dev/thermaltrace/android/
  data/auth/     Session store + Supabase sign-in
  data/api/      Cookie jar, Retrofit, readings repo
  data/model/    JSON models for home readings
  ui/login/      Sign-in screen
  ui/home/       Live sensor list
```

## Next ideas

- SSE live stream (`/api/home/readings/stream`)
- History charts (needs a JSON history API on the server, or reuse export endpoints)
- Alert list / acknowledge
- Deep link to share tokens (`/api/share/{token}/readings`)

## License

MIT (aligned with ThermalTrace).
