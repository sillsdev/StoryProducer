package org.sil.storyproducer.controller

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.TextInputEditText
import android.support.v4.app.ActivityCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.crashlytics.android.Crashlytics
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.Network.VolleySingleton

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
 *  2. onPostCreate() is called and calls the following:
 *
 *  3. addSubmitButtonSave() is called which only parses the TextInpuEditText(not the Spinner input) to check for valid inputs.
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

open class RegistrationActivity : AppCompatActivity() {

    private lateinit var projectLanguageEditText: TextInputEditText
    private lateinit var projectEthnoCodeEditText: TextInputEditText
    private lateinit var projectCountryEditText: TextInputEditText
    private lateinit var projectRegionEditText: TextInputEditText
    private lateinit var projectCityEditText: TextInputEditText
    private lateinit var projectMajorityLanguageEditText: TextInputEditText
    private lateinit var projectOrthographySpinner: Spinner

    private lateinit var translatorNameEditText: TextInputEditText
    private lateinit var translatorEducationEditText: TextInputEditText
    private lateinit var translatorLanguagesEditText: TextInputEditText
    private lateinit var translatorPhoneEditText: TextInputEditText
    private lateinit var translatorEmailEditText: TextInputEditText
    private lateinit var translatorCommunicationPreferenceSpinner: Spinner
    private lateinit var translatorLocationEditText: TextInputEditText

    private lateinit var consultantNameEditText: TextInputEditText
    private lateinit var consultantLanguagesEditText: TextInputEditText
    private lateinit var consultantPhoneEditText: TextInputEditText
    private lateinit var consultantEmailEditText: TextInputEditText
    private lateinit var consultantCommunicationPreferenceSpinner: Spinner
    private lateinit var consultantLocationEditText: TextInputEditText
    private lateinit var consultantLocationTypeSpinner: Spinner

    private lateinit var trainerNameEditText: TextInputEditText
    private lateinit var trainerLanguagesEditText: TextInputEditText
    private lateinit var trainerPhoneEditText: TextInputEditText
    private lateinit var trainerEmailEditText: TextInputEditText
    private lateinit var trainerCommunicationPreferenceSpinner: Spinner
    private lateinit var trainerLocationEditText: TextInputEditText

    private lateinit var archiveEmail1EditText: TextInputEditText
    private lateinit var archiveEmail2EditText: TextInputEditText
    private lateinit var archiveEmail3EditText: TextInputEditText

    private lateinit var sectionViews: Array<View>
    private lateinit var headerViews: Array<View>
    var resp: String? = null
    var testErr = ""

