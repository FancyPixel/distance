package it.fancypixel.distance.global

object Constants {
    const val ACTION_START_FOREGROUND_SERVICE = "it.fancypixel.distance.intent.action.START_FOREGROUND_SERVICE"
    const val ACTION_STOP_FOREGROUND_SERVICE = "it.fancypixel.distance.intent.action.STOP_FOREGROUND_SERVICE"

    const val PREFERENCE_NOTIFICATION_TYPE_ONLY_SOUND = 1
    const val PREFERENCE_NOTIFICATION_TYPE_ONLY_VIBRATE = 2
    const val PREFERENCE_NOTIFICATION_TYPE_BOTH = 3

    const val PREFERENCE_TOLERANCE_LOW = -1
    const val PREFERENCE_TOLERANCE_DEFAULT = 0
    const val PREFERENCE_TOLERANCE_MIN = 1
    const val PREFERENCE_TOLERANCE_HIGH = 2
    const val PREFERENCE_TOLERANCE_MAX = 3

    const val PREFERENCE_DEVICE_LOCATION_POCKET = 1
    const val PREFERENCE_DEVICE_LOCATION_DESK = 2
    const val PREFERENCE_DEVICE_LOCATION_BACKPACK = 3

    const val GLOBAL_ORGANIZATION_ID = 12345

    const val NEAR_DELAY = 10000L
    const val IMMEDIATE_DELAY = 5000L
    const val WARNING_NOTIFICATION_ID = 28
    const val NOTIFICATION_ID = 30
    const val WAKEUP_NOTIFICATION_ID = 31
    const val INACTIVE_NOTIFICATION_ID = 32
    const val REGION_TAG = "REGION_NEAR"
    const val BEACON_ID = "A2B2265F-77F6-4C6A-82ED-297B366FC684"
    const val BEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"

    enum class BeaconDistance(val value: Int) {
        IMMEDIATE(1),
        NEAR(2),
        FAR(3)
    }
}