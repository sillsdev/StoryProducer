package org.sil.storyproducer.controller

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.Secure
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.Network.VolleySingleton
import java.util.*

/**
 * The purpose of this class is to create the Registration activity.
 *
 *
 *
 * Flow of RegistrationActivity:
 *
 *  1. onCreate() is called and calls the following:
 *
 *  * setAccordionListener() is called which adds click listeners to the header sections of the accordion.
 *
 *  1. onPostCreate() is called and calls the following:
 *
 *  1. setupInputFields() is called which takes a root ScrollView.
 *
 *  * getInputFields() is called and takes the root ScrollView and does an in-order
 * traversal of the nodes in the registration xml to find the TextInputEditText
 * and Spinner inputs. Each TextInputEditText and Spinner inputs are added to the
 * sectionViews[] for parsing and saving.
 *
 *  1. addSubmitButtonSave() is called which only parses the TextInpuEditText(not the Spinner input) to check for valid inputs.
 *
 *  * textFieldParsed() is called. This checks to see if all fields were entered
 *  * A confirmation dialog is launched to ask if the user wants to submit the info
 *
 *  1. addRegistrationSkip() is called to set the on click listener for skipping the registration phase temporarily
 *
 *
 *
 * Key classes used in this class:
 *
 *  * [android.widget.Spinner] for input from a selection menu.
 *  * [android.support.design.widget.TextInputEditText] for inputting text for registration fields.
 *
 */
const val FIRST_ACTIVITY_KEY = "first"


open class RegistrationActivity : AppCompatActivity() {

    private val sectionIds = intArrayOf(R.id.language_section, R.id.translator_section, R.id.consultant_section, R.id.trainer_section, R.id.archive_section)
    private val headerIds = intArrayOf(R.id.language_header, R.id.translator_header, R.id.consultant_header, R.id.trainer_header, R.id.archive_header)
    private val sectionViews = arrayOfNulls<View>(sectionIds.size)
    private val headerViews = arrayOfNulls<View>(headerIds.size)
    var resp: String? = null
    var testErr = ""
    var js: MutableMap<String, String> = hashMapOf()

    private var inputFields: List<View>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //first get permissions
        val PERMISSION_ALL = 1
        val PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

        if (!hasPermissions(this, *PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL)
        }

        setContentView(R.layout.activity_registration)

