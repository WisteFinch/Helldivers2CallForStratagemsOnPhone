package indi.wistefinch.callforstratagems

import android.app.Application
import indi.wistefinch.callforstratagems.data.GroupDatabase
import indi.wistefinch.callforstratagems.data.StratagemDatabase

class CFSApplication : Application() {
    val groupDb: GroupDatabase by lazy { GroupDatabase.getDatabase(this) }
    val stratagemDb: StratagemDatabase by lazy { StratagemDatabase.getDatabase(this) }
}