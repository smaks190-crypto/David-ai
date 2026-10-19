package com.example.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonRose
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

data class ParsedVoiceOperation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String,
    val isIncome: Boolean,
    val date: String = LocalDate.now().toString()
)

object VoiceParserHelper {

    // Number word mapping for Russian speech recognition
    private val numberWords = mapOf(
        "ноль" to 0L,
        "один" to 1L, "одна" to 1L, "одну" to 1L,
        "два" to 2L, "две" to 2L,
        "три" to 3L,
        "четыре" to 4L,
        "пять" to 5L,
        "шесть" to 6L,
        "семь" to 7L,
        "восемь" to 8L,
        "девять" to 9L,
        "десять" to 10L,
        "одиннадцать" to 11L,
        "двенадцать" to 12L,
        "тринадцать" to 13L,
        "четырнадцать" to 14L,
        "пятнадцать" to 15L,
        "шестнадцать" to 16L,
        "семнадцать" to 17L,
        "восемнадцать" to 18L,
        "девятнадцать" to 19L,
        "двадцать" to 20L,
        "тридцать" to 30L,
        "сорок" to 40L,
        "пятьдесят" to 50L,
        "шестьдесят" to 60L,
        "семьдесят" to 70L,
        "восемьдесят" to 80L,
        "девяносто" to 90L,
        "сто" to 100L,
        "двести" to 200L,
        "триста" to 300L,
        "четыреста" to 400L,
        "пятьсот" to 500L,
        "шестьсот" to 600L,
        "семьсот" to 700L,
        "восемьсот" to 800L,
        "девятьсот" to 900L,
        "полторы" to 1500L,
        "полтора" to 1500L
    )

    private val incomeKeywords = listOf(
        "зарплата", "зарплату", "зарплаты",
        "аванс", "аванса",
        "доход", "доходы",
        "кешбэк", "кэшбэк", "кэшбек", "кешбек",
        "подарок", "подарили",
        "перевели", "перевод", "скинули", "закинули",
        "вернули", "возврат", "долг вернули",
        "премия", "премию",
        "стипендия", "стипендию",
        "выплата", "пособие", "дивиденды", "проценты"
    )

    private val categories = listOf(
        "Продукты" to listOf(
            "хлеб", "молоко", "мясо", "сыр", "курица", "рыба", "овощи", "фрукты",
            "продукты", "еда", "пятерочка", "перекресток", "магнит", "вкусвилл",
            "лента", "ашан", "супермаркет", "яйца", "масло", "колбаса", "сахар",
            "чай", "кофе домой", "печенье", "вода", "шоколад"
        ),
        "Кафе и Фастфуд" to listOf(
            "кофе", "капучино", "латте", "американо", "кафе", "ресторан", "мак",
            "вкусно и точка", "бургер", "пицца", "суши", "роллы", "шаурма",
            "обед", "ужин", "завтрак", "столовая", "додо", "бургер кинг", "кфс",
            "kfc", "бар", "пиво", "коктейль", "пекарня"
        ),
        "Транспорт" to listOf(
            "такси", "яндекс такси", "uber", "бензин", "заправка", "азс", "лукойл",
            "газпром", "метро", "автобус", "проезд", "троллейбус", "трамвай",
            "каршеринг", "ситидрайв", "делимобиль", "самокат", "парковка",
            "мойка", "шиномонтаж", "билет", "электричка"
        ),
        "Дом и Связь" to listOf(
            "жкх", "квартплата", "интернет", "связь", "телефон", "мтс", "мегафон",
            "билайн", "теле2", "аренда", "свет", "газ", "отопление", "ремонт",
            "леруа", "мебель", "икеа", "хозяйственные", "бытовая химия"
        ),
        "Развлечения" to listOf(
            "кино", "фильм", "билеты в кино", "игры", "steam", "стим", "playstation",
            "подписка", "яндекс плюс", "spotify", "спотифай", "концерт", "театр",
            "боулинг", "клуб", "музей", "кальян", "книга", "книги"
        ),
        "Одежда" to listOf(
            "одежда", "обувь", "куртка", "кроссовки", "ботинки", "джинсы",
            "футболка", "рубашка", "платье", "zara", "вайлдберриз", "wildberries",
            "ozon", "озон", "шоппинг", "носки", "шапка"
        ),
        "Здоровье" to listOf(
            "аптека", "лекарства", "таблетки", "витамины", "врач", "клиника",
            "стоматолог", "зубы", "анализы", "спортзал", "фитнес", "абонемент",
            "массаж", "оптика", "очки"
        ),
        "Доходы" to listOf(
            "зарплата", "аванс", "премия", "доход", "кешбэк", "перевели", "подарили", "стипендия"
        )
    )

