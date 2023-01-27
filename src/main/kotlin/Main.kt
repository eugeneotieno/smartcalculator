package calculator

import java.math.BigInteger
import java.util.*

class Error {
    private var triggered = false

    private fun printError(string: String) {
        triggered = true
        println(string)
    }

    fun reset() {
        triggered = false
    }

    fun triggered() = triggered

    fun invalidExp() = printError("Invalid expression")

    fun unknownCMD() = printError("Unknown command")

    fun unknownVar() = printError("Unknown variable")

    fun invalidAssign() = printError("Invalid assignment")

    fun invalidID() = printError("Invalid identifier")

    fun negExponent() = printError("negative exponent is not supported")

    fun calcTooLarge() = printError("Expression is too large to calculate")

    fun zeroDiv() = printError("Cannot divide by zero")
}

const val HELP = "The program can add, subtract, multiply, and divide numerous whole numbers, save values " +
        "and supports exponentiation. Example:\na = 3\nb = 2\na + 8 * ((4 + a^b) * b + 1) - 6 / (b + 1)"
val inputVariables = mutableMapOf<String, Int>()
val regexVariables = Regex("[a-zA-Z]+")
val regexDigit = Regex("\\d+")

private val numbersMap = mutableMapOf<String, BigInteger>()
private val numbersStack = Stack<BigInteger>()
private val errorObj = Error()

private val operatorStack = Stack<String>()
private val postfixList = mutableListOf<String>()
private var holdVal = ""
private var indexVal = 0
private var lastVal = 0
private var infixVal = ""
private const val SOME_OP = "*/^"
private const val ALL_OP = "+-$SOME_OP"
private const val OP_PLUS = "()$ALL_OP"

fun main() {
    val scanner = Scanner(System.`in`)
    var input = scanner.nextLine()

    while (input != "/exit") {
        if (input != "") {
            if (input[0] != '/') {
                if (input.contains('=')) memoryAdd(input) else calculate(postfixFrom(input))
            } else command(input)
        }
        input = scanner.nextLine()
        if (numbersStack.isNotEmpty()) numbersStack.clear()
        if (errorObj.triggered()) errorObj.reset()
    }
    println("Bye!")
}

private fun memoryAdd(value: String) {
    val values = value.replace(" ", "").split('=').toTypedArray()
    val sequence: CharRange = 'a'..'z'
    if (values.size > 2) errorObj.invalidAssign() else {
        for (char in values[0]) if (!sequence.contains(char.lowercaseChar())) {
            errorObj.invalidID()
            return
        }
        when {
            isNumber(values[1])          -> numbersMap[values[0]] = values[1].toBigInteger()
            memoryGet(values[1]) != null -> numbersMap[values[0]] = memoryGet(values[1]) ?: 0.toBigInteger()
            else                         -> errorObj.invalidAssign()
        }
    }
}

private fun command(command: String) = if (command == "/help") println(HELP) else errorObj.unknownCMD()

private fun isNumber(number: String) = number.toBigIntegerOrNull() != null

private fun memoryGet(value: String): BigInteger? = if (numbersMap.containsKey(value)) numbersMap[value] else null

private fun calculate(postfix: Array<String>) {
    if (postfix.isNotEmpty()) {
        for (element in postfix) {
            when (element) {
                "+", "-", "*", "/", "^" -> {
                    val num2 = numbersStack.pop()
                    val num1 = numbersStack.pop()
                    when (element) {
                        "+", "-", "*" -> operation(element[0], num1, num2)
                        "/"           -> divide(num1, num2)
                        "^"           -> exponent(num1, num2)
                    }
                }
                else                    -> pushNumber(element)
            }
            if (errorObj.triggered()) return
        }
        println(numbersStack.last())
    }
}

private fun operation(op: Char, num1: BigInteger, num2: BigInteger) {
    try {
        var result = num1
        when (op) {
            '+' -> result += num2
            '*' -> result *= num2
            '-' -> result -= num2
        }
        numbersStack.push(result)
    } catch (e: ArithmeticException) {
        errorObj.calcTooLarge()
    }
}

private fun divide(num1: BigInteger, num2: BigInteger) {
    if (num2 == 0.toBigInteger()) errorObj.zeroDiv() else numbersStack.push((num1 / num2))
}

private fun exponent(num1: BigInteger, num2: BigInteger) {
    when {
        num2 < 0.toBigInteger()             -> errorObj.negExponent()
        num2 > Int.MAX_VALUE.toBigInteger() -> errorObj.calcTooLarge()
        else                                -> try {
            val result = num1.pow(num2.toInt())
            numbersStack.push(result)
        } catch (e: ArithmeticException) {
            errorObj.calcTooLarge()
        }
    }
}

