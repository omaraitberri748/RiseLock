Here is a complete native Android app ("RiseLock") built specifically for your
Google Pixel 10 (Android 15/16+, 16 KB page-size compliant) using Kotlin,
Jetpack Compose, CameraX, and Google MediaPipe Pose AI.

Unlike a web app, this is a real Android .apk that:

  - Wakes your Pixel 10 from sleep & takes over the lock screen using
    AlarmManager.setAlarmClock() + a Foreground Service + Full-Screen Intent
  - Rings through the real Android Alarm Volume channel (bypasses silent/Do Not
    Disturb & media volume) with continuous vibration
  - Blocks the Back button while ringing so you cannot swipe it away
  - Runs 100% offline AI Pose Detection for Push-Ups & Squats, plus Math and
    Phone Shake missions (stackable!)
  - Survives phone reboots and tracks your daily wake-up streak

Best of all, you don't need Android Studio—the included GitHub Actions file
automatically downloads the AI model and compiles a ready-to-install .apk in the
cloud for free.

How to Build & Install on Your Pixel 10 (No Coding Tools Needed)

1.  Go to github.com (free account) and click New repository → name it RiseLock
    → click Create repository.
2.  Click creating a new file (or Add file → Create new file) and add the 11
    files below using the exact file paths shown (GitHub automatically creates
    folders when you type / in a filename).
3.  Go to the Actions tab in your repo → click the running Build RiseLock APK
    workflow (wait ~2–3 minutes for the green checkmark).
4.  Open that page on your Pixel 10, scroll to Artifacts, tap
    RiseLock-Pixel10-APK to download the zip, extract app-debug.apk, and tap it
    to install (allow "Install unknown apps" for Files/Chrome if prompted).
5.  Open RiseLock, tap the Permission buttons at the top, and tap ⚡ Test Alarm
    (5 sec)—then lock your screen!

(Tip: If you're on a Windows/Mac computer and don't want to paste 11 files
manually, reply with your OS and I'll give you a 1-click script that generates
the whole folder instantly.)

1. .github/workflows/build.yml

name: Build RiseLock APK

on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Download MediaPipe Pose AI Model
        run: |
          mkdir -p app/src/main/assets
          curl -L -f -o app/src/main/assets/pose_landmarker_lite.task \
            https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.11.1'

      - name: Build Debug APK
        run: gradle assembleDebug

      - name: Upload RiseLock APK
        uses: actions/upload-artifact@v4
        with:
          name: RiseLock-Pixel10-APK
          path: app/build/outputs/apk/debug/app-debug.apk

2. gradle.properties

org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official

3. settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "RiseLock"
include(":app")

4. build.gradle.kts

plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

5. app/build.gradle.kts

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.riselock.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.riselock.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    val cameraxVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // 16 KB page-size compliant for Pixel 10 (Android 15/16+)
    implementation("com.google.mediapipe:tasks-vision:0.10.29")
}

6. app/src/main/AndroidManifest.xml

<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-feature android:name="android.hardware.camera" android:required="false" />

    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

    <application
        android:allowBackup="true"
        android:icon="@android:drawable/ic_lock_idle_alarm"
        android:label="RiseLock"
        android:theme="@android:style/Theme.Material.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".AlarmActivity"
            android:exported="false"
            android:launchMode="singleTop"
            android:showWhenLocked="true"
            android:turnScreenOn="true"
            android:excludeFromRecents="true"
            android:screenOrientation="portrait" />

        <service
            android:name=".AlarmService"
            android:exported="false"
            android:foregroundServiceType="mediaPlayback" />

        <receiver
            android:name=".AlarmReceiver"
            android:exported="false" />

        <receiver
            android:name=".BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>

7. app/src/main/java/com/riselock/app/AlarmPrefs.kt

package com.riselock.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.util.Calendar

data class AlarmConfig(
    val enabled: Boolean = false,
    val hour: Int = 6,
    val minute: Int = 30,
    val pushupsEnabled: Boolean = true,
    val pushupReps: Int = 10,
    val squatsEnabled: Boolean = false,
    val squatReps: Int = 10,
    val mathEnabled: Boolean = true,
    val mathCount: Int = 3,
    val shakeEnabled: Boolean = false,
    val shakeCount: Int = 25,
    val streak: Int = 0
)

