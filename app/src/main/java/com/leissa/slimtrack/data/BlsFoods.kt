package com.leissa.slimtrack.data

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Offline German food table from the Bundeslebensmittelschlüssel (BLS) 4.0.
 * All values are per 100 g of edible portion.
 *
 * Data: Max Rubner-Institut (2025): Bundeslebensmittelschlüssel (BLS), Version 4.0.
 * CC BY 4.0, DOI 10.25826/Data20251217-134202-0, https://blsdb.de
 *
 * The asset is generated from the official workbook with tools/extract_bls.py.
 */
class BlsFoods(private val assets: AssetManager) {

    private class Entry(val food: Food, val searchName: String)

    @Volatile
    private var cached: List<Entry>? = null

    suspend fun search(query: String, limit: Int = 20): List<Food> = withContext(Dispatchers.IO) {
        val needle = normalize(query)
        if (needle.isEmpty()) return@withContext emptyList()
        val tokens = needle.split(' ').filter(String::isNotBlank)
        if (tokens.isEmpty()) return@withContext emptyList()

        entries().asSequence()
            .filter { entry -> tokens.all(entry.searchName::contains) }
            .sortedBy { rank(it.searchName, needle, tokens.first()) }
            .take(limit)
            .map { it.food }
            .toList()
    }

    private fun entries(): List<Entry> = cached ?: synchronized(this) {
        cached ?: load().also { cached = it }
    }

    private fun load(): List<Entry> =
        assets.open(FILE).bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size != 5) return@mapNotNull null
                val name = parts[0].trim()
                val calories = parts[1].toDoubleOrNull()?.roundToInt() ?: return@mapNotNull null
                if (name.isEmpty() || calories < 0) return@mapNotNull null

                val food = Food(
                    name = name,
                    servingLabel = "100 g",
                    calories = calories,
                    proteinGrams = parts[2].toDoubleOrNull() ?: 0.0,
                    carbsGrams = parts[4].toDoubleOrNull() ?: 0.0,
                    fatGrams = parts[3].toDoubleOrNull() ?: 0.0,
                    isCustom = true,
                )
                Entry(food, normalize(name))
            }.toList()
        }

    /** Terms that start a name or a word in it rank above plain substring matches. */
    private fun rank(name: String, needle: String, firstToken: String): Int {
        val kind = when {
            name == needle -> 0
            name.startsWith(needle) -> 1
            name.split(' ').any { it.startsWith(firstToken) } -> 2
            else -> 3
        }
        return kind * 1_000 + name.length.coerceAtMost(999)
    }

    /**
     * Folds case and umlauts so "Brotchen", "Brötchen" and "Broetchen" all match.
     * Both the asset names and the query go through this, so it only needs to be
     * consistent rather than linguistically perfect.
     */
    private fun normalize(text: String): String {
        val folded = buildString(text.length) {
            for (character in text.lowercase()) {
                when (character) {
                    'ä' -> append('a')
                    'ö' -> append('o')
                    'ü' -> append('u')
                    'ß' -> append("ss")
                    else -> append(character)
                }
            }
        }
        return folded
            .replace("ae", "a")
            .replace("oe", "o")
            .replace("ue", "u")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private companion object {
        const val FILE = "bls_foods.tsv"
    }
}
