package me.hiyjeain.android.swipetoloadlayout

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import me.hiyjeain.android.swipetoloadlayout.trigger.SwipeLoadMoreTrigger
import me.hiyjeain.android.swipetoloadlayout.trigger.SwipeTrigger

/**
 * Created on 2020/2/26
 *
 * @author Garrett Xu (hiyjeain@hotmail.com) of YILU Tech Studio (yilu_tech@outlook.com)
 */
class SwipeLoadMoreFooterLayout constructor(context: Context?,
                                            attrs: AttributeSet? = null,
                                            defStyleAttr: Int = 0) :
        FrameLayout(context!!, attrs, defStyleAttr),
        SwipeLoadMoreTrigger,
        SwipeTrigger {
    override fun onLoadMore() {}
    override fun onPrepare() {}
    override fun onMove(y: Int, isComplete: Boolean, automatic: Boolean) {}
    override fun onRelease() {}
    override fun onComplete() {}
    override fun onReset() {}
}