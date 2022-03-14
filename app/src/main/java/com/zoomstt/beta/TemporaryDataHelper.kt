package com.zoomstt.beta


// Class save data local
class TemporaryDataHelper {

    companion object {
        private var instance: TemporaryDataHelper? = null

        @Synchronized
        fun instance(): TemporaryDataHelper {
            if (instance == null) {
                instance = TemporaryDataHelper()
            }
            return instance as TemporaryDataHelper
        }
    }

    var schoolName = ""
    var fontSize = 16
    var transparency = 0


}