package com.android.fitpeo

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.fitpeo.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var locationUtility: LocationUtility
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    //Multiselection Day
    private val dayList = mutableListOf<Int>()
    private val dayArray = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val selectedDay: BooleanArray = BooleanArray(dayArray.size)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        initLocation()
        clickListener()
    }

    private fun initLocation() {
        locationUtility = LocationUtility(this)
        locationUtility.startLocationClient()

        lifecycle.addObserver(locationUtility)

        locationUtility.currentLocation.observe(this, {
            Log.e(localClassName, "initLocation: ${it.first} and ${it.second}")
            latitude = it.first
            longitude = it.second
            val geocoder = Geocoder(this, Locale.getDefault())
            val list: List<Address> =
                geocoder.getFromLocation(latitude, longitude, 1)
            mainBinding.tvLocation.text = list[0].locality
        })
    }

    private fun clickListener() {
        mainBinding.tvScheduleDay.setOnClickListener {
            openDayDialog()
        }

        mainBinding.tvDeskMap.setOnClickListener {
            val uri = String.format(Locale.ENGLISH, "geo:%f,%f", latitude, longitude)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        }
    }

    private fun openDayDialog() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle(R.string.select_day)
            setCancelable(true)
            setMultiChoiceItems(
                dayArray,
                selectedDay
            ) { _, i, boolean ->
                if (boolean) {
                    dayList.add(i)
                    dayList.sort()
                } else {
                    dayList.remove(i)
                }
            }
            setPositiveButton("Ok") { _, _ ->
                val stringBuilder = StringBuilder()
                for (j in 0 until dayList.size) {
                    stringBuilder.append(dayArray[dayList[j]])
                    if (j != dayList.size - 1) {
                        stringBuilder.append(", ")
                    }
                }
                mainBinding.tvScheduleDay.text = stringBuilder.toString()
            }
            setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            setNeutralButton("Clear All") { _, _ ->
                for (j in selectedDay.indices) {
                    selectedDay[j] = false
                    dayList.clear()
                    mainBinding.tvScheduleDay.text = ""
                }
            }
            builder.show()
        }
    }

    override fun onDestroy() {
        lifecycle.removeObserver(locationUtility)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationUtility.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationUtility.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onLocationChanged(location: Location) {
        mainBinding.tvLocation.text = "${location.latitude}"
        Log.e(localClassName, "onLocationChanged: ${location.latitude}")
    }
}

