package me.hiyjeain.android.swipetoloadlayout

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.Scroller
import me.hiyjeain.android.swipetoloadlayout.listener.OnLoadMoreListener
import me.hiyjeain.android.swipetoloadlayout.listener.OnRefreshListener
import me.hiyjeain.android.swipetoloadlayout.trigger.SwipeLoadMoreTrigger
import me.hiyjeain.android.swipetoloadlayout.trigger.SwipeRefreshTrigger
import me.hiyjeain.android.swipetoloadlayout.trigger.SwipeTrigger
import kotlin.math.abs

/**
 * Created on 2020/2/26
 *
 * @author Garrett Xu (hiyjeain@hotmail.com) of YILU Tech Studio (yilu_tech@outlook.com)
 */
open class SwipeToLoadLayout : @JvmOverloads ViewGroup {

    companion object {
        @JvmStatic
        private val TAG = SwipeToLoadLayout::class.java.simpleName
        private const val DEFAULT_SWIPING_TO_REFRESH_TO_DEFAULT_SCROLLING_DURATION = 200
        private const val DEFAULT_RELEASE_TO_REFRESHING_SCROLLING_DURATION = 200
        private const val DEFAULT_REFRESH_COMPLETE_DELAY_DURATION = 300
        private const val DEFAULT_REFRESH_COMPLETE_TO_DEFAULT_SCROLLING_DURATION = 500
        private const val DEFAULT_DEFAULT_TO_REFRESHING_SCROLLING_DURATION = 500
        private const val DEFAULT_SWIPING_TO_LOAD_MORE_TO_DEFAULT_SCROLLING_DURATION = 200
        private const val DEFAULT_RELEASE_TO_LOADING_MORE_SCROLLING_DURATION = 200
        private const val DEFAULT_LOAD_MORE_COMPLETE_DELAY_DURATION = 300
        private const val DEFAULT_LOAD_MORE_COMPLETE_TO_DEFAULT_SCROLLING_DURATION = 300
        private const val DEFAULT_DEFAULT_TO_LOADING_MORE_SCROLLING_DURATION = 300
        private const val DEFAULT_DRAG_RATIO = 0.5f
        private const val INVALID_POINTER = -1
        private const val INVALID_COORDINATE = -1
    }

    private var mAutoScroller: AutoScroller? = null

    var onRefreshListener: OnRefreshListener? = null

    var onLoadMoreListener: OnLoadMoreListener? = null

    private var _headerView: View? = null
    var headerView: View?
        set(value) {
//            if (value is SwipeRefreshTrigger) {
            if (_headerView != null && value !== _headerView) {
                removeView(_headerView)
            }
            if (_headerView !== value) {
                _headerView = value
                addView(value)
            }
//            } else {
//                Log.e(TAG, "Refresh header view must be an implement of SwipeRefreshTrigger")
//            }
        }
        get() = _headerView

    private var mTargetView: View? = null

    private var _footerView: View? = null
    var footerView: View?
        set(value) {
//            if (value is SwipeLoadMoreTrigger) {
            if (_footerView != null && value !== _footerView) {
                removeView(_footerView)
            }
            if (value !== _footerView) {
                _footerView = value
                addView(_footerView)
            }
//            } else {
//                Log.e(TAG, "Load more footer view must be an implement of SwipeLoadTrigger")
//            }
        }
        get() = _footerView

    private var mHeaderHeight = 0

    private var mFooterHeight = 0

    private var mHasHeaderView = false

    private var mHasFooterView = false

    /**
     * indicate whether in debug mode
     */
    var debug = false

    var dragRatio: Float = DEFAULT_DRAG_RATIO

    private var mAutoLoading = false

    /**
     * the threshold of the touch event
     */
    private var mTouchSlop = 0

    /**
     * status of SwipeToLoadLayout
     */
    private var mStatus = STATUS.STATUS_DEFAULT
        set(value) {
            field = value
            if (debug) {
                STATUS.printStatus(value)
            }
        }

    /**
     * target view top offset
     */
    private var mHeaderOffset: Int = 0

    /**
     * target offset
     */
    private var mTargetOffset: Int = 0

    /**
     * target view bottom offset
     */
    private var mFooterOffset: Int = 0

    /**
     * init touch action down point.y
     */
    private var mInitDownY = 0f

    /**
     * init touch action down point.x
     */
    private var mInitDownX = 0f

    /**
     * last touch point.y
     */
    private var mLastY = 0f

    /**
     * last touch point.x
     */
    private var mLastX = 0f

    /**
     * action touch pointer's id
     */
    private var mActivePointerId = 0

    /**
     * **ATTRIBUTE:**
     * a switcher indicate whither refresh function is enabled
     */
    var isRefreshEnabled = true

    /**
     * **ATTRIBUTE:**
     * a switcher indicate whiter load more function is enabled
     */
    var isLoadMoreEnabled = true

    var isRefreshing: Boolean
        set(value) {
            if (!isRefreshEnabled || headerView == null) {
                return
            }
            mAutoLoading = value
            if (value) {
                if (STATUS.isStatusDefault(mStatus)) {
                    mStatus = STATUS.STATUS_SWIPING_TO_REFRESH
                    scrollDefaultToRefreshing()
                }
            } else {
                if (STATUS.isRefreshing(mStatus)) {
                    refreshCallback.onComplete()
                    postDelayed({ scrollRefreshingToDefault() }, refreshCompleteDelayDuration.toLong())
                }
            }
        }
        get() = STATUS.isRefreshing(mStatus)

