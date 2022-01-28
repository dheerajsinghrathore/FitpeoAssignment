package com.android.fitpeo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

open class LocationUtility(private val activity: Activity) :
    LifecycleObserver {

    private var mLocationRequest: LocationRequest? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    var currentLocation = MutableLiveData<Pair<Double, Double>>()

    init {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
    }

    fun startLocationClient() {
        initLocationRequest()
        checkLocationSettings()
    }

    private fun initLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private fun checkLocationSettings() {

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        builder.setAlwaysShow(true)

        val locationSettingTask = LocationServices.getSettingsClient(activity)
            .checkLocationSettings(builder.build())

        locationSettingTask.addOnCompleteListener {

            try {
                locationSettingTask.getResult(ApiException::class.java)
                startLocationUpdates()
            } catch (exception: ApiException) {
                enableGPSDialog(exception)
            }
        }

    }

    private fun enableGPSDialog(exception: ApiException) {
        when (exception.statusCode) {
            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                try {
                    val resolvable = exception as ResolvableApiException
                    resolvable.startResolutionForResult(
                        activity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (e: Exception) {
                    when (e) {
                        is IntentSender.SendIntentException -> {
                        }
                        is ClassCastException -> {
                        }
                    }
                }
            }
            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                Log.e(TAG, "onActivityResult: SETTINGS_CHANGE_UNAVAILABLE")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (checkPermissions()) {
            mLocationRequest?.let {
                fusedLocationProviderClient?.requestLocationUpdates(
                    it,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        } else {
            requestPermissions()
        }

    }


    private val locationCallback: LocationCallback = object : LocationCallback() {

        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val latitude = locationResult.locations[0].latitude
            val longitude = locationResult.locations[0].longitude
            val locationInfo = Pair(latitude, longitude)
            currentLocation.value = locationInfo
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> {
                    startLocationUpdates()
                    Log.e(TAG, "onActivityResult: RESULT_OK")
                }
                Activity.RESULT_CANCELED -> {
                    Log.e(TAG, "onActivityResult: RESULT_CANCELED")
                }
            }
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startLocationUpdates()
            }
        }
    }

    fun onStart() {
        startLocationUpdates()
    }

    fun onStop() {
        stopLocationUpdates()
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_CHECK_SETTINGS
        )
    }


    companion object {
        protected const val TAG = "MainActivity"
        const val REQUEST_CHECK_SETTINGS = 0x1
        const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 100
        const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2

    }

}