# AssistifyRelayApp

AssistifyRelayApp is an Android application that relays incoming SMS messages to a backend server (AssistifyRelayServer). It supports reliable background delivery using a combination of BroadcastReceiver, ForegroundService, WorkManager retry, local persistence and optional Device Admin / battery‑ignore flows to improve background reliability on aggressive OEM ROMs.

This README describes project structure, how to run the server and app, required permissions, important implementation details, debugging tips and common troubleshooting steps.

---

## Features
- Capture incoming SMS and forward to backend immediately (via ForegroundService).
- Enqueue retries with WorkManager if immediate delivery fails.
- Local backup of transactions (LocalTransactionStorage).
- Device registration via FCM (RelayFirebaseMessagingService).
- Optional Device Admin / battery optimization ignore flows for reliability.
- Persistence of setup state (Persistence helper).

---

## Repo structure (app/src/main)
- java/com/example/assistifyrelayapp
  - App.java
  - ForegroundService.java
  - admin/MyDeviceAdminReceiver.java
  - auth/DeviceRegistrationManager.java
  - core/
    - NetClient.java, Persistence.java, NetworkBufferedSender.java, SendAndReceivePreferences.java
    - jsonclass (request/response POJOs)
  - fcm/RelayFirebaseMessagingService.java
  - LocalStorage/LocalTransactionStorage.java
  - session/SessionController.java, HeartbeatWorker.java
  - sms/SmsReceiver.java, IncomingSmsWorker.java, SmsSender.java
  - ui/SetupActivity.java, DashboardActivity.java, MainActivity.java
- res/ layout, values, xml (device admin config)

---

## Requirements
- Android Studio (Arctic Fox / Chipmunk or newer)
- Android SDK with API level >= 26 recommended
- Device (or emulator with SMS capability) for full SMS tests
- Node.js + MongoDB for the server (AssistifyRelayServer) — see server README

---

## Server (AssistifyRelayServer)
Clone and run the server repository (replace owner/path as needed):

```bash
git clone https://github.com/<owner>/AssistifyRelayServer.git
cd AssistifyRelayServer
npm install
# create .env with at least MONGO_URI and PORT
npm start
```

Test server locally from your PC:

```bash
curl -X POST http://localhost:3000/api/v1/incoming-sms \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"SOME_SESSION_ID","from":"+1234567890","body":"test","receivedAt":"1698718200000"}'
```

Make note of the server host/port (use your LAN IP if testing on a physical Android device).

---

## App configuration
1. Open Android Studio and load this project.
2. Add your `google-services.json` to `app/` (for FCM).
3. Set backend base URL:
   - Edit `RetrofitClient.java` or `NetClient.java` to point to your server:
     ```java
     private static final String BASE_URL = "http://192.168.1.10:3000/"; // your PC LAN IP
     ```
4. If server uses HTTP (not HTTPS), allow cleartext for debug:
   - In `AndroidManifest.xml` add in `<application>`:
     ```xml
     android:usesCleartextTraffic="true"
     ```
   - Or create a network security config.

5. Ensure Manifest contains:
   - SMS receiver (static) with intent filter for `android.provider.Telephony.SMS_RECEIVED`.
   - ForegroundService declaration.
   - Permissions:
     - RECEIVE_SMS, SEND_SMS, READ_PHONE_STATE
     - FOREGROUND_SERVICE
     - POST_NOTIFICATIONS (Android 13+)

6. Build & install on a test device (physical device recommended).

---

## Runtime permissions & device setup (user-facing)
- On first run the SetupActivity requests runtime permissions (RECEIVE_SMS, SEND_SMS, READ_PHONE_STATE, POST_NOTIFICATIONS).
- You may optionally:
  - Enable Device Admin (SetupActivity UI) — helps prevent some OEM kills.
  - Ignore battery optimization for the app.
- Persistence stores these setup flags so UI shows current state (Persistence).

---

## How SMS delivery works (important)
- `SmsReceiver` is a statically-registered BroadcastReceiver. When an SMS arrives:
  - It processes message in a background thread using `goAsync()` and an ExecutorService.
  - It creates TransactionData and:
    - Saves to local storage (LocalTransactionStorage)
    - Attempts immediate network send via `ForegroundService` (a short-lived foreground service started with ContextCompat.startForegroundService)
    - Enqueues an `IncomingSmsWorker` (WorkManager) as a retry/fallback if immediate send fails or device is idle.
- This combination maximizes chance of immediate delivery while keeping a reliable retry path.

---

## Troubleshooting / common issues

1. App does not send incoming SMS when phone screen is off or app is closed:
   - Confirm `SmsReceiver` is statically registered in Manifest (not only dynamically).
   - Ensure runtime RECEIVE_SMS permission granted.
   - On Android 8+ background restrictions apply — ForegroundService started from the receiver helps deliver in Doze.
   - Some OEMs aggressively kill background services — ask users to exempt app in battery optimization and autostart settings.
   - Device Admin reduces some OEM restrictions but does not guarantee exemption from battery optimizations.

2. WorkManager delays executing while device is in Doze:
   - WorkManager is for retries — to attempt immediate send use foreground service as implemented.

3. "App can't connect to server":
   - If testing on device, use PC LAN IP and allow cleartext or use HTTPS.
   - Make sure device and PC are on same network.
   - Check logs (Logcat) and NetClient/Retrofit logs if enabled.

4. XML build errors (e.g. unescaped '&'):
   - Replace `&` with `&amp;` in resource XML strings.

---

## Debugging tips
- Use `adb logcat` or Android Studio Logcat. Look for tags:
  - SmsReceiver (TAG = "SmsReceiver")
  - ForegroundService (TAG = "ForegroundService")
  - RelayFCMService (TAG = "RelayFCMService")
- Temporarily enable logging in `NetClient` / OkHttp interceptor to trace HTTP requests/responses.
- Use the fallback endpoint with curl to confirm server accepts incoming-sms payloads.

---

## Important files to review
- Sms flow: `sms/SmsReceiver.java`, `sms/IncomingSmsWorker.java`, `ForegroundService.java`
- Network client: `core/NetClient.java`, `core/ApiService.java`
- Persistence: `core/Persistence.java`
- Setup UI: `ui/SetupActivity.java`
- FCM: `fcm/RelayFirebaseMessagingService.java`

---

## Tests
- Manually send SMS to the device and confirm:
  - Immediate server request attempts (ForegroundService logs)
  - If network fails, WorkManager enqueues and retries when network returns
  - LocalTransactionStorage contains saved transactions
- Simulate server down and restart to verify queued sends are retried.

---

## Contributing
- Fork, branch, and open a PR with a description of the change.
- Keep behavior backwards-compatible and add tests where possible.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
