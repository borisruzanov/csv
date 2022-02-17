package com.boris.expert.csvmagic.view.activities

import android.app.Activity
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.size
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.boris.expert.csvmagic.R
import com.boris.expert.csvmagic.adapters.BImagesAdapter
import com.boris.expert.csvmagic.adapters.BarcodeImageAdapter
import com.boris.expert.csvmagic.adapters.FeedbackAdapter
import com.boris.expert.csvmagic.adapters.InternetImageAdapter
import com.boris.expert.csvmagic.customviews.CustomTextInputEditText
import com.boris.expert.csvmagic.interfaces.APICallback
import com.boris.expert.csvmagic.interfaces.ResponseListener
import com.boris.expert.csvmagic.interfaces.UploadImageCallback
import com.boris.expert.csvmagic.model.CodeHistory
import com.boris.expert.csvmagic.model.Feedback
import com.boris.expert.csvmagic.model.TableObject
import com.boris.expert.csvmagic.room.AppViewModel
import com.boris.expert.csvmagic.utils.*
import com.boris.expert.csvmagic.viewmodel.CodeDetailViewModel
import com.boris.expert.csvmagic.viewmodelfactory.ViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import id.zelory.compressor.Compressor
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern


class CodeDetailActivity : BaseActivity(), View.OnClickListener,
    CustomAlertDialog.CustomDialogListener, View.OnFocusChangeListener {

    private var customAlertDialog: CustomAlertDialog? = null
    private lateinit var context: Context
    private lateinit var toolbar: Toolbar
    private var codeHistory: CodeHistory? = null
    private var tableObject: TableObject? = null
    private lateinit var tableGenerator: TableGenerator
    private lateinit var topImageCodeType: AppCompatImageView
    private lateinit var typeTextHeading: MaterialTextView
    private lateinit var encodeDataTextView: MaterialTextView
    private lateinit var clipboardCopyView: MaterialTextView
    private lateinit var textSearchButton: AppCompatImageButton
    private lateinit var textShareButton: AppCompatImageButton
    private lateinit var typeImageHeading: MaterialTextView
    private lateinit var typeImageView: AppCompatImageView
    private lateinit var pdfSaveButton: AppCompatImageButton
    private lateinit var pdfShareButton: AppCompatImageButton
    private lateinit var codeSequenceView: MaterialTextView
    private lateinit var dateTimeView: MaterialTextView
    private lateinit var dynamicLinkUpdateLayout: CardView
    private lateinit var barcodeDetailWrapperLayout: CardView
    private lateinit var feedbackDetailWrapperLayout: CardView
    private lateinit var feedbackRecyclerView: RecyclerView
    private lateinit var codeDetailNotesWrapperLayout: CardView
    private lateinit var barcodeDataView: FrameLayout
    private lateinit var barcodeImageView: CardView
    private lateinit var updateDynamicLinkInput: TextInputEditText
    private lateinit var updateDynamicButton: AppCompatButton
    private lateinit var protocolGroup: RadioGroup
    private lateinit var appViewModel: AppViewModel
    private lateinit var feedbackCsvExportImageView: AppCompatImageView
    private lateinit var qrCodeHistoryNotesInputField: TextInputEditText
    private lateinit var updateNotesBtn: AppCompatButton
    private lateinit var contentView: ConstraintLayout
    private lateinit var appSettings: AppSettings
    private lateinit var addImageFabBtn: FloatingActionButton
    //    private lateinit var viewModel: DynamicQrViewModel
    private lateinit var barcodeDetailParentLayout: LinearLayout
    private lateinit var dialogSubHeading: MaterialTextView
    private lateinit var tableName: String
    private lateinit var viewModel: CodeDetailViewModel
    var bitmap: Bitmap? = null
    private val pageWidth = 500
    private val pageHeight = 500
    private var pdfFile: File? = null
    private var isShareAfterCreated: Boolean = false
    var selectedProtocol = ""
    var barcodeEditList = mutableListOf<Triple<AppCompatImageView, String, String>>()
    private var counter: Int = 0
    private var csvTableObject:List<Pair<String,String>>?=null
    private var isExternalStorageRequest = false
    private var userCurrentCredits = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_detail)
        initViews()
        setUpToolbar()
        displayCodeDetails()

    }

    // THIS FUNCTION WILL INITIALIZE ALL THE VIEWS AND REFERENCE OF OBJECTS
    private fun initViews() {
        context = this
        appSettings = AppSettings(context)
        tableGenerator = TableGenerator(context)
        viewModel = ViewModelProviders.of(
            this,
            ViewModelFactory(CodeDetailViewModel()).createFor()
        )[CodeDetailViewModel::class.java]
        appViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(this.application)
        ).get(AppViewModel::class.java)
        toolbar = findViewById(R.id.toolbar)
        if (intent != null && intent.hasExtra("HISTORY_ITEM")) {
            codeHistory = intent.getSerializableExtra("HISTORY_ITEM") as CodeHistory
        }

        if (intent != null && intent.hasExtra("TABLE_ITEM")) {
            tableObject = intent.getSerializableExtra("TABLE_ITEM") as TableObject
        }
        if (intent != null && intent.hasExtra("TABLE_NAME")) {
            tableName = intent.getStringExtra("TABLE_NAME") as String
        }

        if (tableName.contains("import")){
            if (Constants.csvItemData != null) {
                csvTableObject = Constants.csvItemData
            }
        }
        addImageFabBtn = findViewById(R.id.add_image_fab)
        topImageCodeType = findViewById(R.id.code_detail_top_image_type)
        typeTextHeading = findViewById(R.id.code_detail_type_text_heading)
        encodeDataTextView = findViewById(R.id.code_detail_encode_data)
        clipboardCopyView = findViewById(R.id.code_detail_clipboard_copy_view)
        clipboardCopyView.setOnClickListener(this)
        textSearchButton = findViewById(R.id.code_detail_text_search_button)
        textSearchButton.setOnClickListener(this)
        textShareButton = findViewById(R.id.code_detail_text_share_button)
        textShareButton.setOnClickListener(this)
        typeImageHeading = findViewById(R.id.code_detail_type_image_heading)
        typeImageView = findViewById(R.id.code_detail_image_type)
        pdfSaveButton = findViewById(R.id.code_detail_pdf_save_button)
        pdfSaveButton.setOnClickListener(this)
        pdfShareButton = findViewById(R.id.code_detail_pdf_share_button)
        pdfShareButton.setOnClickListener(this)
        codeSequenceView = findViewById(R.id.code_detail_code_sequence_view)
        dateTimeView = findViewById(R.id.code_detail_date_time_view)
        dynamicLinkUpdateLayout = findViewById(R.id.code_detail_dynamic_link_update_layout)
        barcodeDataView = findViewById(R.id.code_detail_top_framelayout)
        barcodeImageView = findViewById(R.id.barcode_image_detail_layout)
        barcodeDetailWrapperLayout = findViewById(R.id.code_detail_table_layout)
        feedbackDetailWrapperLayout = findViewById(R.id.code_detail_feedback_layout)
        feedbackRecyclerView = findViewById(R.id.code_detail_feedback_recyclerview)
        feedbackCsvExportImageView = findViewById(R.id.code_detail_feedback_csv_export_image)
        updateDynamicLinkInput = findViewById(R.id.qr_code_history_dynamic_link_input_field)
        updateDynamicButton = findViewById(R.id.dynamic_link_update_btn)
        updateDynamicButton.setOnClickListener(this)
        protocolGroup = findViewById(R.id.http_protocol_group)
        barcodeDetailParentLayout = findViewById(R.id.barcode_detail_wrapper_layout)
        dialogSubHeading = findViewById(R.id.dialog_sub_heading)
        contentView = findViewById(R.id.code_detail_parent_layout)
        protocolGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.http_protocol_rb -> {
                    selectedProtocol = "http://"
                }
                R.id.https_protocol_rb -> {
                    selectedProtocol = "https://"
                }
                else -> {

                }
            }
        }
        codeDetailNotesWrapperLayout = findViewById(R.id.code_detail_notes)
        qrCodeHistoryNotesInputField = findViewById(R.id.qr_code_history_notes_input_field)
        updateNotesBtn = findViewById(R.id.update_notes_btn)
        updateNotesBtn.setOnClickListener(this)

    }

    // THIS FUNCTION WILL RENDER THE ACTION BAR/TOOLBAR
    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = getString(R.string.code_detail_text)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setTitleTextColor(ContextCompat.getColor(context, R.color.black))
    }

    // THIS FUNCTION WILL BIND THE HISTORY CODE DETAIL
    private fun displayCodeDetails() {
        if (codeHistory != null) {
            codeDetailNotesWrapperLayout.visibility = View.VISIBLE
            if (codeHistory!!.codeType == "barcode") {
                topImageCodeType.setImageResource(R.drawable.barcode)
                typeTextHeading.text = getString(R.string.barcode_text_data_heding)
                typeImageHeading.text = getString(R.string.barcode_image_heading)
                typeImageView.setImageResource(R.drawable.barcode)
            } else {
                topImageCodeType.setImageResource(R.drawable.ic_qr_code)
                typeTextHeading.text = getString(R.string.qr_text_data_heading)
                typeImageHeading.text = getString(R.string.qr_image_heading)
                typeImageView.setImageResource(R.drawable.qrcode)
            }
            encodeDataTextView.text = codeHistory!!.data
            codeSequenceView.text = "${getString(R.string.code_text)} ${codeHistory!!.id}"
            dateTimeView.text = getFormattedDate(context, codeHistory!!.createdAt.toLong())
            if (codeHistory!!.notes.isEmpty()) {
                qrCodeHistoryNotesInputField.hint = getString(R.string.notes)
            } else {
                qrCodeHistoryNotesInputField.setText(codeHistory!!.notes)

            }

            if (codeHistory!!.type == "feedback") {
                displayFeedbacksDetail(codeHistory!!.qrId)
            } else {

                if (codeHistory!!.isDynamic.toInt() == 1) {
                    dynamicLinkUpdateLayout.visibility = View.VISIBLE
                    dialogSubHeading.text =
                        "${getString(R.string.current_link_text)} ${codeHistory!!.data}"
                } else {
                    dynamicLinkUpdateLayout.visibility = View.GONE

                }

            }

        } else {
            if (tableObject != null) {
                codeDetailNotesWrapperLayout.visibility = View.GONE
                displayBarcodeDetail()
            }
            else{
                if (csvTableObject != null){
                    codeDetailNotesWrapperLayout.visibility = View.GONE
                    addImageFabBtn.setOnClickListener {
                        showColumnNamesDialog()
                    }
                    addImageFabBtn.visibility = View.VISIBLE
                    displayBarcodeDetail()
                }
            }
        }

    }

    var feedbacksList = mutableListOf<Feedback>()
    lateinit var feedbackAdapter: FeedbackAdapter
    private fun displayFeedbacksDetail(qrId: String) {

        feedbackRecyclerView.layoutManager = LinearLayoutManager(context)
        feedbackRecyclerView.hasFixedSize()
        feedbackAdapter = FeedbackAdapter(context, feedbacksList as ArrayList<Feedback>)
        feedbackRecyclerView.adapter = feedbackAdapter
        feedbackCsvExportImageView.setOnClickListener {
            exportCsv()
        }
        startLoading(context)
        viewModel.callFeedbacks(context, qrId)
        viewModel.getAllFeedbacks().observe(this, { response ->
            dismiss()
            if (response != null) {
                feedbacksList.addAll(response.feedbacks)
                if (feedbacksList.size > 0) {
                    feedbackDetailWrapperLayout.visibility = View.VISIBLE
                    feedbackAdapter.notifyItemRangeChanged(0, feedbacksList.size)
                    feedbackAdapter.setOnItemClickListener(object :
                        FeedbackAdapter.OnItemClickListener {
                        override fun onItemClick(position: Int) {
                            val item = feedbacksList[position]
                            val sharingText =
                                "Feedback: ${item.comment}\nEmail: ${item.email}\nPhone: ${item.phone}\nStars: ${item.rating}\n ${
                                    getString(
                                        R.string.qr_sign
                                    )
                                }"
                            MaterialAlertDialogBuilder(context)
                                .setMessage(sharingText)
                                .setNegativeButton(getString(R.string.cancel_text)) { dialog, which ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton(getString(R.string.share_text)) { dialog, which ->
                                    dialog.dismiss()
                                    shareFeedback(sharingText)
                                }
                                .create().show()
                        }

                    })
                } else {
                    feedbackDetailWrapperLayout.visibility = View.GONE
                }
            }
        })

    }

    var columnsAlert: AlertDialog?=null
    var selectedColumn = ""
    var isCsvAttachImages = false
    private fun showColumnNamesDialog(){
        val columns = mutableListOf<String>()
        columns.addAll(tableGenerator.getTableColumns(tableName)!!.asList())
        columns.removeAt(0)
        selectedColumn = columns[0]
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(getString(R.string.choose_column_attach_image_text))
        builder.setSingleChoiceItems(columns.toTypedArray(),0
        ) { dialog, which ->
            selectedColumn = columns[which]
            isCsvAttachImages = true
            columnsAlert!!.dismiss()
            openAddImageDialog()
        }
        columnsAlert = builder.create()
        columnsAlert!!.setCanceledOnTouchOutside(false)
        columnsAlert!!.show()
    }

    private fun exportCsv() {
        if (feedbacksList.isNotEmpty()) {
            startLoading(context)
            val builder = StringBuilder()
            builder.append("id,qrId,comment,email,phone,rating")

            for (j in 0 until feedbacksList.size) {

                val data = feedbacksList[j]

                builder.append("\n${data.id},${data.qrId},${data.comment},${data.email},${data.phone},${data.rating}")
            }

            try {
                val fileName = "feedbacks_${feedbacksList[0].qrId}.csv"
                val out = openFileOutput(fileName, Context.MODE_PRIVATE)
                out.write((builder.toString()).toByteArray())
                out.close()

                val file = File(filesDir, fileName)
                val path =
                    FileProvider.getUriForFile(context, "com.expert.qrgenerator.fileprovider", file)
                dismiss()
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/csv"
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.putExtra(Intent.EXTRA_STREAM, path)
                startActivity(Intent.createChooser(intent, getString(R.string.share_using)))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            showAlert(context, getString(R.string.table_export_error_text))
        }
    }

    private fun shareFeedback(sharingText: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, sharingText)
        startActivity(Intent.createChooser(intent, getString(R.string.share_using)))
    }

    // THIS FUNCTION WILL HANDLE THE ON BACK ARROW CLICK EVENT
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.code_detail_clipboard_copy_view -> {
                val clipboard: ClipboardManager =
                    getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(
                    clipboard.primaryClipDescription!!.label,
                    encodeDataTextView.text.toString()
                )
                clipboard.setPrimaryClip(clip)
                Toast.makeText(
                    context,
                    getString(R.string.text_saved_clipboard),
                    Toast.LENGTH_SHORT
                ).show()
            }
            R.id.code_detail_text_search_button -> {
                val escapedQuery: String = URLEncoder.encode(
                    encodeDataTextView.text.toString().trim(), "UTF-8"
                )
                val intent = Intent(Intent.ACTION_WEB_SEARCH)
                intent.putExtra(SearchManager.QUERY, escapedQuery)
                startActivity(intent)
            }
            R.id.code_detail_text_share_button -> {
                textShare()
            }
            R.id.code_detail_pdf_save_button -> {
                if (RuntimePermissionHelper.checkStoragePermission(
                        context,
                        Constants.READ_STORAGE_PERMISSION
                    )
                ) {
                    createPdf(false)
                }
            }
            R.id.code_detail_pdf_share_button -> {
                isShareAfterCreated = true
                if (RuntimePermissionHelper.checkStoragePermission(
                        context,
                        Constants.READ_STORAGE_PERMISSION
                    )
                ) {
                    if (pdfFile == null) {
                        createPdf(true)
                    } else {
                        sharePdfFile()
                    }
                }

            }
            R.id.dynamic_link_update_btn -> {
                val value = updateDynamicLinkInput.text.toString().trim()
                if (selectedProtocol.isEmpty()) {
                    showAlert(
                        context,
                        getString(R.string.protocol_error)
                    )
                } else if (value.isEmpty()) {

                    showAlert(
                        context,
                        getString(R.string.required_data_input_error)
                    )

                } else if (value.contains("http://") || value.contains("https://")
                ) {
                    showAlert(
                        context,
                        getString(R.string.without_protocol_error)
                    )
                } else if (!Pattern.compile("^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?\$")
                        .matcher(value).find()
                ) {
                    showAlert(
                        context,
                        getString(R.string.valid_website_error)
                    )
                } else {
                    val hashMap = hashMapOf<String, String>()
                    hashMap["login"] = codeHistory!!.login
                    hashMap["qrId"] = codeHistory!!.qrId
                    hashMap["userUrl"] = "$selectedProtocol$value"
                    hashMap["userType"] = codeHistory!!.userType

                    startLoading(context)
                    viewModel.createDynamicQrCode(context, hashMap)
                    viewModel.getDynamicQrCode().observe(this, { response ->
                        var url = ""
                        dismiss()
                        if (response != null) {
                            url = response.get("generatedUrl").asString
                            url = if (url.contains(":8990")) {
                                url.replace(":8990", "")
                            } else {
                                url
                            }
                            dialogSubHeading.text =
                                "${getString(R.string.current_link_text)} $selectedProtocol$value"
                            encodeDataTextView.text = "$selectedProtocol$value"
                            appViewModel.update("$selectedProtocol$value", url, codeHistory!!.id)
                            showAlert(context, getString(R.string.dynamic_update_success_text))
                        } else {
                            showAlert(context, getString(R.string.something_wrong_error))
                        }
                    })
                }
            }
            R.id.update_notes_btn -> {
                val notesText = qrCodeHistoryNotesInputField.text.toString().trim()
                if (notesText.isNotEmpty()) {
                    codeHistory!!.notes = notesText
                    appViewModel.updateHistory(codeHistory!!)
//                    startLoading(context)
//                    val columns = tableGenerator.getTableColumns(tableName)
//                    if (columns!!.joinToString(",").contains("notes")){
//                        val isSuccess = tableGenerator.updateBarcodeDetail(tableName,"notes",notesText,tableObject!!.id)
//                        if (isSuccess){
                    Toast.makeText(
                        context,
                        getString(R.string.notes_update_success_text),
                        Toast.LENGTH_SHORT
                    ).show()
                    qrCodeHistoryNotesInputField.clearFocus()
                    hideSoftKeyboard(context, qrCodeHistoryNotesInputField)
//                            dismiss()
//                        }
//                    }
//                    else{
//                        tableGenerator.addNewColumn(tableName, Pair("notes","TEXT"),"")
//                        Handler(Looper.myLooper()!!).postDelayed({
//                            val isSuccess = tableGenerator.updateBarcodeDetail(tableName,"notes",notesText,tableObject!!.id)
//                            if (isSuccess){
//                                Toast.makeText(context,getString(R.string.notes_update_success_text),Toast.LENGTH_SHORT).show()
//                                dismiss()
//                            }
//                        },5000)
//
//                    }
                } else {
                    showAlert(context, getString(R.string.empty_text_error))
                }
            }
            else -> {

                val position = v.id
                val id = barcodeEditList[0].second.toInt()
                val triple = if (position == barcodeEditList.size-1) {
                    barcodeEditList[position - 1]
                } else {
                    barcodeEditList[position + 1]
                }


                //Toast.makeText(context, triple.second,Toast.LENGTH_SHORT).show()
                updateBarcodeDetail(id, triple)
            }
        }
    }

    private lateinit var updateInputBox: CustomTextInputEditText
    private fun updateBarcodeDetail(id: Int, triple: Triple<AppCompatImageView, String, String>) {
        val updateBarcodeLayout =
            LayoutInflater.from(context).inflate(R.layout.update_barcode_detail_dialog, null)
        updateInputBox =
            updateBarcodeLayout.findViewById(R.id.update_barcode_detail_text_input_field)
        updateInputBox.onFocusChangeListener = this
        val cleanBrushView =
            updateBarcodeLayout.findViewById<AppCompatImageView>(R.id.update_barcode_detail_cleaning_text_view)
        val cancelBtn =
            updateBarcodeLayout.findViewById<MaterialButton>(R.id.update_barcode_detail_dialog_cancel_btn)
        val updateBtn =
            updateBarcodeLayout.findViewById<MaterialButton>(R.id.update_barcode_detail_dialog_update_btn)

        val imageRecognitionBtn =
            updateBarcodeLayout.findViewById<LinearLayout>(R.id.image_recognition_btn)
        val photoRecognitionBtn =
            updateBarcodeLayout.findViewById<LinearLayout>(R.id.photo_recognition_btn)


        val builder = MaterialAlertDialogBuilder(context)
        builder.setView(updateBarcodeLayout)
        builder.setCancelable(false)
        val alert = builder.create()
        alert.show()
        cancelBtn.setOnClickListener {
            hideSoftKeyboard(context, cancelBtn)
            updateInputBox.clearFocus()
            alert.dismiss()
        }

        updateInputBox.setOnClickListener {
            updateInputBox.requestFocus()
        }

        imageRecognitionBtn.setOnClickListener {
            if (RuntimePermissionHelper.checkCameraPermission(
                    context,
                    Constants.READ_STORAGE_PERMISSION
                )
            ) {
                hideSoftKeyboard(context, updateInputBox)
                pickImageFromGallery()
            }
        }

        photoRecognitionBtn.setOnClickListener {
            if (RuntimePermissionHelper.checkCameraPermission(
                    context, Constants.CAMERA_PERMISSION
                )
            ) {
                hideSoftKeyboard(context, updateInputBox)
                pickImageFromCamera()
            }
        }


        cleanBrushView.setOnClickListener { updateInputBox.setText("") }

        updateBtn.setOnClickListener {

            val value = updateInputBox.text.toString().trim()
            if (value.isNotEmpty()) {
                hideSoftKeyboard(context, updateBtn)
                alert.dismiss()
                val isUpdate = tableGenerator.updateBarcodeDetail(
                    tableName,
                    triple.third,
                    value,
                    id
                )
                if (isUpdate) {
                    if (tableName.contains("import")){
                        csvTableObject = tableGenerator.getUpdateBarcodeDetail1(tableName, id)
                        displayBarcodeDetail()
                    }
                    else{
                        tableObject = tableGenerator.getUpdateBarcodeDetail(tableName, id)
                        displayBarcodeDetail()
                    }

                }
            } else {
                Toast.makeText(context, getString(R.string.empty_text_error), Toast.LENGTH_SHORT)
                    .show()
            }
        }

        updateInputBox.setText(triple.second)
        updateInputBox.setSelection(updateInputBox.text!!.length)
        updateInputBox.requestFocus()
//        Constants.openKeyboar(context)

    }

    private fun pickImageFromGallery() {
        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(
            Intent.createChooser(
                pickPhoto, getString(R.string.choose_image_gallery)
            )
        )

    }

    private fun pickImageFromGallery1() {
        val fileIntent = Intent(Intent.ACTION_PICK)
        fileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        fileIntent.type = "image/*"
        resultLauncher1.launch(fileIntent)
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val cropPicUri = CropImage.getPickImageResultUri(this, data)
                cropImage(cropPicUri)
            }

        }

    private var resultLauncher1 =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//                if (result.resultCode == Activity.RESULT_OK) {
