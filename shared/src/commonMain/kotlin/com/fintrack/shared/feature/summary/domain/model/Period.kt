import com.fintrack.shared.feature.core.util.DateTimeUtils

sealed class Period {
    abstract val code: String
    data class Week(override val code: String) : Period()
    data class Month(override val code: String) : Period()
    data class Year(override val code: String) : Period()

    fun getDateRange(): Pair<String, String>? {
        return try {
            when (this) {
                is Week -> {
                    val range = DateTimeUtils.getIsoWeekRange(code)
                    if (range != null) {
                        range.first.toString() to range.second.toString()
                    } else null
                }
                is Month -> {
                    val range = DateTimeUtils.getMonthRange(code)
                    if (range != null) {
                        range.first.toString() to range.second.toString()
                    } else null
                }
                is Year -> {
                    val year = code.toInt()
                    "$year-01-01" to "$year-12-31"
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
