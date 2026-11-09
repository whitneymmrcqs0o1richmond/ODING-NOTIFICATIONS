# ODING NOTIFICATIONS

Android GCash/Maya notification voice announcer source project. **This ZIP is not an APK.**

## Build without Android Studio using GitHub Actions
1. Create a GitHub repository and upload all project files, including the hidden `.github/workflows/build-apk.yml` file.
2. Go to **Actions > Build ODING NOTIFICATIONS APK > Run workflow**.
3. Open the completed run and download the **ODING_NOTIFICATIONS_APK** artifact ZIP.
4. Extract **ODING_NOTIFICATIONS.apk** and install it on your Android phone.
5. Grant notification access to ODING NOTIFICATIONS.

The app reads only accessible notification text. It cannot confirm payment independently; never rely on voice alone as proof of payment. Parsing is not yet validated against real GCash/Maya notification samples. No SMS, bank credentials, or internet access required.
