package com.boris.expert.csvmagic.view.activities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.volley.VolleyError
import com.boris.expert.csvmagic.R
import com.boris.expert.csvmagic.interfaces.*
import com.boris.expert.csvmagic.model.CodeHistory
import com.boris.expert.csvmagic.model.Feature
import com.boris.expert.csvmagic.model.User
import com.boris.expert.csvmagic.singleton.DriveService
import com.boris.expert.csvmagic.singleton.SheetService
import com.boris.expert.csvmagic.utils.*
import com.boris.expert.csvmagic.view.fragments.CsvFragment
import com.boris.expert.csvmagic.view.fragments.InsalesFragment
import com.boris.expert.csvmagic.view.fragments.ScanFragment
import com.boris.expert.csvmagic.view.fragments.ScannerFragment
import com.boris.expert.csvmagic.viewmodel.MainActivityViewModel
import com.boris.expert.csvmagic.viewmodelfactory.ViewModelFactory
import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.v10.errors.GoogleAdsException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textview.MaterialTextView
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Long.parseLong
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener,
    OnCompleteAction, ScannerInterface, View.OnClickListener,ScanFragment.FragmentChangeListener {


    private lateinit var eventListener: ValueEventListener
    private lateinit var toolbar: Toolbar
    private lateinit var mDrawer: DrawerLayout
    private lateinit var mNavigation: NavigationView
    private lateinit var privacyPolicy: MaterialTextView
    private lateinit var bottomNavigation: BottomNavigationView
    private var encodedTextData: String = " "
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var appSettings: AppSettings
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    var mService: Drive? = null
    var sheetService: Sheets? = null
    private lateinit var auth: FirebaseAuth
    private val scopes = mutableListOf<String>()
    private val transport: HttpTransport? = NetHttpTransport()
    private val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = NetHttpTransport()
    private val jacksonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
    private var user: User? = null
    private var requestLogin: String? = null
    private var scannerFragment: ScannerFragment? = null
    private var callback: LoginCallback? = null
    private lateinit var firebaseDatabase: DatabaseReference
    var googleAdsClient: GoogleAdsClient? = null
    private lateinit var homeCsvButton: AppCompatButton
    private lateinit var homeInsalesButton: AppCompatButton
    private var menu: Menu? = null

    companion object {
        lateinit var context: Context
        lateinit var historyBtn: MaterialButton
        var credential: GoogleAccountCredential? = null

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
//        StrictMode.setThreadPolicy(policy)

        initViews()
        setUpToolbar()
        getAccountsPermission()
        initializeGoogleLoginParameters()

        if (appSettings.getBoolean(getString(R.string.key_tips))) {
            val duration = appSettings.getLong("tt1")
            if (duration.compareTo(0) == 0 || System.currentTimeMillis() - duration > TimeUnit.DAYS.toMillis(
                    1
                )
            ) {

//                SimpleTooltip.Builder(this)
//                    .anchorView(bottomNavigation)
//                    .text(getString(R.string.bottom_navigation_tip_text))
//                    .gravity(Gravity.TOP)
//                    .animated(true)
//                    .transparentOverlay(false)
//                    .onDismissListener { tooltip ->
//                        tooltip.dismiss()
//                        appSettings.putLong("tt1", System.currentTimeMillis())
//                        val currentFragment = supportFragmentManager.findFragmentByTag("scanner")
//                        if (currentFragment != null && currentFragment.isVisible) {
//                            val fragment =
//                                supportFragmentManager.findFragmentById(R.id.fragment_container) as ScannerFragment
//                            fragment.showTableSelectTip()
//                        }
//                    }
//                    .build()
//                    .show()
            }
        }
    }


    // THIS FUNCTION WILL INITIALIZE ALL THE VIEWS AND REFERENCE OF OBJECTS
    private fun initViews() {
        context = this
        appSettings = AppSettings(context)
        firebaseDatabase = FirebaseDatabase.getInstance().reference
        scannerFragment = ScannerFragment()
        auth = Firebase.auth
        viewModel = ViewModelProviders.of(
            this,
            ViewModelFactory(MainActivityViewModel()).createFor()
        )[MainActivityViewModel::class.java]
        toolbar = findViewById(R.id.toolbar)
        mDrawer = findViewById(R.id.drawer)
        historyBtn = findViewById(R.id.history_btn)
        homeCsvButton = findViewById(R.id.home_csv_btn)
        homeInsalesButton = findViewById(R.id.home_insales_btn)
        homeCsvButton.setOnClickListener(this)
        homeInsalesButton.setOnClickListener(this)


        historyBtn.setOnClickListener {
            startActivity(Intent(context, BarcodeHistoryActivity::class.java))
        }
        privacyPolicy = findViewById(R.id.privacy_policy_view)
        privacyPolicy.movementMethod = LinkMovementMethod.getInstance()
        privacyPolicy.paintFlags = privacyPolicy.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        privacyPolicy.setOnClickListener {
            mDrawer.closeDrawer(GravityCompat.START)
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://qrmagicapp.com/privacy-policy-CSV/")
            )
            startActivity(browserIntent)
        }
        mNavigation = findViewById(R.id.navigation)
//        nextStepTextView = findViewById(R.id.next_step_btn)
        bottomNavigation = findViewById(R.id.bottom_navigation)
//        bottomNavigation.setOnNavigationItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.bottom_scanner -> {
//                    supportFragmentManager.beginTransaction()
//                        .replace(R.id.fragment_container, ScannerFragment(), "scanner")
//                        .addToBackStack("scanner")
//                        .commit()
////                    nextStepTextView.visibility = View.GONE
////                    historyBtn.visibility = View.VISIBLE
//                }
//                R.id.bottom_tables -> {
//                    supportFragmentManager.beginTransaction()
//                        .replace(R.id.fragment_container, ScanFragment(), "tables")
//                        .addToBackStack("tables")
//                        .commit()
////                    historyBtn.visibility = View.GONE
////                    nextStepTextView.visibility = View.VISIBLE
//                }
//                else -> {
//
//                }
//            }
//
//            true
//        }
        activeButtonFocus(homeCsvButton, homeInsalesButton,"table")
        supportFragmentManager.beginTransaction()
//                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .add(
                R.id.fragment_container,
                ScanFragment(),
                "table"
            )
            .addToBackStack("table")
            .commit()

        if (intent != null && intent.hasExtra("KEY") && intent.getStringExtra("KEY") == "tables") {
//            bottomNavigation.selectedItemId = R.id.bottom_tables
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.fragment_container, ScanFragment(), "tables")
//                .addToBackStack("tables")
//                .commit()
            //historyBtn.visibility = View.GONE