//                    val data: Intent? = result.data
//                    val cropPicUri = CropImage.getPickImageResultUri(this, data)
//                    cropImage(cropPicUri)
//                }
            if (result.resultCode == Activity.RESULT_OK) {
//                val path: String? = null
                val data: Intent? = result.data
                val clipData: ClipData? = data!!.clipData

                if (clipData != null) {
                    if (clipData.itemCount > 0) {
                        for (i in 0 until clipData.itemCount) {
                            val imageUri = clipData.getItemAt(i).uri
                            multiImagesList.add(
                                ImageManager.getRealPathFromUri(
                                    context,
                                    imageUri
                                )!!
                            )
                        }
                        filePathView!!.text = multiImagesList.joinToString(",")
                        barcodeImageList.clear()
                        barcodeImageList.addAll(multiImagesList)
                        adapter.notifyDataSetChanged()
                        //Log.d("TEST199",multiImagesList.toString())
                    }
                } else {
                    if (data.data != null) {
                        val imageUri = data.data!!
                        multiImagesList.add(
                            ImageManager.getRealPathFromUri(
                                context,
                                imageUri
                            )!!
                        )
                        filePathView!!.text = multiImagesList.joinToString(",")
                        barcodeImageList.clear()
                        barcodeImageList.addAll(multiImagesList)
                        adapter.notifyDataSetChanged()
                    }
                }
//                totalImageSize = if (filePathView!!.text.contains(",")) {
                getTotalImagesSize(filePathView!!.text.split(",").toMutableList())
//                } else {
//                    ImageManager.getFileSize(filePathView!!.text.toString())
//                }
                Log.d("TEST199", "$totalImageSize")
            }
        }

    private var cameraResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->


