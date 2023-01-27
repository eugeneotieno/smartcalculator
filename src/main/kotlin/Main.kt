package calculator

const val HELP = """The program calculates the sum of numbers"""
val inputVariables = mutableMapOf<String, Int>()
val regexVariables = Regex("[a-zA-Z]+")
val regexDigit = Regex("\\d+")

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
                    else {

                        if (input.contains("=")) {
                            val newVariable = input.split("=")
                            if (newVariable.size == 2) {
                                if (regexVariables.matches(newVariable[0].trim())) {
                                    if (!regexDigit.matches(newVariable[1].trim())) {
                                        if (inputVariables.containsKey(newVariable[1].trim())) {
                                            inputVariables[newVariable[0].trim()] = inputVariables[newVariable[1].trim()]!!
                                        } else {
                                            println("Invalid assignment")
                                        }
                                    } else {
                                        try {
                                            val num = newVariable[1].trim().toInt()
                                            inputVariables[newVariable[0].trim()] = num
                                        } catch (e: NumberFormatException) {
                                            println("Invalid assignment")
                                        }
                                    }
                                } else {
                                    println("Invalid identifier")
                                }
                            } else {
                                println("Invalid assignment")
                            }
                        } else {
                            doCalculate(input)
                        }
                    }
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
            result = try {
                item.toInt()
            } catch (e: NumberFormatException) {
                if (regexVariables.matches(item) && inputVariables.containsKey(item)) {
                    inputVariables[item]!!
                } else {
                    println("Unknown variable")
                    break
                }
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
                val regexSign = Regex("[+-]+")
                if (regexSign.matches(item)) {
                    sign = getSign(item)
                } else {
                    if (regexVariables.matches(item) && inputVariables.containsKey(item)) {
                        val num = inputVariables[item]!!
                        if (sign == "+") result += num
                        else result -= num
                    } else {
                        println("Unknown variable")
                        break
                    }
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
