package com.michael.microbudgeting.data.sms

object SmsParser {

    private val EXPENSE_PATTERNS = listOf(
        // General English bank pattern (e.g., spent 150.50 EGP at Carrefour)
        Regex("""(?:debited|spent|paid|charged)[^\d]*([\d,]+\.?\d*)\s*(?:EGP|USD|EUR|GBP|LE)?""", RegexOption.IGNORE_CASE),
        // Arabic bank alert (Egyptian banks: e.g. تم خصم 150.50 جنيه)
        Regex("""تم خصم\s*([\d,]+\.?\d*)\s*(?:جنيه|LE|EGP)?"""),
        // CIB, NBE, Banque Misr common format (e.g., Purchase with amount 1,500.00 EGP)
        Regex("""(?:Purchase|POS|ATM)[^\d]*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
        // Vodafone Cash / Orange Money payments
        Regex("""(?:تحويل|دفع|سداد)[^\d]*([\d,]+\.?\d*)""")
    )

    private val MERCHANT_PATTERNS = listOf(
        Regex("""(?:at|@|merchant|لدى)\s+([A-Za-z\u0600-\u06FF][A-Za-z\u0600-\u06FF\s]{2,30})""", RegexOption.IGNORE_CASE)
    )

    data class ParsedExpense(
        val amount: Double,
        val merchant: String?,
        val rawSms: String,
        val suggestedCategory: String
    )

    fun parse(smsBody: String): ParsedExpense? {
        val amount = EXPENSE_PATTERNS
            .firstNotNullOfOrNull { regex ->
                regex.find(smsBody)?.groupValues?.get(1)
            }
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null

        val merchant = MERCHANT_PATTERNS
            .firstNotNullOfOrNull { regex ->
                regex.find(smsBody)?.groupValues?.get(1)?.trim()
            }

        return ParsedExpense(
            amount = amount,
            merchant = merchant,
            rawSms = smsBody,
            suggestedCategory = inferCategory(smsBody, merchant)
        )
    }

    private fun inferCategory(sms: String, merchant: String?): String {
        val text = "${sms.lowercase()} ${merchant?.lowercase().orEmpty()}"
        return when {
            text.containsAny("grocery", "supermarket", "carrefour", "spinneys", "gourmet", "بقالة", "سوبرماركت", "مترو") -> "Groceries"
            text.containsAny("restaurant", "cafe", "coffee", "pizza", "burger", "starbucks", "didis", "مطعم", "كافيه", "قهوة") -> "Food & Dining"
            text.containsAny("uber", "careem", "taxi", "fuel", "petrol", "shell", "emarat", "mobil", "وقود", "تاكسي", "شيل") -> "Transport"
            text.containsAny("pharmacy", "hospital", "clinic", "doctor", "health", "elezaby", "صيدلية", "مستشفى", "طبيب") -> "Health"
            text.containsAny("electricity", "water", "gas", "internet", "we", "orange", "vodafone", "etisalat", "fawry", "كهرباء", "مياه", "غاز", "فوري") -> "Utilities"
            text.containsAny("amazon", "noon", "jumia", "online", "shopping", "mall", "zara", "h&m", "تسوق", "أونلاين", "شراء") -> "Shopping"
            else -> "Uncategorized"
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