enum class MissionType { PUSHUPS, SQUATS, MATH, SHAKE }
data class MissionSpec(val type: MissionType, val target: Int)

object AlarmPrefs {
    private const val PREFS = "riselock_prefs"
    private const val REQ_ALARM = 7001
    private const val REQ_TEST = 7002

    fun load(context: Context): AlarmConfig {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AlarmConfig(
            enabled = p.getBoolean("enabled", false),
            hour = p.getInt("hour", 6),
            minute = p.getInt("minute", 30),
            pushupsEnabled = p.getBoolean("pushupsEnabled", true),
            pushupReps = p.getInt("pushupReps", 10),
            squatsEnabled = p.getBoolean("squatsEnabled", false),
            squatReps = p.getInt("squatReps", 10),
            mathEnabled = p.getBoolean("mathEnabled", true),
            mathCount = p.getInt("mathCount", 3),
            shakeEnabled = p.getBoolean("shakeEnabled", false),
            shakeCount = p.getInt("shakeCount", 25),
            streak = p.getInt("streak", 0)
        )
    }

    fun save(context: Context, cfg: AlarmConfig) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", cfg.enabled)
            .putInt("hour", cfg.hour)
            .putInt("minute", cfg.minute)
            .putBoolean("pushupsEnabled", cfg.pushupsEnabled)
            .putInt("pushupReps", cfg.pushupReps)
            .putBoolean("squatsEnabled", cfg.squatsEnabled)
            .putInt("squatReps", cfg.squatReps)
            .putBoolean("mathEnabled", cfg.mathEnabled)
            .putInt("mathCount", cfg.mathCount)
            .putBoolean("shakeEnabled", cfg.shakeEnabled)
            .putInt("shakeCount", cfg.shakeCount)
            .apply()
    }

    fun buildMissionQueue(context: Context): List<MissionSpec> {
        val cfg = load(context)
        val list = mutableListOf<MissionSpec>()
        if (cfg.pushupsEnabled) list.add(MissionSpec(MissionType.PUSHUPS, cfg.pushupReps))
        if (cfg.squatsEnabled) list.add(MissionSpec(MissionType.SQUATS, cfg.squatReps))
        if (cfg.mathEnabled) list.add(MissionSpec(MissionType.MATH, cfg.mathCount))
        if (cfg.shakeEnabled) list.add(MissionSpec(MissionType.SHAKE, cfg.shakeCount))
        if (list.isEmpty()) list.add(MissionSpec(MissionType.MATH, 3))
        return list
    }

    fun recordVictory(context: Context): Int {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val last = p.getString("lastDate", "") ?: ""
        var streak = p.getInt("streak", 0)
        if (last != today) {
            val yesterday = LocalDate.now().minusDays(1).toString()
            streak = if (last == yesterday) streak + 1 else 1
            p.edit().putInt("streak", streak).putString("lastDate", today).apply()
        }
        return streak
    }

    fun scheduleDailyAlarm(context: Context, cfg: AlarmConfig) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, cfg.hour)
            set(Calendar.MINUTE, cfg.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val showIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fireIntent = PendingIntent.getBroadcast(
            context, REQ_ALARM,
            Intent(context, AlarmReceiver::class.java).putExtra("is_test", false),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, showIntent), fireIntent)
    }

    fun cancelDailyAlarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fireIntent = PendingIntent.getBroadcast(
            context, REQ_ALARM, Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(fireIntent)
    }

    fun scheduleTestAlarm(context: Context, delaySeconds: Int = 5) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + delaySeconds * 1000L
        val showIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fireIntent = PendingIntent.getBroadcast(
            context, REQ_TEST,
            Intent(context, AlarmReceiver::class.java).putExtra("is_test", true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), fireIntent)
    }
}

8. app/src/main/java/com/riselock/app/AlarmReceivers.kt

