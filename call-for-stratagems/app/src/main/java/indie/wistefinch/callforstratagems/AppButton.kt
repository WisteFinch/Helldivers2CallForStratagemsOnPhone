package indie.wistefinch.callforstratagems

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

class AppButton : ConstraintLayout {
    private var textView: TextView
    private var hintView: TextView
    private var iconView: ImageView
    private var borderTopView: View
    private var borderBottomView: View
    private var bgView: LinearLayout

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        val views = LayoutInflater.from(context).inflate(R.layout.layout_app_button, this, true)
        textView = views.findViewById(R.id.app_button_text)
        hintView = views.findViewById(R.id.app_button_hint)
        iconView = views.findViewById(R.id.app_button_icon)
        borderTopView = views.findViewById(R.id.app_button_border_top)
        borderBottomView = views.findViewById(R.id.app_button_border_bottom)
        bgView = views.findViewById(R.id.app_button_bg)

        val attrs = context.obtainStyledAttributes(attributeSet, R.styleable.AppButton)
        if (attrs.hasValue(R.styleable.AppButton_text)) {
            textView.text = attrs.getString(R.styleable.AppButton_text)!!
        }
        if (attrs.hasValue(R.styleable.AppButton_hint)) {
            hintView.visibility = VISIBLE
            hintView.text = attrs.getString(R.styleable.AppButton_hint)!!
        }
        if (attrs.hasValue(R.styleable.AppButton_icon)) {
            iconView.visibility = VISIBLE
            iconView.setImageDrawable(attrs.getDrawable(R.styleable.AppButton_icon))
            textView.textAlignment = TEXT_ALIGNMENT_CENTER
            hintView.textAlignment = TEXT_ALIGNMENT_CENTER
        }
        if (attrs.hasValue(R.styleable.AppButton_iconWidth)) {
            iconView.visibility = VISIBLE
            iconView.layoutParams.width = attrs.getDimension(R.styleable.AppButton_iconWidth, 50f).toInt()
        }

        borderTopView.setBackgroundResource(R.drawable.clickable_bg_top)
        borderBottomView.setBackgroundResource(R.drawable.clickable_bg_bottom)

        setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    borderTopView.setBackgroundResource(R.drawable.clickable_bg_top)
                    borderBottomView.setBackgroundResource(R.drawable.clickable_bg_bottom)
                    bgView.setBackgroundColor(
                        resources.getColor(
                            R.color.buttonBackground,
                            context.theme
                        )
                    )
                }

                MotionEvent.ACTION_DOWN -> {
                    borderTopView.setBackgroundResource(R.drawable.clickable_bg_top_pressed)
                    borderBottomView.setBackgroundResource(R.drawable.clickable_bg_bottom_pressed)
                    bgView.setBackgroundColor(
                        resources.getColor(
                            R.color.buttonBackgroundPressed,
                            context.theme
                        )
                    )
                    view.performClick()
                }

                MotionEvent.ACTION_CANCEL -> {
                    borderTopView.setBackgroundResource(R.drawable.clickable_bg_top)
                    borderBottomView.setBackgroundResource(R.drawable.clickable_bg_bottom)
                    bgView.setBackgroundColor(
                        resources.getColor(
                            R.color.buttonBackground,
                            context.theme
                        )
                    )
                }
            }
            true
        }
        attrs.recycle()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }
}