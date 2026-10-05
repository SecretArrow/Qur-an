package id.secretarrow.alquran.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.secretarrow.alquran.ui.theme.Gold
import id.secretarrow.alquran.ui.theme.GreenDark
import id.secretarrow.alquran.ui.theme.OrnamentBg
import id.secretarrow.alquran.ui.theme.Teal
import kotlin.math.cos
import kotlin.math.sin

/** Bentuk bintang-oktagon (rub el hizb) untuk badge nomor surah/ayat. */
class OctagramShape(
    private val innerRatio: Float = 0.58f
) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.graphics.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rOuter = size.minDimension / 2f
        val rInner = rOuter * innerRatio
        val path = Path()
        for (k in 0 until 16) {
            val angle = Math.PI / 8 * k - Math.PI / 2
            val r = if (k % 2 == 0) rOuter else rInner
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** Badge nomor surah/ayat bergaya bintang-oktagon. */
@Composable
fun StarBadge(
    number: Int,
    size: Dp = 38.dp,
    tint: Color = Teal,
    borderColor: Color = Gold
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val rOuter = this.size.minDimension / 2f - 1.dp.toPx()
            val rInner = rOuter * 0.58f
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val path = Path()
            for (k in 0 until 16) {
                val angle = Math.PI / 8 * k - Math.PI / 2
                val r = if (k % 2 == 0) rOuter else rInner
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = borderColor, style = Stroke(width = 1.6.dp.toPx()))
        }
        Text(
            text = number.toString(),
            color = tint,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/** Lingkaran logo berisi tulisan "القرآن" untuk app bar. */
@Composable
fun LogoBadge(size: Dp = 34.dp) {
    Box(
        modifier =
            Modifier
                .size(size)
                .background(Teal, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "قرآن",
            color = Color.White,
            fontSize = 13.sp,
            fontFamily = id.secretarrow.alquran.ui.theme.AmiriQuranFamily
        )
    }
}

/**
 * Header ornamen emas di atas daftar ayat:
 * lingkaran kiri (Makkiyah/Madaniyah), tengah (makna surah), lingkaran kanan (jumlah ayat).
 */
@Composable
fun OrnamentHeader(
    leftLabel: String,
    centerLabel: String,
    rightLabel: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(OrnamentBg)
                .border(1.dp, Gold.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        // deretan wajik ornamen
        Canvas(modifier = Modifier.matchParentSize()) {
            val step = 26.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawRect(
                    color = Gold.copy(alpha = 0.25f),
                    topLeft = Offset(x, size.height * 0.12f),
                    size =
                        androidx.compose.ui.geometry
                            .Size(7.dp.toPx(), 7.dp.toPx())
                )
                drawRect(
                    color = Gold.copy(alpha = 0.25f),
                    topLeft = Offset(x, size.height * 0.75f),
                    size =
                        androidx.compose.ui.geometry
                            .Size(7.dp.toPx(), 7.dp.toPx())
                )
                x += step
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .background(Color.Transparent, CircleShape)
                        .border(1.4.dp, Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = leftLabel,
                    color = GreenDark,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .border(1.dp, Gold, RoundedCornerShape(10.dp))
                        .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = centerLabel,
                    color = GreenDark,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .background(Color.Transparent, CircleShape)
                        .border(1.4.dp, Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rightLabel,
                    color = GreenDark,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
