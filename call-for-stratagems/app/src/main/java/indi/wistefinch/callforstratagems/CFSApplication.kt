package indi.wistefinch.callforstratagems

import android.app.Application
import indi.wistefinch.callforstratagems.data.AppDatabase

class CFSApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}