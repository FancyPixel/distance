package it.fancypixel.distance.db.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Bump : RealmObject() {
    @PrimaryKey
    var id: Long = 0

    var beaconUuid: String = ""

    var major: Int = -1

    var location: Int = -1

    var batteryLevel: Int = 0

    var distance: Double = -1.0

    var calculatedDistance: Int = 3

    var date: Long = 0
}