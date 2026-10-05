package com.poslik.caisse.domain.model

data class CartLine(val product: Product, val quantity: Int) {
    init {
        require(quantity > 0) { "La quantité doit être positive" }
    }

    val subtotal: Money get() = product.price * quantity
}

/** Panier immuable : chaque opération renvoie un nouveau panier. L'ordre d'ajout est conservé. */
data class Cart(val lines: List<CartLine> = emptyList()) {

    val isEmpty: Boolean get() = lines.isEmpty()

    val total: Money get() = lines.map { it.subtotal }.sum()

    val itemCount: Int get() = lines.sumOf { it.quantity }

    fun add(product: Product): Cart {
        val existing = lines.indexOfFirst { it.product.id == product.id }
        return if (existing == -1) {
            copy(lines = lines + CartLine(product, 1))
        } else {
            copy(lines = lines.mapIndexed { i, line -> if (i == existing) line.copy(quantity = line.quantity + 1) else line })
        }
    }

    /** Retire une unité ; la ligne disparaît quand la quantité tombe à zéro. */
    fun removeOne(productId: String): Cart = copy(
        lines = lines.mapNotNull { line ->
            when {
                line.product.id != productId -> line
                line.quantity > 1 -> line.copy(quantity = line.quantity - 1)
                else -> null
            }
        },
    )

    fun toSaleLines(): List<SaleLine> = lines.map {
        SaleLine(
            productId = it.product.id,
            productName = it.product.name,
            unitPrice = it.product.price,
            quantity = it.quantity,
        )
    }
}
