package com.andreyaleev.bottomsheet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.NonNull
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_material_bottom_sheet.*

class MainActivity : AppCompatActivity() {

    private var mTextViewState: TextView? = null

    private var bottomSheetLayout: BottomSheetBehaviourView? = null

    private var programmaticallyAddedBottomSheet: BottomSheetBehaviourView? = null
    private var pAddedBottomSheetSecond: BottomSheetBehaviourView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_material_bottom_sheet)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        title = "MaterialBottomSheetActivity"

        bottomSheetLayout = findViewById(R.id.bottomSheetLayout)

//        bottomSheetLayout?.setMaxBottomSheetHeight(0.75f)

        val mBottomSheetBehavior = bottomSheetLayout?.getBehaviour()

        mTextViewState = findViewById(R.id.text_view_state)

        val buttonExpand = findViewById<Button>(R.id.button_expand)
        val buttonCollapse = findViewById<Button>(R.id.button_collapse)

        buttonExpand.setOnClickListener {
            bottomSheetLayout?.expand()
        }

        buttonCollapse.setOnClickListener {
            bottomSheetLayout?.collapse()
        }

        buttonAddNew.setOnClickListener {
            createBottomSheetFromCode()
        }

        buttonAddNewConstructor.setOnClickListener {
            createBottomSheetFromCode2()
        }

        mBottomSheetBehavior?.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> mTextViewState?.text = "Collapsed"
                    BottomSheetBehavior.STATE_DRAGGING -> mTextViewState?.text = "Dragging..."
                    BottomSheetBehavior.STATE_EXPANDED -> mTextViewState?.text = "Expanded"
                    BottomSheetBehavior.STATE_HIDDEN -> mTextViewState?.text = "Hidden"
                    BottomSheetBehavior.STATE_SETTLING -> mTextViewState?.text = "Settling..."
                }
            }

            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) {
                mTextViewState?.text = "Sliding..."
            }
        })

    }

    private fun createBottomSheetFromCode() {
        bottomSheetLayout?.hide()
        if (programmaticallyAddedBottomSheet != null) {
            programmaticallyAddedBottomSheet?.expand()
            return
        }

        programmaticallyAddedBottomSheet = BottomSheetBehaviourView(
            this,
            maxBottomSheetHeight = 0.4f,
            headerLayoutID = R.layout.layout_material_bottomsheet_header,
            contentLayoutID = R.layout.layout_material_bottomsheet_content,
            footerLayoutID = R.layout.layout_material_bottomsheet_footer
        )
        programmaticallyAddedBottomSheet?.layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        root.addView(programmaticallyAddedBottomSheet)

        textViewState2.visibility = View.VISIBLE

        programmaticallyAddedBottomSheet?.getBehaviour()?.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> textViewState2?.text = "Collapsed"
                    BottomSheetBehavior.STATE_DRAGGING -> textViewState2?.text = "Dragging..."
                    BottomSheetBehavior.STATE_EXPANDED -> textViewState2?.text = "Expanded"
                    BottomSheetBehavior.STATE_HIDDEN -> textViewState2?.text = "Hidden"
                    BottomSheetBehavior.STATE_SETTLING -> textViewState2?.text = "Settling..."
                }
            }

            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) {
                textViewState2?.text = "Sliding..."
            }
        })
    }


    private fun createBottomSheetFromCode2() {
        bottomSheetLayout?.hide()
        programmaticallyAddedBottomSheet?.hide()
        if (pAddedBottomSheetSecond != null) {
            pAddedBottomSheetSecond?.expand()
            return
        }
        val contentView = layoutInflater.inflate(R.layout.layout_material_bottomsheet_content, null)
        pAddedBottomSheetSecond = BottomSheetBehaviourView(
            this,
            maxBottomSheetHeight = 0.75f,
            nestedScrollViewForContent = true,
            headerLayoutID = R.layout.layout_material_bottomsheet_header,
            contentLayoutView = contentView,
            footerLayoutID = R.layout.layout_material_bottomsheet_footer
        )

        pAddedBottomSheetSecond?.layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        root.addView(pAddedBottomSheetSecond)

        pAddedBottomSheetSecond?.getBehaviour()?.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> textViewState3?.text = "Collapsed"
                    BottomSheetBehavior.STATE_DRAGGING -> textViewState3?.text = "Dragging..."
                    BottomSheetBehavior.STATE_EXPANDED -> textViewState3?.text = "Expanded"
                    BottomSheetBehavior.STATE_HIDDEN -> textViewState3?.text = "Hidden"
                    BottomSheetBehavior.STATE_SETTLING -> textViewState3?.text = "Settling..."
                }
            }

            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) {
                textViewState3?.text = "Sliding..."
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (bottomSheetLayout != null && bottomSheetLayout?.getBehaviour()?.state != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetLayout?.hide()
        } else {
            super.onBackPressed()
        }
    }
}