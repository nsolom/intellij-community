// "Replace array of boxed with array of primitive" "false"
// ACTION: Add full qualifier
// ACTION: Put parameters on one line
// ACTION: Introduce import alias
// ACTION: Enable a trailing comma by default in the formatter
// ACTION: Convert to vararg parameter (may break code)
annotation class SuperAnnotation(
        val str: <caret>Array<String>
)