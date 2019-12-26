package android.text

class DecimalInputFilter(private val decimalDigits: Int) : InputFilter {
    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        var dotPos = -1
        val len: Int = dest?.length ?: 0
        for (i in 0 until len) {
            val c: Char = dest?.get(i) ?: '0'
            if (c == '.' || c == ',') {
                dotPos = i
                break
            }
        }
        if (dotPos >= 0) { // protects against many dots
            if (source == "." || source == ",") {
                return null
            }
            // if the text is entered before the dot
            if (dend <= dotPos) {
                return ""
            }
            if (len - dotPos > decimalDigits) {
                return ""
            }
        }
        return null
    }

}