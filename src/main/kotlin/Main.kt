fun main() {
    var sum = 0
    val numbers = readln().split("\\s+".toRegex())
    for (number in numbers) {
        sum += number.toInt()
    }

    println(sum)
}