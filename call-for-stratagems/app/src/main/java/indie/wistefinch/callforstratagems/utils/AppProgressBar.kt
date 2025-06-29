package indie.wistefinch.callforstratagems.utils

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import indie.wistefinch.callforstratagems.R

class AppProgressBar : ConstraintLayout {
    private var textView: TextView
    private var progressView: ProgressBar

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        val views = LayoutInflater.from(context).inflate(R.layout.layout_app_progress_bar, this, true)
        textView = views.findViewById(R.id.app_progress_bar_text)
        progressView = views.findViewById(R.id.app_progress_bar_value)

        val attrs = context.obtainStyledAttributes(attributeSet, R.styleable.AppProgressBar)
        if (attrs.hasValue(R.styleable.AppProgressBar_title)) {
            textView.text = attrs.getString(R.styleable.AppProgressBar_title)!!
        }
        if (attrs.hasValue(R.styleable.AppProgressBar_value)) {
            progressView.progress = attrs.getInteger(R.styleable.AppProgressBar_value, 50)
        }
        attrs.recycle()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    fun setText(txt: String) {
        textView.text = txt
    }

    fun setValue(value: Int) {
        progressView.progress = value
    }
}