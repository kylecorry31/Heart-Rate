package com.kylecorry.heart_rate.ui

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.time.Time.hoursUntil
import com.kylecorry.sol.units.Reading
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min


class SimpleLineChart(
    private val chart: LineChart,
    emptyText: String = ""
) {

    private var onValueSelectedListener: OnChartValueSelectedListener? = null

    init {
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)

        chart.xAxis.setDrawLabels(false)
        chart.axisRight.setDrawLabels(false)

        val primaryColor = Resources.androidTextColorPrimary(chart.context)
        val r = primaryColor.red
        val g = primaryColor.green
        val b = primaryColor.blue

        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.gridColor = Color.argb(50, r, g, b)
        chart.axisLeft.textColor = Color.argb(150, r, g, b)
        chart.xAxis.gridColor = Color.argb(50, r, g, b)
        chart.xAxis.textColor = Color.argb(150, r, g, b)
        chart.axisRight.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisRight.setDrawAxisLine(false)
        chart.setNoDataText(emptyText)
        chart.setNoDataTextColor(primaryColor)
    }

    fun configureYAxis(
        minimum: Float? = null,
        maximum: Float? = null,
        granularity: Float? = null,
        labelCount: Int? = null,
        drawGridLines: Boolean = true,
        labelFormatter: ((value: Float) -> String)? = null
    ) {
        if (minimum != null) {
            chart.axisLeft.axisMinimum = minimum
        } else {
            chart.axisLeft.resetAxisMinimum()
        }

        if (maximum != null) {
            chart.axisLeft.axisMaximum = maximum
        } else {
            chart.axisLeft.resetAxisMaximum()
        }

        if (granularity != null) {
            chart.axisLeft.granularity = granularity
        } else {
            chart.axisLeft.isGranularityEnabled = false
        }

        if (labelCount != null && labelCount != 0) {
            chart.axisLeft.setDrawLabels(true)
            chart.axisLeft.setLabelCount(labelCount, true)
        } else if (labelCount == 0) {
            chart.axisLeft.setDrawLabels(false)
        } else {
            chart.axisLeft.setDrawLabels(true)
            chart.axisLeft.setLabelCount(6, false)
        }

        if (labelFormatter == null) {
            chart.axisLeft.valueFormatter = null
        } else {
            chart.axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return labelFormatter.invoke(value)
                }
            }
        }

        chart.axisLeft.setDrawGridLines(drawGridLines)
    }

    fun configureXAxis(
        minimum: Float? = null,
        maximum: Float? = null,
        granularity: Float? = null,
        labelCount: Int? = null,
        drawGridLines: Boolean = false,
        labelFormatter: ((value: Float) -> String)? = null
    ) {
        if (minimum != null) {
            chart.xAxis.axisMinimum = minimum
        } else {
            chart.xAxis.resetAxisMinimum()
        }

        if (maximum != null) {
            chart.xAxis.axisMaximum = maximum
        } else {
            chart.xAxis.resetAxisMaximum()
        }

        if (granularity != null) {
            chart.xAxis.granularity = granularity
        } else {
            chart.xAxis.isGranularityEnabled = false
        }

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM

        if (labelCount != null && labelCount != 0) {
            chart.xAxis.setDrawLabels(true)
            chart.xAxis.setLabelCount(labelCount, true)
        } else if (labelCount == 0) {
            chart.xAxis.setDrawLabels(false)
        } else {
            chart.xAxis.setDrawLabels(true)
            chart.xAxis.setLabelCount(6, false)
        }

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return labelFormatter?.invoke(value) ?: ""
            }
        }

        chart.xAxis.setDrawGridLines(drawGridLines)
    }

    fun plot(data: List<Dataset>) {
        val sets = mutableListOf<LineDataSet>()
        data.forEachIndexed { index, it ->
            val values = it.data.map { Entry(it.first, it.second) }

            val set = LineDataSet(values, "Set ${index + 1}")
            set.color = it.color
            set.fillAlpha = 180
            set.lineWidth = it.lineWidth
            set.setDrawValues(false)
            set.fillColor = it.color
            set.setCircleColor(it.circleColor)
            set.setDrawCircleHole(false)
            set.setDrawCircles(it.circles)
            set.isHighlightEnabled = it.isHighlightEnabled
            set.circleRadius = 1.5f
            set.setDrawFilled(it.filled)

            if (it.cubic) {
                set.mode = LineDataSet.Mode.CUBIC_BEZIER
                set.cubicIntensity = 0.005f
            }

            sets.add(set)
        }
        val lineData = LineData(sets.toList())
        chart.data = lineData
        chart.legend.isEnabled = false
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    fun plot(
        data: List<Pair<Float, Float>>,
        @ColorInt color: Int,
        filled: Boolean = false,
        circles: Boolean = false,
        @ColorInt circleColor: Int = color,
        cubic: Boolean = true,
        lineWidth: Float = 3f,
        isHighlightEnabled: Boolean = true
    ) {
        plot(listOf(Dataset(data, color, filled, circles, circleColor, cubic, lineWidth, isHighlightEnabled)))
    }

    fun plotIndexed(data: List<Float>, @ColorInt color: Int, filled: Boolean = false) {
        plot(data.mapIndexed { index, value -> index.toFloat() to value }, color, filled)
    }

    fun setOnValueSelectedListener(listener: ((point: Point?) -> Unit)?) {
        if (listener == null) {
            chart.setTouchEnabled(false)
            chart.setOnChartValueSelectedListener(null)
        } else {
            chart.setTouchEnabled(true)
            onValueSelectedListener = object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e == null) {
                        listener.invoke(null)
                        return
                    }
                    val datasetIndex = h?.dataSetIndex ?: 0
                    val pointIndex = chart.data.dataSets[datasetIndex].getEntryIndex(e)
                    listener.invoke(Point(datasetIndex, pointIndex, e.x, e.y))
                }

                override fun onNothingSelected() {
                    listener.invoke(null)
                }

            }
            chart.setOnChartValueSelectedListener(onValueSelectedListener)
        }
    }

    fun getPoint(datasetIdx: Int, entryIdx: Int): Point {
        val entry = chart.lineData.getDataSetByIndex(datasetIdx).getEntryForIndex(entryIdx)
        val point = chart.getPixelForValues(entry.x, entry.y, YAxis.AxisDependency.LEFT)
        return Point(datasetIdx, entryIdx, point.x.toFloat(), point.y.toFloat())
    }

    data class Point(val datasetIndex: Int, val pointIndex: Int, val x: Float, val y: Float)

    data class Dataset(
        val data: List<Pair<Float, Float>>,
        @ColorInt val color: Int,
        val filled: Boolean = false,
        val circles: Boolean = false,
        @ColorInt val circleColor: Int = color,
        val cubic: Boolean = true,
        val lineWidth: Float = 3f,
        val isHighlightEnabled: Boolean = true
    )

    companion object {
        fun <T> getDataFromReadings(
            readings: List<Reading<T>>,
            startTime: Instant? = null,
            getY: (T) -> Float
        ): List<Pair<Float, Float>> {
            val first = startTime ?: readings.firstOrNull()?.time ?: return emptyList()
            return readings.map {
                Duration.between(first, it.time).toMillis() / 1000f to getY(it.value)
            }
        }

        fun getYRange(
            data: List<Pair<Float, Float>>,
            granularity: Float,
            minRange: Float
        ): Range<Float> {
            val values = data.map { it.second }
            val minValue = values.minOrNull() ?: 0f
            val maxValue = values.maxOrNull() ?: 0f
            return getRange(minValue, maxValue, granularity, minRange)
        }

        fun getRange(
            minimum: Float,
            maximum: Float,
            granularity: Float,
            minRange: Float
        ): Range<Float> {
            val middle = (minimum + maximum) / 2f
            val start = min(minimum - granularity, middle - minRange / 2)
            val end = max(maximum + granularity, middle + minRange / 2)

            return Range(start, end)
        }
    }

}