export const $dataClass: string = `
@Table(name = "tb_product")
data class Product(
    var code: String? = null,
    @Column(name = "product_ame")
    var name: String? = null,
    var category: String? = null,
    var quantity: Int? = null
): KPojo
`.trim();

export const $select: string = `
Product().select{ it.code + it.quantity }
  .where{ it.name == "Sunglasses" }
  .query()

Product().select()
    .where{ it.name in listOf("Sunglasses", "Bamboo Watch") }
    .orderBy{ it.quantity }
    .query()

Product("7F9V0BpQ2W").select().by{ it.code }.query()

Product().select().where{ it.quantity > 10 }.query()
`.trim();

export const $insert: string = `
Product("a3c2a3fv27", "Sunglasses", "Fashion", 3).insert().execute()

listOf(
    Product("c0a45pzo2a", "Apple Watch", "Fashion", 3),
    Product("q1fj099afc", "Portable Speakers", "Electronics", 2)
)
  .insert().execute()
`.trim();

export const $delete: string = `
Product("a3c2a3fv2").delete()
  .by{ it.code }
  .execute()

Product().delete()
  .where{ it.category == "Fashion" && it.quantity < 4 }
  .execute()

Product("a3c2a3fv2").delete()
  .where{ it.code.eq }
  .execute()
`.trim();

export const $update: string = `
Product("a3c2a3fv2", "Apple Watch", "Electronics", 0)
  .update()
  .by{ it.code }
  .execute()

Product("a3c2a3fv2", "Umbrella", "Fashion", 0)
  .update{ it.name + it.quantity }
  .by{ it.code }
  .execute()

Product().update()
  .set{
    it.name = "Basketball"
    it.category = "Sports"
  }
  .where{ it.code == "a3c2a3fv2" }
  .execute()

Product(name = "Watch").update()
  .set{
    it.quantity = 0
  }
  .where{ it.name.matchBoth }
  .execute()
`.trim();

export const $upsert: string = `
val product = Product("a3c2a3fv2", "Apple Watch", "Electronics", 0)
product.upsert()
  .on{ it.code }
  .execute()

product.upsert{ it.name + it.quantity }
  .onConflict()
  .execute()
`.trim()
