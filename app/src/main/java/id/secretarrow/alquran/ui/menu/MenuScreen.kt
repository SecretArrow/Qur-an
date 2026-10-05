package id.secretarrow.alquran.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.secretarrow.alquran.R
import id.secretarrow.alquran.data.model.LastRead
import id.secretarrow.alquran.ui.theme.GoldBright
import id.secretarrow.alquran.ui.theme.GreenDarkDeep

/**
 * Menu utama: latar masjid blur + logo + 5 tombol menu
 * (Baca Qur'an, Terakhir Baca, Pencarian, Jadwal Sholat, Pengaturan).
 */
@Composable
fun MenuScreen(
    lastRead: LastRead?,
    surahName: (Int) -> String,
    onBacaQuran: () -> Unit,
    onTerakhirBaca: () -> Unit,
    onPencarian: () -> Unit,
    onJadwalSholat: () -> Unit,
    onPengaturan: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2C4A62), GreenDarkDeep, Color(0xFF0D2422))
                    )
                )
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_mosque),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.55f
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "القرآن الكريم",
                color = GoldBright,
                fontSize = 40.sp,
                fontFamily = id.secretarrow.alquran.ui.theme.AmiriQuranFamily,
                textAlign = TextAlign.Center
            )
            Text(
                text = "INDONESIA",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                letterSpacing = 6.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(36.dp))

            val buttonModifier =
                Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 420.dp)

            MenuButton(buttonModifier, "BACA QUR'AN", onBacaQuran)
            Spacer(modifier = Modifier.height(14.dp))
            MenuButton(
                buttonModifier,
                "TERAKHIR BACA",
                onTerakhirBaca,
                subtitle = lastRead?.let { "${surahName(it.surah)} : Ayat ${it.ayah}" }
            )
            Spacer(modifier = Modifier.height(14.dp))
            MenuButton(buttonModifier, "PENCARIAN", onPencarian)
            Spacer(modifier = Modifier.height(14.dp))

            // Landscape: dua kolom, portrait: satu kolom
            Row(
                modifier = buttonModifier,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MenuButton(
                    Modifier.weight(1f),
                    "JADWAL SHOLAT",
                    onJadwalSholat
                )
                MenuButton(
                    Modifier.weight(1f),
                    "PENGATURAN",
                    onPengaturan
                )
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MenuButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(10.dp),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Black.copy(alpha = 0.25f),
                contentColor = Color.White
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = GoldBright
                )
            }
        }
    }
}
