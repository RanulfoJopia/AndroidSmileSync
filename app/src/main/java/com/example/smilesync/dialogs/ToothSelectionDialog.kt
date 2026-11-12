package com.example.smilesync.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.smilesync.R
import com.example.smilesync.model.Measurement

class ToothSelectionDialog(
    context: Context,
    private val measurement: Measurement?,
    private val preselectedTeeth: List<Int>,
    private val onTeethSelected: (List<Int>) -> Unit
) : Dialog(context) {

    private val selectedTeeth = mutableSetOf<Int>()
    private val toothViews = mutableMapOf<Int, View>()
    private val teethWithFindings = mutableListOf<Int>() // Only teeth with annotations

    companion object {
        // FDI tooth numbering system
        val UPPER_RIGHT = listOf(18, 17, 16, 15, 14, 13, 12, 11)
        val UPPER_LEFT = listOf(21, 22, 23, 24, 25, 26, 27, 28)
        val LOWER_LEFT = listOf(38, 37, 36, 35, 34, 33, 32, 31)
        val LOWER_RIGHT = listOf(41, 42, 43, 44, 45, 46, 47, 48)

        val TOOTH_NAMES = mapOf(
            // Upper Right
            18 to "UR8", 17 to "UR7", 16 to "UR6", 15 to "UR5",
            14 to "UR4", 13 to "UR3", 12 to "UR2", 11 to "UR1",
            // Upper Left
            21 to "UL1", 22 to "UL2", 23 to "UL3", 24 to "UL4",
            25 to "UL5", 26 to "UL6", 27 to "UL7", 28 to "UL8",
            // Lower Left
            38 to "LL8", 37 to "LL7", 36 to "LL6", 35 to "LL5",
            34 to "LL4", 33 to "LL3", 32 to "LL2", 31 to "LL1",
            // Lower Right
            41 to "LR1", 42 to "LR2", 43 to "LR3", 44 to "LR4",
            45 to "LR5", 46 to "LR6", 47 to "LR7", 48 to "LR8"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_tooth_selection)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        selectedTeeth.addAll(preselectedTeeth)

        // Extract teeth that have findings/annotations
        extractTeethWithFindings()

        if (teethWithFindings.isEmpty()) {
            findViewById<TextView>(R.id.tv_dialog_title).text = "No Examination Data"
            findViewById<TextView>(R.id.tv_dialog_subtitle).text = 
                "This patient has no dental examination findings. Please perform a 3D dental exam first."
            findViewById<Button>(R.id.btn_confirm).isEnabled = false
        } else {
            setupToothGrids()
            updateSelectedCount()
        }
        
        setupButtons()
    }

    private fun extractTeethWithFindings() {
        if (measurement == null) return

        val teethSet = mutableSetOf<Int>()

        // Get teeth from odontograms (conditions like decay, filling, etc.)
        measurement.odontograms?.forEach { odonto ->
            teethSet.add(odonto.toothNumber)
        }

        // Get teeth from tooth notes (custom annotations)
        measurement.toothNotes?.forEach { note ->
            if (!note.noteNotes.isNullOrBlank()) {
                teethSet.add(note.toothNumber)
            }
        }

        // Get teeth from tooth records with procedures or notes
        measurement.toothRecords?.forEach { tooth ->
            if (!tooth.procedures.isNullOrEmpty() || !tooth.recordNotes.isNullOrBlank()) {
                teethSet.add(tooth.toothNumber)
            }
        }

        teethWithFindings.addAll(teethSet.sorted())
    }

    private fun setupToothGrids() {
        val upperRightGrid = findViewById<GridLayout>(R.id.grid_upper_right)
        val upperLeftGrid = findViewById<GridLayout>(R.id.grid_upper_left)
        val lowerLeftGrid = findViewById<GridLayout>(R.id.grid_lower_left)
        val lowerRightGrid = findViewById<GridLayout>(R.id.grid_lower_right)

        // Only show teeth that have findings
        val upperRight = UPPER_RIGHT.filter { it in teethWithFindings }
        val upperLeft = UPPER_LEFT.filter { it in teethWithFindings }
        val lowerLeft = LOWER_LEFT.filter { it in teethWithFindings }
        val lowerRight = LOWER_RIGHT.filter { it in teethWithFindings }

        populateToothGrid(upperRightGrid, upperRight)
        populateToothGrid(upperLeftGrid, upperLeft)
        populateToothGrid(lowerLeftGrid, lowerLeft)
        populateToothGrid(lowerRightGrid, lowerRight)

        // Hide sections with no teeth
        findViewById<View>(R.id.section_upper).visibility = 
            if (upperRight.isEmpty() && upperLeft.isEmpty()) View.GONE else View.VISIBLE
        findViewById<View>(R.id.section_lower).visibility = 
            if (lowerLeft.isEmpty() && lowerRight.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun populateToothGrid(grid: GridLayout, teeth: List<Int>) {
        teeth.forEach { toothNum ->
            val toothView = layoutInflater.inflate(R.layout.item_tooth_selector, grid, false)
            val tvNumber = toothView.findViewById<TextView>(R.id.tv_tooth_number)
            val tvLabel = toothView.findViewById<TextView>(R.id.tv_tooth_label)
            val indicator = toothView.findViewById<View>(R.id.tooth_condition_indicator)

            tvNumber.text = toothNum.toString()
            tvLabel.text = TOOTH_NAMES[toothNum] ?: ""

            // Set condition indicator color based on measurement data
            val condition = getToothCondition(toothNum)
            when (condition) {
                "needs_treatment" -> indicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.warning_orange)
                )
                "missing" -> indicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.text_gray)
                )
                else -> indicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.success_green)
                )
            }

            // Handle selection
            updateToothViewState(toothView, selectedTeeth.contains(toothNum))

            toothView.setOnClickListener {
                if (selectedTeeth.contains(toothNum)) {
                    selectedTeeth.remove(toothNum)
                } else {
                    selectedTeeth.add(toothNum)
                }
                updateToothViewState(toothView, selectedTeeth.contains(toothNum))
                updateSelectedCount()
            }

            toothViews[toothNum] = toothView
            grid.addView(toothView)
        }
    }

    private fun getToothCondition(toothNum: Int): String {
        if (measurement == null) return "normal"

        // Check if tooth is missing
        val toothRecord = measurement.toothRecords?.find { it.toothNumber == toothNum }
        if (toothRecord?.status == "missing") return "missing"

        // Check odontogram for conditions
        val odontogram = measurement.odontograms?.find { it.toothNumber == toothNum }
        if (odontogram != null && odontogram.conditionType in listOf(1, 7)) { // Caries or Fractured
            return "needs_treatment"
        }

        return "normal"
    }

    private fun updateToothViewState(view: View, isSelected: Boolean) {
        if (isSelected) {
            view.setBackgroundResource(R.drawable.bg_chip_modern_selected)
            view.findViewById<TextView>(R.id.tv_tooth_number).setTextColor(Color.WHITE)
            view.findViewById<TextView>(R.id.tv_tooth_label).setTextColor(Color.WHITE)
        } else {
            view.setBackgroundResource(R.drawable.bg_chip_modern)
            view.findViewById<TextView>(R.id.tv_tooth_number).setTextColor(
                ContextCompat.getColor(context, R.color.text_white)
            )
            view.findViewById<TextView>(R.id.tv_tooth_label).setTextColor(
                ContextCompat.getColor(context, R.color.text_light_gray)
            )
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dismiss()
        }

        findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            onTeethSelected(selectedTeeth.toList().sorted())
            dismiss()
        }

        findViewById<TextView>(R.id.btn_clear_all).setOnClickListener {
            selectedTeeth.clear()
            toothViews.forEach { (_, view) ->
                updateToothViewState(view, false)
            }
            updateSelectedCount()
        }
    }

    private fun updateSelectedCount() {
        findViewById<TextView>(R.id.tv_selected_count).text =
            "Selected: ${selectedTeeth.size} ${if (selectedTeeth.size == 1) "tooth" else "teeth"}"
    }
}