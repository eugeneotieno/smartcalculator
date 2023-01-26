fun main() {
    var calculate = true
    while (calculate) {
        val input = readln()
        if (input.isNotBlank()) {
            if (input == "/exit") {
                println("Bye!")
                calculate = false
            } else {
                var sum = 0
                val numbers = input.split("\\s+".toRegex())
                for (number in numbers) {
                    sum += number.toInt()
                }
                println(sum)
            }
        }
    }
}