private fun pushNumber(string: String) {
    val num: BigInteger? = if (isNumber(string)) string.toBigInteger() else memoryGet(string)
    if (num == null) errorObj.unknownVar() else numbersStack.push(num)
}

fun postfixFrom(infix: String): Array<String> {
    reset()
    infixVal = infix.trim()
    while (infixVal.contains("  ")) infixVal = infixVal.replace("  ", " ")
    lastVal = infixVal.lastIndex
    var shouldBeOperator = false

    while (!errorObj.triggered() && indexVal <= lastVal) {
        if (infixVal[indexVal] == ' ') indexVal++
        if (shouldBeOperator) {
            val op = chkMultiOp()
            if (!errorObj.triggered()) operator(op.toString())
            shouldBeOperator = false
        } else {
            for (i in 1..4) {
                if (errorObj.triggered() || indexVal > lastVal) break
                val char = infixVal[indexVal]
                when (i) {
                    1 -> if (char == '(') leftParen()
                    2 -> if ("+-".contains(char)) chkMinus()
                    3 -> if (OP_PLUS.contains(char)) errorObj.invalidExp() else addNumber()
                    4 -> if (char == ')') rightParen()
                }
            }
            shouldBeOperator = true
        }
    }
    if (!errorObj.triggered()) emptyStack()
    if (errorObj.triggered()) postfixList.clear()
    return postfixList.toTypedArray()
}

private fun reset() {
    if (operatorStack.isNotEmpty()) operatorStack.clear()
    if (postfixList.isNotEmpty()) postfixList.clear()
    if (errorObj.triggered()) errorObj.reset()
    if (holdVal != "") holdVal = ""
    if (indexVal != 0) indexVal = 0
}

private fun chkMultiOp(): Char {
    var count = 0
    var op = infixVal[indexVal]

    while (ALL_OP.contains(infixVal[indexVal])) {
        indexVal++
        count++
        if (chkIndexERROR()) return ' '
        if (ALL_OP.contains(infixVal[indexVal])) {
            if (infixVal[indexVal] != op || SOME_OP.contains(infixVal[indexVal])) {
                errorObj.invalidExp()
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
            if (operatorStack.isEmpty() || operatorStack.peek() == "(" || opIsGreater(op[0])) operatorStack.push(op) else {
                while (operatorStack.isNotEmpty()) if (operatorStack.peek() != "(") postfixList.add(operatorStack.pop()) else break
                operatorStack.push(op)
            }
        }
        else                    -> errorObj.invalidExp()
    }
}

private fun leftParen() {
    while (infixVal[indexVal] == '(') {
        operatorStack.push(infixVal[indexVal].toString())
        indexVal++
        if (chkIndexERROR()) return
    }
}

private fun chkMinus() {
    when (infixVal[indexVal]) {
        '+', '-' -> {
            if (infixVal[indexVal] == '-') holdVal += '-'
            indexVal++
            chkIndexERROR()
        }
    }
}

private fun addNumber() {
    while (indexVal <= lastVal) {
        if (!OP_PLUS.contains(infixVal[indexVal]) && infixVal[indexVal] != ' ') {
            holdVal += infixVal[indexVal]
            indexVal++
        } else break
    }
    if (holdVal != "") {
        postfixList.add(holdVal)
        holdVal = ""
    }
}

private fun rightParen() {
    while (infixVal[indexVal] == ')') {
        var stop = false
        while (!stop && operatorStack.isNotEmpty()) {
            postfixList.add(operatorStack.pop())
            if (operatorStack.isNotEmpty()) if (operatorStack.peek() == "(") stop = true
        }
        if (operatorStack.isEmpty() && !stop) {
            errorObj.invalidExp()
            return
        } else operatorStack.pop()
        indexVal++
        if (indexVal > lastVal) break
    }
}

private fun opIsGreater(char: Char): Boolean {
    when (char) {
        '+', '-' -> return false
        '*', '/' -> return when (operatorStack.peek()) {
            "+", "-" -> true
            else     -> false
        }
        '^'      -> return when (operatorStack.peek()) {
            "^"  -> false
            else -> true
        }
    }
    return false
}

private fun emptyStack() {
    while (operatorStack.isNotEmpty()) {
        val temp = operatorStack.pop()
        if (temp == "(" || temp == ")") {
            errorObj.invalidExp()
            return
        }
        postfixList.add(temp)
    }
}

private fun chkIndexERROR(): Boolean {
    return if (indexVal > lastVal) {
        errorObj.invalidExp()
        true
    } else false
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

