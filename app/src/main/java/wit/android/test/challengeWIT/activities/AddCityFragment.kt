package wit.android.test.challengeWIT.activities

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import wit.android.test.challengeWIT.R
import wit.android.test.challengeWIT.model.CitiesList
import wit.android.test.challengeWIT.model.City
import wit.android.test.challengeWIT.utils.Utils
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class AddCityFragment: DialogFragment() {
    private val webContentLocation : MutableLiveData<String?> = MutableLiveData()
    private var strContent : String? = null
    private var cityNameDrawable: String = ""

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
            dialog.window!!.setLayout(((ViewGroup.LayoutParams.MATCH_PARENT)*0.9).roundToInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }else{
            Log.e(getString(R.string.AddCityFragment_Log_Tag), getString(R.string.Msg_ErrorOpeningDialogBox))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView: View = inflater.inflate(R.layout.fragment_addcity_dialog, container, false)
        val addBtn: Button = rootView.findViewById(R.id.fAdBtnAdd)
        val cancelBtn: Button = rootView.findViewById(R.id.fAdBtnCancel)
        val cityNameEditText: EditText = rootView.findViewById(R.id.fAdEtCityName)

        cityNameEditText.postDelayed({
            cityNameEditText.requestFocus()
            showKeyboard(context, cityNameEditText)
        }, 500)

        webContentLocation.observe(this){
            if (strContent == null)
                Toast.makeText(view?.context, getString(R.string.AddCityFragment_Msg_InvalidName), Toast.LENGTH_LONG).show()
            else
                addCity(cityNameDrawable)
        }

        addBtn.setOnClickListener{
            verifyCityName(cityNameEditText, webContentLocation)
        }

        cancelBtn.setOnClickListener{
            dismiss()
        }

        return rootView
    }

    private fun addCity(cityNameDrawable: String){
        val drawableName: String = getString(R.string.AddCityFragment_OnCreateView_DrawableName_Part1) + cityNameDrawable + getString(
            R.string.AddCityFragment_OnCreateView_DrawableName_Part2)
        val resId = resources.getIdentifier(
            drawableName,
            getString(R.string.Resource_DefType_Drawable),
            context?.applicationContext?.packageName
        )
        var cityExists = false

        CitiesList.list.forEach {
            if (it.name?.lowercase()?.equals(cityNameDrawable) == true) {
                Toast.makeText(view?.context, getString(R.string.AddCityFragment_Msg_NameExist), Toast.LENGTH_SHORT).show()
                cityExists = true
                return
            }
        }
        if (!cityExists) {
            if (resId != 0)
                CitiesList.list.add(City(cityNameDrawable, ContextCompat.getDrawable(requireContext().applicationContext, resId)))
            else
                CitiesList.list.add(City(cityNameDrawable, ContextCompat.getDrawable(requireContext().applicationContext, R.drawable.img_default_city)))
        }
        dismiss()
    }

    private fun showKeyboard(mContext: Context?, view: View?) {
        if (mContext != null && view != null && view.requestFocus()) {
            val inputMethodManager =
                mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun verifyCityName(cityNameEditText: EditText, result: MutableLiveData<String?>){
        val apikey = getString(R.string.ApiKey_OpenWeatherMap)
        cityNameDrawable = cityNameEditText.text.toString().lowercase()
        val strLocation = "http://api.openweathermap.org/geo/1.0/direct?q=$cityNameDrawable&limit=1&appid=$apikey"

        if (TextUtils.isEmpty(cityNameEditText.text.toString())) {
            Toast.makeText(view?.context, getString(R.string.AddCityFragment_Msg_InvalidName), Toast.LENGTH_LONG).show()
        }else {
            thread {
                strContent = Utils.getData(strLocation)
                if (strContent.equals("[]\n")) {
                    Log.e(
                        getString(R.string.AddCityFragment_Log_Tag),
                        getString(R.string.AddCityFragment_Msg_InvalidName)
                    )
                    strContent = null
                    result.postValue(strContent)
                } else {
                    result.postValue(strContent)
                }
            }
        }
    }
}