    var isLoadingMore: Boolean
        set(value) {
            if (!isLoadMoreEnabled || footerView == null) {
                return
            }
            mAutoLoading = value
            if (value) {
                if (STATUS.isStatusDefault(mStatus)) {
                    mStatus = STATUS.STATUS_SWIPING_TO_LOAD_MORE
                    scrollDefaultToLoadingMore()
                }
            } else {
                if (STATUS.isLoadingMore(mStatus)) {
                    loadMoreCallback.onComplete()
                    postDelayed({ scrollLoadingMoreToDefault() }, loadMoreCompleteDelayDuration.toLong())
                }
            }
        }
        get() = STATUS.isLoadingMore(mStatus)

    /**
     * **ATTRIBUTE:**
     * the style default classic
     */
    var style = STYLE.CLASSIC
        set(value) {
            field = value
            requestLayout()
        }

    /**
     * **ATTRIBUTE:**
     * offset to trigger refresh
     */
    var refreshTriggerOffset: Int = 0

    /**
     * **ATTRIBUTE:**
     * offset to trigger load more
     */
    var loadMoreTriggerOffset: Int = 0

    /**
     * **ATTRIBUTE:**
     * the max value of top offset
     */
    var refreshFinalDragOffset: Float = 0F

    /**
     * **ATTRIBUTE:**
     * the max value of bottom offset
     */
    var loadMoreFinalDragOffset: Float = 0F

    /**
     * **ATTRIBUTE:**
     * Scrolling duration swiping to refresh -> default
     */
    var swipingToRefreshToDefaultScrollingDuration: Int = DEFAULT_SWIPING_TO_REFRESH_TO_DEFAULT_SCROLLING_DURATION

    /**
     * **ATTRIBUTE:**
     * Scrolling duration status release to refresh -> refreshing
     */
    var releaseToRefreshToRefreshingScrollingDuration: Int = DEFAULT_RELEASE_TO_REFRESHING_SCROLLING_DURATION

    /**
     * **ATTRIBUTE:**
     * Refresh complete delay duration
     */
    var refreshCompleteDelayDuration: Int = DEFAULT_REFRESH_COMPLETE_DELAY_DURATION

    /**
     * **ATTRIBUTE:**
     * Scrolling duration status refresh complete -> default
     * [.setRefreshing] false
     */
    var refreshCompleteToDefaultScrollingDuration: Int = DEFAULT_REFRESH_COMPLETE_TO_DEFAULT_SCROLLING_DURATION

    /**
     * **ATTRIBUTE:**
     * Scrolling duration status default -> refreshing, mainly for auto refresh
     * [.setRefreshing] true
     */
    var defaultToRefreshingScrollingDuration: Int = DEFAULT_DEFAULT_TO_REFRESHING_SCROLLING_DURATION

    /**
     * **ATTRIBUTE:**
     * Scrolling duration status release to loading more -> loading more
     */
    var releaseToLoadMoreToLoadingMoreScrollingDuration: Int = DEFAULT_RELEASE_TO_LOADING_MORE_SCROLLING_DURATION


    /**
     * **ATTRIBUTE:**
     * Load more complete delay duration
     */
    var loadMoreCompleteDelayDuration: Int = DEFAULT_LOAD_MORE_COMPLETE_DELAY_DURATION

    /**
     * **ATTRIBUTE:**
     * Scrolling duration status load more complete -> default
     * [.setLoadingMore] false
     */
    var loadMoreCompleteToDefaultScrollingDuration: Int = DEFAULT_LOAD_MORE_COMPLETE_TO_DEFAULT_SCROLLING_DURATION

    /**
     * **ATTRIBUTE:**
     * Scrolling duration swiping to load more -> default
     */
    var swipingToLoadMoreToDefaultScrollingDuration: Int = DEFAULT_SWIPING_TO_LOAD_MORE_TO_DEFAULT_SCROLLING_DURATION

    /**
     * **ATTRIBUTE:**
     * Scrolling duration status default -> loading more, mainly for auto load more
     * [.setLoadingMore] true
     */
    var defaultToLoadingMoreScrollingDuration: Int = DEFAULT_DEFAULT_TO_LOADING_MORE_SCROLLING_DURATION

