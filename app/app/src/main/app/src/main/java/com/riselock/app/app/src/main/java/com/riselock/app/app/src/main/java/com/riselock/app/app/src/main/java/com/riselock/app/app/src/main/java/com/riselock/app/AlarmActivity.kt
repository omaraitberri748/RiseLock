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
