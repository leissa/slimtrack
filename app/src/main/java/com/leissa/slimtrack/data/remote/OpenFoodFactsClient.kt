package com.leissa.slimtrack.data.remote

import com.leissa.slimtrack.data.Food
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * Minimal Open Food Facts client. Values are imported per 100 g, which matches the
 * food model's serving-based entries (serving label "100 g").
 */
class OpenFoodFactsClient {

    suspend fun search(query: String): List<Food> = withContext(Dispatchers.IO) {
        val term = query.trim()
        if (term.isEmpty()) return@withContext emptyList()

        val encoded = URLEncoder.encode(term, "UTF-8")
        val url = "https://search.openfoodfacts.org/search?q=$encoded&page_size=20" +
            "&fields=product_name,brands,serving_size,nutriments"
        val hits = JSONObject(get(url)).optJSONArray("hits") ?: JSONArray()

        val seen = LinkedHashMap<String, Food>()
        for (i in 0 until hits.length()) {
            val food = parseHit(hits.optJSONObject(i) ?: continue) ?: continue
            seen.putIfAbsent("${food.name.lowercase()}|${food.calories}", food)
        }
        seen.values.toList()
    }

    suspend fun fetchByBarcode(barcode: String): Food? = withContext(Dispatchers.IO) {
        val code = barcode.trim()
        if (code.isEmpty()) return@withContext null

        val url = "https://world.openfoodfacts.org/api/v2/product/" +
            URLEncoder.encode(code, "UTF-8") +
            ".json?fields=product_name,brands,serving_size,nutriments"
        val root = JSONObject(get(url))
        if (root.optInt("status", 0) != 1) return@withContext null
        val product = root.optJSONObject("product") ?: return@withContext null
        parseProduct(product)
    }

    private fun parseHit(hit: JSONObject): Food? {
        val name = hit.optString("product_name").trim()
        if (name.isEmpty()) return null
        val brand = when (val brands = hit.opt("brands")) {
            is JSONArray -> brands.optString(0)
            else -> hit.optString("brands").substringBefore(',')
        }.trim()
        return buildFood(name, brand, hit.optJSONObject("nutriments") ?: return null)
    }

    private fun parseProduct(product: JSONObject): Food? {
        val name = product.optString("product_name").trim()
        if (name.isEmpty()) return null
        val brand = product.optString("brands").substringBefore(',').trim()
        return buildFood(name, brand, product.optJSONObject("nutriments") ?: return null)
    }

    private fun buildFood(name: String, brand: String, nutriments: JSONObject): Food? {
        var kcal = nutriments.optDouble("energy-kcal_100g", Double.NaN)
        if (kcal.isNaN()) {
            val kj = nutriments.optDouble("energy_100g", Double.NaN)
            if (!kj.isNaN()) kcal = kj / 4.184
        }
        if (kcal.isNaN() || kcal < 0.0 || kcal > 1500.0) return null

        val displayName = if (brand.isNotBlank() && !name.contains(brand, ignoreCase = true)) {
            "$name ($brand)"
        } else {
            name
        }

        return Food(
            name = displayName.take(90),
            servingLabel = "100 g",
            calories = kcal.roundToInt(),
            proteinGrams = nutriments.optDouble("proteins_100g", 0.0).coerceAtLeast(0.0),
            carbsGrams = nutriments.optDouble("carbohydrates_100g", 0.0).coerceAtLeast(0.0),
            fatGrams = nutriments.optDouble("fat_100g", 0.0).coerceAtLeast(0.0),
        )
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "SlimTrack/1.0 (Android app; personal use)")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            val code = connection.responseCode
            if (code !in 200..299) error("Open Food Facts returned HTTP $code")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
