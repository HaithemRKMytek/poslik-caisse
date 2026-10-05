package com.poslik.caisse.domain.model

data class Product(val id: String, val name: String, val price: Money)

/** Les huit produits demandés par l'énoncé, en dur. */
object Catalog {
    val products: List<Product> = listOf(
        Product(id = "espresso", name = "Café express", price = Money(2_500)),
        Product(id = "cappuccino", name = "Cappuccino", price = Money(4_000)),
        Product(id = "the-menthe", name = "Thé à la menthe", price = Money(2_000)),
        Product(id = "jus-orange", name = "Jus d'orange", price = Money(5_000)),
        Product(id = "eau", name = "Eau minérale", price = Money(1_500)),
        Product(id = "croissant", name = "Croissant", price = Money(2_200)),
        Product(id = "sandwich-thon", name = "Sandwich thon", price = Money(7_500)),
        Product(id = "crepe-chocolat", name = "Crêpe chocolat", price = Money(6_000)),
    )

    fun byId(id: String): Product? = products.firstOrNull { it.id == id }
}
