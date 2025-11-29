package com.example.trekapp1.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Listens to the hardware step counter (TYPE_STEP_COUNTER).
 * Sends total steps since boot into the callback.
 */
class StepSensorManager(
    context: Context,
    private val stepsSinceRun: (Long) -> Unit
) : SensorEventListener {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    fun start() {
        stepSensor?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val totalSteps = event.values[0].toLong()
        stepsSinceRun(totalSteps)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Needed to override so this class doesn't get marked abstract
        //Doesn't need to do anything rn though
    }
}
