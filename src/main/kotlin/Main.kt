package calculator

import java.math.BigInteger
import java.util.*

object Error {
    private var TRIGGERED = false

    private fun printError(string: String) {
        TRIGGERED = true
        println(string)
    }

    fun reset() {
        TRIGGERED = false
    }

    fun triggered() = TRIGGERED

    fun unknownCMD() = printError("Unknown command")

    fun unknownVar() = printError("Unknown variable")

    fun invalidExp() = printError("Invalid expression")

    fun invalidAssign() = printError("Invalid assignment")

    fun invalidID() = printError("Invalid identifier")

    fun negExponent() = printError("negative exponent is not supported")

    fun calcTooLarge() = printError("Expression is too large to calculate")

    fun zeroDiv() = printError("Cannot divide by zero")
}

object Postfix {
    private val STACK = Stack<String>()
    private val POSTFIX = mutableListOf<String>()
    private var HOLD = ""
    private var INDEX = 0
    private var LAST = 0
    private var INFIX = ""
    private const val SOME_OP = "*/^"
    private const val ALL_OP = "+-$SOME_OP"
    private const val OP_PLUS = "()$ALL_OP"

    fun convert(infix: String): Array<String> {
        reset()
        INFIX = infix.trim()
        while (INFIX.contains("  ")) INFIX = INFIX.replace("  ", " ")
        LAST = INFIX.lastIndex
        var shouldBeOperator = false

        while (!Error.triggered() && INDEX <= LAST) {
            if (INFIX[INDEX] == ' ') INDEX++
            if (shouldBeOperator) {
                val op = chkMultiOp()
                if (!Error.triggered()) operator(op.toString())
                shouldBeOperator = false
            } else {
                for (i in 1..4) {
                    if (Error.triggered() || INDEX > LAST) break
                    val char = INFIX[INDEX]
                    when (i) {
                        1 -> if (char == '(') leftParen()
                        2 -> if ("+-".contains(char)) chkMinus()
                        3 -> if (OP_PLUS.contains(char)) Error.invalidExp() else addNumber()
                        4 -> if (char == ')') rightParen()
                    }
                }
                shouldBeOperator = true
            }
        }
        if (Error.triggered()) POSTFIX.clear() else emptyStack()
        return POSTFIX.toTypedArray()
    }

    private fun reset() {
        if (STACK.isNotEmpty()) STACK.clear()
        if (POSTFIX.isNotEmpty()) POSTFIX.clear()
        if (HOLD != "") HOLD = ""
        if (INDEX != 0) INDEX = 0
    }

    private fun chkMultiOp(): Char {
        var count = 0
        var op = INFIX[INDEX]

        while (ALL_OP.contains(INFIX[INDEX])) {
            INDEX++
            count++
            if (chkIndexError()) return ' '
            if (ALL_OP.contains(INFIX[INDEX])) {
                if (INFIX[INDEX] != op || SOME_OP.contains(INFIX[INDEX])) {
                    Error.invalidExp()
                    return ' '
                }
            }
        }
        if (op == '-' && count % 2 == 0) op = '+'
        return op
    }

    private fun operator(op: String) {
        when (op) {
            "+", "-", "*", "/", "^" -> {
                if (STACK.isEmpty() || STACK.peek() == "(" || opIsGreater(op[0])) STACK.push(op) else {
                    while (STACK.isNotEmpty()) if (STACK.peek() != "(") POSTFIX.add(STACK.pop()) else break
                    STACK.push(op)
                }
            }
            else -> Error.invalidExp()
        }
    }

    private fun leftParen() {
        while (INFIX[INDEX] == '(') {
            STACK.push(INFIX[INDEX].toString())
            INDEX++
            if (chkIndexError()) return
        }
    }

    private fun chkMinus() {
        when (INFIX[INDEX]) {
            '+', '-' -> {
                if (INFIX[INDEX] == '-') HOLD += '-'
                INDEX++
                chkIndexError()
            }
        }
    }

    private fun addNumber() {
        while (INDEX <= LAST) {
            if (!OP_PLUS.contains(INFIX[INDEX]) && INFIX[INDEX] != ' ') {
                HOLD += INFIX[INDEX]
                INDEX++
            } else break
        }
        if (HOLD != "") {
            POSTFIX.add(HOLD)
            HOLD = ""
        }
    }

    private fun rightParen() {
        while (INFIX[INDEX] == ')') {
            var stop = false
            while (!stop && STACK.isNotEmpty()) {
                POSTFIX.add(STACK.pop())
                if (STACK.isNotEmpty()) if (STACK.peek() == "(") stop = true
            }
            if (STACK.isEmpty() && !stop) {
                Error.invalidExp()
                return
            } else STACK.pop()
            INDEX++
            if (INDEX > LAST) break
        }
    }

