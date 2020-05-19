package com.andreyaleev.bottomsheet

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING

/**
 * Create Bottom Sheet View on any screen
 *
 * View is based on[androidx.coordinatorlayout.widget.CoordinatorLayout]
 * Parent view MUST be full screen (!) for blackout to work correct
 * Also, this view should be embedded into [FrameLayout], [RelativeLayout], [ConstraintLayout] or other,
 * where it can be shown on top of all other views
 *
 * The view has header, content and footer as [LinearLayout]s, each visible if not null
 * Content view LinearLayout is additionally wrapped with [NestedScrollView] by [nestedScrollViewForContent] flag
 *
 * This view encapsulates [BottomSheetBehavior], but you may obtain [BottomSheetBehavior] instance by calling [getBehaviour]
 *
 * Created by Andrey Aleev on 2019-10-10.
 */
class BottomSheetBehaviourView : CoordinatorLayout {

    private var blackoutOnExpand: Boolean = true
    private var showHeaderThumb: Boolean = true
    private var isHideable: Boolean = true

    private var isHeaderHideable: Boolean = false
    private var defaultHidden: Boolean = false
    private var peekHeightByHeader: Boolean = false
    private var nestedScrollViewForContent: Boolean = true
    private var maxBottomSheetHeight: Float = DEFAULT_MAX_HEIGHT
    private var peekHeight: Int = DEFAULT_PEEK_HEIGHT
    private var headerLayoutID: Int = -1
    private var headerAdditionalLayoutID: Int = -1
    private var contentLayoutID: Int = -1
    private var footerLayoutID: Int = -1
    private var isDisabledSwipeDismiss: Boolean = false

    /**
     * Skip collapsed state to hidden
     * NB! use in combination with hideOnOutsideClick = true
     */
    private var skipCollapsed: Boolean = false


    /** [hideOnOutsideClick] has priority over [collapseOnOutsideClick] */
    private var collapseOnOutsideClick: Boolean = true
    private var hideOnOutsideClick: Boolean = false

    private var currentExpandedState: Int = BottomSheetBehavior.STATE_COLLAPSED

    private var showBlackOutAnimator: ViewPropertyAnimator? = null
    private var hideBlackOutAnimator: ViewPropertyAnimator? = null
    private lateinit var flBlackout: FrameLayout
    private lateinit var llHeader: LinearLayout
    private lateinit var llHeaderContent: LinearLayout
    private var nsvContent: NestedScrollView? = null
    private lateinit var llContent: LinearLayout
    private var llHeaderAdditional: LinearLayout? = null
    private lateinit var llFooter: LinearLayout
    private lateinit var vBottomSheetThumb: View
    private lateinit var clBottomSheet: ConstraintLayout

    private var mBottomSheetBehavior: BottomSheetBehavior<View>? = null

    private var onContentLayoutListener =
        ViewTreeObserver.OnGlobalLayoutListener { updateMaxHeightLayoutParams() }

    fun getBehaviour(): BottomSheetBehavior<View>? {
        return mBottomSheetBehavior
    }

    fun isExpanded() = mBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED

    fun toggle() {
        if (mBottomSheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
            expand()
        } else {
            collapse()
        }
    }