        //Initialize sectionViews[] with the integer id's of the various LinearLayouts
        //Add the listeners to the LinearLayouts's header section.
        for (i in sectionIds.indices) {
            sectionViews[i] = findViewById(sectionIds[i])
            headerViews[i] = findViewById(headerIds[i])
            setAccordionListener(findViewById(headerIds[i]), sectionViews[i]!!)
        }
    }

    override fun onPause(){
        super.onPause()
        storeRegistrationInfo()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setupInputFields()
        addSubmitButtonSave()
        addRegistrationSkip()
        addEthnologueQuestion()
    }

    /**
     * Initializes the inputFields to the inputs of this activity.
     */
    private fun setupInputFields() {
        val view = findViewById<ScrollView>(R.id.registration_scroll_view)
        inputFields = getInputFields(view)
    }

    /**
     * Sets the on click listener for the submit button.
     */
    private fun addSubmitButtonSave() {
        val submitButton = findViewById<Button>(R.id.submit_button)
        submitButton.setOnClickListener {
            val databaseEmailField1 = findViewById<EditText>(R.id.input_database_email_1)
            val databaseEmailField2 = findViewById<EditText>(R.id.input_database_email_2)
            val databaseEmailField3 = findViewById<EditText>(R.id.input_database_email_3)
            val databaseEmail1 = databaseEmailField1.text.toString()
            val databaseEmail2 = databaseEmailField2.text.toString()
            val databaseEmail3 = databaseEmailField3.text.toString()
            val completeFields: Boolean

            submitButton.requestFocus()
            if (databaseEmail1.isEmpty() && databaseEmail2.isEmpty() && databaseEmail3.isEmpty()) {
                createErrorDialog(databaseEmailField1)
                databaseEmailField1.requestFocus()
                for (sectionView in sectionViews) {
                    if (sectionView!!.findFocus() != null) {
                        sectionView.visibility = View.VISIBLE
                        toggleKeyboard(SHOW_KEYBOARD, databaseEmailField1)
                    }
                }
            } else {
                completeFields = parseTextFields()
                createSubmitConfirmationDialog(completeFields)
            }
        }
    }

    /**
     * Sets the on click listener for the registration bypass button
     */
    private fun addRegistrationSkip() {
        val skipButton = findViewById<Button>(R.id.bypass_button)
        skipButton.setOnClickListener { showSkipAlertDialog() }
    }

    /**
     * Sets the on click listener for help on ethnologue codes, which sends them to
     * ethnologue.com to browse language names and their corresponding codes
     */
    private fun addEthnologueQuestion() {
        val questionButton = findViewById<Button>(R.id.ethnologue_question_button)
        questionButton.setOnClickListener {
            val url = "https://www.ethnologue.com/browse"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    /**
     * Sets the listener for the back button pressed
     */
    override fun onBackPressed() {
        showExitAlertDialog()
    }

    /**
     * This function takes a scroll view as the root view of a xml layout and searches for
     * TextInputEditText and spinner_item fields to add to the List.
     *
     * @param rootScrollView The root scroll view where all the children will be visited to
     * check if there is an TextInputEditText field.
     * @return The list of input fields that will be parsed either a spinner_item or a
     * TextInputEditText.
     */
    private fun getInputFields(rootScrollView: ScrollView?): List<View> {

        val inputFieldsList = ArrayList<View>()
        val viewStack = Stack<ViewGroup>()
        var storedValue: String
        var storedSpinnerIndex: Int
        var textFieldView: EditText?
        var spinnerView: Spinner

        //error check
        if (rootScrollView == null) {
            return inputFieldsList
        }

        viewStack.push(rootScrollView)

        while (viewStack.size > 0) {
            val currentView = viewStack.pop()
            if (currentView is TextInputLayout) {
                textFieldView = currentView.editText
                if (textFieldView != null) {
                    storedValue = getStoredValueForView(textFieldView)
                    if (!storedValue.isEmpty()) {
                        textFieldView.setText(storedValue)
                    }
                    inputFieldsList.add(textFieldView)
                }
            } else if (currentView is Spinner) {
                spinnerView = currentView
                storedValue = getStoredValueForView(spinnerView)
                if (!storedValue.isEmpty()) {
                    storedSpinnerIndex = getSpinnerIndexFromString(storedValue)
                    if (storedSpinnerIndex >= 0) {
                        spinnerView.setSelection(storedSpinnerIndex)
                    }
                }
                inputFieldsList.add(spinnerView)
            } else {
                //push children onto stack from right to left
                //pushing on in reverse order so that the traversal is in-order traversal
                for (i in currentView.childCount - 1 downTo 0) {
                    val child = currentView.getChildAt(i)
                    if (child is ViewGroup) {
                        viewStack.push(child)
                    }
                }
            }
        }

        return inputFieldsList
    }

    /**
     * Takes a field and searches the registration data for a value corresponding to it
     * @param view the view to be queried
     * @return the value if found or an empty string if no value found
     */
    private fun getStoredValueForView(view: View): String {
        var viewName = resources.getResourceName(view.id)
        viewName = viewName.replace(ID_PREFIX, "")
        return Workspace.registration.getString(viewName, "")
    }

    /**
     * Finds the index of the spinner array given the string value
     * @param value the value to look for
     * @return index of the spinner array
     */
    private fun getSpinnerIndexFromString(value: String): Int {
        var search = resources.getStringArray(R.array.orthography_list)

        for (i in search.indices) {
            if (value == search[i]) {
                return i
            }
        }

        search = resources.getStringArray(R.array.communication_list)
        for (i in search.indices) {
            if (value == search[i]) {
                return i
            }
        }

        search = resources.getStringArray(R.array.location_type_list)
        for (i in search.indices) {
            if (value == search[i]) {
                return i
            }
        }
        return -1
    }

    /**
     * Parse the text fields when the submit button has been clicked.
     *
     * @return true if all fields are filled in, false if any field is blank
     */
    private fun parseTextFields(): Boolean {
        for (i in inputFields!!.indices) {
            val field = inputFields!![i]
            if (field is TextInputEditText) {
                val inputString = field.text.toString()
                if (inputString.trim { it <= ' ' }.isEmpty()) {
                    // Set focus to first empty field and make section visible if hidden
                    field.requestFocus()
                    for (j in sectionViews.indices) {
                        if (sectionViews[j]!!.findFocus() != null) {
                            sectionViews[j]!!.visibility = View.VISIBLE
                            headerViews[j]!!.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.primary, null))
                            toggleKeyboard(SHOW_KEYBOARD, field)
                            return false
                        }
                    }
                    return false
                }
            }

        }
        return true
    }

    private fun postRegistrationInfo() {
        val myContext = this.applicationContext

        val reg = Workspace.registration

        val PhoneId = Secure.getString(myContext.contentResolver,
                Secure.ANDROID_ID)

        js["Key"] = getString(R.string.api_token)
        js["PhoneId"] = PhoneId
        js["TranslatorEmail"] = reg.getString("translator_email", " ")
        js["TranslatorPhone"] = reg.getString("translator_phone", " ")
        js["TranslatorLanguage"] = reg.getString("translator_languages", " ")
        js["ProjectEthnoCode"] = reg.getString("ethnologue", " ")
        js["ProjectLanguage"] = reg.getString("language", " ")
        js["ProjectCountry"] = reg.getString("country", " ")
        js["ProjectMajorityLanguage"] = reg.getString("lwc", " ")
        js["ConsultantEmail"] = reg.getString("consultant_email", " ")
        js["ConsultantPhone"] = reg.getString("consultant_phone", " ")
        js["TrainerEmail"] = reg.getString("trainer_email", " ")
        js["TrainerPhone"] = reg.getString("trainer_phone", " ")

        Log.i("LOG_VOLLEY", js.toString())
        val req = object : StringRequest(Request.Method.POST, getString(R.string.url_register_phone), Response.Listener { response ->
            Log.i("LOG_VOLEY", response)
            resp = response
        }, Response.ErrorListener { error ->
            Log.e("LOG_VOLLEY", error.toString())
            Log.e("LOG_VOLLEY", "HIT ERROR")
            testErr = error.toString()
        }) {
            override fun getParams(): Map<String, String> {
                return js
            }
        }


        val test = VolleySingleton.getInstance(myContext).requestQueue
        test.add(req)


    }

    /**
     * This function stores the registration information to the saved registration file. The
     * preference file is located in getString(R.string.Registration_File_Name).
     */
    private fun storeRegistrationInfo() {
        val reg = Workspace.registration
        val calendar: Calendar = Calendar.getInstance()
        val date: String
        val androidVersion: String = Build.VERSION.RELEASE
        val manufacturer: String = Build.MANUFACTURER
        val model: String = Build.MODEL
        val day: String
        val month: String
        val year: String
        val hour: String
        var min: String

        for (i in inputFields!!.indices) {
            val field = inputFields!![i]
            if (field is TextInputEditText) {
                var textFieldName = resources.getResourceEntryName(field.id)
                textFieldName = textFieldName.replace("input_", "")
                val textFieldText = field.text.toString()
                reg.putString(textFieldName, textFieldText)

                if (textFieldName == "country") {
                    country = textFieldText
                } else if (textFieldName == "ethnologue") {
                    languageCode = textFieldText
                }
            } else if (field is Spinner) {
                var spinnerName = resources.getResourceEntryName(field.id)
                spinnerName = spinnerName.replace("input_", "")
                val spinnerText = field.selectedItem.toString()
                reg.putString(spinnerName, spinnerText)
                //if(spinnerText.equals("Remote")){
                //isRemoteConsultant = true;
                //}
            }

        }
        // Create timestamp for when the data was submitted
        day = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH))
        month = Integer.toString(calendar.get(Calendar.MONTH) + 1)
        year = Integer.toString(calendar.get(Calendar.YEAR))
        hour = Integer.toString(calendar.get(Calendar.HOUR_OF_DAY))
        min = Integer.toString(calendar.get(Calendar.MINUTE))
        if (min.length < 2) {
            min = "0$min"
        }
        date = "$month/$day/$year $hour:$min"
        reg.putString("date", date)

        // Retrieve phone information
        reg.putString("manufacturer", manufacturer)
        reg.putString("model", model)
        reg.putString("android_version", androidVersion)

        //Store whether remote or not
        var isRemote: Boolean? = false
        if (reg.getString("consultant_location_type", "") == "Remote")
            isRemote = true
        reg.putBoolean("isRemote", isRemote!!)

        reg.save(this)

        //Update workspace text
        Workspace.updateStoryLocalCredits(applicationContext)
    }

    /**
     * This function sets the click listeners to implement the accordion functionality
     * for each section of the registration page
     *
     * @param headerView  a variable of type View denoting the field the user will click to open up
     * a section of the registration
     * @param sectionView a variable of type View denoting the section that will open up
     */
    private fun setAccordionListener(headerView: View, sectionView: View) {
        headerView.setOnClickListener {
            if (sectionView.visibility == View.GONE) {
                sectionView.visibility = View.VISIBLE
                headerView.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.primary, null))
                toggleKeyboard(SHOW_KEYBOARD, sectionView)
            } else {
                sectionView.visibility = View.GONE
                headerView.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.black_semi_transparent, null))
                toggleKeyboard(CLOSE_KEYBOARD, sectionView)
            }
        }
    }

    /**
     * Creates an alert dialog asking if the user wants to exit registration (without saving)
     * If they respond yes, finish activity or send them back to MainActivity
     */
    private fun showExitAlertDialog() {
        val dialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.registration_exit_title))
                .setMessage(getString(R.string.registration_exit_message))
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    startActivity(Intent(this@RegistrationActivity, MainActivity::class.java))
                    finish()
                }.create()

        dialog.show()
    }

    /**
     * Creates an alert dialog asking if the user wants to skip registration
     * If they respond yes, finish activity or send them back to MainActivity
     */
    private fun showSkipAlertDialog() {
        val dialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.registration_skip_title))
                .setMessage(getString(R.string.registration_skip_message))
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    //TODO flush all click event prior to showing the registration screen so that this is not invoked if the user inadvertently
                    //clicks on the splash screen
                    startActivity(Intent(this@RegistrationActivity, MainActivity::class.java))
                    finish()
                }.create()

        dialog.show()
    }

    /**
     * Shows error dialog if user did not provide an email to send the information to
     *
     * @param emailTextField the text field to check if it is blank
     */
    private fun createErrorDialog(emailTextField: EditText) {
        val dialog = AlertDialog.Builder(this@RegistrationActivity)
                .setTitle(getString(R.string.registration_error_title))
                .setMessage(getString(R.string.registration_error_message))
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    // The index here comes from the index of the archive section and header
                    // If another section is added or the sections are rearranged, this index
                    // will need to be changed
                    sectionViews[4]!!.visibility = View.VISIBLE
                    headerViews[4]!!.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.primary, null))
                    emailTextField.requestFocus()
                }.create()
        dialog.show()
    }

    /**
     * Creates a dialog to confirm the user wants to submit
     *
     * @param completeFields true if all fields filled in, false if any fields are empty
     */
    private fun createSubmitConfirmationDialog(completeFields: Boolean) {
        val message: String
        if (completeFields) {
            message = getString(R.string.registration_submit_complete_message)
        } else {
            message = getString(R.string.registration_submit_incomplete_message)
        }
        val dialog = AlertDialog.Builder(this@RegistrationActivity)
                .setTitle(getString(R.string.registration_submit_title))
                .setMessage(message)
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    Workspace.registration.complete = true
                    storeRegistrationInfo()
                    postRegistrationInfo()
                    val saveToast = Toast.makeText(this@RegistrationActivity, R.string.registration_saved_successfully, Toast.LENGTH_LONG)
                    saveToast.show()
                    sendEmail()
                }.create()

        dialog.show()
    }

    /**
     * This function toggles the soft input keyboard. Allowing the user to have the keyboard
     * to open or close seamlessly alongside the rest UI.
     * @param showKeyBoard The boolean to be passed in to determine if the keyboard show be shown.
     * @param aView The view associated with the soft input keyboard.
     */
    private fun toggleKeyboard(showKeyBoard: Boolean, aView: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (showKeyBoard) {
            imm.showSoftInput(aView, 0)
        } else {
            imm.hideSoftInputFromWindow(aView.windowToken, 0)
        }
    }

    private fun sendEmail() {

        val reg = Workspace.registration

        val message = formatRegistrationEmail()

        val TO = arrayOf(reg.getString("database_email_1", ""), reg.getString("database_email_2", ""), reg.getString("database_email_3", ""), reg.getString("translator_email", ""), reg.getString("consultant_email", ""), reg.getString("trainer_email", ""))

        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.data = Uri.parse("mailto:")
        emailIntent.type = "text/plain"

        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO)
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "StoryProducer Registration Info")
        emailIntent.putExtra(Intent.EXTRA_TEXT, message)

        try {
            this.startActivity(Intent.createChooser(emailIntent, getText(R.string.registration_submit)))
            this.finish()
            reg.putBoolean(EMAIL_SENT, true)
            reg.save(this)
            Log.i("Finished sending email", "")
        } catch (ex: android.content.ActivityNotFoundException) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            Toast.makeText(this,
                    "There is no email client installed.", Toast.LENGTH_SHORT).show()
        }

    }

    companion object {


        val EMAIL_SENT = "registration_email_sent"

        private val ID_PREFIX = "org.sil.storyproducer:id/input_"
        private val SHOW_KEYBOARD = true
        private val CLOSE_KEYBOARD = false

        //private static boolean isRemoteConsultant = false;
        //public static boolean haveRemoteConsultant(){ return isRemoteConsultant;}

        var country: String? = null
            private set
        var languageCode: String? = null
            private set

        private fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null) {
                for (permission in permissions) {
                    if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        return false
                    }
                }
            }
            return true
        }

        /**
         * Returns a string of formatted fields in readable format based on the registration data
         *
         * @return a well formatted string of registration information
         */
        private fun formatRegistrationEmail(): String {
            // Gives the order for retrieval and printing
            // Empty strings ("") are used to separate sections in the printing phase
            val keyListOrder = arrayOf("date", "", "language", "ethnologue", "country", "location", "town", "lwc", "orthography", "", "translator_name", "translator_education", "translator_languages", "translator_phone", "translator_email", "translator_communication_preference", "translator_location", "", "consultant_name", "consultant_languages", "consultant_phone", "consultant_email", "consultant_communication_preference", "consultant_location", "consultant_location_type", "", "trainer_name", "trainer_languages", "trainer_phone", "trainer_email", "trainer_communication_preference", "trainer_location", "", "manufacturer", "model", "android_version")

            val message = StringBuilder()
            var formattedKey: String

            for (aKeyListOrder in keyListOrder) {
                // Section separation appends newline
                if (aKeyListOrder.isEmpty()) {
                    message.append("\n")
                    // Find key and value and print in clean format
                } else {
                    formattedKey = aKeyListOrder.replace("_", " ")
                    formattedKey = formattedKey.toUpperCase()
                    message.append(formattedKey)
                    message.append(": ")
                    message.append(Workspace.registration.getString(aKeyListOrder, "NA"))
                    message.append("\n")
                }
            }
            return message.toString()
        }
    }
}