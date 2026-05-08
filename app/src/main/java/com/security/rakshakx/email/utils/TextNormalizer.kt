package com.security.rakshakx.email.utils

object TextNormalizer {

    fun normalize(text: String): String {

        return text
            .replace("0", "O")
            .replace("1", "I")
            .replace("3", "E")
            .replace("@", "A")
            .replace("$", "S")
            .replace("4", "A")
            .replace("5", "S")
            .replace("7", "T")
    }
}