//                // THIS LINE OF CODE WILL CHECK THE IMAGE HAS BEEN SELECTED OR NOT
            if (result.resultCode == Activity.RESULT_OK) {
                val text = result.data!!.getStringExtra("SCAN_TEXT")
                updateInputBox.setText(text)
                updateInputBox.setSelection(updateInputBox.text.toString().length)
//                val data: Intent? = result.data
//                val bitmap = data!!.extras!!.get("data") as Bitmap
//                val file = ImageManager.readWriteImage(context,bitmap)
//                cropImage(Uri.fromFile(file))
            }
        }

    private var cameraResultLauncher1 =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            // THIS LINE OF CODE WILL CHECK THE IMAGE HAS BEEN SELECTED OR NOT
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val bitmap = data!!.extras!!.get("data") as Bitmap
                createImageFile(bitmap)

//                totalImageSize = if (filePathView!!.text.contains(",")) {
                getTotalImagesSize(filePathView!!.text.split(",").toMutableList())
//                } else {
//                    ImageManager.getFileSize(filePathView!!.text.toString())
//                }

                Log.d("TEST199", "$totalImageSize")
            }

//                // THIS LINE OF CODE WILL CHECK THE IMAGE HAS BEEN SELECTED OR NOT
//                if (result.resultCode == Activity.RESULT_OK) {
//                    val text = result.data!!.getStringExtra("SCAN_TEXT")
//                    updateInputBox.setText(text)
//                    updateInputBox.setSelection(updateInputBox.text.toString().length)
////                val data: Intent? = result.data
////                val bitmap = data!!.extras!!.get("data") as Bitmap
////                val file = ImageManager.readWriteImage(context,bitmap)
////                cropImage(Uri.fromFile(file))
//                }
        }

    var currentPhotoPath: String? = null
    var totalImageSize: Long = 0
    private fun createImageFile(bitmap: Bitmap) {
        currentPhotoPath = ImageManager.readWriteImage(context, bitmap).absolutePath
        //Constants.captureImagePath = currentPhotoPath
        multiImagesList.add(currentPhotoPath!!)
        filePathView!!.text = multiImagesList.joinToString(",")
        barcodeImageList.clear()
        barcodeImageList.addAll(multiImagesList)
        adapter.notifyDataSetChanged()
    }

    private fun getTotalImagesSize(uploadedUrlList: MutableList<String>) {
        //var total: Long = 0
        totalImageSize = 0
        for (i in 0 until uploadedUrlList.size) {
            Handler(Looper.myLooper()!!).postDelayed({
                lifecycleScope.launch {
                    totalImageSize += if (Constants.compressorFeatureStatus == 0){
                        //val compressedImageFile = Compressor.compress(context, File(uploadedUrlList[i]))
                        ImageManager.getFileSize(File(uploadedUrlList[i]).absolutePath)
                    } else{
                        val compressedImageFile = Compressor.compress(context, File(uploadedUrlList[i]))
                        ImageManager.getFileSize(compressedImageFile.absolutePath)
                    }
                    Log.d("TEST199TOTALSIZE", "$totalImageSize")
                }
            }, (1000 * i + 1).toLong())
        }
//        return totalImageSize
    }

    private fun pickImageFromCamera() {
        val takePictureIntent = Intent(context, OcrActivity::class.java)
        cameraResultLauncher.launch(takePictureIntent)

    }

    private fun pickImageFromCamera1() {

        val cameraIntent =
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraResultLauncher1.launch(cameraIntent)
    }

    private fun cropImage(imageUri: Uri) {

        CropImage.activity(imageUri)
            .setGuidelines(CropImageView.Guidelines.ON)
            .setMultiTouchEnabled(true)
            .start(this)
    }

    private var imageList = mutableListOf<String>()
    private fun displayBarcodeDetail() {
        if (tableObject != null) {
            counter = 0
            barcodeDetailWrapperLayout.visibility = View.VISIBLE
            encodeDataTextView.text = tableObject!!.code_data
            codeSequenceView.text = "${getString(R.string.code_text)} ${tableObject!!.id}"
            dateTimeView.text = tableObject!!.date

            if (barcodeDetailParentLayout.childCount > 0) {
                barcodeDetailParentLayout.removeAllViews()
            }

            barcodeEditList.add(
                Triple(
                    AppCompatImageView(context),
                    tableObject!!.id.toString(),
                    "id"
                )
            )
            val codeDataLayout = LayoutInflater.from(context)
                .inflate(R.layout.barcode_detail_item_row, barcodeDetailParentLayout, false)
            val codeDataColumnValue =
                codeDataLayout.findViewById<com.borjabravo.readmoretextview.ReadMoreTextView>(R.id.bcd_table_column_value)
            val codeDataColumnName =
                codeDataLayout.findViewById<MaterialTextView>(R.id.bcd_table_column_name)
            val codeDataColumnEditView =
                codeDataLayout.findViewById<AppCompatImageView>(R.id.bcd_edit_view)
            codeDataColumnEditView.id = counter
            barcodeEditList.add(
                Triple(
                    codeDataColumnEditView,
                    tableObject!!.code_data,
                    "code_data"
                )
            )
            codeDataColumnEditView.setOnClickListener(this)
            codeDataColumnValue.text = tableObject!!.code_data
            codeDataColumnName.text = "code_data"
            barcodeDetailParentLayout.addView(codeDataLayout)
            val dateLayout = LayoutInflater.from(context)
                .inflate(R.layout.barcode_detail_item_row, barcodeDetailParentLayout, false)
            val dateColumnValue =
                dateLayout.findViewById<com.borjabravo.readmoretextview.ReadMoreTextView>(R.id.bcd_table_column_value)
            val dateColumnName =
                dateLayout.findViewById<MaterialTextView>(R.id.bcd_table_column_name)
            val dateColumnEditView = dateLayout.findViewById<AppCompatImageView>(R.id.bcd_edit_view)
            counter += 1
            dateColumnEditView.id = counter
            barcodeEditList.add(Triple(dateColumnEditView, tableObject!!.date, "date"))
            dateColumnEditView.setOnClickListener(this)
            dateColumnValue.text = tableObject!!.date
            dateColumnName.text = "date"
            barcodeDetailParentLayout.addView(dateLayout)
            val imageLayout = LayoutInflater.from(context)
                .inflate(R.layout.barcode_detail_item_row, barcodeDetailParentLayout, false)
            val imageColumnValue =
                imageLayout.findViewById<com.borjabravo.readmoretextview.ReadMoreTextView>(R.id.bcd_table_column_value)
            val imageColumnName =
                imageLayout.findViewById<MaterialTextView>(R.id.bcd_table_column_name)
            val imageColumnEditView =
                imageLayout.findViewById<AppCompatImageView>(R.id.bcd_edit_view)
            //counter += 1
//            imageColumnEditView.id = counter
//            barcodeEditList.add(Triple(imageColumnEditView, tableObject!!.image, "image"))
//            imageColumnEditView.setOnClickListener(this)
//            imageColumnValue.text = tableObject!!.image
//            imageColumnName.text = "image"
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(10, 5, 5, 5)
            }
            if (tableObject!!.image.isNotEmpty()) {
                if (imageList.size > 0) {
                    imageList.clear()
                }
                if (tableObject!!.image.contains(" ")) {
                    imageList.addAll(tableObject!!.image.split(" ").toList())
                } else {
                    imageList.add(tableObject!!.image)
                }


                val barcodeImageRecyclerView = RecyclerView(context)
                barcodeImageRecyclerView.setBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.light_gray
                    )
                )
                barcodeImageRecyclerView.layoutParams = params
                barcodeImageRecyclerView.layoutManager =
                    LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                barcodeImageRecyclerView.hasFixedSize()
                val adapter = BarcodeImageAdapter(
                    context,
                    imageList as ArrayList<String>
                )
                barcodeImageRecyclerView.adapter = adapter
                adapter.setOnItemClickListener(object : BarcodeImageAdapter.OnItemClickListener {
                    override fun onItemDeleteClick(position: Int) {
                        val builder = MaterialAlertDialogBuilder(context)
                        builder.setMessage(getString(R.string.delete_barcode_image_message))
                        builder.setCancelable(false)
                        builder.setNegativeButton(getString(R.string.no_text)) { dialog, which ->
                            dialog.dismiss()
                        }
                        builder.setPositiveButton(getString(R.string.yes_text)) { dialog, which ->
                            dialog.dismiss()
                            imageList.removeAt(position)

                            tableGenerator.updateBarcodeDetail(
                                tableName, "image", if (imageList.size > 0) {
                                    imageList.joinToString(",")
                                } else {
                                    ""
                                }, tableObject!!.id
                            )
                            adapter.notifyItemRemoved(position)
                        }
                        val alert = builder.create()
                        alert.show()
                    }

                    override fun onAddItemEditClick(position: Int) {

                    }

                    override fun onImageClick(position: Int) {
                        val url = imageList[position]
                        openLink(context, url)
                    }

                })
                barcodeDetailParentLayout.addView(barcodeImageRecyclerView)
            } else {
//                val emptyTextView = MaterialTextView(context)
//                emptyTextView.layoutParams = params
//                emptyTextView.text = getString(R.string.add_image_text)
//                emptyTextView.setTextColor(
//                    ContextCompat.getColor(
//                        context,
//                        R.color.primary_positive_color
//                    )
//                )
//                emptyTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18F)
//                barcodeDetailParentLayout.addView(emptyTextView)
//                emptyTextView.setOnClickListener {
//                    openAddImageDialog()
//                }
            }
