package com.leissa.slimtrack.data

import android.content.Context
import com.leissa.slimtrack.data.remote.FddbClient
import com.leissa.slimtrack.data.remote.OpenFoodFactsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency container. Small enough that a DI framework would be overkill:
 * it owns the database and seeds it on first launch.
 */
class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database = SlimTrackDatabase.build(context)

    val foodDao = database.foodDao()
    val diaryDao = database.diaryDao()
    val goalsDao = database.goalsDao()
    val profileDao = database.profileDao()
    val weightDao = database.weightDao()
    val exerciseDao = database.exerciseDao()

    val openFoodFacts = OpenFoodFactsClient()
    val fddb = FddbClient()
    val blsFoods = BlsFoods(context.applicationContext.assets)

    init {
        scope.launch {
            // The profile row is the first-launch marker: seed foods only then, so
            // deleting every food later does not silently bring the starter list back.
            if (profileDao.get() == null) {
                if (foodDao.count() == 0) {
                    foodDao.insertAll(SeedFoods.items)
                }
                profileDao.upsert(Profile())
            }
            if (goalsDao.get() == null) {
                goalsDao.upsert(Goals())
            }
        }
    }
}
