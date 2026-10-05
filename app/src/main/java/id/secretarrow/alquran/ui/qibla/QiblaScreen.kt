package id.secretarrow.alquran.ui.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.secretarrow.alquran.core.QiblaUtils
import id.secretarrow.alquran.ui.theme.GreenDark
import id.secretarrow.alquran.ui.theme.OrangeTajwid
import id.secretarrow.alquran.ui.theme.Teal
import kotlin.math.cos
import kotlin.math.sin

/** Kompas arah kiblat: jarum merah menunjuk kiblat, tampilkan sudut & jarak. */
@Composable
fun QiblaScreen(
    onBack: () -> Unit,
    latitude: Double,
    longitude: Double,
    locationLabel: String
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val azimuth = remember { mutableFloatStateOf(0f) }
    val hasCompass =
        remember {
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
        }

    DisposableEffect(hasCompass) {
        if (!hasCompass) return@DisposableEffect onDispose { }
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var gravityValues: FloatArray? = null
        var magneticValues: FloatArray? = null
        val listener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR -> {
                            val r = FloatArray(9)
                            SensorManager.getRotationMatrixFromVector(r, event.values)
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(r, orientation)
                            azimuth.floatValue = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        }
                        Sensor.TYPE_ACCELEROMETER -> gravityValues = event.values.clone()
                        Sensor.TYPE_MAGNETIC_FIELD -> magneticValues = event.values.clone()
                    }
                    if (rotationSensor == null && gravityValues != null && magneticValues != null) {
                        val r = FloatArray(9)
                        val i = FloatArray(9)
                        if (SensorManager.getRotationMatrix(r, i, gravityValues, magneticValues)) {
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(r, orientation)
                            azimuth.floatValue = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        }
                    }
                }

                override fun onAccuracyChanged(
                    sensor: Sensor?,
                    accuracy: Int
                ) = Unit
            }
        rotationSensor?.also { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        accelerometer?.also { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        magnetometer?.also { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val qiblaAngle = remember(latitude, longitude) { QiblaUtils.qiblaDirection(latitude, longitude) }
    val distance = remember(latitude, longitude) { QiblaUtils.distanceToKaabaKm(latitude, longitude) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                title = { Text("Arah Qiblat", color = Color.White, style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
            )
        }
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = locationLabel.substringBeforeLast(" - "),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = locationLabel.substringAfterLast(" - ", "").ifEmpty { "INDONESIA" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp
            )
            if (!hasCompass) {
                Text(
                    text = "Perangkat tidak memiliki sensor kompas.\nArah kiblat: $qiblaAngle° dari Utara.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                CompassView(
                    azimuth = azimuth.floatValue,
                    qiblaAngle = qiblaAngle.toFloat(),
                    modifier =
                        Modifier
                            .padding(vertical = 28.dp)
                            .size(280.dp)
                )
            }
            Text(
                text = "Qiblat %.2f° dari Utara".format(qiblaAngle),
                fontWeight = FontWeight.SemiBold,
                color = id.secretarrow.alquran.ui.theme.GoldText,
                fontSize = 16.sp
            )
            Text(
                text = "Jarak ke Ka'bah ± ${distance.toInt()} KM",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = "Letakkan perangkat mendatar, jauh dari logam, lalu putar hingga jarum merah menunjuk ke atas.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier =
                    Modifier
                        .padding(top = 20.dp)
                        .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CompassView(
    azimuth: Float,
    qiblaAngle: Float,
    modifier: Modifier = Modifier
) {
    val ringColor = Teal
    val faceColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f

        // ring luar
        drawCircle(color = ringColor, radius = radius, center = Offset(cx, cy))
        drawCircle(color = Color(0xFF37474F), radius = radius * 0.92f, center = Offset(cx, cy))
        drawCircle(color = faceColor, radius = radius * 0.86f, center = Offset(cx, cy))

        // arah mata angin (ikut azimuth, sehingga N selalu utara sejati relatif perangkat)
        val labels = listOf("N" to 0f, "NE" to 45f, "E" to 90f, "SE" to 135f, "S" to 180f, "SW" to 225f, "W" to 270f, "NW" to 315f)
        labels.forEach { (label, angle) ->
            val rad = Math.toRadians((angle - azimuth).toDouble())
            val lx = cx + (radius * 0.72f * cos(rad).toFloat())
            val ly = cy + (radius * 0.72f * sin(rad).toFloat())
            drawContext.canvas.nativeCanvas.apply {
                val paint =
                    android.graphics.Paint().apply {
                        color = if (label == "N") android.graphics.Color.rgb(0, 121, 107) else android.graphics.Color.DKGRAY
                        textSize = (radius * 0.12f)
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                drawText(label, lx, ly + paint.textSize / 3, paint)
            }
        }

        // jarum kiblat (merah)
        rotate(degrees = qiblaAngle - azimuth, pivot = Offset(cx, cy)) {
            drawLine(
                color = OrangeTajwid,
                start = Offset(cx, cy),
                end = Offset(cx, cy - radius * 0.8f),
                strokeWidth = radius * 0.06f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = OrangeTajwid,
                radius = radius * 0.05f,
                center = Offset(cx, cy - radius * 0.8f)
            )
        }
        // pusat
        drawCircle(color = Color(0xFF37474F), radius = radius * 0.08f, center = Offset(cx, cy), style = Stroke(width = 4f))
    }
}
