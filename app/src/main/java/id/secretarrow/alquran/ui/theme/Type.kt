package id.secretarrow.alquran.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.secretarrow.alquran.R

val AmiriQuranFamily = FontFamily(Font(R.font.amiri_quran))
val NotoNaskhFamily = FontFamily(Font(R.font.noto_naskh_arabic))

val AppTypography =
    Typography(
        titleLarge =
            TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp
            ),
        titleMedium =
            TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
        bodyLarge =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 23.sp
            ),
        bodyMedium =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
        labelMedium =
            TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = 0.6.sp
            )
    )
