package com.poslik.caisse.e2e

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.annotation.StringRes
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.poslik.caisse.FirebaseEmulatorRunner
import com.poslik.caisse.MainActivity
import com.poslik.caisse.R
import com.poslik.caisse.domain.model.Catalog
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Scénario complet sur un vrai appareil, contre les émulateurs Firebase (voir `.github/scripts/e2e.sh`).
 *
 * Il se joue en deux lancements séparés de l'instrumentation, donc deux processus : la phase 2
 * vérifie ce qui se passe au redémarrage de l'app. Ignoré sans l'argument `firebaseEmulator=true`.
 */
@RunWith(AndroidJUnit4::class)
class CaisseEndToEndTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Before
    fun requireEmulator() {
        assumeTrue(InstrumentationRegistry.getArguments().getString(FirebaseEmulatorRunner.ARG_EMULATOR) == "true")
    }

    @Test
    fun phase1_onlineOfflineAndPrinterFailure() {
        // Création du compte, puis réservation du code caisse (pré-rempli à C01).
        waitForTag("login-email")
        compose.onNodeWithText(string(R.string.login_switch_to_sign_up)).performClick()
        compose.onNodeWithTag("login-email").performTextInput("e2e-${System.currentTimeMillis()}@poslik.test")
        compose.onNodeWithTag("login-password").performTextInput("secret-e2e")
        compose.onNodeWithTag("login-submit").performClick()
        waitForText(string(R.string.setup_submit))
        compose.onNodeWithText(string(R.string.setup_submit)).performClick()
        waitForTag("checkout")
        screenshot("1-caisse")

        // En ligne : la vente arrive dans Firebase.
        sell("C01-000001")
        eventually("1 vente dans Firebase") { assertEquals(1, remoteSales().length()) }

        // Hors ligne : on continue d'encaisser, les numéros se suivent.
        setNetwork(enabled = false)
        waitForText(string(R.string.network_offline))
        sell("C01-000002")
        sell("C01-000003")
        screenshot("2-hors-ligne")

        // Retour du réseau : les deux ventes partent, sans doublon.
        setNetwork(enabled = true)
        eventually("3 ventes dans Firebase") { assertSalesWithoutDuplicates(remoteSales(), expected = 3) }
        waitForText(string(R.string.network_synced))
        screenshot("3-synchronise")

        // Panne imprimante : le ticket est enregistré et marqué en échec, jusque dans Firebase.
        compose.onNodeWithTag("printer-failure").performClick()
        sell("C01-000004")
        eventually("ticket 4 en échec dans Firebase") {
            assertEquals("FAILED", remoteSales().ticket("000004").optString("printStatus"))
        }
        screenshot("4-panne-imprimante")
    }

    @Test
    fun phase2_failedTicketReprintedAtStartup() {
        // Nouveau processus : la session et la caisse sont conservées, l'imprimante « remarche ».
        waitForTag("checkout")
        eventually("ticket 4 réimprimé au démarrage") {
            val sales = remoteSales()
            assertSalesWithoutDuplicates(sales, expected = 4)
            assertEquals("PRINTED", sales.ticket("000004").optString("printStatus"))
        }
        // Les tickets déjà imprimés ne repartent jamais à l'impression.
        val sales = remoteSales()
        for (key in listOf("000001", "000002", "000003")) {
            assertEquals("$key imprimé une seule fois", 1, sales.ticket(key).optInt("printAttempts"))
        }
        screenshot("5-apres-redemarrage")
    }

    private fun sell(expectedTicket: String) {
        compose.onNodeWithTag("product-${Catalog.products[0].id}").performClick()
        compose.onNodeWithTag("checkout").performClick()
        waitForText(context.getString(R.string.pos_last_ticket, expectedTicket))
    }

    private fun assertSalesWithoutDuplicates(sales: JSONObject, expected: Int) {
        val keys = sales.keys().asSequence().sorted().toList()
        assertEquals((1..expected).map { "%06d".format(it) }, keys)
        assertEquals(expected, keys.map { sales.getJSONObject(it).getString("saleId") }.toSet().size)
    }

    private fun JSONObject.ticket(key: String): JSONObject = optJSONObject(key) ?: JSONObject()

    /**
     * Lit la base de l'émulateur par son API REST, avec le jeton administrateur de l'émulateur :
     * la vérification ne dépend ni du SDK ni du compte de l'app qu'elle contrôle.
     */
    private fun remoteSales(): JSONObject {
        val url = URL("http://${FirebaseEmulatorRunner.HOST}:9000/sales/C01.json?ns=$DATABASE_NAMESPACE")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("Authorization", "Bearer owner")
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
        }
        try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return if (body == "null") JSONObject() else JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun eventually(what: String, assertion: () -> Unit) {
        val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(SYNC_TIMEOUT_SECONDS)
        while (true) {
            try {
                return assertion()
            } catch (e: Throwable) {
                if (System.currentTimeMillis() > deadline) {
                    screenshot("echec")
                    throw AssertionError("Délai dépassé : $what", e)
                }
                Thread.sleep(POLL_MILLIS)
            }
        }
    }

    private fun setNetwork(enabled: Boolean) {
        val state = if (enabled) "enable" else "disable"
        shell("svc wifi $state")
        shell("svc data $state")
    }

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }

    private fun waitForTag(tag: String) = compose.waitUntil(UI_TIMEOUT_MILLIS) {
        compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitForText(text: String) = compose.waitUntil(UI_TIMEOUT_MILLIS) {
        compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun string(@StringRes id: Int) = context.getString(id)

    private fun screenshot(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val dir = File(context.getExternalFilesDir(null), "e2e").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val UI_TIMEOUT_MILLIS = 30_000L
        const val SYNC_TIMEOUT_SECONDS = 90L
        const val TIMEOUT_MILLIS = 15_000
        const val DATABASE_NAMESPACE = "poslik-caisse-android-default-rtdb"
        const val POLL_MILLIS = 1_000L
    }
}
