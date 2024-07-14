package indie.wistefinch.callforstratagems.layout

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager

/**
 * Custom grid layout Manager, disable user to scroll.
 */
class AppGridLayoutManager(context: Context?, row: Int): GridLayoutManager(context, row) {

    override fun canScrollVertically() = false
    override fun canScrollHorizontally() = false

}