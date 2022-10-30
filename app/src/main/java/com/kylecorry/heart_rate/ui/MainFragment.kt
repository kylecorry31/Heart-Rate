package com.kylecorry.heart_rate.ui

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.get
import androidx.core.graphics.red
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.toBitmap
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.heart_rate.R
import com.kylecorry.heart_rate.databinding.FragmentMainBinding
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.filters.MovingAverageFilter
import com.kylecorry.sol.math.optimization.DispersionExtremaFinder
import com.kylecorry.sol.math.optimization.Extremum
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.units.Reading
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

@AndroidEntryPoint
class MainFragment : BoundFragment<FragmentMainBinding>() {

    @Inject
    lateinit var formatter: FormatService

    private var started = false
    private val maxHistory = Duration.ofSeconds(10)
    private val readings = mutableListOf<Reading<Float>>()
    private val filter = MovingAverageFilter(5)

    private val camera by lazy {
        Camera(
            requireContext(),
            viewLifecycleOwner,
            previewView = binding.preview,
            targetResolution = Size(200, 200)
        )
    }

    private val chart by lazy {
        SimpleLineChart(binding.chart)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chart.configureYAxis(
            granularity = 1f,
            labelCount = 5
        )
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMainBinding {
        return FragmentMainBinding.inflate(layoutInflater, container, false)
    }

    override fun onResume() {
        super.onResume()
        requestPermissions(listOf(Manifest.permission.CAMERA)) {
            camera.start(this::onCameraUpdate)
        }
    }

    override fun onPause() {
        super.onPause()
        camera.stop(this::onCameraUpdate)
        camera.setTorch(false)
        started = false
    }

    private fun onCameraUpdate(): Boolean {
        if (!started) {
            started = true
            camera.setTorch(true)
        }
        process(camera.image?.image?.toBitmap())
        camera.image?.close()
        return true
    }

    private fun process(image: Bitmap?) {
        image ?: return
        var r = 0f
        val total = image.width * image.height.toFloat()
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                r += image[x, y].red / total
            }
        }

        if (r in 100f..250f) {
            val now = Instant.now()
            val maxTime = now.minus(maxHistory)
            val filtered = filter.filter(r)
            readings.removeIf { it.time < maxTime }
            readings.add(Reading(filtered, now))
            calculateHeartRate()
        }

        image.recycle()
    }


    private fun calculateHeartRate() {
        val dt = Duration.between(readings.first().time, readings.last().time)

        if (dt < Duration.ofSeconds(1) && readings.size > 3) {
            return
        }

        val beats = findPeaks(readings)

        val durations = mutableListOf<Duration>()
        for (i in 1 until beats.size) {
            durations.add(Duration.between(beats[i - 1].time, beats[i].time))
        }

        val averageDuration = 1 / (durations.map { it.toMillis() }.average() / 1000 / 60)

        val bpm = if (!averageDuration.isNaN()) {
            averageDuration.roundToInt()
        } else {
            0
        }
        binding.homeTitle.title.text = bpm.toString()
        val rawSet = SimpleLineChart.Dataset(
            SimpleLineChart.getDataFromReadings(readings) { it },
            Resources.color(requireContext(), R.color.red)
        )

        val beatSet = SimpleLineChart.Dataset(
            SimpleLineChart.getDataFromReadings(beats, readings.first().time) { it },
            Color.WHITE,
            circles = true,
            circleColor = Color.WHITE
        )

        chart.plot(listOf(rawSet, beatSet))
    }

    private fun findPeaks(values: List<Reading<Float>>): List<Reading<Float>> {
        val lag = 5
        val influence = 0.2f
        val threshold = 1.5f

        return DispersionExtremaFinder(lag, threshold, influence).find(values.map { it.value })
            .filter { it.isHigh }
            .map { values[it.point.x.toInt()] }
    }

}