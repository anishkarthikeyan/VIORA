package com.viora.app.core.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class VioraWarningOverlayView(
    context: Context,
    warning: VioraWarning,
    onViewDetails: () -> Unit,
    onDismiss: () -> Unit
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(12))
        background = GradientDrawable().apply {
            setColor(Color.rgb(21, 26, 35))
            cornerRadius = dp(14).toFloat()
            setStroke(dp(1), riskColor(warning.riskLevel))
        }

        addText(warning.riskLevel.name, 11f, riskColor(warning.riskLevel))
        addText(warning.title, 18f, Color.WHITE, Typeface.BOLD, 4)
        addText(warning.explanation, 14f, Color.LTGRAY, Typeface.NORMAL, 6)

        val actions = LinearLayout(context).apply { gravity = Gravity.END }
        actions.addView(Button(context).apply {
            text = "View Details"
            setOnClickListener { onViewDetails() }
        }, actionParams())
        actions.addView(Button(context).apply {
            text = "Dismiss"
            setOnClickListener { onDismiss() }
        }, actionParams())
        addView(actions, matchWidthParams(8))
    }

    private fun addText(textValue: String, size: Float, color: Int, style: Int = Typeface.NORMAL, top: Int = 0) {
        addView(TextView(context).apply {
            text = textValue
            textSize = size
            setTextColor(color)
            setTypeface(typeface, style)
        }, matchWidthParams(top))
    }

    private fun matchWidthParams(top: Int) = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }

    private fun actionParams() = LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT
    ).apply { marginStart = dp(6) }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun riskColor(level: VioraRiskLevel) = when (level) {
        VioraRiskLevel.SAFE -> Color.rgb(0, 230, 118)
        VioraRiskLevel.VERIFY -> Color.rgb(255, 214, 0)
        VioraRiskLevel.SUSPICIOUS -> Color.rgb(255, 145, 0)
        VioraRiskLevel.DANGEROUS -> Color.rgb(255, 23, 68)
    }
}