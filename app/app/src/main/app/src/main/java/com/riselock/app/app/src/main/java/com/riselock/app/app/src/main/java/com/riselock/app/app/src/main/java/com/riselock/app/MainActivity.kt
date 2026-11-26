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