//
            val emptyTextView = MaterialTextView(context)
                emptyTextView.layoutParams = params
                emptyTextView.text = getString(R.string.add_image_text)
                emptyTextView.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.primary_positive_color
                    )
                )
                emptyTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18F)
                barcodeDetailParentLayout.addView(emptyTextView)
                emptyTextView.setOnClickListener {
                    openAddImageDialog()
                }

            for (i in 0 until tableObject!!.dynamicColumns.size) {
                val item = tableObject!!.dynamicColumns[i]

                val layout = LayoutInflater.from(context)
                    .inflate(R.layout.barcode_detail_item_row, barcodeDetailParentLayout, false)
                val columnValue = layout.findViewById<com.borjabravo.readmoretextview.ReadMoreTextView>(R.id.bcd_table_column_value)
                val columnName = layout.findViewById<MaterialTextView>(R.id.bcd_table_column_name)
                val columnEditView = layout.findViewById<AppCompatImageView>(R.id.bcd_edit_view)
                columnValue.setTrimCollapsedText(getString(R.string.see_more_text))
                columnValue.setTrimExpandedText(getString(R.string.see_less_text))
                counter += 1
                columnEditView.id = counter
                barcodeEditList.add(Triple(columnEditView, item.second, item.first))
                columnEditView.setOnClickListener(this)
                columnValue.text = item.second
                columnName.text = item.first
                barcodeDetailParentLayout.addView(layout)

            }

        }
        else if (tableName.contains("import") && csvTableObject != null){
            barcodeDetailWrapperLayout.visibility = View.VISIBLE
            counter = 0
            if (barcodeDetailParentLayout.childCount > 0) {
                barcodeDetailParentLayout.removeAllViews()
            }

            for (i in 0 until csvTableObject!!.size) {
                //if (i == 0){continue}
                val item = csvTableObject!![i]

                val layout = LayoutInflater.from(context)
                    .inflate(R.layout.barcode_detail_item_row, barcodeDetailParentLayout, false)
                val columnValue = layout.findViewById<com.borjabravo.readmoretextview.ReadMoreTextView>(R.id.bcd_table_column_value)
                val columnName = layout.findViewById<MaterialTextView>(R.id.bcd_table_column_name)
                val columnEditView = layout.findViewById<AppCompatImageView>(R.id.bcd_edit_view)
                columnValue.setTrimCollapsedText(getString(R.string.see_more_text))
                columnValue.setTrimExpandedText(getString(R.string.see_less_text))
                columnEditView.id = counter
                barcodeEditList.add(Triple(columnEditView, item.second, item.first))
                columnEditView.setOnClickListener(this)
                columnValue.text = item.second
                columnName.text = item.first
                if (i == 0){continue}
                counter += 1
                barcodeDetailParentLayout.addView(layout)

            }
        }
    }

    private var barcodeImagesRecyclerView: RecyclerView? = null
    private var filePathView: MaterialTextView? = null
    private var barcodeImageList = mutableListOf<String>()
    private lateinit var adapter: BarcodeImageAdapter
    var multiImagesList = mutableListOf<String>()
    private var availableStorageMemory: Float = 0F
    var url = " "
    private fun openAddImageDialog() {
        val dialogLayout =
            LayoutInflater.from(context).inflate(R.layout.add_barcode_image_dialog, null)
        barcodeImagesRecyclerView =
            dialogLayout.findViewById(R.id.selected_barcode_images_recyclerview)
        val submitBtn =
            dialogLayout.findViewById<MaterialButton>(R.id.add_barcode_image_dialog_submit_btn)
        val cancelBtn =
            dialogLayout.findViewById<MaterialButton>(R.id.add_barcode_image_dialog_cancel_btn)
        filePathView = dialogLayout.findViewById(R.id.filePath)

        val cameraImageView = dialogLayout.findViewById<AppCompatImageView>(R.id.camera_image_view)
        val imagesImageView = dialogLayout.findViewById<AppCompatImageView>(R.id.images_image_view)
        val internetImageView = dialogLayout.findViewById<AppCompatImageView>(R.id.internet_image_view)

        cameraImageView.setOnClickListener {
            if (RuntimePermissionHelper.checkCameraPermission(
                    context, Constants.CAMERA_PERMISSION
                )
            ) {
                pickImageFromCamera1()
            }
        }

        imagesImageView.setOnClickListener {
            isExternalStorageRequest = true
            if (RuntimePermissionHelper.checkStoragePermission(
                    context,
                    Constants.READ_STORAGE_PERMISSION
                )
            ) {
                isExternalStorageRequest = false
                pickImageFromGallery1()
            }
        }

        internetImageView.setOnClickListener {
            val searchedImagesList = mutableListOf<String>()
            val tempImageList = mutableListOf<String>()
            val internetSearchLayout = LayoutInflater.from(context)
                .inflate(R.layout.internet_image_search_dialog_layout, null)
            val loader =
                internetSearchLayout.findViewById<ProgressBar>(R.id.image_loader_view)
            val searchBoxView =
                internetSearchLayout.findViewById<TextInputEditText>(R.id.text_input_field)
            val searchBtnView =
                internetSearchLayout.findViewById<MaterialButton>(R.id.internet_image_search_btn)
            val internetImageRecyclerView =
                internetSearchLayout.findViewById<RecyclerView>(R.id.internet_search_image_recyclerview)
            val closeBtn = internetSearchLayout.findViewById<AppCompatImageView>(R.id.search_image_dialog_close)
            val builder = MaterialAlertDialogBuilder(context)
            builder.setCancelable(false)
            builder.setView(internetSearchLayout)
            val iAlert = builder.create()
            iAlert.show()

            closeBtn.setOnClickListener {
                if (tempImageList.isNotEmpty()){
                    multiImagesList.addAll(tempImageList)
                    filePathView!!.text = multiImagesList.joinToString(",")
                    getTotalImagesSize(
                        filePathView!!.text.split(
                            ","
                        ).toMutableList()
                    )
                    barcodeImageList.clear()
                    tempImageList.clear()
                    barcodeImageList.addAll(multiImagesList)
                    adapter.notifyDataSetChanged()
                }
                iAlert.dismiss()
            }

            internetImageRecyclerView.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)//GridLayoutManager(context, 2)
            internetImageRecyclerView.hasFixedSize()
            val internetImageAdapter = InternetImageAdapter(
                context,
                searchedImagesList as ArrayList<String>
            )
            internetImageRecyclerView.adapter = internetImageAdapter
            internetImageAdapter.setOnItemClickListener(object :
                InternetImageAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    val selectedImage = searchedImagesList[position]

                }

                override fun onItemAttachClick(btn:MaterialButton, position: Int) {
                    if (btn.text.toString()
                            .toLowerCase(Locale.ENGLISH) == "attach"
                    ) {


//                                    requireActivity().runOnUiThread {
//                                        //loader.visibility = View.VISIBLE
                        btn.text = getString(R.string.please_wait)
//                                    }

                        val selectedImage = searchedImagesList[position]
                        val bitmap: Bitmap? = ImageManager.getBitmapFromURL(
                            context,
                            selectedImage
                        )
                        if (bitmap != null) {
                            ImageManager.saveMediaToStorage(
                                context,
                                bitmap,
                                object : ResponseListener {
                                    override fun onSuccess(result: String) {
                                        if (loader.visibility == View.VISIBLE) {
                                            loader.visibility = View.INVISIBLE
                                        }

                                        if (result.isNotEmpty()) {
                                            tempImageList.add(
                                                ImageManager.getRealPathFromUri(
                                                    context,
                                                    Uri.parse(result)
                                                )!!
                                            )
                                            btn.text = getString(R.string.attached_text)
                                            btn.setBackgroundColor(ContextCompat.getColor(context,R.color.dark_gray))
                                        } else {
                                            btn.text = getString(R.string.attach_text)
                                            showAlert(
                                                context,
                                                getString(R.string.something_wrong_error)
                                            )
                                        }
                                    }

                                })
                        } else {
//                                        if (loader.visibility == View.VISIBLE) {
//                                            loader.visibility = View.INVISIBLE
//                                        }
                            btn.text = getString(R.string.attach_text)
                            showAlert(
                                context,
                                getString(R.string.something_wrong_error)
                            )
                        }
                    } else {
                        btn.text = getString(R.string.attach_text)
                        btn.setBackgroundColor(ContextCompat.getColor(context,R.color.primary_positive_color))
                        tempImageList.removeAt(position)
                    }
                }

            })


            searchBtnView.setOnClickListener {
                var creditChargePrice: Float = 0F
                if (searchBoxView.text.toString().trim().isNotEmpty()) {


                    val firebaseDatabase = FirebaseDatabase.getInstance().reference
                    firebaseDatabase.child("SearchImagesLimit")
                        .addListenerForSingleValueEvent(object :
                            ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val creditPrice = snapshot.child("credits")
                                    .getValue(Int::class.java) as Int
                                val images = snapshot.child("images")
                                    .getValue(Int::class.java) as Int
                                creditChargePrice = creditPrice.toFloat() / images

                                    userCurrentCredits = appSettings.getString(Constants.userCreditsValue) as String

                                    if (userCurrentCredits.isNotEmpty() && (userCurrentCredits != "0" || userCurrentCredits != "0.0") && userCurrentCredits.toFloat() >= creditChargePrice)
                                    {
                                        hideSoftKeyboard(
                                            context,
                                            searchBtnView
                                        )
                                        //Constants.hideKeyboar(requireActivity())
                                        val query = searchBoxView.text.toString().trim()
                                        runOnUiThread {
                                            loader.visibility = View.VISIBLE
                                        }

                                        searchInternetImages(
                                            context,
                                            query,
                                            object : APICallback {
                                                override fun onSuccess(response: JSONObject) {
                                                    if (loader.visibility == View.VISIBLE) {
                                                        loader.visibility =
                                                            View.INVISIBLE
                                                    }

                                                    val items =
                                                        response.getJSONArray("items")
                                                    if (items.length() > 0) {
                                                        searchedImagesList.clear()
                                                        for (i in 0 until items.length()) {
                                                            val item =
                                                                items.getJSONObject(i)
                                                            if (item.has("link")) {
                                                                searchedImagesList.add(
                                                                    item.getString(
                                                                        "link"
                                                                    )
                                                                )
                                                            }
                                                        }
                                                        internetImageAdapter.notifyItemRangeChanged(
                                                            0,
                                                            searchedImagesList.size
                                                        )

                                                    }
                                                    //userCurrentCredits = appSettings.getString(Constants.userCreditsValue) as String
                                                    val hashMap = HashMap<String, Any>()
                                                    val remaining =
                                                        userCurrentCredits.toFloat() - creditChargePrice
                                                    Log.d("TEST199", "$remaining")
                                                    hashMap["credits"] =
                                                        remaining.toString()
                                                    firebaseDatabase.child(Constants.firebaseUserCredits)
                                                        .child(Constants.firebaseUserId)
                                                        .updateChildren(hashMap)
                                                        .addOnSuccessListener {
                                                            getUserCredits(
                                                               context
                                                            )
                                                        }
                                                        .addOnFailureListener {

                                                        }
                                                }

                                                override fun onError(error: VolleyError) {
                                                    if (loader.visibility == View.VISIBLE) {
                                                        loader.visibility =
                                                            View.INVISIBLE
                                                    }

                                                    showAlert(
                                                        context,
                                                        error.localizedMessage!!
                                                    )
                                                }

                                            })
                                    } else
                                    {
                                        MaterialAlertDialogBuilder(context)
                                            .setMessage(getString(R.string.low_credites_error_message))
                                            .setCancelable(false)
                                            .setNegativeButton(getString(R.string.no_text)){dialog,which->
                                                dialog.dismiss()
                                            }
                                            .setPositiveButton(getString(R.string.buy_credits)){dialog,which ->
                                                dialog.dismiss()
                                                startActivity(Intent(context,UserScreenActivity::class.java))
                                            }
                                            .create().show()
                                    }
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }

                        })


                } else {
                    if (loader.visibility == View.VISIBLE) {
                        loader.visibility = View.INVISIBLE
                    }

                    showAlert(
                        context,
                        getString(R.string.empty_text_error)
                    )
                }
            }

        }

        barcodeImagesRecyclerView!!.layoutManager = LinearLayoutManager(
            context, RecyclerView.HORIZONTAL,
            false
        )
        barcodeImagesRecyclerView!!.hasFixedSize()
        adapter = BarcodeImageAdapter(
            context,
            barcodeImageList as ArrayList<String>
        )
        barcodeImagesRecyclerView!!.adapter = adapter

        adapter.setOnItemClickListener(object :
            BarcodeImageAdapter.OnItemClickListener {
            override fun onItemDeleteClick(position: Int) {
//                            val image = barcodeImageList[position]
                val builder = MaterialAlertDialogBuilder(context)
                builder.setMessage(getString(R.string.delete_barcode_image_message))
                builder.setCancelable(false)
                builder.setNegativeButton(getString(R.string.no_text)) { dialog, which ->
                    dialog.dismiss()
                }
                builder.setPositiveButton(getString(R.string.yes_text)) { dialog, which ->
                    dialog.dismiss()
                    barcodeImageList.removeAt(position)
                    multiImagesList.removeAt(position)
                    if (multiImagesList.isNotEmpty()){
                        filePathView!!.text = multiImagesList.joinToString(",")
                        getTotalImagesSize(filePathView!!.text.split(",").toMutableList())
                    }
                    adapter.notifyItemRemoved(position)
                }
                val alert = builder.create()
                alert.show()

            }

            override fun onAddItemEditClick(position: Int) {

            }

            override fun onImageClick(position: Int) {

            }

        })

        val builder = MaterialAlertDialogBuilder(context)
        builder.setView(dialogLayout)
        builder.setCancelable(false)
        val alert = builder.create()
        alert.show()

        cancelBtn.setOnClickListener {
            alert.dismiss()
        }

        submitBtn.setOnClickListener {
            availableStorageMemory =
                if (appSettings.getString(Constants.memory)!!.isEmpty()) {
                    0F
                } else {
                    Constants.convertMegaBytesToBytes(
                        appSettings.getString(
                            Constants.memory
                        )!!.toFloat()
                    )
                }

            if (totalImageSize.toInt() == 0) {
                showAlert(
                    context,
                    getString(R.string.image_attach_error)
                )
            } else if (totalImageSize <= availableStorageMemory) {
                val expiredAt: Long = appSettings.getLong(Constants.expiredAt)
                if (System.currentTimeMillis() > expiredAt && expiredAt.toInt() != 0) {
                    showAlert(
                        context,
                        getString(R.string.subscription_expired_text)
                    )
                } else {
                    alert.dismiss()
                    // UPLOAD IMAGES
                    if (filePathView!!.text.toString().isNotEmpty()) {

                        uploadImageOnFirebaseStorage(object : UploadImageCallback {
                            override fun onSuccess(imageUrl: String) {
                                updateStorageSize(totalImageSize, "minus")
                                // IF isUpload IS TRUE THEN DATA SAVE WITH IMAGE URL
                                // ELSE DISPLAY THE EXCEPTION MESSAGE WITHOUT DATA SAVING
                                startLoading(context)
                                if (url.isNotEmpty()) {
                                    if (multiImagesList.isNotEmpty()) {
                                        multiImagesList.clear()
                                    }

                                    if (url.contains(" ")) {
                                        imageList.addAll(url.split(" "))
                                    } else {
                                        imageList.add(url)
                                    }

                                    if (isCsvAttachImages && selectedColumn.isNotEmpty()){
                                        var pair:Pair<String,String>?= null
                                        for (i in 0 until csvTableObject!!.size){
                                            if (i != 0){
                                                val tempPair = csvTableObject!![i]
                                                if (tempPair.first == selectedColumn){
                                                    pair = tempPair
                                                    break
                                                }
                                            }
                                        }
                                        if (pair == null){
                                            showAlert(context,getString(R.string.selected_column_name_error_text))
                                        }
                                        else{
                                            var tempText = pair.second
                                            tempText = "$tempText,${imageList.joinToString(",")}"
                                            val isUpdated = tableGenerator.updateBarcodeDetail(
                                                tableName,
                                                selectedColumn,
                                                tempText,
                                                csvTableObject!![0].second.toInt()
                                            )
                                            if (isUpdated){
                                                selectedColumn = ""
                                                isCsvAttachImages = false
                                                csvTableObject = tableGenerator.getUpdateBarcodeDetail1(tableName, csvTableObject!![0].second.toInt())
                                            }
                                            filePathView!!.text = ""
                                            barcodeImageList.clear()
                                            adapter.notifyDataSetChanged()
                                        }
                                    }
                                    else{
                                        tableGenerator.updateBarcodeDetail(
                                            tableName,
                                            "image",
                                            imageList.joinToString(" "),
                                            tableObject!!.id
                                        )
                                        tableObject!!.image = url
                                        adapter.notifyDataSetChanged()
                                    }


                                    Handler(Looper.myLooper()!!).postDelayed({
                                        dismiss()
                                        displayBarcodeDetail()
                                    }, 1000)
                                }
                            }
                        })
                    }
                }
            } else {
                val b1 = MaterialAlertDialogBuilder(context)
                    .setCancelable(false)
                    .setTitle(getString(R.string.alert_text))
                    .setMessage(getString(R.string.storage_available_error_text))
                    .setNegativeButton(getString(R.string.close_text)) { dialog, which ->
                        dialog.dismiss()
                    }
                    .setNeutralButton(getString(R.string.buy_storage_text)) { dialog, which ->
                        dialog.dismiss()
                        alert.dismiss()
                        startActivity(
                            Intent(
                                context,
                                UserScreenActivity::class.java
                            )
                        )
                    }
                val iAlert = b1.create()
                iAlert.show()
                iAlert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.purple_700
                    )
                )

            }
        }
    }

    var uploadedUrlList = mutableListOf<String>()
    var array: List<String> = mutableListOf()
    var index = 0
    private fun uploadImageOnFirebaseStorage(listener: UploadImageCallback) {
        startLoading(context)
        val stringRequest = object : StringRequest(
            Method.POST, "https://itmagic.app/api/get_user_packages.php",
            Response.Listener {
                val response = JSONObject(it)
                if (response.getInt("status") == 200) {
                    if (response.has("package") && !response.isNull("package")) {
                        val packageDetail: JSONObject? = response.getJSONObject("package")

                        val availableSize = packageDetail!!.getString("size")
                        Constants.userServerAvailableStorageSize = availableSize
                        appSettings.putString(Constants.memory, availableSize)
                        val endDate = packageDetail.getString("end_date")
                        val expiredTimeMili =
                            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(endDate)!!.time

                        var currentStorageSize: Float = 0F
                        if (availableSize.isNotEmpty()) {
                            currentStorageSize =
                                Constants.convertMegaBytesToBytes(availableSize.toFloat())
                            if (totalImageSize <= currentStorageSize) {
                                val currentMiliSeconds = System.currentTimeMillis()
                                if (expiredTimeMili >= currentMiliSeconds) {
                                    val imageList = filePathView!!.text.toString()
                                    if (imageList.contains(",")) {
                                        array = imageList.split(",")
                                        uploadImage(listener)
                                    } else {

                                        if (FirebaseAuth.getInstance().currentUser != null) {
                                            val userId =
                                                FirebaseAuth.getInstance().currentUser!!.uid

                                            val imageBase64String =
                                                ImageManager.convertImageToBase64(
                                                    context,
                                                    filePathView!!.text.toString()
                                                )
                                            uploadImageOnServer(
                                                context,
                                                imageBase64String,
                                                userId,
                                                object : UploadImageCallback {
                                                    override fun onSuccess(imageUrl: String) {
                                                        dismiss()
                                                        url = imageUrl
                                                        listener.onSuccess("")
                                                    }

                                                })
                                        }
                                    }
                                } else {
                                    dismiss()
                                    showAlert(context, getString(R.string.subscription_expired_text1))
                                }
                            } else {
                                dismiss()
                                showAlert(
                                    context,
                                    getString(R.string.insufficient_storage_error_message)
                                )
                            }
                        }


                    }
                }
            }, Response.ErrorListener {
                dismiss()
                Log.d("TEST199", it.localizedMessage!!)

            }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = Constants.firebaseUserId
                return params
            }
        }

        stringRequest.retryPolicy = object : RetryPolicy {
            override fun getCurrentTimeout(): Int {
                return 50000
            }

            override fun getCurrentRetryCount(): Int {
                return 50000
            }

            @Throws(VolleyError::class)
            override fun retry(error: VolleyError) {
            }
        }

        VolleySingleton(context).addToRequestQueue(stringRequest)
    }

    private fun uploadImage(callback: UploadImageCallback) {
        startLoading(context)
        if (FirebaseAuth.getInstance().currentUser != null) {

            val userId = FirebaseAuth.getInstance().currentUser!!.uid
            val imagePath = array[index]
            val imageBase64String = ImageManager.convertImageToBase64(context, imagePath)
            Handler(Looper.myLooper()!!).postDelayed({
                uploadImageOnServer(
                    context,
                    imageBase64String,
                    userId,
                    object : UploadImageCallback {
                        override fun onSuccess(imageUrl: String) {
                            Log.d("TEST199", imageUrl)
                            uploadedUrlList.add(imageUrl)
                            if (index == array.size - 1) {
                                url = uploadedUrlList.joinToString(" ")
                                uploadedUrlList.clear()
                                index = 0
                                dismiss()
                                callback.onSuccess("")
                            } else {
                                index++
                                uploadImage(callback)
                            }
                        }

                    })
            }, 1000)
        }
    }

    private fun updateStorageSize(size: Long, type: String) {
        var currentStorageSize: Float = 0F
        if (Constants.userServerAvailableStorageSize.isNotEmpty()) {
            currentStorageSize =
                Constants.convertMegaBytesToBytes(Constants.userServerAvailableStorageSize.toFloat()) - size

            val remainingMb = Constants.convertBytesToMegaBytes(currentStorageSize).toString()
            appSettings.putString(Constants.memory, remainingMb)
            updateMemorySize(
                context,
                remainingMb,
                0,
                Constants.firebaseUserId,
                0,
                "",
                object : APICallback {
                    override fun onSuccess(response: JSONObject) {
                        val stringRequest = object : StringRequest(
                            Method.POST, "https://itmagic.app/api/get_user_packages.php",
                            Response.Listener {
                                val updatedResponse = JSONObject(it)
                                if (updatedResponse.getInt("status") == 200) {
                                    if (updatedResponse.has("package") && !updatedResponse.isNull("package")) {
                                        val packageDetail: JSONObject? =
                                            updatedResponse.getJSONObject("package")
                                        if (packageDetail != null) {
                                            val availableSize = packageDetail.getString("size")
                                            Constants.userServerAvailableStorageSize = availableSize
                                        }
                                    }
                                }
                            }, Response.ErrorListener {
                                Log.d("TEST199", it.localizedMessage!!)

                            }) {
                            override fun getParams(): MutableMap<String, String> {
                                val params = HashMap<String, String>()
                                params["user_id"] = Constants.firebaseUserId
                                return params
                            }
                        }

                        stringRequest.retryPolicy = object : RetryPolicy {
                            override fun getCurrentTimeout(): Int {
                                return 50000
                            }

                            override fun getCurrentRetryCount(): Int {
                                return 50000
                            }

                            @Throws(VolleyError::class)
                            override fun retry(error: VolleyError) {
                            }
                        }

                        VolleySingleton(context).addToRequestQueue(stringRequest)
                    }

                    override fun onError(error: VolleyError) {

                    }

                })
        } else {
            val stringRequest = object : StringRequest(
                Method.POST, "https://itmagic.app/api/get_user_packages.php",
                Response.Listener {
                    val response = JSONObject(it)
                    if (response.getInt("status") == 200) {
                        if (response.has("package") && !response.isNull("package")) {
                            val packageDetail: JSONObject? = response.getJSONObject("package")
                            if (packageDetail != null) {
                                val availableSize = packageDetail.getString("size")
                                Constants.userServerAvailableStorageSize = availableSize

                                currentStorageSize =
                                    Constants.convertMegaBytesToBytes(availableSize.toFloat()) - size

                                val remainingMb =
                                    Constants.convertBytesToMegaBytes(currentStorageSize).toString()
                                updateMemorySize(
                                    context,
                                    remainingMb,
                                    0,
                                    Constants.firebaseUserId,
                                    0,
                                    "",
                                    object : APICallback {
                                        override fun onSuccess(response: JSONObject) {

                                        }

                                        override fun onError(error: VolleyError) {

                                        }

                                    })
                            }
                        }
                    }
                }, Response.ErrorListener {
                    Log.d("TEST199", it.localizedMessage!!)

                }) {
                override fun getParams(): MutableMap<String, String> {
                    val params = HashMap<String, String>()
                    params["user_id"] = Constants.firebaseUserId
                    return params
                }
            }

            stringRequest.retryPolicy = object : RetryPolicy {
                override fun getCurrentTimeout(): Int {
                    return 50000
                }

                override fun getCurrentRetryCount(): Int {
                    return 50000
                }

                @Throws(VolleyError::class)
                override fun retry(error: VolleyError) {
                }
            }

            VolleySingleton(context).addToRequestQueue(stringRequest)
        }

//        databaseReference.child(Constants.firebaseUserFeatureDetails)
//            .child(Constants.firebaseUserId)
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (snapshot.hasChildren()) {
//                        val pSize: Float? =
//                            snapshot.child("memory").getValue(String::class.java)!!.toFloat()
//                        if (pSize != null) {
//                            currentStorageSize = if (type == "add") {
//                                Constants.convertMegaBytesToBytes(pSize) + size
//                            } else {
//                                Constants.convertMegaBytesToBytes(pSize) - size
//                            }
//                            val remainingMb = Constants.convertBytesToMegaBytes(
//                                currentStorageSize
//                            ).toString()
//
//                            val params = HashMap<String, Any>()
//                            params["memory"] = remainingMb
//                            databaseReference.child(Constants.firebaseUserFeatureDetails)
//                                .child(Constants.firebaseUserId).updateChildren(params)
//                            totalImageSize = 0
//                            appSettings.putString(
//                                Constants.memory,
//                                remainingMb
//                            )
//                        }
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//
//                }
//
//            })
    }


    // THIS FUNCTION WILL SHARE THE CODE TEXT TO OTHERS
    private fun textShare() {
        val intent = Intent(Intent.ACTION_SEND)
        val shareBody =
            "${getString(R.string.app_name)} \n ${encodeDataTextView.text.toString().trim()}"
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(intent, getString(R.string.share_using)))
    }

    // THIS FUNCTION WILL CREATE THE PDF FILE FROM CODE DETAIL
    private fun createPdf(isShareAfterCreated: Boolean) {
        bitmap = if (codeHistory!!.codeType == "qr") {
            BitmapFactory.decodeResource(resources, R.drawable.qrcode)
        } else {
            BitmapFactory.decodeResource(resources, R.drawable.barcode)
        }
        var codeWidth: Int? = null
        var codeHeight: Int? = null
        if (codeHistory!!.codeType == "qr") {
            codeWidth = 200
            codeHeight = 200
        } else {
            codeWidth = 400
            codeHeight = 200
        }
        bitmap = Bitmap.createScaledBitmap(bitmap!!, codeWidth, codeHeight, false)


        try {
            val path = "${applicationContext.getExternalFilesDir("")}/PDF"
            val dir = File(path)
            if (!dir.exists()) dir.mkdirs()
            val fileName = "pdf_${codeHistory!!.createdAt}.pdf"
            val file = File(dir, fileName)
            pdfFile = file
            val fOut = FileOutputStream(file)

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()
            val titlePaint = Paint()
            val dataPaint = Paint()
            titlePaint.textSize = 50.toFloat()
            titlePaint.textAlign = Paint.Align.CENTER
            titlePaint.color = Color.RED
            val appName = getString(R.string.app_name)
            val xPos = (canvas.width / 2) - (appName.length) / 2
            val yPos =
                (canvas.height / 2 - (titlePaint.descent() + titlePaint.ascent()) / 2).toInt()
            canvas.drawText(appName, xPos.toFloat(), 40.toFloat(), titlePaint)
            paint.textAlign = Paint.Align.CENTER
            val xCodePos = (canvas.width / 2) - (bitmap!!.width) / 2
            canvas.drawBitmap(bitmap!!, xCodePos.toFloat(), 50.toFloat(), paint)
            canvas.drawText(
                encodeDataTextView.text.toString(),
                30.toFloat(),
                280.toFloat(),
                dataPaint
            )
            val typePaint = Paint()
            typePaint.textAlign = Paint.Align.RIGHT
            typePaint.color = Color.BLUE
            typePaint.textAlign = Paint.Align.RIGHT
            val codeType = codeHistory!!.codeType.toUpperCase(Locale.ENGLISH)
            canvas.drawText(codeType, canvas.width / 2.toFloat(), 260.toFloat(), typePaint)
            val datePaint = Paint()
            datePaint.textSize = 16.toFloat()
            canvas.drawText(
                getFormattedDate(context, codeHistory!!.createdAt.toLong()),
                30.toFloat(),
                300.toFloat(),
                datePaint
            )

            document.finishPage(page)
            document.writeTo(fOut)
            document.close()
            Toast.makeText(this, getString(R.string.pdf_saved_success_text), Toast.LENGTH_SHORT)
                .show()
            if (isShareAfterCreated) {
                sharePdfFile()
            }
        } catch (e: IOException) {
            e.printStackTrace()
//            Toast.makeText(this, "Something wrong: $e", Toast.LENGTH_SHORT).show()
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.READ_STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isExternalStorageRequest){
                        isExternalStorageRequest = false
                        hideSoftKeyboard(context, contentView)
                        pickImageFromGallery1()
                    }
                    else if (isShareAfterCreated) {
                        createPdf(true)
                        isShareAfterCreated = false
                    } else {
                        createPdf(false)
                    }
                } else {
                    MaterialAlertDialogBuilder(context)
                        .setMessage(getString(R.string.external_storage_permission_error))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.ok_text)) { dialog, which ->
                            dialog.dismiss()
                        }
                        .create().show()
                }
            }
            Constants.CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hideSoftKeyboard(context, contentView)
                    pickImageFromCamera1()
                }
            }

            else -> {

            }
        }
    }


    // FUNCTION WILL SHARE THE PDF FILE
    private fun sharePdfFile() {
        if (pdfFile != null) {
            if (pdfFile!!.exists()) {
                val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context,
                        context.applicationContext.packageName + ".fileprovider", pdfFile!!
                    )

                } else {
                    Uri.fromFile(pdfFile)
                }
                val fileShareIntent = Intent(Intent.ACTION_SEND)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    fileShareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (fileUri != null) {

                    fileShareIntent.type = "application/pdf"
                    fileShareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
                    startActivity(Intent.createChooser(fileShareIntent, "Share File"))
                }
            } else {
                showAlert(context, getString(R.string.pdf_create_failed_error))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            val result = CropImage.getActivityResult(data)
            val imgUri = result.uri
            try {
                TextRecogniser.runTextRecognition(applicationContext, updateInputBox, imgUri)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (hasFocus) {
            //openDialog()
            Constants.openKeyboar(context)
        }
    }

    private fun openDialog() {
        customAlertDialog = CustomAlertDialog()
        customAlertDialog!!.setFocusListener(this)
        customAlertDialog!!.show(supportFragmentManager, "dialog")
    }

    override fun onEraseBtnClick(alertDialog: CustomAlertDialog) {
        updateInputBox.setText("")
    }

    override fun onImageRecognitionBtnClick(alertDialog: CustomAlertDialog) {
        if (customAlertDialog != null) {
            customAlertDialog!!.dismiss()
            if (RuntimePermissionHelper.checkCameraPermission(
                    context,
                    Constants.READ_STORAGE_PERMISSION
                )
            ) {
                hideSoftKeyboard(context, updateInputBox)
                pickImageFromGallery()
            }
        }
    }

    override fun onPhotoRecognitionBtnClick(alertDialog: CustomAlertDialog) {
        if (customAlertDialog != null) {
            customAlertDialog!!.dismiss()
            if (RuntimePermissionHelper.checkCameraPermission(
                    context, Constants.CAMERA_PERMISSION
                )
            ) {
                hideSoftKeyboard(context, updateInputBox)
                pickImageFromCamera()
            }
        }
    }

    override fun onDismissBtnClick(alertDialog: CustomAlertDialog) {
        if (customAlertDialog != null) {
            customAlertDialog!!.dismiss()
            Constants.openKeyboar(context)
        }
    }


}