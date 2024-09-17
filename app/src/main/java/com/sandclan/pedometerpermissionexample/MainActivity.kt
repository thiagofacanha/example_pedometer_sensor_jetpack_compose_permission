package com.sandclan.pedometerpermissionexample

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.sandclan.pedometerpermissionexample.ui.theme.PedometerPermissionExampleTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update


class MainActivity : ComponentActivity(), SensorEventListener {
    private val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private var pedometerDetectedSensor: Sensor? = null
    private var pedometerSinceLastRebootSensor: Sensor? = null

    private val pedometerCounter = MutableStateFlow(0)
    private val pedometerSinceLastRebootCounter = MutableStateFlow(0)

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pedometerDetectedSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        pedometerSinceLastRebootSensor =
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        sensorManager.registerListener(
            this,
            pedometerDetectedSensor!!,
            SensorManager.SENSOR_DELAY_FASTEST
        )
        sensorManager.registerListener(
            this,
            pedometerSinceLastRebootSensor!!,
            SensorManager.SENSOR_DELAY_FASTEST
        )

        setContent {
            PedometerPermissionExampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val activityRecognitionPermission =
                                rememberPermissionState(permission = Manifest.permission.ACTIVITY_RECOGNITION)
                            LaunchedEffect(Unit) {
                                activityRecognitionPermission.launchPermissionRequest()
                            }
                            when {
                                activityRecognitionPermission.status.isGranted -> {
                                    Text(text = "Steps Since app started = ${pedometerCounter.collectAsState().value}")
                                    Text(text = "Steps Since phone started = ${pedometerSinceLastRebootCounter.collectAsState().value}")
                                }

                                activityRecognitionPermission.status.shouldShowRationale -> {
                                    Button(onClick = {
                                        activityRecognitionPermission.launchPermissionRequest()
                                    }) { }
                                }
                            }
                        } else {
                            Text(text = "VERSION.SDK_INT < Q. Need another code for that.")
                        }


                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        sensorEvent?.let { event ->
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                pedometerCounter.update { it.plus(1) }
            } else if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                pedometerSinceLastRebootCounter.update { event.values[0].toInt() }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}