    fun expand() {
        // showBlackout before expanding
        if (blackoutOnExpand) {
            showBlackout()
        }
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun collapse() {
        if (skipCollapsed) {
            hide(true)
        } else {
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    fun hide(forced: Boolean = true) {
        if (forced) {
            setHideable(true)
        }
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN

        if (isHideable.not()) {
            collapse()
        }
    }

    private fun setHideable(hideable: Boolean) {
        this.isHideable = hideable
        mBottomSheetBehavior?.isHideable = hideable
    }

    fun setHideOnOutsideClick(hideOnOutsideClick: Boolean) {
        this.hideOnOutsideClick = hideOnOutsideClick
        invalidateBehavior()
    }

    fun setCollapseOnOutsideClick(collapseOnOutsideClick: Boolean) {
        this.collapseOnOutsideClick = collapseOnOutsideClick
        invalidateBehavior()
    }

    /**
     * Set maximum BottomSheetHeight
     * maxHeight: value from 0..1, where screen height is counts as 1
     */
    fun setMaxBottomSheetHeight(maxHeight: Float) {
        if (maxHeight > 1 || maxHeight < 0) {
            this.maxBottomSheetHeight = DEFAULT_MAX_HEIGHT
        } else {
            this.maxBottomSheetHeight = maxHeight
        }
        invalidateBehavior()
    }

    fun setShowHeaderThumb(showThumb: Boolean) {
        this.showHeaderThumb = showThumb
        invalidateBehavior()
    }

    fun setHeaderView(headerView: View?) {
        llHeaderContent.removeAllViews()
        if (headerView != null) {
            llHeaderContent.addView(headerView)
            llHeaderContent.visibility = View.VISIBLE
        } else {
            headerLayoutID = -1
            llHeaderContent.visibility = View.GONE
        }
        invalidateBehavior()
    }

    fun setFooterView(footerView: View?) {
        llFooter.removeAllViews()
        if (footerView != null) {
            llFooter.addView(footerView)
            llFooter.visibility = View.VISIBLE
        } else {
            footerLayoutID = -1
            llFooter.visibility = View.GONE
        }

        invalidateBehavior()
    }

    fun setContentView(contentView: View?) {
        llContent.removeAllViews()
        if (contentView != null) {
            llContent.visibility = View.VISIBLE
            llContent.addView(contentView)
        } else {
            contentLayoutID = -1
            llContent.visibility = View.GONE
        }
        invalidateBehavior()
    }

    fun setHeaderAdditionalLayout(@LayoutRes layoutID: Int) {
        this.headerAdditionalLayoutID = layoutID

        val newHeaderView = inflate(context, layoutID, null)
        llHeaderAdditional?.removeAllViews()
        llHeaderAdditional?.addView(newHeaderView)
        llHeader.visibility = View.VISIBLE
        invalidateBehavior()
    }

    fun setHeaderlayout(@LayoutRes headerLayoutID: Int) {
        this.headerLayoutID = headerLayoutID

        val newHeaderView = inflate(context, headerLayoutID, null)
        llHeaderContent.removeAllViews()
        llHeaderContent.addView(newHeaderView)
        llHeader.visibility = View.VISIBLE
        invalidateBehavior()
    }

    fun setContentlayout(@LayoutRes contentLayoutID: Int) {
        this.contentLayoutID = contentLayoutID

        val contentView = inflate(context, contentLayoutID, null)
        nsvContent?.visibility = View.VISIBLE
        llContent.removeAllViews()
        llContent.addView(contentView)
        llContent.visibility = View.VISIBLE
        invalidateBehavior()
    }

    fun setFooterlayout(@LayoutRes footerLayoutID: Int) {
        this.footerLayoutID = footerLayoutID

        val footerView = inflate(context, footerLayoutID, null)
        llFooter.removeAllViews()
        llFooter.addView(footerView)
        llFooter.visibility = View.VISIBLE
        invalidateBehavior()
    }

    fun setFooterVisible(visible: Boolean) {
        llFooter.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
        invalidateBehavior()
    }

    private fun getMaxAllowedHeightInPxs(): Float {
        return getScreenHeight(context) * maxBottomSheetHeight
    }

    /**
     * constructor for instantiating from xml
     */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {

        val a = context.obtainStyledAttributes(
            attrs, R.styleable.BottomSheetBehaviourView, 0, 0
        )
        nestedScrollViewForContent = a.getBoolean(
            R.styleable.BottomSheetBehaviourView_nestedScrollViewForContent,
            true
        )
        isHideable = a.getBoolean(
            R.styleable.BottomSheetBehaviourView_isHideable,
            true
        )
        showHeaderThumb = a.getBoolean(
            R.styleable.BottomSheetBehaviourView_showHeaderThumb,
            true
        )
        blackoutOnExpand = a.getBoolean(
            R.styleable.BottomSheetBehaviourView_blackoutOnExpand,
            true
        )
        defaultHidden = a.getBoolean(
            R.styleable.BottomSheetBehaviourView_defaultHidden,
            false
        )
        peekHeightByHeader =
            a.getBoolean(
                R.styleable.BottomSheetBehaviourView_peekHeightByHeader,
                true
            )
        maxBottomSheetHeight =
            a.getFloat(
                R.styleable.BottomSheetBehaviourView_maxBottomSheetHeight,
                DEFAULT_MAX_HEIGHT
            )
        peekHeight = a.getInt(
            R.styleable.BottomSheetBehaviourView_peekHeight,
            DEFAULT_PEEK_HEIGHT
        )

        headerLayoutID = a.getResourceId(
            R.styleable.BottomSheetBehaviourView_headerLayoutID,
            -1
        )
        contentLayoutID = a.getResourceId(
            R.styleable.BottomSheetBehaviourView_contentLayoutID,
            -1
        )
        footerLayoutID = a.getResourceId(
            R.styleable.BottomSheetBehaviourView_footerLayoutID,
            -1
        )
        isHeaderHideable = a.getBoolean(
            R.styleable.BottomSheetBehaviourView_isHeaderHideable, false
        )
        collapseOnOutsideClick = a.getBoolean(
            R.styleable.BottomSheetBehaviourView_collapseOnOutsideClick, true
        )
        hideOnOutsideClick = a.getBoolean(
            R.styleable.BottomSheetBehaviourView_hideOnOutsideClick, false
        )
        isDisabledSwipeDismiss = a.getBoolean(
            R.styleable.BottomSheetBehaviourView_isDisabledSwipeDismiss, false
        )
        skipCollapsed = a.getBoolean(
            R.styleable.BottomSheetBehaviourView_aflSkipCollapsed,
            false
        )
        a.recycle()
        initViews()
    }

    /**
     * constructor for creating instance programmatically
     *
     * call example:
     *
     * val mBottomSheet = BottomSheetBehaviourView(
    context,
    maxBottomSheetHeight = 0.75f,
    headerLayoutID = R.layout.layout_header,
    contentLayoutID = R.layout.layout_content
    )

    Important! Add LayoutParams.MATCH_PARENT when adding programmatically
    mBottomSheet.layoutParams =
    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    root.addView(mBottomSheet)

    Important! view instance for views have priority over @LayoutRes layout ids
     */
    constructor(
        context: Context,
        defaultHidden: Boolean = false,
        isHideable: Boolean = true,
        nestedScrollViewForContent: Boolean = true,
        blackoutOnExpand: Boolean = true,
        showHeaderThumb: Boolean = true,
        peekHeightByHeader: Boolean = true,
        maxBottomSheetHeight: Float = DEFAULT_MAX_HEIGHT,
        peekHeight: Int = DEFAULT_PEEK_HEIGHT,
        @LayoutRes headerLayoutID: Int = -1,
        headerLayoutView: View? = null,
        @LayoutRes contentLayoutID: Int = -1,
        contentLayoutView: View? = null,
        @LayoutRes footerLayoutID: Int = -1,
        footerLayoutView: View? = null,
        isHeaderHideable: Boolean = false,
        collapseOnOutsideClick: Boolean = true,
        hideOnOutsideClick: Boolean = false,
        skipCollapsed: Boolean = false
    ) : super(context) {
        this.nestedScrollViewForContent = nestedScrollViewForContent
        this.isHideable = isHideable
        this.defaultHidden = defaultHidden
        this.blackoutOnExpand = blackoutOnExpand
        this.showHeaderThumb = showHeaderThumb
        this.peekHeightByHeader = peekHeightByHeader
        this.maxBottomSheetHeight = maxBottomSheetHeight
        this.peekHeight = peekHeight
        this.headerLayoutID = headerLayoutID
        this.contentLayoutID = contentLayoutID
        this.footerLayoutID = footerLayoutID
        this.isHeaderHideable = isHeaderHideable
        this.collapseOnOutsideClick = collapseOnOutsideClick
        this.hideOnOutsideClick = hideOnOutsideClick
        this.skipCollapsed = skipCollapsed
        initViews(headerLayoutView, contentLayoutView, footerLayoutView)
    }


    /**
     * Views initializing. Should be called only once from constructor
     */
    private fun initViews(
        headerLayoutView: View? = null,
        contentLayoutView: View? = null,
        footerLayoutView: View? = null
    ) {

        val inflatedView = if (nestedScrollViewForContent) {
            inflate(context, R.layout.bottom_sheet_behavior_view, this)
        } else {
            inflate(context, R.layout.bottom_sheet_behavior_view_without_nsv, this)
        }
        flBlackout = inflatedView.findViewById(R.id.flBlackout)
        llHeader = inflatedView.findViewById(R.id.llHeader)
        llHeaderContent = inflatedView.findViewById(R.id.llHeaderContent)
        llHeaderAdditional = inflatedView.findViewById(R.id.llHeaderAdditional)
        llContent = inflatedView.findViewById(R.id.llContent)
        nsvContent = inflatedView.findViewById(R.id.nsvContent)
        llFooter = inflatedView.findViewById(R.id.llFooter)
        vBottomSheetThumb = inflatedView.findViewById(R.id.vBottomSheetThumb)
        clBottomSheet = inflatedView.findViewById(R.id.clBottomSheet)

        // header
        var headerView = headerLayoutView
        if (headerView == null && headerLayoutID != -1) {
            headerView = inflate(context, headerLayoutID, null)
        }
        if (headerView != null) {
            llHeaderContent.addView(headerView)
            llHeaderContent.visibility = View.VISIBLE
        } else {
            llHeaderContent.visibility = View.GONE
        }

        // footer
        var footerView = footerLayoutView
        if (footerView == null && footerLayoutID != -1) {
            footerView = inflate(context, footerLayoutID, null)
        }
        if (footerView != null) {
            llFooter.addView(footerView)
            llFooter.visibility = View.VISIBLE
        } else {
            llFooter.visibility = View.GONE
        }

        // content
        var contentView = contentLayoutView
        if (contentView == null && contentLayoutID != -1) {
            contentView = inflate(context, contentLayoutID, null)
        }
        if (contentView != null) {
            nsvContent?.visibility = View.VISIBLE
            llContent.addView(contentView)
            llContent.visibility = View.VISIBLE
            llContent.viewTreeObserver?.addOnGlobalLayoutListener(onContentLayoutListener)
        } else {
            llContent.visibility = View.GONE
        }

        mBottomSheetBehavior?.skipCollapsed = skipCollapsed

        invalidateBehavior()
    }

    /**
     * updates behavior, max height and invalidates whole view
     */
    fun invalidateBehavior() {

        if (showHeaderThumb) {
            vBottomSheetThumb.visibility = View.VISIBLE
        }
        if (isHeaderHideable) {
            vBottomSheetThumb.visibility = View.INVISIBLE
        }
        if (isExpanded()) {
            flBlackout.visibility = if (blackoutOnExpand) {
                View.VISIBLE
            } else {
                View.GONE
            }
        } else {
            flBlackout.visibility = View.GONE
        }

        flBlackout.setOnClickListener {
            when {
                hideOnOutsideClick -> hide()
                collapseOnOutsideClick -> collapse()
                isExpanded() -> collapse()
            }
        }

        mBottomSheetBehavior = BottomSheetBehavior.from(clBottomSheet)

        mBottomSheetBehavior?.isHideable = isHideable

        mBottomSheetBehavior?.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                ViewCompat.setElevation(
                    vBottomSheetThumb,
                    dpToPx(context, THUMB_SELECTION_ELEVATION_IN_DP).toFloat()
                )
                vBottomSheetThumb.isPressed = true
                vBottomSheetThumb.invalidate()
            }

            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {
                if (newState != STATE_DRAGGING) {
                    ViewCompat.setElevation(vBottomSheetThumb, dpToPx(context, 2f).toFloat())
                    vBottomSheetThumb.isPressed = false
                    vBottomSheetThumb.invalidate()
                }
                changeBlackout(newState)
            }
        })

        if (defaultHidden) {
            currentExpandedState = BottomSheetBehavior.STATE_HIDDEN
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        }

        updateMaxHeightLayoutParams()
    }

