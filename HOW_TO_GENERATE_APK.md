# Pixel 6a Call Recorder - APK Generation Guide

Follow these 3 easy steps to generate your APK file:

### Method A: In Android Studio (Takes 2 minutes)
1. Extract this ZIP file into a folder.
2. Open **Android Studio** and click **Open** -> Select the extracted folder.
3. Wait for Gradle sync to complete.
4. Click on the top menu: **Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**.
5. In bottom right, click **locate** to find your compiled `app-debug.apk`!
6. Transfer this `app-debug.apk` to your Pixel 6a via WhatsApp/Cable and tap to install!

### Method B: Automatic Cloud Build via GitHub (No PC software needed)
1. Upload this project to a free GitHub repository.
2. Click on the **Actions** tab on GitHub.
3. The included workflow (`.github/workflows/build-apk.yml`) will automatically compile the APK in 60 seconds.
4. Download the ready `Pixel6a-CallRecorder-Debug-APK.zip` from the GitHub Actions summary page!
