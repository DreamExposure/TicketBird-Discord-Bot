package org.dreamexposure.ticketbird.extensions

fun MutableList<String>.asStringList(): String {
    val builder = StringBuilder()

    for ((i, str) in this.withIndex()) {
        if (str.isNotBlank()) {
            if (i == 0) builder.append(str)
            else builder.append(",").append(str)
        }
    }

    return builder.toString()
}

fun MutableList<String>.setFromString(strList: String) {
    this += strList.split(",").filter(String::isNotBlank)
}

// For java
fun setFromString(strList: String): MutableList<String> {
    return strList.split(",").filter(String::isNotBlank).toMutableList()
}