    private fun opIsGreater(char: Char): Boolean {
        when (char) {
            '+', '-' -> return false
            '*', '/' -> return when (STACK.peek()) {
                "+", "-" -> true
                else -> false
            }
            '^' -> return when (STACK.peek()) {
                "^" -> false
                else -> true
            }
        }
        return false
    }

    private fun emptyStack() {
        while (STACK.isNotEmpty()) {
            val temp = STACK.pop()
            if (temp == "(" || temp == ")") {
                Error.invalidExp()
                return
            }
            POSTFIX.add(temp)
        }
    }

    private fun chkIndexError(): Boolean {
        return if (INDEX > LAST) {
            Error.invalidExp()
            true
        } else false
    }
}

const val HELP = "The program can add, subtract, multiply, and divide numerous whole numbers, save values " +
        "and supports exponentiation. Example:\na = 3\nb = 2\na + 8 * ((4 + a^b) * b + 1) - 6 / (b + 1)"
val inputVariables = mutableMapOf<String, Int>()
val regexVariables = Regex("[a-zA-Z]+")
val regexDigit = Regex("\\d+")
private val numberStack = Stack<Long>()
private val numbersInput = mutableMapOf<String, Long>()

fun main() {
    var input = readln()
    while (input != "/exit") {
        if (input != "") {
            if (input[0] != '/') {
                if (input.contains('=')) memoryAdd(input) else doMath(Postfix.convert(input))
            } else command(input)
        }
        input = readln()
        if (numberStack.isNotEmpty()) numberStack.clear()
        if (Error.triggered()) Error.reset()
    }
    println("Bye!")
}

private fun memoryAdd(value: String) {
    val values = value.replace(" ", "").split('=').toTypedArray()
    val sequence: CharRange = 'a'..'z'
    if (values.size > 2) Error.invalidAssign() else {
        for (char in values[0]) if (!sequence.contains(char.lowercaseChar())) {
            Error.invalidID()
            return
        }
        when {
            isNumber(values[1]) -> numbersInput[values[0]] = values[1].toLong()
            memoryGet(values[1]) != null -> numbersInput[values[0]] = memoryGet(values[1]) ?: 0
            else -> Error.invalidAssign()
        }
    }
}

private fun command(command: String) = if (command == "/help") println(HELP) else Error.unknownCMD()

private fun isNumber(number: String) = number.toLongOrNull() != null

private fun memoryGet(value: String): Long? = if (numbersInput.containsKey(value)) numbersInput[value] else null

private fun doMath(postfix: Array<String>) {
    if (!Error.triggered()) {
        for (element in postfix) {
            when (element) {
                "+", "-", "*", "/", "^" -> {
                    val num2 = numberStack.pop().toLong()
                    val num1 = numberStack.pop().toLong()
                    when (element) {
                        "+", "-", "*" -> operation(element[0], num1, num2)
                        "/" -> divide(num1, num2)
                        "^" -> exponent(num1, num2)
                    }
                }
                else -> pushNumber(element)
            }
            if (Error.triggered()) return
        }
        println(numberStack.last())
    }
}

private fun operation(op: Char, num1: Long, num2: Long) {
    var result = num1.toBigInteger()
    when (op) {
        '+' -> result += num2.toBigInteger()
        '*' -> result *= num2.toBigInteger()
        '-' -> result -= num2.toBigInteger()
    }
    if (!tooBigChk(result)) numberStack.push(result.toLong())
}

private fun tooBigChk(num: BigInteger): Boolean {
    return if (num > Long.MAX_VALUE.toBigInteger() || num < Long.MIN_VALUE.toBigInteger()) {
        Error.calcTooLarge()
        true
    } else false
}

private fun divide(num1: Long, num2: Long) {
    if (num2 == 0L) Error.zeroDiv() else numberStack.push((num1 / num2))
}

private fun exponent(num1: Long, num2: Long) {
    when {
        num2 < 0 -> Error.negExponent()
        num2 > Int.MAX_VALUE.toLong() -> Error.calcTooLarge()
        num2 == 0L -> numberStack.push(1L)
        else -> {
            var num3 = num1.toBigInteger()
            if (num2 > 1) {
                repeat((num2 - 1).toInt()) {
                    num3 *= num1.toBigInteger()
                    if (tooBigChk(num3)) return
                }
            }
            numberStack.push(num3.toLong())
        }
    }
}

private fun pushNumber(string: String) {
    val num: Long? = if (isNumber(string)) string.toLong() else memoryGet(string)
    if (num == null) Error.unknownVar() else numberStack.push(num.toLong())
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

