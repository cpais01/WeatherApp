package wit.android.test.challengeWIT.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import wit.android.test.challengeWIT.R
import wit.android.test.challengeWIT.databinding.ActivityMainBinding
import wit.android.test.challengeWIT.model.CitiesList
import wit.android.test.challengeWIT.model.City
import wit.android.test.challengeWIT.utils.Utils
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(){
    private lateinit var binding : ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var coarseLocationPermission = false
    private var timeOfDay: String? = null
    private var locationEnabled = false
    private lateinit var shared : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        shared = getSharedPreferences("shared preferences", MODE_PRIVATE)
        binding.amLvCities.adapter = CitiesAdapter()
        timeOfDay = getString(R.string.MainActivity_TimeOfDay_Day)

        binding.amLvCities.setOnItemClickListener {parent,view, position, id ->
            val selectedItem = parent.getItemAtPosition(position) as City
            val intent = Intent(this, TemperatureActivity::class.java)
            intent.putExtra(getString(R.string.MainActivity_TemperatureActivity_PutExtra_Tag_CityName),selectedItem.name)
            intent.putExtra(getString(R.string.MainActivity_TemperatureActivity_PutExtra_Tag_TimeOfDay),timeOfDay)
            startActivity(intent)
            finish()
        }

        binding.amLlCurrentLocation.setOnClickListener {
            val currentCity = binding.amTvCurrentLocationValue.text.toString()
            if (currentCity.equals(resources.getString(R.string.ActivityMain_DefaultCityName)) || currentCity.isEmpty()){
                Toast.makeText(this, getString(R.string.MainActivity_onCreate_Msg_LocationNotAvailable), Toast.LENGTH_LONG).show()
            }else{
                val intent = Intent(this, TemperatureActivity::class.java)
                intent.putExtra(getString(R.string.MainActivity_TemperatureActivity_PutExtra_Tag_CityName),currentCity)
                intent.putExtra(getString(R.string.MainActivity_TemperatureActivity_PutExtra_Tag_TimeOfDay),timeOfDay)
                startActivity(intent)
                finish()
            }
        }

        binding.amFbPlus.setOnClickListener {
            val dialog = AddCityFragment()
            dialog.show(supportFragmentManager, getString(R.string.MainActivity_onCreate_Dialog_Tag))
        }
        verifyPermissions()
        verifyTimeOfDay()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
        getSharedPreferences()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        updateSharedPreferences()
    }

    private fun updateSharedPreferences(){
        val citiesName = ArrayList<String>()
        CitiesList.list.forEach {
            citiesName.add(it.name.toString())
        }
        val jsonArray = JSONArray(citiesName)
        with(shared.edit()){
            putString("list", jsonArray.toString())
            apply()
        }
    }

    private fun getSharedPreferences(){
        val value = shared.getString("list", null)
        val gson = GsonBuilder().create()
        val list = gson.fromJson<ArrayList<String>>(value, object :TypeToken<ArrayList<String>>(){}.type)
        if (list != null)
            updateCityList(list)
    }

    private fun updateCityList(list: ArrayList<String>) {
        CitiesList.list = ArrayList()
        addCities()

        list.forEach outer@{ itList ->
            CitiesList.list.forEach inner@{ itCitiesList ->
                if (itList.equals(itCitiesList.name, ignoreCase = true))
                    return@outer
            }
            val cityNameDrawable = itList.lowercase()
            val drawableName: String =
                getString(R.string.AddCityFragment_OnCreateView_DrawableName_Part1) + cityNameDrawable + getString(
                    R.string.AddCityFragment_OnCreateView_DrawableName_Part2
                )
            val resId = resources.getIdentifier(
                drawableName,
                getString(R.string.Resource_DefType_Drawable),
                this.packageName
            )
            if (resId != 0)
                CitiesList.list.add(City(cityNameDrawable, ContextCompat.getDrawable(this, resId)))
            else
                CitiesList.list.add(City(cityNameDrawable, ContextCompat.getDrawable(this, R.drawable.img_default_city)))
        }
    }

    private fun verifyPermissions(){
        coarseLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!coarseLocationPermission)
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION))
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        coarseLocationPermission = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (locationEnabled || !coarseLocationPermission)
            return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null)
                currentLocation = location
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        locationEnabled = true
    }

    private fun stopLocationUpdates(){
        if (!locationEnabled)
            return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        locationEnabled = false
    }

    private val locationRequest = LocationRequest.create().apply {
        interval = 10000
        fastestInterval = 2000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation = locationResult.lastLocation
            if (lastLocation !=null)
                currentLocation = lastLocation
        }
    }

    private var currentLocation = Location("")
        get() = field
        set(value){
            field = value
            updateCityData(value)
            binding.amLvCities.invalidateViews()
        }

    private fun updateCityData(location : Location){
        try{
            if (Utils.verifyNetworkState(this)){
                val geocoder = Geocoder(this)
                val address : List<Address> = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val cityName = address[0].locality

                binding.amTvCurrentLocationValue.text = cityName
            }else{
                Toast.makeText(this,getString(R.string.Msg_NoNetwork),Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.Msg_Location_Error),Toast.LENGTH_LONG).show()
        }
    }

    private fun verifyTimeOfDay(){
        timeOfDay = if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 8..19) getString(
            R.string.MainActivity_TimeOfDay_Day) else getString(
                    R.string.MainActivity_TimeOfDay_Night)

        if (timeOfDay == getString(R.string.MainActivity_TimeOfDay_Night)){
            val imgBackgroundName: String = getString(R.string.MainActivity_mainBackgroundImgName) + timeOfDay
            val resId = resources.getIdentifier(imgBackgroundName, getString(R.string.Resource_DefType_Drawable), applicationContext.packageName)
            binding.amLlBackgroundImg.background = ContextCompat.getDrawable(this, resId)
            binding.amTvTitle.setTextColor(ContextCompat.getColor(applicationContext, R.color.white))
        }
    }

    private fun addCities(){
        CitiesList.list.add(City("Lisbon", ContextCompat.getDrawable(this, R.drawable.img_lisbon_horizontal)))
        CitiesList.list.add(City("Madrid", ContextCompat.getDrawable(this, R.drawable.img_madrid_horizontal)))
        CitiesList.list.add(City("Paris", ContextCompat.getDrawable(this, R.drawable.img_paris_horizontal)))
        CitiesList.list.add(City("Berlin", ContextCompat.getDrawable(this, R.drawable.img_berlin_horizontal)))
        CitiesList.list.add(City("Copenhagen", ContextCompat.getDrawable(this, R.drawable.img_copenhagen_horizontal)))
        CitiesList.list.add(City("Rome", ContextCompat.getDrawable(this, R.drawable.img_rome_horizontal)))
        CitiesList.list.add(City("London", ContextCompat.getDrawable(this, R.drawable.img_london_horizontal)))
        CitiesList.list.add(City("Dublin", ContextCompat.getDrawable(this, R.drawable.img_dublin_horizontal)))
        CitiesList.list.add(City("Prague", ContextCompat.getDrawable(this, R.drawable.img_prague_horizontal)))
        CitiesList.list.add(City("Vienna", ContextCompat.getDrawable(this, R.drawable.img_vienna_horizontal)))
    }

    private class CitiesAdapter : BaseAdapter(){
        override fun getCount(): Int {
            return CitiesList.list.size
        }

        override fun getItem(position: Int): Any {
            return CitiesList.list[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        @SuppressLint("ViewHolder")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = LayoutInflater.from(parent!!.context).inflate(R.layout.item_weather_city, parent, false)
            val city = CitiesList.list[position]

            view.findViewById<TextView>(R.id.iWtvCityName).text = city.name
            view.findViewById<ImageView>(R.id.iWIvItem).background = city.backgroundImg
            view.findViewById<Button>(R.id.iwBtnDeleteItem).setOnClickListener {
                CitiesList.list.remove(city)
                this.notifyDataSetChanged()
            }
            return view
        }
    }
}