//            nextStepTextView.visibility = View.VISIBLE
        } else {
//            nextStepTextView.visibility = View.GONE
            //historyBtn.visibility = View.VISIBLE
//            supportFragmentManager.beginTransaction().add(
//                R.id.fragment_container,
//                ScannerFragment(),
//                "scanner"
//            )
//                .addToBackStack("scanner")
//                .commit()
        }

//        try {
//            googleAdsClient = GoogleAdsClient.newBuilder().fromPropertiesFile().build()
//        } catch (fnfe: FileNotFoundException) {
//            System.err.printf(
//                "Failed to load GoogleAdsClient configuration from file. Exception: %s%n", fnfe
//            )
//        } catch (ioe: IOException) {
//            System.err.printf("Failed to create GoogleAdsClient. Exception: %s%n", ioe)
//        }
//
//        try {
//            GenerateKeywords.runExample(
//                    googleAdsClient!!,
//                parseLong("INSERT_CUSTOMER_ID_HERE"),
//                parseLong("1000"),
//                listOf(parseLong("21167"),
//                    parseLong("21385")),
//                    listOf("iphone"),
//                    null
//                )
//        } catch (gae: GoogleAdsException) {
//            // GoogleAdsException is the base class for most exceptions thrown by an API request.
//            // Instances of this exception have a message and a GoogleAdsFailure that contains a
//            // collection of GoogleAdsErrors that indicate the underlying causes of the
//            // GoogleAdsException.
//            System.err.printf(
//                "Request ID %s failed due to GoogleAdsException. Underlying errors:%n",
//                gae.requestId
//            )
//            var i = 0
//            for (googleAdsError in gae.getGoogleAdsFailure().getErrorsList()) {
//                System.err.printf("  Error %d: %s%n", i++, googleAdsError)
//            }
//        }

    }

    private fun activeButtonFocus(btn: AppCompatButton, otherBtn: AppCompatButton, type: String) {

        btn.setBackgroundColor(ContextCompat.getColor(context,R.color.secondary_positive_color))
//        btn.isFocusableInTouchMode = true
//        btn.requestFocus()
//        otherBtn.isFocusable = false
//        otherBtn.isFocusableInTouchMode = false
        otherBtn.setBackgroundColor(ContextCompat.getColor(context,R.color.primary_positive_color))
    }

    private fun getAccountsPermission() {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.GET_ACCOUNTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    Manifest.permission.GET_ACCOUNTS
                )
            ) {
                Log.e("Accounts", "Permission Granted")
                initializeGoogleLoginParameters()
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.GET_ACCOUNTS),
                    0
                )
            }
        }
    }

    // THIS FUNCTION WILL RENDER THE ACTION BAR/TOOLBAR
    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = getString(R.string.app_name)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setTitleTextColor(ContextCompat.getColor(context, R.color.black))

        val toggle = ActionBarDrawerToggle(this, mDrawer, toolbar, 0, 0)
        mDrawer.addDrawerListener(toggle)
        toggle.syncState()
        mNavigation.setNavigationItemSelectedListener(this)

        toolbar.setNavigationOnClickListener {
            hideSoftKeyboard(context, mDrawer)
            if (mDrawer.isDrawerOpen(GravityCompat.START)) {
                mDrawer.closeDrawer(GravityCompat.START)
            } else {
                mDrawer.openDrawer(GravityCompat.START)
            }
        }
    }

    // THIS FUNCTION WILL INITIALIZE THE GOOGLE LOGIN PARAMETERS
    private fun initializeGoogleLoginParameters() {
        scopes.add(DriveScopes.DRIVE_METADATA_READONLY)
        scopes.add(SheetsScopes.SPREADSHEETS_READONLY)
        scopes.add(SheetsScopes.DRIVE)
        scopes.add(SheetsScopes.SPREADSHEETS)
//        scopes.add(DriveScopes.DRIVE)
//        scopes.add(DriveScopes.DRIVE_APPDATA)

        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
//            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
//            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, signInOptions)

        val acct: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)
        if (acct != null) {

            credential = GoogleAccountCredential.usingOAuth2(
                applicationContext, scopes
            )
                .setBackOff(ExponentialBackOff())
                .setSelectedAccount(acct.account)

            mService = Drive.Builder(
                transport, jsonFactory, credential
            ).setHttpRequestInitializer { request ->
                credential!!.initialize(request)
                request!!.connectTimeout = 300 * 60000  // 300 minutes connect timeout
                request.readTimeout = 300 * 60000  // 300 minutes read timeout
            }
                .setApplicationName(getString(R.string.app_name))
                .build()

            try {
                sheetService = Sheets.Builder(
                    httpTransport,
                    jacksonFactory,
                    credential
                )
                    .setApplicationName(getString(R.string.app_name))
                    .build()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            DriveService.saveDriveInstance(mService!!)
            SheetService.saveGoogleSheetInstance(sheetService!!)
            saveUserUpdatedDetail(acct, "last")
        } else {
            checkUserLoginStatus()
        }

        if (intent != null && intent.hasExtra("REQUEST") && intent.getStringExtra("REQUEST") == "login") {
            requestLogin = "login"
            startLogin()
        }
    }


    private fun saveUserUpdatedDetail(acct: GoogleSignInAccount?, isLastSignUser: String) {
        try {

            // IF PART WILL RUN IF USER LOGGED AND ACCOUNT DETAIL NOT EMPTY
            if (acct != null && acct.displayName.isNullOrEmpty()) {
                startLogin()
            } else if (acct != null) {
                val personName = acct.displayName
                val personGivenName = acct.givenName
                val personFamilyName = acct.familyName
                val personEmail = acct.email
                val personId = acct.id
                val personPhoto: Uri? = acct.photoUrl
                val user = User(
                    personName!!,
                    personGivenName!!,
                    personFamilyName!!,
                    personEmail!!,
                    personId!!,
                    personPhoto!!.toString()
                )
                appSettings.putUser(Constants.user, user)
                Constants.userData = user
                if (callback != null) {
                    callback!!.onSuccess()
                } else {
//                    val scanFragment = supportFragmentManager.findFragmentByTag("tables")
//                    if (scanFragment != null && scanFragment.isVisible) {
//                        val scannerFragment1 =
//                            supportFragmentManager.findFragmentById(R.id.fragment_container) as ScanFragment
//                        scannerFragment1.restart()
//                    }
                }
                if (isLastSignUser == "new") {
                    if (auth.currentUser != null) {
                        Constants.firebaseUserId = auth.currentUser!!.uid
                        getUserFeaturesDetails1(context, Constants.firebaseUserId)
                    }
                    appSettings.putBoolean(Constants.isLogin, true)
                    Toast.makeText(
                        context,
                        getString(R.string.user_signin_success_text),
                        Toast.LENGTH_SHORT
                    ).show()

                    val currentFragment = supportFragmentManager.findFragmentByTag("scanner")
                    if (currentFragment != null && currentFragment.isVisible) {
                        val scannerFragment =
                            supportFragmentManager.findFragmentById(R.id.fragment_container) as ScannerFragment
                        scannerFragment.restart()
                    }

                    val scanFragment = supportFragmentManager.findFragmentByTag("tables")
                    if (scanFragment != null && scanFragment.isVisible) {
                        val tableFragment =
                            supportFragmentManager.findFragmentById(R.id.fragment_container) as ScanFragment
                        tableFragment.restart()
                    }
                }

                checkUserLoginStatus()
                if (requestLogin!!.isNotEmpty() && requestLogin == "login") {
                    startActivity(Intent(context, TablesActivity::class.java))
                }

            }
            // ELSE PART WILL WORK WHEN USER LOGGED BUT ACCOUNT DETAIL EMPTY
            // AND IN CASE ACCOUNT DETAIL IS EMPTY THEN APP FETCH THE ACCOUNT DETAIL FROM PREFERENCE FOR AVOID NULL ANC CRASH THE APP
//            else {
//                val userDetail = appSettings.getUser(Constants.user)
//                val user = User(
//                    userDetail.personName,
//                    userDetail.personGivenName,
//                    userDetail.personFamilyName,
//                    userDetail.personEmail,
//                    userDetail.personId,
//                    userDetail.personPhoto
//                )
//                appSettings.putUser(Constants.user, user)
//                Constants.userData = user
//            }
        } catch (e: Exception) {

        }

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.dynamic_links -> {
                startActivity(Intent(context, DynamicQrActivity::class.java))
            }
            R.id.sheets -> {
                if (appSettings.getBoolean(Constants.isLogin)) {
                    startActivity(Intent(context, SheetsActivity::class.java))
                } else {
                    startLogin()
                }

            }
            R.id.user_screen -> {
                getSearchImageDetail()
                startActivity(Intent(context, UserScreenActivity::class.java))
            }
            R.id.credit -> {
                startActivity(Intent(context, CreditActivity::class.java))
            }
            R.id.purchase_feature -> {
                startActivity(Intent(context, PurchaseFeatureActivity::class.java))
            }
            R.id.insales -> {
                startActivity(Intent(context, SalesCustomersActivity::class.java))
            }
//            R.id.insales_login -> {
//                startActivity(Intent(context, SalesCustomersActivity::class.java))
//            }
            R.id.tickets -> {
                startActivity(Intent(context, SupportTicketsActivity::class.java))
            }
            R.id.nav_setting -> {
                startActivity(Intent(context, SettingsActivity::class.java))
            }
            R.id.tables -> {
                startActivity(Intent(context, TablesActivity::class.java))
            }
            R.id.tables_data -> {
                startActivity(Intent(context, TablesDataActivity::class.java))
            }
            R.id.nav_rateUs -> {
                rateUs(this)
            }
            R.id.nav_recommend -> {
                shareApp()
            }
            R.id.nav_contact_support -> {
                contactSupport(this)
            }
            R.id.login -> {
                startLogin()
            }
            R.id.field_list -> {
                startActivity(Intent(context, FieldListsActivity::class.java))
            }
            R.id.profile -> {
                startActivity(Intent(context, ProfileActivity::class.java))
            }
            R.id.logout -> {
                MaterialAlertDialogBuilder(context)
                    .setTitle(getString(R.string.logout))
                    .setMessage(getString(R.string.logout_warning_text))
                    .setNegativeButton(getString(R.string.cancel_text)) { dialog, which ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(getString(R.string.logout)) { dialog, which ->
                        dialog.dismiss()
                        appSettings.remove(Constants.dbExport)
                        DatabaseHandler.exporter(context, object : BackupListener {
                            override fun onSuccess() {
                                startLoading(context)
                                signOut()
                                DatabaseHandler.importer(context, "logout")
                            }

                            override fun onFailure() {

                            }

                        }, "login")

                    }
                    .create().show()
            }
            else -> {
            }
        }
        mDrawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                //hideSoftKeyboard(context, mDrawer)
                return true
            }
            R.id.help -> {
                startActivity(Intent(context, HelpActivity::class.java))
                return true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }

    }

    private fun startLogin() {
        val signInIntent = mGoogleSignInClient.signInIntent
        googleLauncher.launch(signInIntent)
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(
            Intent.EXTRA_TEXT,
            getString(R.string.share_app_message) + "https://play.google.com/store/apps/details?id=" + packageName
        )
        startActivity(shareIntent)

    }

    private fun signOut() {

        mGoogleSignInClient.revokeAccess().addOnCompleteListener(this) {
//
            mGoogleSignInClient.signOut().addOnCompleteListener(this) {
                FirebaseAuth.getInstance().signOut()
                dismiss()
//                appSettings.clear()
                appSettings.remove(Constants.dbImport)
                appSettings.remove(Constants.dbExport)
                appSettings.remove(Constants.isLogin)
                appSettings.remove(Constants.user)
                Constants.compressorFeatureStatus = 0
                Constants.modesSwitcherFeatureStatus = 0
                Constants.premiumSupportFeatureStatus = 0
                Constants.unlimitedTablesFeatureStatus = 0
                Toast.makeText(context, getString(R.string.logout_success_text), Toast.LENGTH_SHORT)
                    .show()
                Constants.userData = null
                Constants.sheetService = null
                Constants.mService = null

                startActivity(Intent(context, StartAppActivity::class.java)).apply {
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    finish()
                }

//                val currentFragment = supportFragmentManager.findFragmentByTag("scanner")
//                if (currentFragment != null && currentFragment.isVisible) {
//                    val scannerFragment =
//                        supportFragmentManager.findFragmentById(R.id.fragment_container) as ScannerFragment
//                    scannerFragment.restart()
//                }
//
//                val scanFragment = supportFragmentManager.findFragmentByTag("tables")
//                if (scanFragment != null && scanFragment.isVisible) {
//                    val tableFragment =
//                        supportFragmentManager.findFragmentById(R.id.fragment_container) as ScanFragment
//                    tableFragment.restart()
//                }


                //checkUserLoginStatus()
            }
//
        }

    }

    // THIS GOOGLE LAUNCHER WILL HANDLE RESULT
    private var googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