    /**
     * Converts verbal Russian numbers into standard digit strings.
     * e.g. "две тысячи пятьсот" -> "2500", "сто двадцать" -> "120"
     */
    fun normalizeNumberWords(text: String): String {
        val words = text.lowercase(Locale.ROOT)
            .replace(",", " , ")
            .replace(".", " . ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val resultWords = mutableListOf<String>()
        var i = 0

        while (i < words.size) {
            val word = words[i]

            // Check if current word is a number word
            if (numberWords.containsKey(word) || word.startsWith("тысяч")) {
                var currentSum = 0L
                var total = 0L
                var matchedAny = false

                while (i < words.size) {
                    val w = words[i]
                    val num = numberWords[w]

                    if (num != null) {
                        matchedAny = true
                        if (num >= 1000L) {
                            total += if (currentSum == 0L) num else currentSum * (num / 1000L * 1000L)
                            currentSum = 0L
                        } else {
                            currentSum += num
                        }
                        i++
                    } else if (w.startsWith("тысяч")) {
                        matchedAny = true
                        total += if (currentSum == 0L) 1000L else currentSum * 1000L
                        currentSum = 0L
                        i++
                    } else {
                        break
                    }
                }

                if (matchedAny) {
                    val finalValue = total + currentSum
                    if (finalValue > 0) {
                        resultWords.add(finalValue.toString())
                    }
                    continue
                }
            }

            resultWords.add(word)
            i++
        }

        return resultWords.joinToString(" ")
    }

    /**
     * Parses continuous speech text into structured ParsedVoiceOperation list.
     * Supports phrases with multiple operations separated by conjunctions, commas, or numbers.
     * e.g.: "такси 450 кофе 250 и зарплата 60000"
     */
    fun parseVoiceInput(rawText: String): List<ParsedVoiceOperation> {
        if (rawText.isBlank()) return emptyList()

        val normalized = normalizeNumberWords(rawText)
        val operations = mutableListOf<ParsedVoiceOperation>()

        // Split around common conjunctions and punctuation: "и", "а ещё", "а также", "плюс", "затем", ","
        val splitRegex = Regex("(?<=\\d)\\s+(?:и|а\\s+ещё|а\\s+также|плюс|затем|,)\\s+|\\s+(?:и|а\\s+ещё|а\\s+также|плюс|затем|,)\\s+|\\s*,\\s*|\\s*;\\s*")
        val rawSegments = normalized.split(splitRegex).map { it.trim() }.filter { it.isNotBlank() }

        // Also check if single segment contains multiple distinct (title + number) patterns
        val finalSegments = mutableListOf<String>()
        val multiNumberPattern = Regex("(?<=\\d{1,8})\\s+(?=[а-яА-Яa-zA-Z]{2,})")

        for (seg in rawSegments) {
            val subSegments = seg.split(multiNumberPattern).map { it.trim() }.filter { it.isNotBlank() }
            if (subSegments.size > 1) {
                finalSegments.addAll(subSegments)
            } else {
                finalSegments.add(seg)
            }
        }

        for (segment in finalSegments) {
            val operation = parseSingleSegment(segment)
            if (operation != null) {
                operations.add(operation)
            }
        }

        return operations
    }

    private fun parseSingleSegment(segment: String): ParsedVoiceOperation? {
        val cleaned = segment.lowercase(Locale.ROOT)

        // Find numbers with optional suffixes (k, тыс, руб, р)
        // e.g., "500", "2.5к", "3500р", "1500 руб"
        val amountRegex = Regex("(\\d+(?:[.,]\\d+)?)\\s*(тыс\\w*|к|k|руб\\w*|р)?")
        val match = amountRegex.findAll(cleaned).lastOrNull() ?: return null

        val rawNumberStr = match.groupValues[1].replace(',', '.')
        val suffix = match.groupValues[2].lowercase(Locale.ROOT)
        var amount = rawNumberStr.toDoubleOrNull() ?: return null

        if (suffix.startsWith("к") || suffix == "k" || suffix.startsWith("тыс")) {
            amount *= 1000.0
        }

        if (amount <= 0.0) return null

        // Remove the amount portion to get the title
        val titleCandidate = cleaned.removeRange(match.range)
            .replace(Regex("\\b(на|за|в|купил|купила|потратил|потратила|оплатил|оплатила|рублей|рубля|руб|р)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val isIncome = incomeKeywords.any { cleaned.contains(it) }

        val category = if (isIncome) {
            "Доходы"
        } else {
            detectCategory(cleaned)
        }

        val displayTitle = if (titleCandidate.isNotBlank()) {
            titleCandidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } else {
            if (isIncome) "Поступление" else category
        }

        return ParsedVoiceOperation(
            title = displayTitle,
            amount = amount,
            category = category,
            isIncome = isIncome
        )
    }

    private fun detectCategory(text: String): String {
        for ((catName, keywords) in categories) {
            if (catName == "Доходы") continue
            for (keyword in keywords) {
                if (text.contains(keyword)) {
                    return catName
                }
            }
        }
        return "Прочее"
    }

    fun getCategoryIcon(category: String): ImageVector {
        return when (category) {
            "Продукты" -> Icons.Default.ShoppingCart
            "Кафе и Фастфуд" -> Icons.Default.Fastfood
            "Транспорт" -> Icons.Default.DirectionsCar
            "Дом и Связь" -> Icons.Default.Home
            "Развлечения" -> Icons.Default.Movie
            "Одежда" -> Icons.Default.ShoppingBag
            "Здоровье" -> Icons.Default.LocalPharmacy
            "Доходы" -> Icons.Default.Payments
            else -> Icons.Default.Category
        }
    }

    fun getCategoryColor(category: String, isIncome: Boolean): Color {
        if (isIncome) return NeonEmerald
        return when (category) {
            "Продукты" -> NeonAmber
            "Кафе и Фастфуд" -> NeonRose
            "Транспорт" -> NeonCyan
            "Дом и Связь" -> NeonIndigo
            "Развлечения" -> Color(0xFFA855F7)
            "Одежда" -> Color(0xFFEC4899)
            "Здоровье" -> Color(0xFF38BDF8)
            else -> Color(0xFF94A3B8)
        }
    }

    fun formatAmount(amount: Double, isIncome: Boolean): String {
        val format = NumberFormat.getNumberInstance(Locale("ru", "RU"))
        format.maximumFractionDigits = 0
        val formatted = format.format(amount)
        return if (isIncome) "+$formatted ₽" else "-$formatted ₽"
    }
}
