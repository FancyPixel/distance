package it.fancypixel.distance.db

import androidx.lifecycle.LiveData
import io.realm.Case
import io.realm.Realm
import io.realm.RealmResults
import it.fancypixel.distance.components.RealmLiveData
import it.fancypixel.distance.db.models.Bump
import it.fancypixel.distance.global.Constants
import it.fancypixel.distance.utils.asLiveData
import org.altbeacon.beacon.Beacon
import java.util.*

class BumpRepository {
    private val realm by lazy { Realm.getDefaultInstance() }

    fun getBumpWithPositiveUuid(uuid: String): RealmResults<Bump>? = realm.where(Bump::class.java).contains("beaconUuid", uuid, Case.INSENSITIVE).findAll()

    fun getTodayBumps(): RealmResults<Bump>? {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        return realm.where(Bump::class.java).greaterThan("date", today.timeInMillis).findAll()
    }

    fun getTodayBumpsLiveData(): RealmLiveData<Bump>? {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        return realm.where(Bump::class.java).greaterThan("date", today.timeInMillis).findAllAsync()?.asLiveData()
    }

    fun clearOldBumps() {
        realm.executeTransactionAsync {
            val lastUseFullTime = Calendar.getInstance()
            lastUseFullTime.set(Calendar.HOUR_OF_DAY, 0)
            lastUseFullTime.set(Calendar.MINUTE, 0)
            lastUseFullTime.set(Calendar.SECOND, 0)
            lastUseFullTime.set(Calendar.MILLISECOND, 0)
            lastUseFullTime.add(Calendar.DAY_OF_YEAR, -21)

            val list = it.where(Bump::class.java).lessThan("date", lastUseFullTime.timeInMillis).findAll()
            list.deleteAllFromRealm()
        }
    }

    fun addNewBump(beacon: Beacon, location: Int, level: Int, calculatedMinDistance: Double) {
        realm.executeTransactionAsync {
            val currentIdNum: Number? = it.where(Bump::class.java).max("id")
            val bump = it.createObject(Bump::class.java, currentIdNum?.toInt()?.plus(1) ?: 1)
            bump.beaconUuid = beacon.id1.toString()
            bump.major = beacon.id2.toInt()
            bump.location = location
            bump.batteryLevel = level
            bump.distance = beacon.distance
            bump.calculatedDistance = when {
                beacon.distance < calculatedMinDistance / 2 -> {
                    Constants.BeaconDistance.IMMEDIATE.value
                }
                beacon.distance < calculatedMinDistance -> {
                    Constants.BeaconDistance.NEAR.value
                }
                else -> {
                    Constants.BeaconDistance.FAR.value
                }
            }
            bump.date = Calendar.getInstance().timeInMillis
        }
    }

}