//                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
//                try {
//                    // Google Sign In was successful, authenticate with Firebase
//                    val account = task.getResult(ApiException::class.java)
//                    Log.d("TAG", "firebaseAuthWithGoogle:" + account.id)
//                    firebaseAuthWithGoogle(account.idToken!!)
//                } catch (e: ApiException) {
//                    // Google Sign In failed, update UI appropriately
//                    Log.w("TAG", "Google sign in failed", e)
//                }

                GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .addOnSuccessListener(object : OnSuccessListener<GoogleSignInAccount> {
                        override fun onSuccess(googleSignInAccount: GoogleSignInAccount?) {
                            credential = GoogleAccountCredential.usingOAuth2(
                                context,
                                scopes
                            )
                                .setBackOff(ExponentialBackOff())
                                .setSelectedAccount(googleSignInAccount!!.account)
//                            if (googleSignInAccount != null) {
//                                credential.selectedAccount = googleSignInAccount.account
//                            }

                            mService = Drive.Builder(
                                transport, jsonFactory, credential
                            ).setHttpRequestInitializer { request ->
                                credential!!.initialize(request)
                                request!!.connectTimeout =
                                    300 * 60000  // 300 minutes connect timeout
                                request.readTimeout =
                                    300 * 60000  // 300 minutes read timeout
                            }
                                .setApplicationName(getString(R.string.app_name))
                                .build()

                            try {
                                sheetService = Sheets.Builder(
                                    httpTransport,
                                    jacksonFactory,
                                    credential
                                )
                                    .setApplicationName(getString(R.string.app_name))
                                    .build()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            DriveService.saveDriveInstance(mService!!)
                            SheetService.saveGoogleSheetInstance(sheetService!!)
                            firebaseAuthWithGoogle(googleSignInAccount.idToken!!)
                            saveUserUpdatedDetail(googleSignInAccount, "new")
                        }
                    }).addOnFailureListener(object : OnFailureListener {
                        override fun onFailure(p0: java.lang.Exception) {
                            showAlert(context, p0.localizedMessage!!)
                        }

                    })
            }
        }


    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                }
            }
    }

    private fun handleSignInResult(acct: GoogleSignInAccount) {
        try {

//            startLoading(context)
//            val hashMap = hashMapOf<String, String>()
//            hashMap["personName"] = acct.displayName.toString()
//            hashMap["personGivenName"] = acct.givenName.toString()
//            hashMap["personFamilyName"] = acct.familyName.toString()
//            hashMap["personEmail"] = acct.email.toString()
//            hashMap["personId"] = acct.id.toString()
//            hashMap["personPhoto"] = acct.photoUrl.toString()
//
//            viewModel.signUp(context, hashMap)
//            viewModel.getSignUp().observe(this, { response ->
//                dismiss()
//                if (response != null) {
//                    if (response.has("errorMessage")) {
//
//                    } else {
            saveUserUpdatedDetail(acct, "new")
//                    }
//                } else {
//                    showAlert(context, getString(R.string.something_wrong_error))
//                }
//            })

        } catch (e: ApiException) {
            var s = e
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
        }
    }


    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentByTag("table")
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START)
        } else if (fragment != null && fragment.isVisible) {
            finish()
        } else {
            //bottomNavigation.selectedItemId = R.id.bottom_scanner
            activeButtonFocus(homeCsvButton, homeInsalesButton,"table")
            supportFragmentManager.beginTransaction().replace(
                R.id.fragment_container,
                ScanFragment(),
                "table"
            )
                .addToBackStack("table")
                .commit()
        }
    }

    // THIS METHOD WILL CALL AFTER SELECT THE QR TYPE WITH INPUT DATA
    override fun onTypeSelected(data: String, position: Int, type: String) {
        var url = ""
        val hashMap = hashMapOf<String, String>()
        hashMap["login"] = "qrmagicapp"
        hashMap["qrId"] = System.currentTimeMillis().toString()
        hashMap["userType"] = "free"
        if (position == 2) {


            hashMap["userUrl"] = data

            startLoading(context)
            viewModel.createDynamicQrCode(context, hashMap)
            viewModel.getDynamicQrCode().observe(this, Observer { response ->
                dismiss()
                if (response != null) {
                    url = response.get("generatedUrl").asString
                    url = if (url.contains(":8990")) {
                        url.replace(":8990", "")
                    } else {
                        url
                    }
                    val qrHistory = CodeHistory(
                        hashMap["login"]!!,
                        hashMap["qrId"]!!,
                        hashMap["userUrl"]!!,
                        type,
                        hashMap["userType"]!!,
                        "qr",
                        "create",
                        "",
                        "1",
                        url,
                        System.currentTimeMillis().toString(),
                        ""
                    )

                    val intent = Intent(context, DesignActivity::class.java)
                    intent.putExtra("ENCODED_TEXT", url)
                    intent.putExtra("QR_HISTORY", qrHistory)
                    startActivity(intent)
                } else {
                    showAlert(context, getString(R.string.something_wrong_error))
                }
            })
        } else {
            encodedTextData = data

            val qrHistory = CodeHistory(
                hashMap["login"]!!,
                hashMap["qrId"]!!,
                encodedTextData,
                type,
                hashMap["userType"]!!,
                "qr",
                "create",
                "",
                "0",
                "",
                System.currentTimeMillis().toString(),
                ""
            )
            val intent = Intent(context, DesignActivity::class.java)
            intent.putExtra("ENCODED_TEXT", encodedTextData)
            intent.putExtra("QR_HISTORY", qrHistory)
            startActivity(intent)
        }

    }

    private fun getUserCurrentFeatures() {
        if (auth.currentUser != null) {
            val uid = auth.currentUser!!.uid
            Constants.firebaseUserId = uid
            getUserFeaturesDetails(context, uid, object : APICallback {
                override fun onSuccess(response: JSONObject) {
                    if (response.getInt("status") == 200) {
                        val features = response.getJSONArray("features")

                        if (features.length() > 0) {
                            for (i in 0 until features.length()) {
                                val feature = features.getJSONObject(i)
                                val status = feature.getInt("status")
                                when (feature.getString("feature")) {
                                    "compressor_feature" -> {
                                        Constants.compressorFeatureStatus = status
                                    }
                                    "modes_switcher" -> {
                                        Constants.modesSwitcherFeatureStatus = status
                                    }
                                    "premium_support" -> {
//                                        mNavigation.menu.findItem(R.id.tickets).isVisible = status == 1
                                        Constants.premiumSupportFeatureStatus = status
                                    }
                                    "unlimited_tables" -> {
                                        Constants.unlimitedTablesFeatureStatus = status
                                    }
                                    else -> {

                                    }
                                }
                            }

                            val currentFragment =
                                supportFragmentManager.findFragmentByTag("scanner")
                            if (currentFragment != null && currentFragment.isVisible) {
                                val scannerFragment =
                                    supportFragmentManager.findFragmentById(R.id.fragment_container) as ScannerFragment
                                scannerFragment.restart()
                            }

                            val scanFragment = supportFragmentManager.findFragmentByTag("tables")
                            if (scanFragment != null && scanFragment.isVisible) {
                                val tableFragment =
                                    supportFragmentManager.findFragmentById(R.id.fragment_container) as ScanFragment
                                tableFragment.restart()
                            }
                        } else {
                            Constants.compressorFeatureStatus = 0
                            Constants.modesSwitcherFeatureStatus = 0
                            Constants.premiumSupportFeatureStatus = 0
                            Constants.unlimitedTablesFeatureStatus = 0
                        }
                    }
                }

                override fun onError(error: VolleyError) {

                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        //getUserCurrentFeatures()
//        if (appSettings.getString("INSALES_STATUS") == "logged"){
//            mNavigation.menu.findItem(R.id.insales_login).isVisible = false
//            mNavigation.menu.findItem(R.id.insales).isVisible = true
//        }else{
//            mNavigation.menu.findItem(R.id.insales).isVisible = false
//            mNavigation.menu.findItem(R.id.insales_login).isVisible = true
//        }
    }

    private fun checkUserLoginStatus() {
        if (appSettings.getBoolean(Constants.isLogin)) {
            mNavigation.menu.findItem(R.id.login).isVisible = false
            mNavigation.menu.findItem(R.id.logout).isVisible = true
            mNavigation.menu.findItem(R.id.profile).isVisible = false
            mNavigation.menu.findItem(R.id.tables).isVisible = true
            mNavigation.menu.findItem(R.id.credit).isVisible = false
            mNavigation.menu.findItem(R.id.user_screen).isVisible = true
//            mNavigation.menu.findItem(R.id.tickets).isVisible = Constants.premiumSupportFeatureStatus == 1
            mNavigation.menu.findItem(R.id.purchase_feature).isVisible = false
            mNavigation.menu.findItem(R.id.field_list).isVisible = true
            mNavigation.menu.findItem(R.id.insales).isVisible = false
            getSearchImageDetail()
            checkAndStartTrialPeriod()
            getPrices()
//            mNavigation.menu.findItem(R.id.dynamic_links).isVisible = true

//            getCurrentSubscriptionDetail(context)

            DatabaseHandler.exporter(context, object : BackupListener {
                override fun onSuccess() {
                }

                override fun onFailure() {

                }

            }, "logout")

            Handler(Looper.myLooper()!!).postDelayed({
                if (auth.currentUser != null) {
                    getUserCredits(context)
                    Constants.firebaseUserId = auth.uid!!
                    getUserPackageDetail(context)
                }

                add5MbFreeStorage()
                val alreadyImported = appSettings.getString(Constants.dbImport)
                if (alreadyImported != null && alreadyImported.isEmpty()) {
                    DatabaseHandler.importer(context, "login")
                }

            }, 2000)


        } else {

            mNavigation.menu.findItem(R.id.login).isVisible = true
            mNavigation.menu.findItem(R.id.logout).isVisible = false
            mNavigation.menu.findItem(R.id.profile).isVisible = false
            mNavigation.menu.findItem(R.id.tables).isVisible = false
            mNavigation.menu.findItem(R.id.credit).isVisible = false
            mNavigation.menu.findItem(R.id.user_screen).isVisible = false
            mNavigation.menu.findItem(R.id.tickets).isVisible = false
            mNavigation.menu.findItem(R.id.purchase_feature).isVisible = false
            mNavigation.menu.findItem(R.id.field_list).isVisible = false
            mNavigation.menu.findItem(R.id.insales).isVisible = false

        }

//        if (appSettings.getString("INSALES_STATUS") == "logged"){
//            mNavigation.menu.findItem(R.id.insales_login).isVisible = false
//            mNavigation.menu.findItem(R.id.insales).isVisible = true
//        }else{
//            mNavigation.menu.findItem(R.id.insales).isVisible = false
//            mNavigation.menu.findItem(R.id.insales_login).isVisible = true
//        }

    }

    private fun getPrices() {
        firebaseDatabase.child(Constants.firebasePrices).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pDetailsPrice: String =
                    snapshot.child("rainforest").child("p_details_price").getValue(
                        Float::class.java
                    ).toString()
                val pListPrice: String =
                    snapshot.child("rainforest").child("p_list_price").getValue(
                        Float::class.java
                    ).toString()
                val characters =
                    snapshot.child("translator").child("characters").getValue(Int::class.java)
                val translatorPrice: String = snapshot.child("translator").child("price").getValue(
                    Float::class.java
                ).toString()

                appSettings.putString("P_DETAILS_PRICE", pDetailsPrice)
                appSettings.putString("P_LIST_PRICE", pListPrice)
                appSettings.putInt("TRANSLATOR_CHARACTERS_LIMIT", characters!!)
                appSettings.putString("TRANSLATOR_CHARACTERS_PRICE", translatorPrice)
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }


    private fun checkAndStartTrialPeriod() {
        if (auth.currentUser != null) {
            val id = auth.uid as String
            Constants.firebaseUserId = id
            startLoading(context)
            activeTrialFeatures(context, Constants.firebaseUserId, object : APICallback {
                override fun onSuccess(response: JSONObject) {
                    dismiss()
                    getUserCurrentFeatures()
                }

                override fun onError(error: VolleyError) {
                    dismiss()
                }

            })
        }
    }

    private fun add5MbFreeStorage() {

        if (auth.currentUser != null) {
            val id = auth.uid as String
            Constants.firebaseUserId = id

            val reference = firebaseDatabase.child("FREE_MB_USERS").child(id)

            reference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.hasChildren()) {

                        val feature = Feature(0, 1, 1, 30, 100.0F, 0, 0)
                        purchaseFeatures(context, feature, id, object : APICallback {
                            override fun onSuccess(response: JSONObject) {
                                if (response.has("package") && !response.isNull("package")) {
                                    firebaseDatabase.child("FREE_MB_USERS")
                                        .child(id).child("status").setValue("yes")
                                }

                            }

                            override fun onError(error: VolleyError) {
                                Log.d("TEST199", error.localizedMessage!!)
                            }

                        })
                    }

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })

        }

//        if (!appSettings.getBoolean("is_first_time")){
//
//            var totalMemory = 0
//            var foundMemory:Float = 0F
//            var isFoundValue = false
//            val auth = FirebaseAuth.getInstance()
//            if (auth.currentUser != null) {
//                val id = auth.uid as String
//                Constants.firebaseUserId = id
//
//            firebaseDatabase.child(Constants.firebaseUserFeatureDetails).child(id)
//                .addListenerForSingleValueEvent(object : ValueEventListener{
//                    override fun onDataChange(dataSnapshot: DataSnapshot) {
//                        if (dataSnapshot.exists()) {
//                            if (dataSnapshot.hasChild("memory")) {
//                                isFoundValue = true
//                                foundMemory =
//                                    dataSnapshot.child("memory").getValue(String::class.java)!!
//                                        .toFloat()
//                                totalMemory =
//                                    dataSnapshot.child("total_memory").getValue(Int::class.java)!!
//                            } else {
//                                isFoundValue = false
//                            }
//                            val params = HashMap<String,Any>()
//                            if (isFoundValue) {
//                                val tMemory = foundMemory + 5
//                                val total = totalMemory + 5
//                                params["memory"] = tMemory.toString()
//                                params["total_memory"] = total
//                            } else {
//                                params["memory"] = 5.toString()
//                                params["total_memory"] = 5
//                            }
//
//                            firebaseDatabase.child(Constants.firebaseUserFeatureDetails)
//                                .child(id)
//                                .setValue(params)
//                                .addOnSuccessListener {
//                                    // Write was successful!
//                                    // ...
//                                    appSettings.putBoolean("is_first_time", true)
//                                }
//                                .addOnFailureListener {
//                                    // Write failed
//                                    // ...
//                                    Log.d("TEST199",it.localizedMessage!!)
//                                }
//                        } else {
//                            val params = HashMap<String,Any>()
//                            params["memory"] = 5.toString()
//                            params["total_memory"] = 5
//
//                            firebaseDatabase.child(Constants.firebaseUserFeatureDetails)
//                                .child(id)
//                                .setValue(params)
//                                .addOnSuccessListener {
//                                    // Write was successful!
//                                    // ...
//                                    appSettings.putBoolean("is_first_time", true)
//                                }
//                                .addOnFailureListener {
//                                    // Write failed
//                                    // ...
//                                    Log.d("TEST199",it.localizedMessage!!)
//                                }
//                        }
//                    }
//
//                    override fun onCancelled(error: DatabaseError) {
//
//                    }
//
//                })
//            }
//        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeGoogleLoginParameters()
            }
        } else if (requestCode == 100) {

//                saveToDriveAppFolder();

        }
    }

    override fun login(callback: LoginCallback) {
        this.callback = callback
        startLogin()
    }

    override fun onPause() {
        hideKeyboard(context, this)
        super.onPause()
    }

    override fun onDestroy() {
        hideKeyboard(context, this)
        super.onDestroy()

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val fragment = supportFragmentManager.findFragmentByTag("scanner")
        fragment?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.home_csv_btn -> {
                activeButtonFocus(homeCsvButton, homeInsalesButton,"table")
                supportFragmentManager.beginTransaction()
//                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(
                        R.id.fragment_container,
                        ScanFragment(),
                        "table"
                    )
                    .addToBackStack("table")
                    .commit()

            }
            R.id.home_insales_btn -> {
                activeButtonFocus(homeInsalesButton, homeCsvButton,"insales")
                supportFragmentManager.beginTransaction()
//                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(
                        R.id.fragment_container,
                        InsalesFragment(),
                        "insales"
                    )
                    .addToBackStack("insales")
                    .commit()

            }
            else -> {

            }
        }
    }

    override fun onChange() {
        supportFragmentManager.beginTransaction()
//                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(
                R.id.fragment_container,
                ScannerFragment(),
                "scanner"
            )
            .addToBackStack("scanner")
            .commit()
    }


}