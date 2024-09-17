package com.example.location4

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SearchView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize Places API (replace with your API key)
        Places.initialize(applicationContext, "YOUR_API_KEY")

        checkLocationPermission()

        // Initialize search bar and ListView
        val searchView: SearchView = findViewById(R.id.search_view)
        val placesListView: ListView = findViewById(R.id.places_list)

        // SearchView interaction
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Perform the search for specific places
                query?.let {
                    searchForPlaces(it)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    // Check for location permission
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            getLastKnownLocation()
        }
    }

    // Request permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastKnownLocation()
            } else {
                // Handle permission denied
                Log.e("MainActivity", "Location permission denied")
            }
        }
    }

    // Get the user's current location
    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                Log.d("MainActivity", "Current location: ${location.latitude}, ${location.longitude}")
                // Fetch nearby places based on the current location
                searchForPlaces("restaurant") // Example query
            }
        }
    }

    // Search for places near the user's location
    private fun searchForPlaces(query: String) {
        val placesClient = Places.createClient(this)
        val placeFields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.RATING, Place.Field.LAT_LNG)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLocation = LatLng(it.latitude, it.longitude)
                val request = FindCurrentPlaceRequest.newInstance(placeFields)

                placesClient.findCurrentPlace(request).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val response = task.result
                        val places = response.placeLikelihoods.map { it.place }
                        updatePlacesList(places)
                    } else {
                        Log.e("MainActivity", "Error fetching places: ${task.exception}")
                    }
                }
            }
        }
    }

    // Update ListView with places data
    private fun updatePlacesList(places: List<Place>) {
        val placesListView: ListView = findViewById(R.id.places_list)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, places.map { it.name })
        placesListView.adapter = adapter

        placesListView.setOnItemClickListener { _, _, position, _ ->
            val selectedPlace = places[position]
            showPlaceDetails(selectedPlace)
        }
    }

    // Show details of the selected place
    private fun showPlaceDetails(place: Place) {
        AlertDialog.Builder(this)
            .setTitle(place.name)
            .setMessage("Address: ${place.address}\nRating: ${place.rating}")
            .setPositiveButton("OK", null)
            .show()
    }
}
