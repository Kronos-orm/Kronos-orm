data class PlainUser(
    var id: Int,
    var name: String,
)

fun box(): String {
    val user = PlainUser(1, "Ada").apply {
        id = 2
        name = "Grace"
    }

    return if (user == PlainUser(2, "Grace")) "OK" else "Fail: $user"
}
