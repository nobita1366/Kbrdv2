package com.flexboard.ime

data class KeyDef(
    val label: String,
    val output: String = label,
    val widthWeight: Float = 1f,
    val type: KeyType = KeyType.CHAR,
    val longPress: String? = null
)

enum class KeyType { CHAR, SHIFT, BACKSPACE, ENTER, SPACE, SYMBOLS, LANGUAGE, NUMBERS, COMMA, PERIOD, AUTOTYPE, CLIPBOARD }

data class KbLayout(val rows: List<List<KeyDef>>)

object KeyboardLayouts {

    private fun row(vararg labels: String, longs: Map<String, String> = emptyMap()): List<KeyDef> =
        labels.map { KeyDef(it, longPress = longs[it]) }

    val ENGLISH_QWERTY = KbLayout(
        rows = listOf(
            listOf("1","2","3","4","5","6","7","8","9","0").map { KeyDef(it) },
            row("q","w","e","r","t","y","u","i","o","p", longs = mapOf("e" to "èéêë","u" to "ùúûü","i" to "ìíîï","o" to "òóôõö")),
            row("a","s","d","f","g","h","j","k","l", longs = mapOf("a" to "àáâãäå")),
            listOf(
                KeyDef("⇧", "shift", widthWeight = 1.5f, type = KeyType.SHIFT)
            ) + row("z","x","c","v","b","n","m") + listOf(
                KeyDef("⌫", "back", widthWeight = 1.5f, type = KeyType.BACKSPACE)
            ),
            listOf(
                KeyDef("?123", "symbols", widthWeight = 1.4f, type = KeyType.SYMBOLS),
                KeyDef("🌐", "lang", widthWeight = 1.0f, type = KeyType.LANGUAGE),
                KeyDef(",", ",", widthWeight = 1.0f, type = KeyType.COMMA),
                KeyDef("space", " ", widthWeight = 4.5f, type = KeyType.SPACE),
                KeyDef(".", ".", widthWeight = 1.0f, type = KeyType.PERIOD),
                KeyDef("⏎", "enter", widthWeight = 1.6f, type = KeyType.ENTER)
            )
        )
    )

    val SYMBOLS = KbLayout(
        rows = listOf(
            row("1","2","3","4","5","6","7","8","9","0"),
            row("@","#","$","_","&","-","+","(",")","/"),
            listOf(KeyDef("=\\<", "more", widthWeight = 1.4f, type = KeyType.SYMBOLS))
                + row("*","\"","'",":",";","!","?")
                + listOf(KeyDef("⌫", "back", widthWeight = 1.4f, type = KeyType.BACKSPACE)),
            listOf(
                KeyDef("ABC", "abc", widthWeight = 1.4f, type = KeyType.SYMBOLS),
                KeyDef("🌐", "lang", widthWeight = 1.0f, type = KeyType.LANGUAGE),
                KeyDef(",", ",", widthWeight = 1.0f, type = KeyType.COMMA),
                KeyDef("space", " ", widthWeight = 4.5f, type = KeyType.SPACE),
                KeyDef(".", ".", widthWeight = 1.0f, type = KeyType.PERIOD),
                KeyDef("⏎", "enter", widthWeight = 1.6f, type = KeyType.ENTER)
            )
        )
    )

    val URDU_PHONETIC = KbLayout(
        rows = listOf(
            row("1","2","3","4","5","6","7","8","9","0"),
            row("ق","و","ع","ر","ت","ے","ء","ی","ہ","پ"),
            row("ا","س","د","ف","گ","ح","ج","ک","ل"),
            listOf(KeyDef("⇧", "shift", widthWeight = 1.5f, type = KeyType.SHIFT))
                + row("ز","خ","چ","و","ب","ن","م")
                + listOf(KeyDef("⌫", "back", widthWeight = 1.5f, type = KeyType.BACKSPACE)),
            listOf(
                KeyDef("?123", "symbols", widthWeight = 1.4f, type = KeyType.SYMBOLS),
                KeyDef("🌐", "lang", widthWeight = 1.0f, type = KeyType.LANGUAGE),
                KeyDef("،", "،", widthWeight = 1.0f, type = KeyType.COMMA),
                KeyDef("space", " ", widthWeight = 4.5f, type = KeyType.SPACE),
                KeyDef("۔", "۔", widthWeight = 1.0f, type = KeyType.PERIOD),
                KeyDef("⏎", "enter", widthWeight = 1.6f, type = KeyType.ENTER)
            )
        )
    )

    val ARABIC = KbLayout(
        rows = listOf(
            row("1","2","3","4","5","6","7","8","9","0"),
            row("ض","ص","ث","ق","ف","غ","ع","ه","خ","ح"),
            row("ش","س","ي","ب","ل","ا","ت","ن","م"),
            listOf(KeyDef("⇧", "shift", widthWeight = 1.5f, type = KeyType.SHIFT))
                + row("ئ","ء","ؤ","ر","لا","ى","ة")
                + listOf(KeyDef("⌫", "back", widthWeight = 1.5f, type = KeyType.BACKSPACE)),
            listOf(
                KeyDef("?123", "symbols", widthWeight = 1.4f, type = KeyType.SYMBOLS),
                KeyDef("🌐", "lang", widthWeight = 1.0f, type = KeyType.LANGUAGE),
                KeyDef("،", "،", widthWeight = 1.0f, type = KeyType.COMMA),
                KeyDef("space", " ", widthWeight = 4.5f, type = KeyType.SPACE),
                KeyDef(".", ".", widthWeight = 1.0f, type = KeyType.PERIOD),
                KeyDef("⏎", "enter", widthWeight = 1.6f, type = KeyType.ENTER)
            )
        )
    )

    val NUMPAD = KbLayout(
        rows = listOf(
            row("7","8","9"),
            row("4","5","6"),
            row("1","2","3"),
            listOf(
                KeyDef("ABC", "abc", widthWeight = 1f, type = KeyType.SYMBOLS),
                KeyDef("0", "0", widthWeight = 1f),
                KeyDef("⌫", "back", widthWeight = 1f, type = KeyType.BACKSPACE)
            )
        )
    )

    fun forLanguage(code: String): KbLayout = when (code) {
        "ur" -> URDU_PHONETIC
        "ar" -> ARABIC
        else -> ENGLISH_QWERTY
    }

    val LANGUAGE_CYCLE = listOf("en", "ur", "ar")
    fun nextLanguage(current: String): String {
        val idx = LANGUAGE_CYCLE.indexOf(current).let { if (it < 0) 0 else it }
        return LANGUAGE_CYCLE[(idx + 1) % LANGUAGE_CYCLE.size]
    }
}
