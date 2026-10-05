package com.poslik.caisse.ui.pos

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.poslik.caisse.domain.model.Cart
import com.poslik.caisse.domain.model.Catalog
import com.poslik.caisse.ui.theme.CaisseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PosContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tappingAProductAddsItAndCheckoutIsCalledOnce() {
        val added = mutableListOf<String>()
        var checkouts = 0
        val cart = Cart().add(Catalog.products[0])
        compose.setContent {
            CaisseTheme {
                PosContent(
                    registerCode = "C01",
                    state = PosUiState(cart = cart),
                    actions = PosActions(onAddProduct = { added += it.id }, onCheckout = { checkouts++ }),
                )
            }
        }

        compose.onNodeWithTag("product-croissant").performClick()
        compose.onNodeWithTag("checkout").assertIsEnabled().performClick()

        assertEquals(listOf("croissant"), added)
        assertEquals(1, checkouts)
    }

    @Test
    fun checkoutIsDisabledForAnEmptyCart() {
        compose.setContent {
            CaisseTheme { PosContent(registerCode = "C01", state = PosUiState(), actions = PosActions()) }
        }

        compose.onNodeWithTag("checkout").assertIsNotEnabled()
    }
}