    private lateinit var inputFields: Array<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!hasPermissions(this, *permissions)) {
            val permissionsAll = 1
            ActivityCompat.requestPermissions(this, permissions, permissionsAll)
        }

        setContentView(R.layout.activity_registration)

        // TODO @pwhite: Some of the ugliness could be removed while
        // simultaneously improving user experience if some field for the
        // translator, consultant, and trainer were put in a `Person`
        // abstraction. The UI could reflect this be reusing the same fragment
        // for each of the forms. Or perhaps the ROCC can serve some
        // pre-populated people which the user can select from; this would mean
        // that the user would not have to fill out each of the forms and
        // potentially make a mistake with an email, for example. Of course,
        // this ought not to be required since the application might not be
        // online.

        // @pwhite: This is a lot of boilerplate to load the current values of
        // the registration.json file. I'm not the most pleased, but I find
        // this far better than doing a walk of the UI tree to find all the
        // text and spinner controls and setting them from a JSON object based
        // on some string manipulation of the android resource IDs. This was a
        // previous solution which has been replaced by this ugliness; although
        // this solution is rather ugly, it is extremely clear what is
        // happening, and quite easy to modify. In addition, this is likely
        // more efficient than the previous solution since we know exactly what
        // controls exist and thus don't need to walk the UI tree to find them.

        // NOW @pwhite: Add phone make, model and manufacturer as before.
        val orthographySpinnerOptions = resources.getStringArray(R.array.orthography_list)
        val communicationSpinnerOptions = resources.getStringArray(R.array.communication_list)
        val locationTypeSpinnerOptions = resources.getStringArray(R.array.location_type_list)

        projectLanguageEditText = findViewById(R.id.input_language)
        projectLanguageEditText.setText(Workspace.registration.projectLanguage)
        projectEthnoCodeEditText = findViewById(R.id.input_ethnologue)
        projectEthnoCodeEditText.setText(Workspace.registration.projectEthnoCode)
        projectCountryEditText = findViewById(R.id.input_country)
        projectCountryEditText.setText(Workspace.registration.projectCountry)
        projectRegionEditText = findViewById(R.id.input_location)
        projectRegionEditText.setText(Workspace.registration.projectRegion)
        projectCityEditText = findViewById(R.id.input_town)
        projectCityEditText.setText(Workspace.registration.projectCity)
        projectMajorityLanguageEditText = findViewById(R.id.input_lwc)
        projectMajorityLanguageEditText.setText(Workspace.registration.projectMajorityLanguage)
        projectOrthographySpinner = findViewById(R.id.input_orthography)
        projectOrthographySpinner.setSelection(
            indexOfOrZero(orthographySpinnerOptions, Workspace.registration.projectOrthography))

        translatorNameEditText = findViewById(R.id.input_translator_name)
        translatorNameEditText.setText(Workspace.registration.translatorName)
        translatorEducationEditText = findViewById(R.id.input_translator_education)
        translatorEducationEditText.setText(Workspace.registration.translatorEducation)
        translatorLanguagesEditText = findViewById(R.id.input_translator_languages)
        translatorLanguagesEditText.setText(Workspace.registration.translatorLanguages)
        translatorPhoneEditText = findViewById(R.id.input_translator_phone)
        translatorPhoneEditText.setText(Workspace.registration.translatorPhone)
        translatorEmailEditText = findViewById(R.id.input_translator_email)
        translatorEmailEditText.setText(Workspace.registration.translatorEmail)
        translatorCommunicationPreferenceSpinner = findViewById(R.id.input_translator_communication_preference)
        translatorCommunicationPreferenceSpinner.setSelection(
            indexOfOrZero(communicationSpinnerOptions, Workspace.registration.translatorCommunicationPreference))
        translatorLocationEditText = findViewById(R.id.input_translator_location)
        translatorLocationEditText.setText(Workspace.registration.translatorLocation)

        consultantNameEditText = findViewById(R.id.input_consultant_name)
        consultantNameEditText.setText(Workspace.registration.consultantName)
        consultantLanguagesEditText = findViewById(R.id.input_consultant_languages)
        consultantLanguagesEditText.setText(Workspace.registration.consultantLanguages)
        consultantPhoneEditText = findViewById(R.id.input_consultant_phone)
        consultantPhoneEditText.setText(Workspace.registration.consultantPhone)
        consultantEmailEditText = findViewById(R.id.input_consultant_email)
        consultantEmailEditText.setText(Workspace.registration.consultantEmail)
        consultantCommunicationPreferenceSpinner = findViewById(R.id.input_consultant_communication_preference)
        consultantCommunicationPreferenceSpinner.setSelection(
            indexOfOrZero(communicationSpinnerOptions, Workspace.registration.consultantCommunicationPreference))
        consultantLocationEditText = findViewById(R.id.input_consultant_location)
        consultantLocationEditText.setText(Workspace.registration.consultantLocation)
        consultantLocationTypeSpinner = findViewById(R.id.input_consultant_location_type)
        consultantLocationTypeSpinner.setSelection(
            indexOfOrZero(locationTypeSpinnerOptions, Workspace.registration.consultantLocationType))

        trainerNameEditText = findViewById(R.id.input_trainer_name)
        trainerNameEditText.setText(Workspace.registration.trainerName)
        trainerLanguagesEditText = findViewById(R.id.input_trainer_languages)
        trainerLanguagesEditText.setText(Workspace.registration.trainerLanguages)
        trainerPhoneEditText = findViewById(R.id.input_trainer_phone)
        trainerPhoneEditText.setText(Workspace.registration.trainerPhone)
        trainerEmailEditText = findViewById(R.id.input_trainer_email)
        trainerEmailEditText.setText(Workspace.registration.trainerEmail)
        trainerCommunicationPreferenceSpinner = findViewById(R.id.input_trainer_communication_preference)
        trainerCommunicationPreferenceSpinner.setSelection(
            indexOfOrZero(communicationSpinnerOptions, Workspace.registration.trainerCommunicationPreference))
        trainerLocationEditText = findViewById(R.id.input_trainer_location)
        trainerLocationEditText.setText(Workspace.registration.trainerLocation)

        archiveEmail1EditText = findViewById(R.id.input_database_email_1)
        archiveEmail1EditText.setText(Workspace.registration.archiveEmail1)
        archiveEmail2EditText = findViewById(R.id.input_database_email_2)
        archiveEmail2EditText.setText(Workspace.registration.archiveEmail2)
        archiveEmail3EditText = findViewById(R.id.input_database_email_3)
        archiveEmail3EditText.setText(Workspace.registration.archiveEmail3)

        inputFields = arrayOf(
                projectLanguageEditText,
                projectEthnoCodeEditText,
                projectCountryEditText,
                projectRegionEditText,
                projectCityEditText,
                projectMajorityLanguageEditText,
                projectOrthographySpinner,
                translatorNameEditText,
                translatorEducationEditText,
                translatorLanguagesEditText,
                translatorPhoneEditText,
                translatorEmailEditText,
                translatorCommunicationPreferenceSpinner,
                translatorLocationEditText,
                consultantNameEditText,
                consultantLanguagesEditText,
                consultantPhoneEditText,
                consultantEmailEditText,
                consultantCommunicationPreferenceSpinner,
                consultantLocationEditText,
                consultantLocationTypeSpinner,
                trainerNameEditText,
                trainerLanguagesEditText,
                trainerPhoneEditText,
                trainerEmailEditText,
                trainerCommunicationPreferenceSpinner,
                trainerLocationEditText,
                archiveEmail1EditText,
                archiveEmail2EditText,
                archiveEmail3EditText)

        // Initialize sectionViews[] with the integer id's of the various LinearLayouts
        // Add the listeners to the LinearLayouts's header section.

        sectionViews = arrayOf(
                findViewById(R.id.language_section),
                findViewById(R.id.translator_section),
                findViewById(R.id.consultant_section),
                findViewById(R.id.trainer_section),
                findViewById(R.id.archive_section))
        headerViews = arrayOf(
                findViewById(R.id.language_header),
                findViewById(R.id.translator_header),
                findViewById(R.id.consultant_header),
                findViewById(R.id.trainer_header),
                findViewById(R.id.archive_header))

        for (i in sectionViews.indices) {
            setAccordionListener(headerViews[i], sectionViews[i])
        }
    }

    fun indexOfOrZero(arr: Array<String>, string: String): Int {
        return maxOf(0, arr.indexOf(string))
    }

    override fun onPause() {
        super.onPause()
        storeRegistrationInfo()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        addSubmitButtonSave()
        addRegistrationSkip()
        addEthnologueQuestion()
    }

    /**
     * Sets the on click listener for the submit button.
     */
    private fun addSubmitButtonSave() {
        val submitButton = findViewById<Button>(R.id.submit_button)
        submitButton.setOnClickListener {
            val databaseEmail1 = archiveEmail1EditText.text.toString()
            val databaseEmail2 = archiveEmail2EditText.text.toString()
            val databaseEmail3 = archiveEmail3EditText.text.toString()
            submitButton.requestFocus()
            if (databaseEmail1.isEmpty() && databaseEmail2.isEmpty() && databaseEmail3.isEmpty()) {
                createErrorDialog(archiveEmail1EditText)
                archiveEmail1EditText.requestFocus()
                for (sectionView in sectionViews) {
                    if (sectionView.findFocus() != null) {
                        sectionView.visibility = View.VISIBLE
                        toggleKeyboard(SHOW_KEYBOARD, archiveEmail1EditText)
                    }
                }
            } else {
                val message = getString(if (checkAllFieldsFilledOutOrFocusEmptyField()) {
                    R.string.registration_submit_complete_message
                } else {
                    R.string.registration_submit_incomplete_message
                })
                val dialog = AlertDialog.Builder(this@RegistrationActivity)
                        .setTitle(getString(R.string.registration_submit_title))
                        .setMessage(message)
                        .setNegativeButton(getString(R.string.no), null)
                        .setPositiveButton(getString(R.string.yes)) { _, _ ->
                            Workspace.registration.registrationComplete = true
                            storeRegistrationInfo()
                            postRegistrationInfo()
                            val saveToast = Toast.makeText(this@RegistrationActivity, R.string.registration_saved_successfully, Toast.LENGTH_LONG)
                            saveToast.show()
                            sendEmail()
                        }.create()
                dialog.show()
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
     * Parse the text fields when the submit button has been clicked.
     *
     * @return true if all fields are filled in, false if any field is blank
     */
    private fun checkAllFieldsFilledOutOrFocusEmptyField(): Boolean {
        for (field in inputFields) {
            if (field is TextInputEditText) {
                if (field.text.toString().trim().isEmpty()) {
                    // Set focus to first empty field and make section visible if hidden
                    field.requestFocus()
                    for (j in sectionViews.indices) {
                        if (sectionViews[j].findFocus() != null) {
                            sectionViews[j].visibility = View.VISIBLE
                            headerViews[j].setBackgroundColor(ResourcesCompat.getColor(resources, R.color.primary, null))
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
        val reg = Workspace.registration
        val js = HashMap<String, String>()
        js["PhoneId"] = Settings.Secure.getString(
                this.applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        js["Key"] = getString(R.string.api_token)
        js["TranslatorEmail"] = reg.translatorEmail
        js["TranslatorPhone"] = reg.translatorPhone
        js["TranslatorLanguage"] = reg.translatorLanguages
        js["ProjectEthnoCode"] = reg.projectEthnoCode
        js["ProjectLanguage"] = reg.projectLanguage
        js["ProjectCountry"] = reg.projectCountry
        js["ProjectMajorityLanguage"] = reg.projectMajorityLanguage
        js["ConsultantEmail"] = reg.consultantEmail
        js["ConsultantPhone"] = reg.consultantPhone
        js["TrainerEmail"] = reg.trainerEmail
        js["TrainerPhone"] = reg.trainerPhone

        Log.i("LOG_VOLLEY", js.toString())
        val registerPhoneUrl = Workspace.getRoccUrlPrefix(this) + getString(R.string.url_register_phone)
        val req = object : StringRequest(Method.POST, registerPhoneUrl, Response.Listener { response ->
            Log.i("LOG_VOLLEY", response)
            resp = response
        }, Response.ErrorListener { error ->
            Log.e("LOG_VOLLEY", "Failed to send registration message")
            Log.e("LOG_VOLLEY", error.toString())
            testErr = error.toString()
        }) {
            override fun getParams(): Map<String, String> {
                return js
            }
        }

        val test = VolleySingleton.getInstance(this.applicationContext).requestQueue
        test.add(req)
    }

    /**
     * This function stores the registration information to the saved registration file. The
     * preference file is located in getString(R.string.Registration_File_Name).
     */
    private fun storeRegistrationInfo() {
        val reg = Workspace.registration
        reg.projectLanguage = projectLanguageEditText.text.toString()
        reg.projectEthnoCode = projectEthnoCodeEditText.text.toString()
        reg.projectCountry = projectCountryEditText.text.toString()
        reg.projectRegion = projectRegionEditText.text.toString()
        reg.projectCity = projectCityEditText.text.toString()
        reg.projectMajorityLanguage = projectMajorityLanguageEditText.text.toString()
        reg.projectOrthography = projectOrthographySpinner.selectedItem?.toString() ?: ""

        reg.translatorName = translatorNameEditText.text.toString()
        reg.translatorEducation = translatorEducationEditText.text.toString()
        reg.translatorLanguages = translatorLanguagesEditText.text.toString()
        reg.translatorPhone = translatorPhoneEditText.text.toString()
        reg.translatorEmail = translatorEmailEditText.text.toString()
        reg.translatorCommunicationPreference = translatorCommunicationPreferenceSpinner.selectedItem.toString()
        reg.translatorLocation = translatorLocationEditText.text.toString()

        reg.consultantName = consultantNameEditText.text.toString()
        reg.consultantLanguages = consultantLanguagesEditText.text.toString()
        reg.consultantPhone = consultantPhoneEditText.text.toString()
        reg.consultantEmail = consultantEmailEditText.text.toString()
        reg.consultantCommunicationPreference = consultantCommunicationPreferenceSpinner.selectedItem.toString()
        reg.consultantLocation = consultantLocationEditText.text.toString()
        reg.consultantLocationType = consultantLocationTypeSpinner.selectedItem.toString()

        reg.trainerName = trainerNameEditText.text.toString()
        reg.trainerLanguages = trainerLanguagesEditText.text.toString()
        reg.trainerPhone = trainerPhoneEditText.text.toString()
        reg.trainerEmail = trainerEmailEditText.text.toString()
        reg.trainerCommunicationPreference = trainerCommunicationPreferenceSpinner.selectedItem.toString()
        reg.trainerLocation = trainerLocationEditText.text.toString()

        reg.archiveEmail1 = archiveEmail1EditText.text.toString()
        reg.archiveEmail2 = archiveEmail2EditText.text.toString()
        reg.archiveEmail3 = archiveEmail3EditText.text.toString()

        reg.save(this)

        // Update workspace text
        Workspace.updateStoryLocalCredits(applicationContext)
    }

    /**
     * This function sets the click listeners to implement the accordion functionality
     * for each section of the registration page
     *
     * @param headerView a variable of type View denoting the field the user will click to open up
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
                    // TODO flush all click event prior to showing the registration screen so that this is not invoked if the user inadvertently
                    // clicks on the splash screen
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
                    sectionViews[4].visibility = View.VISIBLE
                    headerViews[4].setBackgroundColor(ResourcesCompat.getColor(resources, R.color.primary, null))
                    emailTextField.requestFocus()
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
        val message = StringBuilder()
        message.append("Date: ${reg.date}\n\n")
        message.append("Language: ${reg.projectLanguage}\n")
        message.append("Ethnologue: ${reg.projectEthnoCode}\n")
        message.append("Country: ${reg.projectCountry}\n")
        message.append("Location: ${reg.projectRegion}\n")
        message.append("Town: ${reg.projectCity}\n")
        message.append("Lwc: ${reg.projectMajorityLanguage}\n")
        message.append("Orthography: ${reg.projectOrthography}\n\n")
        message.append("Translator Name: ${reg.translatorName}\n")
        message.append("Translator Education: ${reg.translatorEducation}\n")
        message.append("Translator Languages: ${reg.translatorLanguages}\n")
        message.append("Translator Phone: ${reg.translatorPhone}\n")
        message.append("Translator Email: ${reg.translatorEmail}\n")
        message.append("Translator Communication Preference: ${reg.trainerCommunicationPreference}\n")
        message.append("Translator Location: ${reg.translatorLocation}\n\n")
        message.append("Consultant Name: ${reg.consultantName}\n")
        message.append("Consultant Languages: ${reg.consultantLanguages}\n")
        message.append("Consultant Phone: ${reg.consultantPhone}\n")
        message.append("Consultant Email: ${reg.consultantEmail}\n")
        message.append("Consultant Communication Preference: ${reg.consultantCommunicationPreference}\n")
        message.append("Consultant Location: ${reg.consultantLocation}\n")
        message.append("Consultant Location Type: ${reg.consultantLocationType}\n\n")
        message.append("Trainer Name: ${reg.trainerName}\n")
        message.append("Trainer Languages: ${reg.trainerLanguages}\n")
        message.append("Trainer Phone: ${reg.trainerPhone}\n")
        message.append("Trainer Email: ${reg.trainerEmail}\n")
        message.append("Trainer Communication Preference: ${reg.trainerCommunicationPreference}\n")
        message.append("Trainer Location: ${reg.trainerLocation}\n\n")
        message.append("Phone Manufacturer: ${Build.MANUFACTURER}\n")
        message.append("Phone Model: ${Build.MODEL}\n")
        message.append("Android Version: ${Build.VERSION.RELEASE}\n")

        val toAddresses = arrayOf(
                reg.archiveEmail1, reg.archiveEmail2, reg.archiveEmail3,
                reg.translatorEmail, reg.consultantEmail, reg.trainerEmail)

        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.data = Uri.parse("mailto:")
        emailIntent.type = "text/plain"

        emailIntent.putExtra(Intent.EXTRA_EMAIL, toAddresses)
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "StoryProducer Registration Info")
        emailIntent.putExtra(Intent.EXTRA_TEXT, message.toString())

        try {
            this.startActivity(Intent.createChooser(emailIntent, getText(R.string.registration_submit)))
            reg.registrationEmailSent = true
            reg.save(this)
            Log.i("Finished sending email", "")
            // TODO @pwhite: Redirect to main activity after the email has been sent.
        } catch (ex: android.content.ActivityNotFoundException) {
            Crashlytics.logException(ex)
            Toast.makeText(this,
                    "There is no email client installed.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {

        private const val SHOW_KEYBOARD = true
        private const val CLOSE_KEYBOARD = false

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
    }
}

open class WorkspaceDialogUpdateActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Now, let's find the workspace path.
        Workspace.initializeWorkspace(this)
        val dialog = AlertDialog.Builder(this)
                .setTitle(Html.fromHtml("<b>${getString(R.string.update_workspace)}</b>"))
                .setMessage(Html.fromHtml(getString(R.string.workspace_selection_help)))
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                    startActivityForResult(intent, RQS_OPEN_DOCUMENT_TREE)
                }.create()
        dialog.show()
        super.onCreate(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == RQS_OPEN_DOCUMENT_TREE) {
            Workspace.setupWorkspacePath(this, data?.data!!)
            contentResolver.takePersistableUriPermission(data.data!!,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        intent = Intent(this, RegistrationActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val RQS_OPEN_DOCUMENT_TREE = 52
    }
}

class WorkspaceUpdateActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Workspace.clearWorkspace()
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        startActivityForResult(intent, RQS_OPEN_DOCUMENT_TREE)
        super.onCreate(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == RQS_OPEN_DOCUMENT_TREE) {
            Workspace.setupWorkspacePath(this, data?.data!!)
            contentResolver.takePersistableUriPermission(data.data!!,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        intent = Intent(this, RegistrationActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val RQS_OPEN_DOCUMENT_TREE = 52
    }
}
