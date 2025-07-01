package indie.wistefinch.callforstratagems.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import indie.wistefinch.callforstratagems.R

class DialogEditList(context: Context, activity: Activity, list: List<String>, title: String) {
    private var editFinishedListener: (list: List<String>) -> Unit = {}
    private val recyclerView: RecyclerView

    private val adapter: DialogEditListAdapter by lazy { DialogEditListAdapter() }

    init {
        val dialog = AlertDialog.Builder(context).create()
        val view: View = View.inflate(context, R.layout.dialog_edit_list, null)
        dialog.setCanceledOnTouchOutside(false)

        view.findViewById<TextView>(R.id.dlg_edit_list_title).text =
            String.format(context.getString(R.string.dlg_edit_list_title), title)
        recyclerView = view.findViewById(R.id.dlg_edit_list_recycler)
        recyclerView.adapter = adapter
        recyclerView.recycledViewPool.setMaxRecycledViews(0, 0)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        adapter.setData(list)

        view.findViewById<AppButton>(R.id.dlg_edit_list_add).setOnClickListener {
            adapter.add("")
        }

        view.findViewById<AppButton>(R.id.dlg_edit_list_confirm).setOnClickListener {
            dialog.cancel()
        }

        dialog.setOnCancelListener {
            editFinishedListener(adapter.getData())
        }

        dialog.show()
        dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.window!!.setContentView(view)
    }

    fun onEditFinished(listener: (list: List<String>) -> Unit) {
        editFinishedListener = listener
    }
}