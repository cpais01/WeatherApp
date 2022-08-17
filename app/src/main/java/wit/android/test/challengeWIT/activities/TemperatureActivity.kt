package wit.android.test.challengeWIT.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONObject
import wit.android.test.challengeWIT.R
import wit.android.test.challengeWIT.databinding.ActivityTemperatureBinding
import wit.android.test.challengeWIT.utils.Utils
import kotlin.concurrent.thread

class TemperatureActivity : AppCompatActivity() {
    private lateinit var binding : ActivityTemperatureBinding
    private val webContentLocation : MutableLiveData<String?> = MutableLiveData()
    private val webContentWeatherData : MutableLiveData<String?> = MutableLiveData()
    private var strContent : String? = null
    private var latitude : String? = null
    private var longitude : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTemperatureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val cityName : String = intent.getStringExtra(getString(R.string.MainActivity_TemperatureActivity_PutExtra_Tag_CityName)).toString()
        val timeOfDay : String = intent.getStringExtra(getString(R.string.MainActivity_TemperatureActivity_PutExtra_Tag_TimeOfDay)).toString()
        val apikey = getString(R.string.ApiKey_OpenWeatherMap)
        binding.atTvTitle.text = cityName
        val cityNameAPI = cityName.lowercase()
        var imgBackgroundName: String = getString(R.string.TemperatureActivity_onCreate_V_ImgBackgroundName_Part1) + cityName + getString(
                    R.string.TemperatureActivity_onCreate_V_ImgBackgroundName_Part2) + timeOfDay
        var resId = resources.getIdentifier(imgBackgroundName, getString(R.string.Resource_DefType_Drawable), applicationContext.packageName)

        if (resId == 0){
            imgBackgroundName = getString(R.string.TemperatureActivity_onCreate_V_ImgBackgroundName_Part1) + getString(R.string.TemperatureActivity_unknown_city_drawable_name) + getString(
                R.string.TemperatureActivity_onCreate_V_ImgBackgroundName_Part2) + timeOfDay
            resId = resources.getIdentifier(imgBackgroundName, getString(R.string.Resource_DefType_Drawable), applicationContext.packageName)
        }

        binding.atLLFullBackground.background = ContextCompat.getDrawable(this, resId)
        binding.atLLFullBackground.background.alpha = 150

        binding.atBtnBack.setOnClickListener {
            val intentHome = Intent(this, MainActivity::class.java)
            startActivity(intentHome)
            finish()
        }

        if (!Utils.verifyNetworkState(this)) {
            Toast.makeText(this,getString(R.string.Msg_NoNetwork),Toast.LENGTH_LONG).show()
            finish()
            return
        }

        webContentWeatherData.observe(this){
            updateView()
        }

        webContentLocation.observe(this){
            if (strContent != null){
                setLocationVariables(strContent)
                val strWeather = "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$apikey&units=" + getString(
                    R.string.TemperatureActivity_updateView_temperatureUnit)
                getLocationDataAsync(strWeather, webContentWeatherData)
            }else{
                Log.e(getString(R.string.TemperatureActivity_Log_Tag), getString(R.string.Msg_Unable_Retrieve_Data))
                Toast.makeText(this, getString(R.string.Msg_Unable_Retrieve_Data), Toast.LENGTH_LONG).show()
            }
        }

        val strLocation = "http://api.openweathermap.org/geo/1.0/direct?q=$cityNameAPI&limit=1&appid=$apikey"
        getLocationDataAsync(strLocation, webContentLocation)
    }

    private fun getLocationDataAsync(str: String, result: MutableLiveData<String?>){
        thread {
            strContent = Utils.getData(str)
            result.postValue(strContent)
        }
    }

    private fun updateView(){
        try{
            val jsonObject = JSONObject(strContent!!)
            val main = jsonObject.getJSONObject(getString(R.string.TemperatureActivity_updateView_Json_Main))
            val temp = main[getString(R.string.TemperatureActivity_updateView_Json_Temp)]
            val tempMax = main[getString(R.string.TemperatureActivity_updateView_Json_TempMax)]
            val tempMin = main[getString(R.string.TemperatureActivity_updateView_Json_TempMin)]
            val humidity = main[getString(R.string.TemperatureActivity_updateView_Json_Humidity)]
            val pressure = main[getString(R.string.TemperatureActivity_updateView_Json_Pressure)]
            val windObject = jsonObject.getJSONObject(getString(R.string.TemperatureActivity_updateView_Json_Wind))
            val wind = windObject[getString(R.string.TemperatureActivity_updateView_Json_Speed)]
            val weatherJson = jsonObject.getJSONArray(getString(R.string.TemperatureActivity_updateView_Json_Weather)).getJSONObject(0)
            val weatherIcon = weatherJson[getString(R.string.TemperatureActivity_updateView_Json_Icon)]

            Picasso.get().load("http://openweathermap.org/img/wn/$weatherIcon@2x.png").into(binding.atIvTemperatureIcon)
            binding.atTvTemperature.text = getString(R.string.TemperatureActivity_updateView_tempMetric, temp.toString())
            binding.atTvTempMaxValue.text = getString(R.string.TemperatureActivity_updateView_tempMetric, tempMax.toString())
            binding.atTvTempMinValue.text = getString(R.string.TemperatureActivity_updateView_tempMetric, tempMin.toString())
            binding.atTvHumidityValue.text = humidity.toString() + getString(R.string.TemperatureActivity_updateView_humidityMetric)
            binding.atTvPressureValue.text = getString(R.string.TemperatureActivity_updateView_pressureMetric, pressure.toString())
            binding.atTvWindValue.text = getString(R.string.TemperatureActivity_updateView_windMetric, wind.toString())
        }catch (_ : Exception){
            Log.e(getString(R.string.TemperatureActivity_Log_Tag), getString(R.string.Msg_JsonError))
            Toast.makeText(this, getString(R.string.Msg_Unable_Retrieve_Data), Toast.LENGTH_LONG).show()
        }
    }

    private fun setLocationVariables(strContent: String?){
        try{
            val jsonArray = JSONArray(strContent)
            val jsonObject: JSONObject = jsonArray.getJSONObject(0)
            latitude = jsonObject.getString(getString(R.string.TemperatureActivity_setLocationVariables_Json_Lat))
            longitude = jsonObject.getString(getString(R.string.TemperatureActivity_setLocationVariables_Json_Lon))
        }catch (_ : Exception){
            Log.e(getString(R.string.TemperatureActivity_Log_Tag), getString(R.string.Msg_JsonError))
            Toast.makeText(this, getString(R.string.Msg_Unable_Retrieve_Data), Toast.LENGTH_LONG).show()
        }
    }
}