    private fun invalidatePeekHeight() {
        if (peekHeightByHeader) {
            peekHeight = calculateViewHeight(llHeader)
        }
        mBottomSheetBehavior?.peekHeight = peekHeight
    }

    fun updateMaxHeightLayoutParams() {

        invalidatePeekHeight()

        val maxHeightInPxs = getMaxAllowedHeightInPxs()

        val headerHeight = calculateViewHeight(llHeader)
        val footerHeight = if (llFooter.visibility == View.VISIBLE) {
            calculateViewHeight(llFooter)
        } else {
            0
        }

        val actualContentHeight = llContent.measuredHeight

        if (actualContentHeight > 0) {
            llContent.viewTreeObserver.removeOnGlobalLayoutListener(onContentLayoutListener)
        }

        val maxContentHeight = maxHeightInPxs - headerHeight - footerHeight
        val contentHeight = if (actualContentHeight > 0) {
            actualContentHeight.coerceAtMost(maxContentHeight.toInt())
        } else {
            maxContentHeight.toInt()
        }

        val maxAllowedHeight = maxHeightInPxs.toInt()
        val actualOverallHeight = actualContentHeight + headerHeight + footerHeight
        val overallHeight = actualOverallHeight.coerceAtMost(maxAllowedHeight)

        if (nestedScrollViewForContent) {
            val lpNsvContent = nsvContent?.layoutParams as ViewGroup.LayoutParams
            lpNsvContent.height = contentHeight
            nsvContent?.layoutParams = lpNsvContent
        } else {
            val lpContent = llContent.layoutParams as ViewGroup.LayoutParams
            lpContent.height = contentHeight
            llContent.layoutParams = lpContent
        }

        val lp = clBottomSheet.layoutParams as ViewGroup.LayoutParams
        lp.height = overallHeight
        clBottomSheet.layoutParams = lp

        requestLayout()
        invalidate()
    }