package com.riselock.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isTest = intent.getBooleanExtra("is_test", false)
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("is_test", isTest)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val cfg = AlarmPrefs.load(context)
            if (cfg.enabled) {
                AlarmPrefs.scheduleDailyAlarm(context, cfg)
            }
        }
    }
}

9. app/src/main/java/com/riselock/app/AlarmService.kt

package com.riselock.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    companion object {
        const val CHANNEL_ID = "riselock_alarm_channel"
        const val NOTIF_ID = 1001
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isTest = intent?.getBooleanExtra("is_test", false) ?: false
        createChannel()

        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra("is_test", isTest)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("RISELOCK ALARM")
            .setContentText("Complete your missions to silence the alarm!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )

        startAlarmAudio()
        startVibration()

        // Launch immediately if phone screen is currently unlocked
        try {
            startActivity(fullScreenIntent)
        } catch (_: Exception) {
        }

        return START_STICKY
    }

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "RiseLock Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Wake-up alarms that take over the lock screen"
            setSound(null, null) // MediaPlayer handles continuous alarm audio
            enableVibration(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    private fun startAlarmAudio() {
        if (mediaPlayer?.isPlaying == true) return
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentVol = am.getStreamVolume(AudioManager.STREAM_ALARM)
            if (currentVol == 0) {
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, (maxVol * 0.7).toInt().coerceAtLeast(1), 0)
            }

            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 600, 300, 600, 300)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    override fun onDestroy() {
        mediaPlayer?.run {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

10. app/src/main/java/com/riselock/app/MainActivity.kt

package com.riselock.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFFF4D1A),
                    background = Color(0xFF0B0B0F),
                    surface = Color(0xFF16161F)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0B0F)) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    var cfg by remember { mutableStateOf(AlarmPrefs.load(ctx)) }
    var permTick by remember { mutableIntStateOf(0) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permTick++ }

    // Permission checks for Pixel 10 (Android 15/16)
    val hasCam = remember(permTick) {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val hasNotif = remember(permTick) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val hasFullScreen = remember(permTick) {
        if (Build.VERSION.SDK_INT >= 34) {
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).canUseFullScreenIntent()
        } else true
    }
    val hasExactAlarm = remember(permTick) {
        if (Build.VERSION.SDK_INT >= 31) {
            (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        } else true
    }

    fun update(newCfg: AlarmConfig) {
        cfg = newCfg
        AlarmPrefs.save(ctx, newCfg)
        if (newCfg.enabled) {
            AlarmPrefs.scheduleDailyAlarm(ctx, newCfg)
        } else {
            AlarmPrefs.cancelDailyAlarm(ctx)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RISELOCK",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF4D1A)
            )
            Box(
                modifier = Modifier
                    .background(Color(0xFF16161F), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("🔥 ${cfg.streak} Day Streak", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Pixel 10 Permissions Setup Card
        if (!hasCam || !hasNotif || !hasFullScreen || !hasExactAlarm) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF261414)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pixel 10 Setup Required", fontWeight = FontWeight.Bold, color = Color(0xFFFF8A65))
                    if (!hasCam || !hasNotif) {
                        Button(
                            onClick = {
                                val perms = mutableListOf(Manifest.permission.CAMERA)
                                if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                permLauncher.launch(perms.toTypedArray())
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Grant Camera & Notifications") }
                    }
                    if (!hasFullScreen && Build.VERSION.SDK_INT >= 34) {
                        Button(
                            onClick = {
                                ctx.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                        Uri.parse("package:${ctx.packageName}")
                                    )
                                )
                                permTick++
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Enable Lock-Screen Wakeup") }
                    }
                    if (!hasExactAlarm && Build.VERSION.SDK_INT >= 31) {
                        Button(
                            onClick = {
                                ctx.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${ctx.packageName}")
                                    )
                                )
                                permTick++
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Allow Exact Alarms") }
                    }
                    TextButton(onClick = { permTick++ }, modifier = Modifier.align(Alignment.End)) {
                        Text("Refresh Status", color = Color.LightGray)
                    }
                }
            }
        }

        // Time Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161F)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        TimePickerDialog(
                            ctx,
                            { _, h, m -> update(cfg.copy(hour = h, minute = m)) },
                            cfg.hour,
                            cfg.minute,
                            false
                        ).show()
                    }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("DAILY WAKE-UP", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format("%02d:%02d", cfg.hour, cfg.minute),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("Tap time to change", fontSize = 12.sp, color = Color.Gray)
                }
                Switch(
                    checked = cfg.enabled,
                    onCheckedChange = { on ->
                        update(cfg.copy(enabled = on))
                        Toast.makeText(
                            ctx,
                            if (on) "Alarm armed for ${String.format("%02d:%02d", cfg.hour, cfg.minute)}" else "Alarm off",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }

        // Missions Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161F)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("WAKE-UP MISSIONS (NO SNOOZE)", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

                MissionRow(
                    title = "💪 Push-Ups (AI Camera)",
                    checked = cfg.pushupsEnabled,
                    count = cfg.pushupReps,
                    unit = "reps",
                    step = 5,
                    min = 5,
                    onToggle = { update(cfg.copy(pushupsEnabled = it)) },
                    onCount = { update(cfg.copy(pushupReps = it)) }
                )

                MissionRow(
                    title = "🦵 Squats (AI Camera)",
                    checked = cfg.squatsEnabled,
                    count = cfg.squatReps,
                    unit = "reps",
                    step = 5,
                    min = 5,
                    onToggle = { update(cfg.copy(squatsEnabled = it)) },
                    onCount = { update(cfg.copy(squatReps = it)) }
                )

                MissionRow(
                    title = "🧠 Math Problems",
                    checked = cfg.mathEnabled,
                    count = cfg.mathCount,
                    unit = "probs",
                    step = 1,
                    min = 1,
                    onToggle = { update(cfg.copy(mathEnabled = it)) },
                    onCount = { update(cfg.copy(mathCount = it)) }
                )

                MissionRow(
                    title = "📳 Power Shake",
                    checked = cfg.shakeEnabled,
                    count = cfg.shakeCount,
                    unit = "shakes",
                    step = 10,
                    min = 10,
                    onToggle = { update(cfg.copy(shakeEnabled = it)) },
                    onCount = { update(cfg.copy(shakeCount = it)) }
                )
            }
        }

        // Test Alarm Button
        Button(
            onClick = {
                AlarmPrefs.scheduleTestAlarm(ctx, 5)
                Toast.makeText(ctx, "Lock your Pixel now! Alarm rings in 5 seconds...", Toast.LENGTH_LONG).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("⚡ TEST ALARM IN 5 SECONDS", fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}

@Composable
private fun MissionRow(
    title: String,
    checked: Boolean,
    count: Int,
    unit: String,
    step: Int,
    min: Int,
    onToggle: (Boolean) -> Unit,
    onCount: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Checkbox(checked = checked, onCheckedChange = onToggle)
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { if (count - step >= min) onCount(count - step) },
                contentPadding = PaddingValues(4.dp)
            ) { Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            Text("$count $unit", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            TextButton(
                onClick = { onCount(count + step) },
                contentPadding = PaddingValues(4.dp)
            ) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

11. app/src/main/java/com/riselock/app/AlarmActivity.kt

package com.riselock.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.random.Random

class AlarmActivity : ComponentActivity() {
    private var completed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Block back button until all missions are complete
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (completed) finish()
            }
        })

        val isTest = intent.getBooleanExtra("is_test", false)
        val missions = AlarmPrefs.buildMissionQueue(this)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFFF4D1A),
                    background = Color(0xFF140707),
                    surface = Color(0xFF1A1A24)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF140707)) {
                    AlarmMissionRunner(
                        missions = missions,
                        onAllComplete = {
                            completed = true
                            stopService(Intent(this, AlarmService::class.java))
                            val streak = AlarmPrefs.recordVictory(this)
                            if (!isTest) {
                                val cfg = AlarmPrefs.load(this)
                                if (cfg.enabled) AlarmPrefs.scheduleDailyAlarm(this, cfg)
                            }
                            streak
                        },
                        onExit = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmMissionRunner(
    missions: List<MissionSpec>,
    onAllComplete: () -> Int,
    onExit: () -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    var victoryStreak by remember { mutableStateOf<Int?>(null) }

    if (victoryStreak != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🏆", fontSize = 72.sp)
            Spacer(Modifier.height(12.dp))
            Text("YOU'RE UP!", fontSize = 34.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("🔥 $victoryStreak Day Streak", fontSize = 20.sp, color = Color(0xFFFF4D1A), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("DONE", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
        return
    }

    val current = missions[index]
    val advance = {
        if (index + 1 >= missions.size) {
            victoryStreak = onAllComplete()
        } else {
            index++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFF4D1A), RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🔊 WAKE UP!", fontWeight = FontWeight.Black, color = Color.White)
                Text("Mission ${index + 1} / ${missions.size}", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        key(index) {
            when (current.type) {
                MissionType.PUSHUPS -> CameraPoseMission(isPushup = true, target = current.target, onDone = advance)
                MissionType.SQUATS -> CameraPoseMission(isPushup = false, target = current.target, onDone = advance)
                MissionType.MATH -> MathMission(target = current.target, onDone = advance)
                MissionType.SHAKE -> ShakeMission(target = current.target, onDone = advance)
            }
        }
    }
}

@Composable
private fun CameraPoseMission(isPushup: Boolean, target: Int, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val hasCam = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    if (!hasCam) {
        Text("Camera permission missing — solve 3 math problems instead!", color = Color.Yellow)
        MathMission(target = 3, onDone = onDone)
        return
    }

    var reps by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("Position yourself in frame...") }
    var stage by remember { mutableStateOf("up") }
    var finished by remember { mutableStateOf(false) }

    val executor = remember { Executors.newSingleThreadExecutor() }
    val closed = remember { AtomicBoolean(false) }
    var lastFrameW by remember { mutableIntStateOf(480) }
    var lastFrameH by remember { mutableIntStateOf(640) }
    var lastTs by remember { mutableLongStateOf(0L) }

    val poseLandmarker = remember {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker_lite.task")
                .build()
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setNumPoses(1)
                .setResultListener { result: PoseLandmarkerResult, _ ->
                    if (finished) return@setResultListener
                    val lm = result.landmarks().firstOrNull()
                    if (lm == null || lm.size < 29) {
                        status = "Step into camera view"
                        return@setResultListener
                    }

                    val (a, b, c) = if (isPushup) {
                        val lv = lm[11].visibility().orElse(0f) + lm[13].visibility().orElse(0f) + lm[15].visibility().orElse(0f)
                        val rv = lm[12].visibility().orElse(0f) + lm[14].visibility().orElse(0f) + lm[16].visibility().orElse(0f)
                        if (lv >= rv) Triple(lm[11], lm[13], lm[15]) else Triple(lm[12], lm[14], lm[16])
                    } else {
                        val lv = lm[23].visibility().orElse(0f) + lm[25].visibility().orElse(0f) + lm[27].visibility().orElse(0f)
                        val rv = lm[24].visibility().orElse(0f) + lm[26].visibility().orElse(0f) + lm[28].visibility().orElse(0f)
                        if (lv >= rv) Triple(lm[23], lm[25], lm[27]) else Triple(lm[24], lm[26], lm[28])
                    }

                    val angle = calcJointAngle(
                        a.x() * lastFrameW, a.y() * lastFrameH,
                        b.x() * lastFrameW, b.y() * lastFrameH,
                        c.x() * lastFrameW, c.y() * lastFrameH
                    )
                    val downThresh = if (isPushup) 100.0 else 105.0
                    val upThresh = if (isPushup) 150.0 else 155.0

                    if (angle < downThresh && stage == "up") {
                        stage = "down"
                        status = "Good depth! Now push UP ↑"
                    } else if (angle > upThresh && stage == "down") {
                        stage = "up"
                        val nextReps = reps + 1
                        reps = nextReps
                        status = "Rep $nextReps counted! Go DOWN ↓"
                        if (nextReps >= target) {
                            finished = true
                            ContextCompat.getMainExecutor(ctx).execute { onDone() }
                        }
                    } else {
                        status = if (stage == "up") "Lower down (${angle.toInt()}°)" else "Push back up (${angle.toInt()}°)"
                    }
                }
                .setErrorListener { }
                .build()
            PoseLandmarker.createFromOptions(ctx, options)
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            closed.set(true)
            poseLandmarker?.close()
            executor.shutdown()
        }
    }

    Text(if (isPushup) "💪 DO PUSH-UPS" else "🦵 DO SQUATS", fontSize = 24.sp, fontWeight = FontWeight.Black)
    Text("$reps / $target", fontSize = 54.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF4D1A))
    Text(status, fontSize = 15.sp, color = Color(0xFFFACC15), fontWeight = FontWeight.Bold)

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(20.dp)),
        factory = { viewCtx ->
            val previewView = PreviewView(viewCtx)
            val providerFuture = ProcessCameraProvider.getInstance(viewCtx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { imageProxy ->
                    try {
                        if (!closed.get() && poseLandmarker != null) {
                            val bmp = imageProxy.toBitmap()
                            val rot = imageProxy.imageInfo.rotationDegrees
                            val rotated = if (rot != 0) {
                                val m = Matrix().apply { postRotate(rot.toFloat()) }
                                Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                            } else bmp
                            lastFrameW = rotated.width
                            lastFrameH = rotated.height
                            val now = SystemClock.uptimeMillis()
                            val ts = if (now <= lastTs) lastTs + 1 else now
                            lastTs = ts
                            poseLandmarker.detectAsync(BitmapImageBuilder(rotated).build(), ts)
                        }
                    } finally {
                        imageProxy.close()
                    }
                }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        viewCtx as LifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        analysis
                    )
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(viewCtx))
            previewView
        }
    )
}

