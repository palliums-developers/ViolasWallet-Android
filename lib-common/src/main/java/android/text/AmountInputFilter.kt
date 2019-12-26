package android.text

class AmountInputFilter(private val integerDigits: Int, private val decimalDigits: Int) :
    InputFilter {
    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        dest?.let {
            val dot = '.'
            val splitArray = it.split(dot)
            val decimalLength = if (splitArray.size > 1) {
                splitArray[1].length
            } else {
                0
            }
            val integerLength = if (splitArray.isNotEmpty()) {
                splitArray[0].length
            } else {
                0
            }
            val integerExcess = integerLength >= integerDigits
            val decimalExcess = decimalLength >= decimalDigits
            val existsDot = it.contains(dot)
            val inputIsDot = source == dot.toString()

            if ((integerExcess && existsDot) || (integerExcess && inputIsDot)) {
                if (decimalExcess) {
                    return ""
                }
                return null
            } else if (integerExcess) {
                return ""
            } else if (decimalExcess) {
                return ""
            } else {
                return null
            }
        }
        return null
    }

}