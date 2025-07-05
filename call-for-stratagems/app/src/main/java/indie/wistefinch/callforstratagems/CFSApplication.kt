package indie.wistefinch.callforstratagems

import android.app.Application
import indie.wistefinch.callforstratagems.data.AsrKeywordDatabase
import indie.wistefinch.callforstratagems.data.GroupDatabase
import indie.wistefinch.callforstratagems.data.StratagemDatabase

class CFSApplication : Application() {
    val groupDb: GroupDatabase by lazy { GroupDatabase.getDatabase(this) }
    val stratagemDb: StratagemDatabase by lazy { StratagemDatabase.getDatabase(this) }
    val asrKeywordDb: AsrKeywordDatabase by lazy { AsrKeywordDatabase.getDatabase(this) }
}