package calculator

import java.lang.NumberFormatException

const val HELP = """The program calculates the sum of numbers"""

fun main() {
    start()
}

fun start() {
    var calculate = true
    while (calculate) {
        val input = readln()
        if (input.isNotBlank()) {
            when(input) {
                "/exit" -> {
                    println("Bye!")
                    calculate = false
                }
                "/help" -> {
                    println(HELP)
                }
                else -> {
                    if (input.contains("/"))  println("Unknown command")
                    else doCalculate(input)
                }
            }
        }
    }
}

fun doCalculate(input: String) {
    var result = 0
    var sign = "+"
    val items = input.split("\\s+".toRegex())
    for (item in items) {
        if (result == 0) {
            try {
                result = item.toInt()
            } catch (e: NumberFormatException) {
                println("Invalid expression")
                break
            }
        } else {
            if (isNumeric(item)) {
                try {
                    val num = item.toInt()
                    if (sign == "+") result += num
                    else result -= num
                } catch (e: NumberFormatException) {
                    println("Invalid expression")
                    break
                }
            } else {
                val regex = Regex("[+-]+")
                if (regex.matches(item)) {
                    sign = getSign(item)
                } else {
                    println("Invalid expression")
                    break
                }
            }
        }
    }
    println(result)
}

fun isNumeric(toCheck: String): Boolean {
    val regex = "-?[0-9]+(\\.[0-9]+)?".toRegex()
    return toCheck.matches(regex)
}

fun getSign(sign: String): String {
    return if (sign.contains("+")) {
        "+"
    } else {
        if (sign.length % 2 == 0) {
            "+"
        } else {
            "-"
        }
    }
}