    /**
     * the style enum
     */
    object STYLE {
        const val CLASSIC = 0
        const val ABOVE = 1
        const val BLEW = 2
        const val SCALE = 3
    }


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SwipeToLoadLayout, defStyleAttr, 0)
        try {
            val attributeCount: Int = a.indexCount
            for (i in 0 until attributeCount) {
                when (val attr = a.getIndex(i)) {
                    R.styleable.SwipeToLoadLayout_refresh_enabled -> {
                        isRefreshEnabled = a.getBoolean(attr, true)
                    }
                    R.styleable.SwipeToLoadLayout_load_more_enabled -> {
                        isLoadMoreEnabled = a.getBoolean(attr, true)
                    }
                    R.styleable.SwipeToLoadLayout_swipe_style -> {
                        style = a.getInt(attr, STYLE.CLASSIC)
                    }
                    R.styleable.SwipeToLoadLayout_drag_ratio -> {
                        dragRatio = a.getFloat(attr, DEFAULT_DRAG_RATIO)
                    }
                    R.styleable.SwipeToLoadLayout_refresh_final_drag_offset -> {
                        refreshFinalDragOffset = a.getDimensionPixelOffset(attr, 0).toFloat()
                    }
                    R.styleable.SwipeToLoadLayout_load_more_final_drag_offset -> {
                        loadMoreFinalDragOffset = a.getDimensionPixelOffset(attr, 0).toFloat()
                    }
                    R.styleable.SwipeToLoadLayout_refresh_trigger_offset -> {
                        refreshTriggerOffset = a.getDimensionPixelOffset(attr, 0)
                    }
                    R.styleable.SwipeToLoadLayout_load_more_trigger_offset -> {
                        loadMoreTriggerOffset = a.getDimensionPixelOffset(attr, 0)
                    }
                    R.styleable.SwipeToLoadLayout_swiping_to_refresh_to_default_scrolling_duration -> {
                        swipingToRefreshToDefaultScrollingDuration = a.getInt(attr, DEFAULT_SWIPING_TO_REFRESH_TO_DEFAULT_SCROLLING_DURATION)
                    }
                    R.styleable.SwipeToLoadLayout_release_to_refreshing_scrolling_duration -> {
                        releaseToRefreshToRefreshingScrollingDuration = a.getInt(attr, DEFAULT_RELEASE_TO_REFRESHING_SCROLLING_DURATION)
                    }
                    R.styleable.SwipeToLoadLayout_refresh_complete_delay_duration -> {
                        refreshCompleteDelayDuration = a.getInt(attr, DEFAULT_REFRESH_COMPLETE_DELAY_DURATION)
                    }
                    R.styleable.SwipeToLoadLayout_refresh_complete_to_default_scrolling_duration -> {
                        refreshCompleteToDefaultScrollingDuration = a.getInt(attr, DEFAULT_REFRESH_COMPLETE_TO_DEFAULT_SCROLLING_DURATION)
                    }
                    R.styleable.SwipeToLoadLayout_default_to_refreshing_scrolling_duration -> {
                        defaultToRefreshingScrollingDuration = a.getInt(attr, DEFAULT_DEFAULT_TO_REFRESHING_SCROLLING_DURATION)
                    }
                    R.styleable.SwipeToLoadLayout_swiping_to_load_more_to_default_scrolling_duration -> {
                        swipingToLoadMoreToDefaultScrollingDuration = a.getInt(attr, DEFAULT_SWIPING_TO_LOAD_MORE_TO_DEFAULT_SCROLLING_DURATION)
                    }
                    R.styleable.SwipeToLoadLayout_release_to_loading_more_scrolling_duration -> {
                        releaseToLoadMoreToLoadingMoreScrollingDuration = a.getInt(attr, DEFAULT_RELEASE_TO_LOADING_MORE_SCROLLING_DURATION)
                    }
                    R.styleable.SwipeToLoadLayout_load_more_complete_delay_duration -> {
                        loadMoreCompleteDelayDuration = a.getInt(attr, DEFAULT_LOAD_MORE_COMPLETE_DELAY_DURATION)
                    }
                    R.styleable.SwipeToLoadLayout_load_more_complete_to_default_scrolling_duration -> {
                        loadMoreCompleteToDefaultScrollingDuration = a.getInt(attr, DEFAULT_LOAD_MORE_COMPLETE_TO_DEFAULT_SCROLLING_DURATION)
                    }
                    R.styleable.SwipeToLoadLayout_default_to_loading_more_scrolling_duration -> {
                        defaultToLoadingMoreScrollingDuration = a.getInt(attr, DEFAULT_DEFAULT_TO_LOADING_MORE_SCROLLING_DURATION)
                    }
                }
            }
        } finally {
            a.recycle()
        }
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        mAutoScroller = AutoScroller()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        when (childCount) {
            0 -> { // no child return
                return
            }
            in 1..3 -> {
                _headerView = findViewById(R.id.swipe_refresh_header)
                mTargetView = findViewById(R.id.swipe_target)
                _footerView = findViewById(R.id.swipe_load_more_footer)
            }
            else -> { // more than three children: unsupported!
                throw IllegalStateException("Children num must equal or less than 3")
            }
        }
        if (mTargetView == null) {
            return
        }
        if (headerView is SwipeTrigger) {
            headerView?.visibility = View.GONE
        }
        if (footerView is SwipeTrigger) {
            footerView?.visibility = View.GONE
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // header
        headerView?.let {
            val headerView: View = it
            measureChildWithMargins(headerView, widthMeasureSpec, 0, heightMeasureSpec, 0)
            val lp = headerView.layoutParams as MarginLayoutParams
            mHeaderHeight = headerView.measuredHeight + lp.topMargin + lp.bottomMargin
            if (refreshTriggerOffset < mHeaderHeight) {
                refreshTriggerOffset = mHeaderHeight
            }
        }
        // target
        mTargetView?.let {
            val targetView: View = it
            measureChildWithMargins(targetView, widthMeasureSpec, 0, heightMeasureSpec, 0)
        }
        footerView?.let {
            val footerView: View = it
            measureChildWithMargins(footerView, widthMeasureSpec, 0, heightMeasureSpec, 0)
            val lp = footerView.layoutParams as MarginLayoutParams
            mFooterHeight = footerView.measuredHeight + lp.topMargin + lp.bottomMargin
            if (loadMoreTriggerOffset < mFooterHeight) {
                loadMoreTriggerOffset = mFooterHeight
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutChildren()
        mHasHeaderView = headerView != null
        mHasFooterView = footerView != null
    }

    class LayoutParams : MarginLayoutParams {
        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs)
        constructor(width: Int, height: Int) : super(width, height)
        constructor(source: MarginLayoutParams?) : super(source)
        constructor(source: ViewGroup.LayoutParams?) : super(source)
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams? {
        return LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams? {
        return LayoutParams(p)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams? {
        return LayoutParams(context, attrs)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP ->
                // swipeToRefresh -> finger up -> finger down if the status is still swipeToRefresh
                // in onInterceptTouchEvent ACTION_DOWN event will stop the scroller
                // if the event pass to the child view while ACTION_MOVE(condition is false)
                // in onInterceptTouchEvent ACTION_MOVE the ACTION_UP or ACTION_CANCEL will not be
                // passed to onInterceptTouchEvent and onTouchEvent. Instead It will be passed to
                // child view's onTouchEvent. So we must deal this situation in dispatchTouchEvent
                onActivePointerUp()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = event.getPointerId(0)
                mInitDownY = getMotionEventY(event, mActivePointerId).also { mLastY = it }
                mInitDownX = getMotionEventX(event, mActivePointerId).also { mLastX = it }

                // if it isn't an ing status or default status
                if (STATUS.isSwipingToRefresh(mStatus) or
                        STATUS.isSwipingToLoadMore(mStatus) or
                        STATUS.isReleaseToRefresh(mStatus) or
                        STATUS.isReleaseToLoadMore(mStatus)) {
                    // abort autoScrolling, not trigger the method #autoScrollFinished()
                    mAutoScroller?.abortIfRunning()
                    if (debug) {
                        Log.i(TAG, "Another finger down, abort auto scrolling, let the new finger handle")
                    }
                }

                if (STATUS.isSwipingToRefresh(mStatus) or
                        STATUS.isSwipingToLoadMore(mStatus) or
                        STATUS.isReleaseToRefresh(mStatus) or
                        STATUS.isReleaseToLoadMore(mStatus)) {
                    return true
                }
                // let children view handle the ACTION_DOWN;

                // 1. children consumed:
                // if at least one of children onTouchEvent() ACTION_DOWN return true.
                // ACTION_DOWN event will not return to SwipeToLoadLayout#onTouchEvent().
                // but the others action can be handled by SwipeToLoadLayout#onInterceptTouchEvent()

                // 2. children not consumed:
                // if children onTouchEvent() ACTION_DOWN return false.
                // ACTION_DOWN event will return to SwipeToLoadLayout's onTouchEvent().
                // SwipeToLoadLayout#onTouchEvent() ACTION_DOWN return true to consume the ACTION_DOWN event.

                // anyway: handle action down in onInterceptTouchEvent() to init is an good option
            }
            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId == INVALID_POINTER) {
                    return false
                }
                val y: Float = getMotionEventY(event, mActivePointerId)
                val x: Float = getMotionEventX(event, mActivePointerId)
                val yInitDiff = y - mInitDownY
                val xInitDiff = x - mInitDownX
                mLastY = y
                mLastX = x
                val moved = (abs(yInitDiff) > abs(xInitDiff)) && (abs(yInitDiff) > mTouchSlop)

                val triggerCondition = (yInitDiff > 0 && moved && onCheckCanRefresh()) ||  // refresh trigger condition
                        (yInitDiff < 0 && moved && onCheckCanLoadMore()) //load more trigger condition
                if (triggerCondition) {
                    // if the refresh's or load more's trigger condition  is true,
                    // intercept the move action event and pass it to SwipeToLoadLayout#onTouchEvent()
                    return true
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(event)
                mInitDownY = getMotionEventY(event, mActivePointerId).also { mLastY = it }
                mInitDownX = getMotionEventX(event, mActivePointerId).also { mLastX = it }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> mActivePointerId = INVALID_POINTER
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = event.getPointerId(0)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // take over the ACTION_MOVE event from SwipeToLoadLayout#onInterceptTouchEvent()
                // if condition is true
                val y: Float = getMotionEventY(event, mActivePointerId)
                val x: Float = getMotionEventX(event, mActivePointerId)
                val yDiff = y - mLastY
                val xDiff = x - mLastX
                mLastY = y
                mLastX = x

                when {
                    (abs(xDiff) > abs(yDiff)) && (abs(xDiff) > mTouchSlop) -> {
                        return false
                    }
                    STATUS.isStatusDefault(mStatus) && (yDiff > 0 && onCheckCanRefresh()) -> {
                        refreshCallback.onPrepare()
                        mStatus = STATUS.STATUS_SWIPING_TO_REFRESH
                    }

                    STATUS.isStatusDefault(mStatus) && (yDiff < 0 && onCheckCanLoadMore()) -> {
                        loadMoreCallback.onPrepare()
                        mStatus = STATUS.STATUS_SWIPING_TO_LOAD_MORE
                    }
                    STATUS.isRefreshStatus(mStatus) && (mTargetOffset <= 0) -> {
                        mStatus = STATUS.STATUS_DEFAULT
                        fixCurrentStatusLayout()
                        return false
                    }
                    STATUS.isLoadMoreStatus(mStatus) && (mTargetOffset >= 0) -> {
                        mStatus = STATUS.STATUS_DEFAULT
                        fixCurrentStatusLayout()
                        return false
                    }
                }
                when {
                    STATUS.isRefreshStatus(mStatus) && (STATUS.isSwipingToRefresh(mStatus) || STATUS.isReleaseToRefresh(mStatus)) -> {
                        mStatus = if (mTargetOffset >= refreshTriggerOffset) {
                            STATUS.STATUS_RELEASE_TO_REFRESH
                        } else {
                            STATUS.STATUS_SWIPING_TO_REFRESH
                        }
                        fingerScroll(yDiff)
                    }
                    STATUS.isLoadMoreStatus(mStatus) && (STATUS.isSwipingToLoadMore(mStatus) || STATUS.isReleaseToLoadMore(mStatus)) -> {
                        mStatus = if (-mTargetOffset >= loadMoreTriggerOffset) {
                            STATUS.STATUS_RELEASE_TO_LOAD_MORE
                        } else {
                            STATUS.STATUS_SWIPING_TO_LOAD_MORE
                        }
                        fingerScroll(yDiff)
                    }
                }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId != INVALID_POINTER) {
                    mActivePointerId = pointerId
                }
                mInitDownY = getMotionEventY(event, mActivePointerId).also { mLastY = it }
                mInitDownX = getMotionEventX(event, mActivePointerId).also { mLastX = it }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(event)
                mInitDownY = getMotionEventY(event, mActivePointerId).also { mLastY = it }
                mInitDownX = getMotionEventX(event, mActivePointerId).also { mLastX = it }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (mActivePointerId == INVALID_POINTER) {
                    return false
                }
                mActivePointerId = INVALID_POINTER
            }
        }
        return super.onTouchEvent(event)
    }

    protected val canChildScrollUp: Boolean
        get() = mTargetView?.canScrollVertically(-1) ?: false

    protected val canChildScrollDown: Boolean
        get() = mTargetView?.canScrollVertically(1) ?: false


    private fun layoutChildren() {
        val width = measuredWidth
        val height = measuredHeight

        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        val paddingRight = paddingRight
        val paddingBottom = paddingBottom

        if (mTargetView == null) {
            return
        }

        // layout header
        headerView?.let {
            val headerView: View = it
            val lp = headerView.layoutParams as MarginLayoutParams
            val headerLeft = paddingLeft + lp.leftMargin
            val headerTop: Int
            headerTop = when (style) {
                STYLE.CLASSIC ->  // classic
                    paddingTop + lp.topMargin - mHeaderHeight + mHeaderOffset
                STYLE.ABOVE ->  // classic
                    paddingTop + lp.topMargin - mHeaderHeight + mHeaderOffset
                STYLE.BLEW ->  // blew
                    paddingTop + lp.topMargin
                STYLE.SCALE ->  // scale
                    paddingTop + lp.topMargin - mHeaderHeight / 2 + mHeaderOffset / 2
                else ->  // classic
                    paddingTop + lp.topMargin - mHeaderHeight + mHeaderOffset
            }
            val headerRight = headerLeft + headerView.measuredWidth
            val headerBottom = headerTop + headerView.measuredHeight
            headerView.layout(headerLeft, headerTop, headerRight, headerBottom)
        }

        // layout target
        mTargetView?.let {
            val targetView: View = it
            val lp = targetView.layoutParams as MarginLayoutParams
            val targetLeft = paddingLeft + lp.leftMargin
            val targetTop: Int
            targetTop = when (style) {
                STYLE.CLASSIC ->  // classic
                    paddingTop + lp.topMargin + mTargetOffset
                STYLE.ABOVE ->  // above
                    paddingTop + lp.topMargin
                STYLE.BLEW ->  // classic
                    paddingTop + lp.topMargin + mTargetOffset
                STYLE.SCALE ->  // classic
                    paddingTop + lp.topMargin + mTargetOffset
                else ->  // classic
                    paddingTop + lp.topMargin + mTargetOffset
            }
            val targetRight = targetLeft + targetView.measuredWidth
            val targetBottom = targetTop + targetView.measuredHeight
            targetView.layout(targetLeft, targetTop, targetRight, targetBottom)
        }

        // layout footer
        footerView?.let {
            val footerView: View = it
            val lp = footerView.layoutParams as MarginLayoutParams
            val footerLeft = paddingLeft + lp.leftMargin
            val footerBottom: Int
            footerBottom = when (style) {
                STYLE.CLASSIC ->  // classic
                    height - paddingBottom - lp.bottomMargin + mFooterHeight + mFooterOffset
                STYLE.ABOVE ->  // classic
                    height - paddingBottom - lp.bottomMargin + mFooterHeight + mFooterOffset
                STYLE.BLEW ->  // blew
                    height - paddingBottom - lp.bottomMargin
                STYLE.SCALE ->  // scale
                    height - paddingBottom - lp.bottomMargin + mFooterHeight / 2 + mFooterOffset / 2
                else ->  // classic
                    height - paddingBottom - lp.bottomMargin + mFooterHeight + mFooterOffset
            }
            val footerTop = footerBottom - footerView.measuredHeight
            val footerRight = footerLeft + footerView.measuredWidth
            footerView.layout(footerLeft, footerTop, footerRight, footerBottom)
        }

        when (style) {
            STYLE.CLASSIC, STYLE.ABOVE -> {
                if (headerView != null) {
                    headerView!!.bringToFront()
                }
                if (footerView != null) {
                    footerView!!.bringToFront()
                }
            }
            STYLE.BLEW, STYLE.SCALE -> {
                if (mTargetView != null) {
                    mTargetView!!.bringToFront()
                }
            }
        }
    }

    private fun fixCurrentStatusLayout() {
        when {
            STATUS.isRefreshing(mStatus) -> {
                mTargetOffset = ((refreshTriggerOffset + 0.5f).toInt())
                mHeaderOffset = mTargetOffset
                mFooterOffset = 0
                layoutChildren()
                invalidate()
            }
            STATUS.isStatusDefault(mStatus) -> {
                mTargetOffset = 0
                mHeaderOffset = 0
                mFooterOffset = 0
                layoutChildren()
                invalidate()
            }
            STATUS.isLoadingMore(mStatus) -> {
                mTargetOffset = (-(loadMoreTriggerOffset + 0.5f)).toInt()
                mHeaderOffset = 0
                mFooterOffset = mTargetOffset
                layoutChildren()
                invalidate()
            }
        }
    }

    private fun fingerScroll(yDiff: Float) {
        val ratio: Float = dragRatio
        var yScrolled = yDiff * ratio
        // make sure (targetOffset>0 -> targetOffset=0 -> default status)
        // or (targetOffset<0 -> targetOffset=0 -> default status)
        // forbidden fling (targetOffset>0 -> targetOffset=0 ->targetOffset<0 -> default status)
        // or (targetOffset<0 -> targetOffset=0 ->targetOffset>0 -> default status)
        // I am so smart :)
        val tmpTargetOffset = yScrolled + mTargetOffset
        if (tmpTargetOffset > 0 && mTargetOffset < 0 || tmpTargetOffset < 0 && mTargetOffset > 0) {
            yScrolled = -mTargetOffset.toFloat()
        }
        if (refreshFinalDragOffset >= refreshTriggerOffset && tmpTargetOffset > refreshFinalDragOffset) {
            yScrolled = refreshFinalDragOffset - mTargetOffset
        } else if (loadMoreFinalDragOffset >= loadMoreTriggerOffset && -tmpTargetOffset > loadMoreFinalDragOffset) {
            yScrolled = -loadMoreFinalDragOffset - mTargetOffset
        }
        if (STATUS.isRefreshStatus(mStatus)) {
            refreshCallback.onMove(mTargetOffset, isComplete = false, automatic = false)
        } else if (STATUS.isLoadMoreStatus(mStatus)) {
            loadMoreCallback.onMove(mTargetOffset, isComplete = false, automatic = false)
        }
        updateScroll(yScrolled)
    }

    private fun autoScroll(yScrolled: Float) {
        when {
            STATUS.isSwipingToRefresh(mStatus) -> {
                refreshCallback.onMove(mTargetOffset, isComplete = false, automatic = true)
            }
            STATUS.isReleaseToRefresh(mStatus) -> {
                refreshCallback.onMove(mTargetOffset, isComplete = false, automatic = true)
            }
            STATUS.isRefreshing(mStatus) -> {
                refreshCallback.onMove(mTargetOffset, isComplete = true, automatic = true)
            }
            STATUS.isSwipingToLoadMore(mStatus) -> {
                loadMoreCallback.onMove(mTargetOffset, isComplete = false, automatic = true)
            }
            STATUS.isReleaseToLoadMore(mStatus) -> {
                loadMoreCallback.onMove(mTargetOffset, isComplete = false, automatic = true)
            }
            STATUS.isLoadingMore(mStatus) -> {
                loadMoreCallback.onMove(mTargetOffset, isComplete = true, automatic = true)
            }
        }
        updateScroll(yScrolled)
    }

    private fun updateScroll(yScrolled: Float) {
        if (yScrolled == 0f) {
            return
        }
        mTargetOffset += yScrolled.toInt()
        if (STATUS.isRefreshStatus(mStatus)) {
            mHeaderOffset = mTargetOffset
            mFooterOffset = 0
        } else if (STATUS.isLoadMoreStatus(mStatus)) {
            mFooterOffset = mTargetOffset
            mHeaderOffset = 0
        }
        if (debug) {
            Log.i(TAG, "mTargetOffset = $mTargetOffset")
        }
        layoutChildren()
        invalidate()
    }

    private fun onActivePointerUp() {
        when {
            STATUS.isSwipingToRefresh(mStatus) -> { // simply return
                scrollSwipingToRefreshToDefault()
            }
            STATUS.isSwipingToLoadMore(mStatus) -> { // simply return
                scrollSwipingToLoadMoreToDefault()
            }
            STATUS.isReleaseToRefresh(mStatus) -> { // return to header height and perform refresh
                refreshCallback.onRelease()
                scrollReleaseToRefreshToRefreshing()
            }
            STATUS.isReleaseToLoadMore(mStatus) -> { // return to footer height and perform loadMore
                loadMoreCallback.onRelease()
                scrollReleaseToLoadMoreToLoadingMore()
            }
        }
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = ev.actionIndex
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mActivePointerId = ev.getPointerId(newPointerIndex)
        }
    }

    private fun scrollDefaultToRefreshing() {
        mAutoScroller?.autoScroll((refreshTriggerOffset + 0.5f).toInt(), defaultToRefreshingScrollingDuration)
    }

    private fun scrollDefaultToLoadingMore() {
        mAutoScroller?.autoScroll((-(loadMoreTriggerOffset + 0.5f)).toInt(), defaultToLoadingMoreScrollingDuration)
    }

    private fun scrollSwipingToRefreshToDefault() {
        mAutoScroller?.autoScroll(-mHeaderOffset, swipingToRefreshToDefaultScrollingDuration)
    }

    private fun scrollSwipingToLoadMoreToDefault() {
        mAutoScroller?.autoScroll(-mFooterOffset, swipingToLoadMoreToDefaultScrollingDuration)
    }

    private fun scrollReleaseToRefreshToRefreshing() {
        mAutoScroller?.autoScroll(mHeaderHeight - mHeaderOffset, releaseToRefreshToRefreshingScrollingDuration)
    }

    private fun scrollReleaseToLoadMoreToLoadingMore() {
        mAutoScroller?.autoScroll(-mFooterOffset - mFooterHeight, releaseToLoadMoreToLoadingMoreScrollingDuration)
    }

    private fun scrollRefreshingToDefault() {
        mAutoScroller?.autoScroll(-mHeaderOffset, refreshCompleteToDefaultScrollingDuration)
    }

    private fun scrollLoadingMoreToDefault() {
        mAutoScroller?.autoScroll(-mFooterOffset, loadMoreCompleteToDefaultScrollingDuration)
    }

    private fun autoScrollFinished() {
        val mLastStatus = mStatus
        when {
            STATUS.isReleaseToRefresh(mStatus) -> {
                mStatus = STATUS.STATUS_REFRESHING
                fixCurrentStatusLayout()
                refreshCallback.onRefresh()
            }
            STATUS.isRefreshing(mStatus) -> {
                mStatus = STATUS.STATUS_DEFAULT
                fixCurrentStatusLayout()
                refreshCallback.onReset()
            }
            STATUS.isSwipingToRefresh(mStatus) -> if (mAutoLoading) {
                mAutoLoading = false
                mStatus = STATUS.STATUS_REFRESHING
                fixCurrentStatusLayout()
                refreshCallback.onRefresh()
            } else {
                mStatus = STATUS.STATUS_DEFAULT
                fixCurrentStatusLayout()
                refreshCallback.onReset()
            }
            STATUS.isStatusDefault(mStatus) -> {
            }
            STATUS.isSwipingToLoadMore(mStatus) -> if (mAutoLoading) {
                mAutoLoading = false
                mStatus = STATUS.STATUS_LOADING_MORE
                fixCurrentStatusLayout()
                loadMoreCallback.onLoadMore()
            } else {
                mStatus = STATUS.STATUS_DEFAULT
                fixCurrentStatusLayout()
                loadMoreCallback.onReset()
            }
            STATUS.isLoadingMore(mStatus) -> {
                mStatus = STATUS.STATUS_DEFAULT
                fixCurrentStatusLayout()
                loadMoreCallback.onReset()
            }
            STATUS.isReleaseToLoadMore(mStatus) -> {
                mStatus = STATUS.STATUS_LOADING_MORE
                fixCurrentStatusLayout()
                loadMoreCallback.onLoadMore()
            }
            else -> {
                throw java.lang.IllegalStateException("illegal state: " + STATUS.getStatus(mStatus))
            }
        }
        if (debug) {
            Log.i(TAG, STATUS.getStatus(mLastStatus) + " -> " + STATUS.getStatus(mStatus))
        }
    }

    private fun onCheckCanRefresh(): Boolean {
        return isRefreshEnabled && !canChildScrollUp && mHasHeaderView && refreshTriggerOffset > 0
    }

    private fun onCheckCanLoadMore(): Boolean {
        return isLoadMoreEnabled && !canChildScrollDown && mHasFooterView && loadMoreTriggerOffset > 0
    }

    private fun getMotionEventY(event: MotionEvent, activePointerId: Int): Float {
        val index = event.findPointerIndex(activePointerId)
        return if (index < 0) {
            INVALID_COORDINATE.toFloat()
        } else event.getY(index)
    }

    private fun getMotionEventX(event: MotionEvent, activePointId: Int): Float {
        val index = event.findPointerIndex(activePointId)
        return if (index < 0) {
            INVALID_COORDINATE.toFloat()
        } else event.getX(index)
    }

    private var refreshCallback: RefreshCallback = object : RefreshCallback() {
        override fun onPrepare() {
            if (headerView != null && headerView is SwipeTrigger && STATUS.isStatusDefault(mStatus)) {
                headerView!!.visibility = View.VISIBLE
                (headerView as SwipeTrigger).onPrepare()
            }
        }

        override fun onMove(y: Int, isComplete: Boolean, automatic: Boolean) {
            if (headerView != null && headerView is SwipeTrigger && STATUS.isRefreshStatus(mStatus)) {
                if (headerView!!.visibility != View.VISIBLE) {
                    headerView!!.visibility = View.VISIBLE
                }
                (headerView as SwipeTrigger).onMove(y, isComplete, automatic)
            }
        }

        override fun onRelease() {
            if (headerView != null && headerView is SwipeTrigger && STATUS.isReleaseToRefresh(mStatus)) {
                (headerView as SwipeTrigger).onRelease()
            }
        }

        override fun onRefresh() {
            if (headerView != null && STATUS.isRefreshing(mStatus)) {
                if (headerView is SwipeRefreshTrigger) {
                    (headerView as SwipeRefreshTrigger).onRefresh()
                }
                onRefreshListener?.onRefresh()
            }
        }

        override fun onComplete() {
            if (headerView != null && headerView is SwipeTrigger) {
                (headerView as SwipeTrigger).onComplete()
            }
        }

        override fun onReset() {
            if (headerView != null && headerView is SwipeTrigger && STATUS.isStatusDefault(mStatus)) {
                (headerView as SwipeTrigger).onReset()
                headerView!!.visibility = View.GONE
            }
        }
    }

    private var loadMoreCallback: LoadMoreCallback = object : LoadMoreCallback() {
        override fun onPrepare() {
            if (footerView != null && footerView is SwipeTrigger && STATUS.isStatusDefault(mStatus)) {
                footerView!!.visibility = View.VISIBLE
                (footerView as SwipeTrigger).onPrepare()
            }
        }

        override fun onMove(y: Int, isComplete: Boolean, automatic: Boolean) {
            if (footerView != null && footerView is SwipeTrigger && STATUS.isLoadMoreStatus(mStatus)) {
                if (footerView!!.visibility != View.VISIBLE) {
                    footerView!!.visibility = View.VISIBLE
                }
                (footerView as SwipeTrigger).onMove(y, isComplete, automatic)
            }
        }

        override fun onRelease() {
            if (footerView != null && footerView is SwipeTrigger && STATUS.isReleaseToLoadMore(mStatus)) {
                (footerView as SwipeTrigger).onRelease()
            }
        }

        override fun onLoadMore() {
            if (footerView != null && STATUS.isLoadingMore(mStatus)) {
                if (footerView is SwipeLoadMoreTrigger) {
                    (footerView as SwipeLoadMoreTrigger).onLoadMore()
                }
                onLoadMoreListener?.onLoadMore()
            }
        }

        override fun onComplete() {
            if (footerView != null && footerView is SwipeTrigger) {
                (footerView as SwipeTrigger).onComplete()
            }
        }

        override fun onReset() {
            if (footerView != null && footerView is SwipeTrigger && STATUS.isStatusDefault(mStatus)) {
                (footerView as SwipeTrigger).onReset()
                footerView!!.visibility = View.GONE
            }
        }
    }

    internal abstract class RefreshCallback : SwipeTrigger, SwipeRefreshTrigger

    internal abstract class LoadMoreCallback : SwipeTrigger, SwipeLoadMoreTrigger

    private inner class AutoScroller : Runnable {
        private val mScroller: Scroller = Scroller(this@SwipeToLoadLayout.context)
        private var mmLastY = 0
        private var mRunning = false
        private var mAbort = false
        override fun run() {
            val finish = !mScroller.computeScrollOffset() || mScroller.isFinished
            val currY = mScroller.currY
            val yDiff = currY - mmLastY
            if (finish) {
                finish()
            } else {
                mmLastY = currY
                this@SwipeToLoadLayout.autoScroll(yDiff.toFloat())
                this@SwipeToLoadLayout.post(this)
            }
        }

        /**
         * remove the post callbacks and reset default values
         */
        private fun finish() {
            mmLastY = 0
            mRunning = false
            this@SwipeToLoadLayout.removeCallbacks(this)
            // if abort by user, don't call
            if (!mAbort) {
                this@SwipeToLoadLayout.autoScrollFinished()
            }
        }

        /**
         * abort scroll if it is scrolling
         */
        fun abortIfRunning() {
            if (mRunning) {
                if (!mScroller.isFinished) {
                    mAbort = true
                    mScroller.forceFinished(true)
                }
                finish()
                mAbort = false
            }
        }

        /**
         * The param yScrolled here isn't final pos of y.
         * It's just like the yScrolled param in the
         * [.updateScroll]
         *
         * @param yScrolled
         * @param duration
         */
        internal fun autoScroll(yScrolled: Int, duration: Int) {
            this@SwipeToLoadLayout.removeCallbacks(this)
            mmLastY = 0
            if (!mScroller.isFinished) {
                mScroller.forceFinished(true)
            }
            mScroller.startScroll(0, 0, 0, yScrolled, duration)
            this@SwipeToLoadLayout.post(this)
            mRunning = true
        }

    }

    private object STATUS {
        internal const val STATUS_REFRESH_RETURNING = -4
        internal const val STATUS_REFRESHING = -3
        internal const val STATUS_RELEASE_TO_REFRESH = -2
        internal const val STATUS_SWIPING_TO_REFRESH = -1
        internal const val STATUS_DEFAULT = 0
        internal const val STATUS_SWIPING_TO_LOAD_MORE = 1
        internal const val STATUS_RELEASE_TO_LOAD_MORE = 2
        internal const val STATUS_LOADING_MORE = 3
        internal const val STATUS_LOAD_MORE_RETURNING = 4
        internal fun isRefreshing(status: Int): Boolean {
            return status == STATUS_REFRESHING
        }

        internal fun isLoadingMore(status: Int): Boolean {
            return status == STATUS_LOADING_MORE
        }

        internal fun isReleaseToRefresh(status: Int): Boolean {
            return status == STATUS_RELEASE_TO_REFRESH
        }

        internal fun isReleaseToLoadMore(status: Int): Boolean {
            return status == STATUS_RELEASE_TO_LOAD_MORE
        }

        internal fun isSwipingToRefresh(status: Int): Boolean {
            return status == STATUS_SWIPING_TO_REFRESH
        }

        internal fun isSwipingToLoadMore(status: Int): Boolean {
            return status == STATUS_SWIPING_TO_LOAD_MORE
        }

        internal fun isRefreshStatus(status: Int): Boolean {
            return status < STATUS_DEFAULT
        }

        internal fun isLoadMoreStatus(status: Int): Boolean {
            return status > STATUS_DEFAULT
        }

        internal fun isStatusDefault(status: Int): Boolean {
            return status == STATUS_DEFAULT
        }

        internal fun getStatus(status: Int): String {
            return when (status) {
                STATUS_REFRESH_RETURNING -> "status_refresh_returning"
                STATUS_REFRESHING -> "status_refreshing"
                STATUS_RELEASE_TO_REFRESH -> "status_release_to_refresh"
                STATUS_SWIPING_TO_REFRESH -> "status_swiping_to_refresh"
                STATUS_DEFAULT -> "status_default"
                STATUS_SWIPING_TO_LOAD_MORE -> "status_swiping_to_load_more"
                STATUS_RELEASE_TO_LOAD_MORE -> "status_release_to_load_more"
                STATUS_LOADING_MORE -> "status_loading_more"
                STATUS_LOAD_MORE_RETURNING -> "status_load_more_returning"
                else -> "status_illegal!"
            }
        }

        internal fun printStatus(status: Int) {
            Log.i(TAG, "printStatus:" + getStatus(status))
        }
    }
}
