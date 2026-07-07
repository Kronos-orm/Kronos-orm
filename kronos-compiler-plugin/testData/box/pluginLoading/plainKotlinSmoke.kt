fun box(): String {
    val values = listOf("kronos", "official", "compiler-test")
    return if (values.joinToString("/") == "kronos/official/compiler-test") "OK" else "Fail"
}
