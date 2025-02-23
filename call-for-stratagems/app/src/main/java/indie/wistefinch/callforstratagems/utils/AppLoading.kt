package indie.wistefinch.callforstratagems.utils

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import indie.wistefinch.callforstratagems.R

class AppLoading : ConstraintLayout {
    private var outer: ImageView
    private var inner: ImageView

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        val views = LayoutInflater.from(context).inflate(R.layout.layout_app_loading, this, true)
        outer = views.findViewById(R.id.loading_outer_ring)
        inner = views.findViewById(R.id.loading_inner_ring)
        val outerAnimator = ObjectAnimator.ofFloat(outer, "rotation", 0f, 360f)
        outerAnimator.setDuration(1000)
        outerAnimator.repeatCount = ObjectAnimator.INFINITE
        outerAnimator.interpolator = null
        outerAnimator.start()
        val innerAnimator = ObjectAnimator.ofFloat(inner, "rotation", 0f, -360f)
        innerAnimator.setDuration(1000)
        innerAnimator.repeatCount = ObjectAnimator.INFINITE
        innerAnimator.interpolator = null
        innerAnimator.start()
    }
}