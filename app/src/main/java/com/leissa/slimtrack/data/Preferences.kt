package com.leissa.slimtrack.data

import android.content.Context

/**
 * Small key-value store for UI preferences that are too trivial for the database,
 * such as where the user left the Add Food sheet last time.
 */
class Preferences(context: Context) {
    private val store = context.applicationContext
        .getSharedPreferences("preferences", Context.MODE_PRIVATE)

    /** Name of the last Add Food tab the user picked, or null before the first choice. */
    var lastAddFoodMode: String?
        get() = store.getString(KEY_ADD_FOOD_MODE, null)
        set(value) {
            store.edit().putString(KEY_ADD_FOOD_MODE, value).apply()
        }

    private companion object {
        const val KEY_ADD_FOOD_MODE = "last_add_food_mode"
    }
}
