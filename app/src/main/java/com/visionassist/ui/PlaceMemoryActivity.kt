package com.visionassist.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.appbar.MaterialToolbar
import com.visionassist.R
import com.visionassist.places.PlaceMemoryManager
import com.visionassist.places.PlaceOfInterest
import timber.log.Timber

/**
 * Activity for managing Places of Interest (location-based reminders)
 */
class PlaceMemoryActivity : AppCompatActivity(), PlaceMemoryManager.PlaceMemoryListener {

    companion object {
        private const val TAG = "PlaceMemoryActivity"
    }

    private lateinit var placeMemoryManager: PlaceMemoryManager
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlaceAdapter
    private lateinit var emptyView: View
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var progressLoading: ProgressBar

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val backgroundLocation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] ?: false
        } else {
            true
        }

        if (fineLocation && backgroundLocation) {
            Toast.makeText(this, "位置权限已获取", Toast.LENGTH_SHORT).show()
            loadPlaces()
        } else {
            Toast.makeText(this, "需要位置权限才能使用地理围栏功能", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_memory)

        placeMemoryManager = PlaceMemoryManager(this)
        placeMemoryManager.listener = this

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()
        checkPermissionsAndLoad()
    }

    private fun setupUI() {
        recyclerView = findViewById(R.id.recycler_places)
        emptyView = findViewById(R.id.empty_view)
        fabAdd = findViewById(R.id.fab_add_place)
        progressLoading = findViewById(R.id.progress_loading)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        adapter = PlaceAdapter(
            onPlaceClick = { place -> showPlaceDetails(place) },
            onPlaceToggle = { place, isActive -> togglePlaceActive(place, isActive) },
            onPlaceDelete = { place -> confirmDeletePlace(place) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            showAddPlaceDialog()
        }
    }

    private fun checkPermissionsAndLoad() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                loadPlaces()
            }
            else -> {
                requestLocationPermissions()
            }
        }
    }

    private fun requestLocationPermissions() {
        // First request foreground location
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun loadPlaces() {
        progressLoading.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE
        val places = placeMemoryManager.getAllPlaces()
        updateUI(places)
    }

    private fun updateUI(places: List<PlaceOfInterest>) {
        progressLoading.visibility = View.GONE
        adapter.submitList(places)
        emptyView.visibility = if (places.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (places.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddPlaceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_place, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.input_place_name)
        val reminderInput = dialogView.findViewById<TextInputEditText>(R.id.input_reminder_text)
        val radiusInput = dialogView.findViewById<TextInputEditText>(R.id.input_radius)

        AlertDialog.Builder(this)
            .setTitle("添加地点")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val name = nameInput.text?.toString()?.trim() ?: ""
                val reminder = reminderInput.text?.toString()?.trim() ?: ""
                val radius = radiusInput.text?.toString()?.toFloatOrNull() ?: 20f

                if (name.isNotEmpty()) {
                    addPlace(name, reminder, radius)
                } else {
                    Toast.makeText(this, "请输入地点名称", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addPlace(name: String, reminderText: String, radiusMeters: Float) {
        // Get current location for the place
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    placeMemoryManager.addPlace(
                        name = name,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        radiusMeters = radiusMeters,
                        reminderText = reminderText,
                        isActive = true
                    ) { id ->
                        if (id > 0) {
                            Toast.makeText(this, "地点已添加", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "添加失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "无法获取当前位置", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "获取位置失败: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "需要位置权限", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPlaceDetails(place: PlaceOfInterest) {
        val message = buildString {
            append("名称: ${place.name}\n")
            append("提醒: ${place.reminderText}\n")
            append("半径: ${place.radiusMeters}米\n")
            append("状态: ${if (place.isActive) "启用" else "禁用"}\n")
            append("坐标: ${place.latitude}, ${place.longitude}")
        }

        AlertDialog.Builder(this)
            .setTitle(place.name)
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun togglePlaceActive(place: PlaceOfInterest, isActive: Boolean) {
        placeMemoryManager.setPlaceActive(place.id, isActive) { success ->
            if (success) {
                val status = if (isActive) "启用" else "禁用"
                Toast.makeText(this, "地点已$status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeletePlace(place: PlaceOfInterest) {
        AlertDialog.Builder(this)
            .setTitle("删除地点")
            .setMessage("确定要删除 \"${place.name}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                placeMemoryManager.deletePlace(place) { success ->
                    if (success) {
                        Toast.makeText(this, "地点已删除", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        placeMemoryManager.initializeGeofences { success ->
            Timber.d("$TAG: Geofences initialized: $success")
        }
    }

    override fun onPlacesLoaded(places: List<PlaceOfInterest>) {
        runOnUiThread { updateUI(places) }
    }

    override fun onPlaceAdded(place: PlaceOfInterest) {
        runOnUiThread { loadPlaces() }
    }

    override fun onPlaceUpdated(place: PlaceOfInterest) {
        runOnUiThread { loadPlaces() }
    }

    override fun onPlaceDeleted(place: PlaceOfInterest) {
        runOnUiThread { loadPlaces() }
    }

    override fun onError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * RecyclerView adapter for places list
 */
class PlaceAdapter(
    private val onPlaceClick: (PlaceOfInterest) -> Unit,
    private val onPlaceToggle: (PlaceOfInterest, Boolean) -> Unit,
    private val onPlaceDelete: (PlaceOfInterest) -> Unit
) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    private var places: List<PlaceOfInterest> = emptyList()

    fun submitList(newPlaces: List<PlaceOfInterest>) {
        places = newPlaces
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(places[position])
    }

    override fun getItemCount(): Int = places.size

    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName: android.widget.TextView = itemView.findViewById(R.id.text_place_name)
        private val textReminder: android.widget.TextView = itemView.findViewById(R.id.text_reminder)
        private val switchActive: android.widget.Switch = itemView.findViewById(R.id.switch_active)
        private val btnDelete: android.widget.ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(place: PlaceOfInterest) {
            textName.text = place.name
            textReminder.text = place.reminderText
            switchActive.isChecked = place.isActive

            itemView.setOnClickListener { onPlaceClick(place) }
            switchActive.setOnCheckedChangeListener { _, isChecked ->
                onPlaceToggle(place, isChecked)
            }
            btnDelete.setOnClickListener { onPlaceDelete(place) }
        }
    }
}