@Composable
private fun MathMission(target: Int, onDone: () -> Unit) {
    var solved by remember { mutableIntStateOf(0) }
    var a by remember { mutableIntStateOf(Random.nextInt(6, 19)) }
    var b by remember { mutableIntStateOf(Random.nextInt(3, 10)) }
    var c by remember { mutableIntStateOf(Random.nextInt(5, 25)) }
    var input by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🧠 SOLVE MATH", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("$solved / $target", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF4D1A))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                Text("$a × $b + $c = ?", fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it.filter { ch -> ch.isDigit() || ch == '-' }; wrong = false },
            label = { Text(if (wrong) "Wrong! Try again" else "Enter answer") },
            isError = wrong,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (input.toIntOrNull() == a * b + c) {
                    solved++
                    if (solved >= target) {
                        onDone()
                    } else {
                        a = Random.nextInt(6, 19)
                        b = Random.nextInt(3, 10)
                        c = Random.nextInt(5, 25)
                        input = ""
                    }
                } else {
                    wrong = true
                    input = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("SUBMIT", fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ShakeMission(target: Int, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var shakes by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastHit = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val mag = sqrt((x * x + y * y + z * z).toDouble())
                val now = System.currentTimeMillis()
                if (mag > 22.0 && now - lastHit > 180) {
                    lastHit = now
                    shakes++
                    if (shakes >= target) onDone()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("📳 SHAKE YOUR PIXEL!", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("$shakes / $target", fontSize = 58.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF4D1A))
        LinearProgressIndicator(
            progress = { (shakes.toFloat() / target.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)),
            color = Color(0xFF22C55E)
        )
        Text("Shake firmly until the bar fills up.", color = Color.LightGray)
    }
}

private fun calcJointAngle(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Double {
    val rad = atan2((cy - by).toDouble(), (cx - bx).toDouble()) -
        atan2((ay - by).toDouble(), (ax - bx).toDouble())
    var deg = abs(Math.toDegrees(rad))
    if (deg > 180.0) deg = 360.0 - deg
    return deg
}

Want to Add More PushClock Features Next?

Once you've got this first build installed on your Pixel 10, tell me if you want
to upgrade it with:

  - Multiple Alarms & Custom Day Schedules (Mon–Fri vs Weekends)
  - Custom MP3 / Spotify-style Wake-Up Sounds + gradual volume ramp
  - Barcode / QR Scanner Mission (scan your toothpaste or coffee jar to turn it
    off)
  - Wake-Up Check (pings you 5 minutes later to confirm you didn't go back to
    sleep)
