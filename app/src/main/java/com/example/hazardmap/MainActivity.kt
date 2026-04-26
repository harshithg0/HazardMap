package com.example.hazardmap

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var searchView: SearchView

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            enableUserLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupUI()
    }

    private fun setupUI() {
        searchView = findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) searchLocation(query)
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean = false
        })

        findViewById<FloatingActionButton>(R.id.btn_zoom_in).setOnClickListener {
            if (::googleMap.isInitialized) {
                googleMap.animateCamera(CameraUpdateFactory.zoomIn())
            }
        }

        findViewById<FloatingActionButton>(R.id.btn_zoom_out).setOnClickListener {
            if (::googleMap.isInitialized) {
                googleMap.animateCamera(CameraUpdateFactory.zoomOut())
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        googleMap.setOnMapClickListener { latLng ->
            showAddMarkerDialog(latLng)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation()
        } else {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun searchLocation(locationName: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        Thread {
            try {
                val addressList: List<Address>? = geocoder.getFromLocationName(locationName, 1)
                runOnUiThread {
                    if (!addressList.isNullOrEmpty()) {
                        val address = addressList[0]
                        val latLng = LatLng(address.latitude, address.longitude)
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(locationName)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        )
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    } else {
                        Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Network error or Geocoder service unavailable", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun showAddMarkerDialog(latLng: LatLng) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_marker, null)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.et_description)
        val typeGroup = dialogView.findViewById<RadioGroup>(R.id.rg_hazard_type)

        AlertDialog.Builder(this)
            .setTitle("Add Hazard")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val description = descriptionInput.text.toString()
                val color = when (typeGroup.checkedRadioButtonId) {
                    R.id.rb_fire -> BitmapDescriptorFactory.HUE_RED
                    R.id.rb_flood -> BitmapDescriptorFactory.HUE_BLUE
                    R.id.rb_pothole -> BitmapDescriptorFactory.HUE_YELLOW
                    else -> BitmapDescriptorFactory.HUE_MAGENTA
                }
                addHazardMarker(latLng, description, color)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addHazardMarker(latLng: LatLng, title: String, color: Float) {
        googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(color))
        )
    }

    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                }
            }
        }
    }
}
