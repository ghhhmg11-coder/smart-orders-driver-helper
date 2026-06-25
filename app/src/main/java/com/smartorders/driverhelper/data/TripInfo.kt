package com.smartorders.driverhelper.data

data class TripInfo(
    val price: Float,
    val pickupMinutes: Float,
    val pickupDistanceKm: Float,
    val rawText: String
)

fun parseTripInfo(text: String): TripInfo? {
    // Extract price — pattern like 7.47 ﷼ or ﷼ 7.47 or 7.47 ر.س
    val priceRegex = Regex("""(\d+(?:\.\d+)?)\s*[﷼ر]""")
    val priceAltRegex = Regex("""[﷼ر]\s*(\d+(?:\.\d+)?)""")
    val priceMatch = priceRegex.find(text) ?: priceAltRegex.find(text)
    val price = priceMatch?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: return null

    // Extract pickup minutes — pattern like "يبعد 3 دقائق" or "3 دقائق" or just the number before دقائق
    val minutesRegex = Regex("""(\d+(?:\.\d+)?)\s*دقائق""")
    val minutesMatch = minutesRegex.find(text)
    val minutes = minutesMatch?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f

    // Extract pickup distance km — pattern like "يبعد 1.1 كم" or "1.1 كم"
    val distanceRegex = Regex("""(\d+(?:\.\d+)?)\s*كم""")
    val distanceMatch = distanceRegex.find(text)
    val distance = distanceMatch?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f

    return TripInfo(
        price = price,
        pickupMinutes = minutes,
        pickupDistanceKm = distance,
        rawText = text
    )
}
