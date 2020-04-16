package it.fancypixel.distance.global

object Constants {
    const val ACTION_START_FOREGROUND_SERVICE = "it.fancypixel.distance.intent.action.START_FOREGROUND_SERVICE"
    const val ACTION_STOP_FOREGROUND_SERVICE = "it.fancypixel.distance.intent.action.STOP_FOREGROUND_SERVICE"
    const val ACTION_UPDATE_THE_SERVICE_NOTIFICATION = "it.fancypixel.distance.intent.action.UPDATE_THE_SERVICE_NOTIFICATION"
    const val ACTION_CHANGE_DEVICE_LOCATION_TO_POCKET = "it.fancypixel.distance.intent.action.CHANGE_DEVICE_LOCATION_TO_POCKET"
    const val ACTION_CHANGE_DEVICE_LOCATION_TO_DESK = "it.fancypixel.distance.intent.action.CHANGE_DEVICE_LOCATION_TO_DESK"
    const val ACTION_CHANGE_DEVICE_LOCATION_TO_BACKPACK = "it.fancypixel.distance.intent.action.CHANGE_DEVICE_LOCATION_TO_BACKPACK"

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
}