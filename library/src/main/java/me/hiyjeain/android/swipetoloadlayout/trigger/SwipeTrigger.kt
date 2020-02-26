package me.hiyjeain.android.swipetoloadlayout.trigger

/**
 * Created on 2020/2/26
 *
 * @author Garrett Xu (hiyjeain@hotmail.com) of YILU Tech Studio (yilu_tech@outlook.com)
 */
interface SwipeTrigger {
    fun onPrepare()
    fun onMove(y: Int, isComplete: Boolean, automatic: Boolean)
    fun onRelease()
    fun onComplete()
    fun onReset()
}