    private fun changeBlackout(newState: Int) {
        if (blackoutOnExpand) {
            flBlackout.visibility = View.VISIBLE
            when (newState) {
                BottomSheetBehavior.STATE_COLLAPSED -> hideBlackout()
                BottomSheetBehavior.STATE_HIDDEN -> hideBlackout()
                BottomSheetBehavior.STATE_EXPANDED -> showBlackout()
            }
        } else {
            flBlackout.visibility = View.GONE
        }
    }

    private fun showBlackout() {
        if (flBlackout.alpha > 0.0f) {
            return
        }
        if (blackoutOnExpand) {
            flBlackout.visibility = View.VISIBLE
        }
        showBlackOutAnimator = flBlackout.animate().apply {
            alpha(DEFAULT_MAX_BLACKOUT)?.duration = DEFAULT_BLACKOUT_DURATION
            setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationStart(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    flBlackout.isClickable = true
                    flBlackout.isFocusable = true
                }
            })
            start()
        }
        if (isHeaderHideable) {
            vBottomSheetThumb.visibility = View.GONE
            llHeaderAdditional?.visibility = View.GONE
            llHeaderContent.visibility = View.GONE
            updateMaxHeightLayoutParams()
        }
    }

    private fun hideBlackout() {
        if (flBlackout.alpha == 0.0f) {
            return
        }
        hideBlackOutAnimator = flBlackout.animate().apply {
            alpha(0.0f)?.duration = DEFAULT_BLACKOUT_DURATION
            setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationStart(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    flBlackout.isClickable = false
                    flBlackout.isFocusable = false
                }
            })
            start()
        }
        if (isHeaderHideable) {
            vBottomSheetThumb.visibility = View.GONE
            llHeaderAdditional?.visibility = View.GONE
            llHeaderContent.visibility = View.GONE
            updateMaxHeightLayoutParams()
        }
    }

    companion object {
        const val THUMB_SELECTION_ELEVATION_IN_DP = 6f
        const val DEFAULT_MAX_HEIGHT = 0.5f
        const val DEFAULT_MAX_BLACKOUT = 0.75f
        const val DEFAULT_PEEK_HEIGHT = 100
        const val DEFAULT_BLACKOUT_DURATION = 250L
    }
}