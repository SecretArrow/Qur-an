package id.secretarrow.alquran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E smoke test: aplikasi terbuka, menu tampil, daftar surah & pembaca bekerja.
 */
@RunWith(AndroidJUnit4::class)
class AppE2ETest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun menuUtamaMenampilkanLimaTombol() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText("BACA QUR'AN")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("BACA QUR'AN").assertIsDisplayed()
        composeRule.onNodeWithText("PENCARIAN").assertIsDisplayed()
        composeRule.onNodeWithText("JADWAL SHOLAT").assertIsDisplayed()
        composeRule.onNodeWithText("PENGATURAN").assertIsDisplayed()
    }

    @Test
    fun bukaDaftarSurahDanBukaAlFatihah() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText("BACA QUR'AN")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("BACA QUR'AN").performClick()

        // Tab SURAH tampil
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasText("SURAH")).fetchSemanticsNodes().isNotEmpty()
        }

        // Baris Al-Fatihah muncul (data termuat async)
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasText("Al-Fatihah", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasText("Al-Fatihah", substring = true))[0].performClick()

        // Pembaca: terjemahan ayat 1 Al-Fatihah tampil
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule
                .onAllNodes(hasText("Dengan nama Allah", substring = true))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun bukaPencarianMenampilkanMode() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText("PENCARIAN")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("PENCARIAN").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText("Mendekati")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Mendekati").assertIsDisplayed()
        composeRule.onNodeWithText("Terperinci").assertIsDisplayed()
    }
}
