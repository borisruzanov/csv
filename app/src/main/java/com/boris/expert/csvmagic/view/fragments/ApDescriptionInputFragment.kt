package com.boris.expert.csvmagic.view.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boris.expert.csvmagic.R
import com.boris.expert.csvmagic.adapters.FieldListsAdapter
import com.boris.expert.csvmagic.model.ListItem
import com.boris.expert.csvmagic.utils.*
import com.boris.expert.csvmagic.view.activities.BaseActivity
import com.boris.expert.csvmagic.view.activities.FieldListsActivity
import com.boris.expert.csvmagic.view.activities.OcrActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.IOException


class ApDescriptionInputFragment : Fragment() {

    private lateinit var appSettings: AppSettings
    private lateinit var apDescriptionView: TextInputEditText
    private lateinit var tableGenerator: TableGenerator
    private var listId: Int? = null
    private lateinit var adapter: FieldListsAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appSettings = AppSettings(requireActivity())
        tableGenerator = TableGenerator(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_ap_description_input, container, false)

        initViews(v)

        return v
    }

    private fun initViews(view: View){
        apDescriptionView  = view.findViewById(R.id.ap_description)
        val apDescriptionSpinner = view.findViewById<AppCompatSpinner>(R.id.ap_description_options_spinner)
        val apDescriptionListBtn = view.findViewById<MaterialButton>(R.id.ap_description_list_with_fields_btn)
        val apDescriptionDefaultInputBox = view.findViewById<TextInputEditText>(R.id.ap_description_non_changeable_default_text_input)
        val apDescriptionListSpinner = view.findViewById<AppCompatSpinner>(R.id.ap_description_list_spinner)

        val apDescriptionCameraRecView = view.findViewById<LinearLayout>(R.id.ap_description_camera_layout)
        val apDescriptionImageRecView = view.findViewById<LinearLayout>(R.id.ap_description_images_layout)
        val apDescriptionVoiceRecView = view.findViewById<LinearLayout>(R.id.ap_description_voice_layout)

        apDescriptionCameraRecView.setOnClickListener {

            if (RuntimePermissionHelper.checkCameraPermission(
                    requireActivity(), Constants.CAMERA_PERMISSION
                )
            ) {
                BaseActivity.hideSoftKeyboard(requireActivity(), apDescriptionCameraRecView)
                pickImageFromCamera()
            }
        }
        apDescriptionImageRecView.setOnClickListener {

            if (RuntimePermissionHelper.checkCameraPermission(
                    requireActivity(),
                    Constants.READ_STORAGE_PERMISSION
                )
            ) {
                BaseActivity.hideSoftKeyboard(requireActivity(), apDescriptionImageRecView)
                pickImageFromGallery()
            }
        }
        apDescriptionVoiceRecView.setOnClickListener {

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
            }
            voiceResultLauncher.launch(intent)
        }

        apDescriptionView.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
              appSettings.putString("AP_PRODUCT_DESCRIPTION",s.toString())
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        val apDescriptionSpinnerSelectedPosition = appSettings.getInt("AP_DESCRIPTION_SPINNER_SELECTED_POSITION")
        val apDescriptionDefaultValue = appSettings.getString("AP_DESCRIPTION_DEFAULT_VALUE")
        val apDescriptionListId = appSettings.getInt("AP_DESCRIPTION_LIST_ID")
        apDescriptionSpinner.setSelection(apDescriptionSpinnerSelectedPosition)
        apDescriptionListBtn.setOnClickListener {
            openListWithFieldsDialog("ap_description")
        }
        if (apDescriptionSpinnerSelectedPosition == 1) {
            apDescriptionVoiceRecView.visibility = View.GONE
            apDescriptionCameraRecView.visibility = View.GONE
            apDescriptionImageRecView.visibility = View.GONE
            apDescriptionListBtn.visibility = View.GONE
            apDescriptionListSpinner.visibility = View.GONE
            apDescriptionDefaultInputBox.visibility = View.VISIBLE
            apDescriptionView.visibility = View.VISIBLE
            apDescriptionDefaultInputBox.setText(apDescriptionDefaultValue)
            apDescriptionView.setText(apDescriptionDefaultValue)
        } else if (apDescriptionSpinnerSelectedPosition == 2) {
            apDescriptionVoiceRecView.visibility = View.GONE
            apDescriptionCameraRecView.visibility = View.GONE
            apDescriptionImageRecView.visibility = View.GONE
            apDescriptionDefaultInputBox.visibility = View.GONE
            apDescriptionListBtn.visibility = View.VISIBLE
            apDescriptionView.visibility = View.GONE
            apDescriptionListSpinner.visibility = View.VISIBLE
            val listOptions: String = tableGenerator.getListValues(apDescriptionListId)
            val listValues = listOptions.split(",")
            if (listValues.isNotEmpty()){
                appSettings.putString("AP_PRODUCT_DESCRIPTION",listValues[0])
            }
            val apDescriptionSpinnerAdapter = ArrayAdapter(
                requireActivity(),
                android.R.layout.simple_spinner_item,
                listValues
            )
            apDescriptionSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            apDescriptionListSpinner.adapter = apDescriptionSpinnerAdapter

            apDescriptionListSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    appSettings.putString("AP_PRODUCT_DESCRIPTION",parent!!.selectedItem.toString())
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }

        } else if (apDescriptionSpinnerSelectedPosition == 3) {
            apDescriptionListBtn.visibility = View.GONE
            apDescriptionListSpinner.visibility = View.GONE
            apDescriptionDefaultInputBox.visibility = View.GONE
            apDescriptionCameraRecView.visibility = View.GONE
            apDescriptionImageRecView.visibility = View.GONE
            apDescriptionView.visibility = View.VISIBLE
            apDescriptionVoiceRecView.visibility = View.VISIBLE
        } else if (apDescriptionSpinnerSelectedPosition == 4) {
            apDescriptionListBtn.visibility = View.GONE
            apDescriptionListSpinner.visibility = View.GONE
            apDescriptionDefaultInputBox.visibility = View.GONE
            apDescriptionVoiceRecView.visibility = View.GONE
            apDescriptionImageRecView.visibility = View.GONE
            apDescriptionView.visibility = View.VISIBLE
            apDescriptionCameraRecView.visibility = View.VISIBLE
        } else if (apDescriptionSpinnerSelectedPosition == 5) {
            apDescriptionListBtn.visibility = View.GONE
            apDescriptionListSpinner.visibility = View.GONE
            apDescriptionDefaultInputBox.visibility = View.GONE
            apDescriptionVoiceRecView.visibility = View.GONE
            apDescriptionCameraRecView.visibility = View.GONE
            apDescriptionView.visibility = View.VISIBLE
            apDescriptionImageRecView.visibility = View.VISIBLE
        } else {
            apDescriptionVoiceRecView.visibility = View.GONE
            apDescriptionCameraRecView.visibility = View.GONE
            apDescriptionImageRecView.visibility = View.GONE
            apDescriptionListBtn.visibility = View.GONE
            apDescriptionDefaultInputBox.visibility = View.GONE
            apDescriptionListSpinner.visibility = View.GONE
            apDescriptionView.visibility = View.VISIBLE
        }

        apDescriptionDefaultInputBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                appSettings.putString("AP_DESCRIPTION_DEFAULT_VALUE", s.toString())
                appSettings.putString("AP_PRODUCT_DESCRIPTION",s.toString())
            }

        })

        apDescriptionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                appSettings.putInt("AP_DESCRIPTION_SPINNER_SELECTED_POSITION", position)
                if (position == 1) {
                    apDescriptionVoiceRecView.visibility = View.GONE
                    apDescriptionCameraRecView.visibility = View.GONE
                    apDescriptionImageRecView.visibility = View.GONE
                    apDescriptionListBtn.visibility = View.GONE
                    apDescriptionListSpinner.visibility = View.GONE
                    apDescriptionDefaultInputBox.visibility = View.VISIBLE
                    apDescriptionView.visibility = View.VISIBLE
                    apDescriptionDefaultInputBox.setText(apDescriptionDefaultValue)
                    apDescriptionView.setText(apDescriptionDefaultValue)
                } else if (position == 2) {
                    apDescriptionVoiceRecView.visibility = View.GONE
                    apDescriptionCameraRecView.visibility = View.GONE
                    apDescriptionImageRecView.visibility = View.GONE
                    apDescriptionDefaultInputBox.visibility = View.GONE
                    apDescriptionListBtn.visibility = View.VISIBLE
                    apDescriptionView.visibility = View.GONE
                    apDescriptionListSpinner.visibility = View.VISIBLE
                    val listOptions: String = tableGenerator.getListValues(apDescriptionListId)
                    val listValues = listOptions.split(",")
                    if (listValues.isNotEmpty()){
                        appSettings.putString("AP_PRODUCT_DESCRIPTION",listValues[0])
                    }
                    val apDescriptionSpinnerAdapter = ArrayAdapter(
                        requireActivity(),
                        android.R.layout.simple_spinner_item,
                        listValues
                    )
                    apDescriptionSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    apDescriptionListSpinner.adapter = apDescriptionSpinnerAdapter

                    apDescriptionListSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            appSettings.putString("AP_PRODUCT_DESCRIPTION",parent!!.selectedItem.toString())
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {

                        }

                    }

                } else if (position == 3) {
                    apDescriptionListBtn.visibility = View.GONE
                    apDescriptionListSpinner.visibility = View.GONE
                    apDescriptionDefaultInputBox.visibility = View.GONE
                    apDescriptionCameraRecView.visibility = View.GONE
                    apDescriptionImageRecView.visibility = View.GONE
                    apDescriptionView.visibility = View.VISIBLE
                    apDescriptionVoiceRecView.visibility = View.VISIBLE
                } else if (position == 4) {
                    apDescriptionListBtn.visibility = View.GONE
                    apDescriptionListSpinner.visibility = View.GONE
                    apDescriptionDefaultInputBox.visibility = View.GONE
                    apDescriptionVoiceRecView.visibility = View.GONE
                    apDescriptionImageRecView.visibility = View.GONE
                    apDescriptionView.visibility = View.VISIBLE
                    apDescriptionCameraRecView.visibility = View.VISIBLE
                } else if (position == 5) {
                    apDescriptionListBtn.visibility = View.GONE
                    apDescriptionListSpinner.visibility = View.GONE
                    apDescriptionDefaultInputBox.visibility = View.GONE
                    apDescriptionVoiceRecView.visibility = View.GONE
                    apDescriptionCameraRecView.visibility = View.GONE
                    apDescriptionView.visibility = View.VISIBLE
                    apDescriptionImageRecView.visibility = View.VISIBLE
                } else {
                    apDescriptionVoiceRecView.visibility = View.GONE
                    apDescriptionCameraRecView.visibility = View.GONE
                    apDescriptionImageRecView.visibility = View.GONE
                    apDescriptionListBtn.visibility = View.GONE
                    apDescriptionDefaultInputBox.visibility = View.GONE
                    apDescriptionListSpinner.visibility = View.GONE
                    apDescriptionView.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
    }

    private fun openListWithFieldsDialog(fieldType: String) {

        val listItems = mutableListOf<ListItem>()
        val layout =
            LayoutInflater.from(context).inflate(
                R.layout.list_with_fields_value_layout,
                null
            )
        val listWithFieldsValueRecyclerView =
            layout.findViewById<RecyclerView>(R.id.list_with_fields_recycler_view)
        listWithFieldsValueRecyclerView.layoutManager = LinearLayoutManager(context)
        listWithFieldsValueRecyclerView.hasFixedSize()
        adapter = FieldListsAdapter(requireActivity(), listItems as ArrayList<ListItem>)
        listWithFieldsValueRecyclerView.adapter = adapter


        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setView(layout)
        builder.setCancelable(true)
        val alert = builder.create()
        alert.show()
        val tempList = tableGenerator.getList()
        if (tempList.isNotEmpty()) {
            listItems.clear()
            listItems.addAll(tempList)
            adapter.notifyDataSetChanged()
        } else {
            adapter.notifyDataSetChanged()
        }


        adapter.setOnItemClickListener(object : FieldListsAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val listValue = listItems[position]
                listId = listValue.id
                val list = tableGenerator.getListValues(listId!!)
                if (list.isNotEmpty()) {
                    //selectedListTextView.text = listValue.value
                        appSettings.putInt("AP_DESCRIPTION_LIST_ID", listId!!)
                    appSettings.putString("AP_PRODUCT_DESCRIPTION",list.split(",")[0])
                    alert.dismiss()
                } else {
                    MaterialAlertDialogBuilder(requireActivity())
                        .setMessage(getString(R.string.field_list_value_empty_error_text))
                        .setNegativeButton(getString(R.string.cancel_text)) { dialog, which ->
                            dialog.dismiss()
                        }
                        .setPositiveButton(getString(R.string.add_text)) { dialog, which ->
                            dialog.dismiss()
                            addTableDialog(listId!!)
                        }
                        .create().show()
                }

            }

            override fun onAddItemClick(position: Int) {
                alert.dismiss()
                val intent = Intent(context, FieldListsActivity::class.java)
//                    intent.putExtra("TABLE_NAME", tableName)
//                    intent.putExtra("FLAG", "yes")
                requireActivity().startActivity(intent)
            }
        })
    }

    private fun addTableDialog(id: Int) {
        val listValueLayout = LayoutInflater.from(context).inflate(
            R.layout.add_list_value_layout,
            null
        )
        val heading = listValueLayout.findViewById<MaterialTextView>(R.id.dialog_heading)
        heading.text = getString(R.string.list_value_hint_text)
        val listValueInputBox = listValueLayout.findViewById<TextInputEditText>(R.id.add_list_value_input_field)
        val listValueAddBtn = listValueLayout.findViewById<MaterialButton>(R.id.add_list_value_btn)
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setView(listValueLayout)
        val alert = builder.create()
        alert.show()
        listValueAddBtn.setOnClickListener {
            if (listValueInputBox.text.toString().isNotEmpty()) {
                val value = listValueInputBox.text.toString().trim()
                tableGenerator.insertListValue(id, value)
                alert.dismiss()
            } else {
                BaseActivity.showAlert(
                    requireActivity(),
                    getString(R.string.add_list_value_error_text)
                )
            }
        }
    }

    private var voiceResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            // THIS LINE OF CODE WILL CHECK THE IMAGE HAS BEEN SELECTED OR NOT
            if (result.resultCode == Activity.RESULT_OK) {
                val spokenText: String =
                    result.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        .let { results ->
                            results!!.get(0)
                        }
                val currentPItemTitle = apDescriptionView.text.toString().trim()
                val stringBuilder = java.lang.StringBuilder()
                stringBuilder.append(currentPItemTitle)
                stringBuilder.append(spokenText)
                apDescriptionView.setText(stringBuilder.toString())
                appSettings.putString("AP_PRODUCT_DESCRIPTION",apDescriptionView.text.toString().trim())
            }
        }

    fun pickImageFromGallery() {
        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher1.launch(
            Intent.createChooser(
                pickPhoto, getString(R.string.choose_image_gallery)
            )
        )
    }

    private var resultLauncher1 =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val cropPicUri = CropImage.getPickImageResultUri(requireActivity(), data)
                cropImage(cropPicUri)
            }
        }

    private fun cropImage(imageUri: Uri) {

        CropImage.activity(imageUri)
            .setGuidelines(CropImageView.Guidelines.ON)
            .setMultiTouchEnabled(true)
            .start(requireActivity())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            val result = CropImage.getActivityResult(data)
            val imgUri = result.uri
            try {
                TextRecogniser.runTextRecognition(requireActivity(), apDescriptionView, imgUri)
                Handler(Looper.myLooper()!!).postDelayed({
                    appSettings.putString("AP_PRODUCT_DESCRIPTION",apDescriptionView.text.toString().trim())
                },2000)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        //super.onActivityResult(requestCode, resultCode, data)
    }

    fun pickImageFromCamera() {
        //        startActivity(Intent(context,OcrActivity::class.java))
        val takePictureIntent = Intent(context, OcrActivity::class.java)
        cameraResultLauncher1.launch(takePictureIntent)
    }

    private var cameraResultLauncher1 =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            // THIS LINE OF CODE WILL CHECK THE IMAGE HAS BEEN SELECTED OR NOT
            if (result.resultCode == Activity.RESULT_OK) {
                val text = result.data!!.getStringExtra("SCAN_TEXT")
                val currentPItemTitle = apDescriptionView.text.toString().trim()
                val stringBuilder = java.lang.StringBuilder()
                stringBuilder.append(currentPItemTitle)
                stringBuilder.append(text)
                apDescriptionView.setText(stringBuilder.toString())
                appSettings.putString("AP_PRODUCT_DESCRIPTION",apDescriptionView.text.toString().trim())
            }
        }

}