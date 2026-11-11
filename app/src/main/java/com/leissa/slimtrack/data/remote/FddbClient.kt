package com.leissa.slimtrack.data.remote

import com.leissa.slimtrack.data.Food
import com.leissa.slimtrack.data.FoodSearchResult
import com.leissa.slimtrack.data.FoodSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * Minimal FDDB client.
 *
 * FDDB has no public API, so this reads the anonymous website search page and,
 * for the product the user actually picks, the product page's nutrition block.
 * Values are per 100 g. Use sparingly: one request per search and one per pick.
 */
class FddbClient {

    suspend fun search(query: String, limit: Int = 20): List<FoodSearchResult> =
        withContext(Dispatchers.IO) {
            val term = query.trim()
            if (term.isEmpty()) return@withContext emptyList()

            val url = "$BASE/db/de/suche/?udd=0&cat=site-search&search=" +
                URLEncoder.encode(term, "UTF-8")
            parseSearch(get(url))
                .distinctBy { it.name.lowercase() }
                .take(limit)
        }

    /** Reads energy and macros for 100 g from the product page behind [result]. */
    suspend fun fetchProduct(result: FoodSearchResult): Food? = withContext(Dispatchers.IO) {
        val reference = result.reference ?: return@withContext null
        fetchProductPage(reference, result.name)
    }

    /**
     * Resolves a scanned barcode to the FDDB product behind it. The site's own
     * search page ignores barcodes, but its autocomplete endpoint matches them.
     */
    suspend fun fetchByBarcode(barcode: String): Food? = withContext(Dispatchers.IO) {
        val code = barcode.filter(Char::isDigit)
        if (code.isEmpty()) return@withContext null

        val hit = parseAutocomplete(get("$BASE/xml/ac/1/de/$code")).firstOrNull()
            ?: return@withContext null
        fetchProduct(hit)
    }

    private fun fetchProductPage(reference: String, name: String): Food? {
        val page = get(BASE + reference)

        val calories = quickstat(page, "Kalorien")
            ?: tableValue(page, "Kalorien", "kcal")
            ?: return null
        if (calories < 0.0) return null

        return Food(
            name = name.take(90),
            servingLabel = "100 g",
            calories = calories.roundToInt(),
            proteinGrams = quickstat(page, "Protein") ?: tableValue(page, "Protein", "g") ?: 0.0,
            carbsGrams = quickstat(page, "Kohlenhydrate")
                ?: tableValue(page, "Kohlenhydrate", "g")
                ?: 0.0,
            fatGrams = quickstat(page, "Fett") ?: tableValue(page, "Fett", "g") ?: 0.0,
            isCustom = true,
        )
    }

    private fun parseAutocomplete(html: String): List<FoodSearchResult> =
        AUTOCOMPLETE.findAll(html).mapNotNull { match ->
            val reference = match.groupValues[1]
            val name = unescape(match.groupValues[2].replace(TAG, " "))
                .replace(WHITESPACE, " ")
                .trim()
            if (name.isEmpty()) return@mapNotNull null
            FoodSearchResult(name = name.take(90), source = FoodSource.FDDB, reference = reference)
        }.toList()

    private fun parseSearch(html: String): List<FoodSearchResult> =
        ROW.findAll(html).mapNotNull { rowMatch ->
            val row = rowMatch.groupValues[1]
            val link = LINK.find(row) ?: return@mapNotNull null
            val reference = link.groupValues[1]
            if (!reference.startsWith("/db/de/lebensmittel/")) return@mapNotNull null

            val name = unescape(link.groupValues[2]).trim()
            if (name.isEmpty()) return@mapNotNull null
            val brand = BRAND.find(row)?.groupValues?.get(1)?.let(::unescape)?.trim()
            val displayName = if (brand != null && brand.isNotBlank() &&
                !name.contains(brand, ignoreCase = true)
            ) {
                "$name ($brand)"
            } else {
                name
            }

            FoodSearchResult(
                name = displayName.take(90),
                source = FoodSource.FDDB,
                reference = reference,
            )
        }.toList()

    private fun quickstat(html: String, label: String): Double? {
        val match = Regex(
            "quickstat__label[^>]*>\\s*${Regex.escape(label)}\\s*</span>\\s*" +
                "<span[^>]*quickstat__value[^>]*>([^<]+)<",
        ).find(html) ?: return null
        return parseNumber(match.groupValues[1])
    }

    private fun tableValue(html: String, label: String, unit: String): Double? {
        val match = Regex(
            "<div[^>]*>${Regex.escape(label)}</div>\\s*<div>\\s*([0-9.,]+)\\s*$unit",
        ).find(html) ?: return null
        return parseNumber(match.groupValues[1])
    }

    /**
     * FDDB prints German numbers such as "6,3 g" and "2.257" (thousands dot).
     * A dot is only a thousands separator when the value also has a decimal comma,
     * or when it is followed by three digits; otherwise it is treated as a decimal
     * point so an English-formatted "0.5" is not read as 5.
     */
    private fun parseNumber(raw: String): Double? {
        val numeric = NUMBER.find(raw)?.value?.replace(" ", "") ?: return null
        if (numeric.isEmpty()) return null
        val normalized = if (numeric.contains(',')) {
            numeric.replace(".", "").replace(',', '.')
        } else {
            val dots = numeric.count { it == '.' }
            val decimals = numeric.substringAfterLast('.').length
            if (dots == 1 && decimals in 1..2) numeric else numeric.replace(".", "")
        }
        return normalized.toDoubleOrNull()
    }

    private fun unescape(text: String): String =
        ENTITY.replace(text) { match ->
            val body = match.groupValues[1]
            val replacement = when {
                body.startsWith("#x", ignoreCase = true) ->
                    body.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) }

                body.startsWith("#") ->
                    body.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) }

                else -> NAMED_ENTITIES[body]
            }
            replacement ?: match.value
        }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            // The site rejects obvious bot agents; a normal mobile browser UA works.
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36",
            )
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")
            connection.setRequestProperty("Accept-Language", "de-DE,de;q=0.9")
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            val code = connection.responseCode
            if (code !in 200..299) error("FDDB returned HTTP $code")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val BASE = "https://fddb.info"

        val NUMBER = Regex("[0-9][0-9.,\\s]*")
        val ROW = Regex("<div class='rl[ab]'[^>]*>(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
        val LINK = Regex("<a\\s+href='([^']+)'[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
        val AUTOCOMPLETE = Regex(
            "window\\.location\\.href='(/db/de/lebensmittel/[^']+)'[^>]*>\\s*<div id='acelement\\d+'>(.*?)</div>",
            RegexOption.DOT_MATCHES_ALL,
        )
        val TAG = Regex("<[^>]*>")
        val WHITESPACE = Regex("\\s+")
        val BRAND = Regex(
            "<span class='rld'>[^<]*<a[^>]*>(.*?)</a>",
            RegexOption.DOT_MATCHES_ALL,
        )
        val ENTITY = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);")

        val NAMED_ENTITIES = mapOf(
            "amp" to "&",
            "lt" to "<",
            "gt" to ">",
            "quot" to "\"",
            "apos" to "'",
            "nbsp" to " ",
            "auml" to "ä",
            "ouml" to "ö",
            "uuml" to "ü",
            "Auml" to "Ä",
            "Ouml" to "Ö",
            "Uuml" to "Ü",
            "szlig" to "ß",
            "eacute" to "é",
            "egrave" to "è",
            "agrave" to "à",
            "ccedil" to "ç",
        )
    }
}
