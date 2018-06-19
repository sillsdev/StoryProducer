package org.sil.storyproducer.controller;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Workspace;
import org.sil.storyproducer.tools.Network.VolleySingleton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import android.provider.Settings.Secure;

/**
 * The purpose of this class is to create the Registration activity.
 * <p>
 * <p>Flow of RegistrationActivity:</p>
 * <ol>
 * <li>onCreate() is called and calls the following:</li>
 * <ul>
 * <li>setAccordionListener() is called which adds click listeners to the header sections of the accordion.</li>
 * </ul>
 * <li>onPostCreate() is called and calls the following:</li>
 * <ol>
 * <li>setupInputFields() is called which takes a root ScrollView.</li>
 * <ul>
 * <li>getInputFields() is called and takes the root ScrollView and does an in-order
 * traversal of the nodes in the registration xml to find the TextInputEditText
 * and Spinner inputs. Each TextInputEditText and Spinner inputs are added to the
 * sectionViews[] for parsing and saving.</li>
 * </ul>
 * <li>addSubmitButtonSave() is called which only parses the TextInpuEditText(not the Spinner input) to check for valid inputs.</li>
 * <ul>
 * <li>textFieldParsed() is called. This checks to see if all fields were entered</li>
 * <li>A confirmation dialog is launched to ask if the user wants to submit the info</li>
 * </ul>
 * <li>addRegistrationSkip() is called to set the on click listener for skipping the registration phase temporarily</li>
 * </ol>
 * </ol>
 * <p>Key classes used in this class:</p>
 * <ul>
 * <li>{@link android.widget.Spinner} for input from a selection menu.</li>
 * <li>{@link android.support.design.widget.TextInputEditText} for inputting text for registration fields.</li>
 * <li>{@link android.content.SharedPreferences} for saving registration information.</li>
 * </ul>
 */
public class RegistrationActivity extends AppCompatActivity {


    public static final String FIRST_ACTIVITY_KEY = "first";
    public static final String EMAIL_SENT = "registration_email_sent";

    private static final String ID_PREFIX = "org.sil.storyproducer:id/input_";
    private static final int RQS_OPEN_DOCUMENT_TREE = 52;

    private final int [] sectionIds = {R.id.language_section, R.id.translator_section,R.id.consultant_section,R.id.trainer_section,R.id.archive_section};
    private final int [] headerIds = {R.id.language_header, R.id.translator_header, R.id.consultant_header, R.id.trainer_header, R.id.archive_header};
    private View[] sectionViews = new View[sectionIds.length];
    private View[] headerViews = new View[headerIds.length];
    private static final boolean SHOW_KEYBOARD = true;
    private static final boolean CLOSE_KEYBOARD = false;
    private static final boolean PARSE_ALL_FIELDS = true;

    //private static boolean isRemoteConsultant = false;
    private static String country;
    private static String languageCode;
    public String resp = null;
    public String testErr = "";
    public  Map<String,String> js;

    private List<View> inputFields;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //first get permissions
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE};

        if(!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        setContentView(R.layout.activity_registration);

        //Now, let's find the workspace path.
        Workspace.INSTANCE.initializeWorskpace(this);
        if(!Workspace.INSTANCE.getWorkspace().exists()){
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, RQS_OPEN_DOCUMENT_TREE);
        }

        //Initialize sectionViews[] with the integer id's of the various LinearLayouts
        //Add the listeners to the LinearLayouts's header section.
        for (int i = 0; i < sectionIds.length; i++) {
            sectionViews[i] = findViewById(sectionIds[i]);
            headerViews[i] = findViewById(headerIds[i]);
            setAccordionListener(findViewById(headerIds[i]), sectionViews[i]);
        }
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK && requestCode == RQS_OPEN_DOCUMENT_TREE){
            Workspace.INSTANCE.setWorkspace(DocumentFile.fromTreeUri(this, data.getData()));
            Workspace.INSTANCE.updateStories(this);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupInputFields();
        addSubmitButtonSave();
        addRegistrationSkip();
        addEthnologueQuestion();
    }

    /**
     * Initializes the inputFields to the inputs of this activity.
     */
    private void setupInputFields() {
        View view = findViewById(R.id.scroll_view);

        //Find the top level linear layout
        if (view instanceof ScrollView) {
            ScrollView scrollView = (ScrollView) view;
            inputFields = getInputFields(scrollView);
        }
    }

    /**
     * Sets the on click listener for the submit button.
     */
    private void addSubmitButtonSave() {
        final SharedPreferences prefs = this.getSharedPreferences(this.getString(R.string.registration_filename), MODE_PRIVATE);
        final Button submitButton = findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                final EditText databaseEmailField1 = findViewById(R.id.input_database_email_1);
                final EditText databaseEmailField2 = findViewById(R.id.input_database_email_2);
                final EditText databaseEmailField3 = findViewById(R.id.input_database_email_3);
                String databaseEmail1 = databaseEmailField1.getText().toString();
                String databaseEmail2 = databaseEmailField2.getText().toString();
                String databaseEmail3 = databaseEmailField3.getText().toString();
                boolean completeFields;

                submitButton.requestFocus();
                if (databaseEmail1.isEmpty() && databaseEmail2.isEmpty() && databaseEmail3.isEmpty()) {
                    createErrorDialog(databaseEmailField1);
                    databaseEmailField1.requestFocus();
                    for (View sectionView : sectionViews) {
                        if (sectionView.findFocus() != null) {
                            sectionView.setVisibility(View.VISIBLE);
                            toggleKeyboard(SHOW_KEYBOARD, databaseEmailField1);
                        }
                    }
                } else {
                    completeFields = parseTextFields();
                    createSubmitConfirmationDialog(completeFields);
                }

            }
        });
    }

    /**
     * Sets the on click listener for the registration bypass button
     */
    private void addRegistrationSkip() {
        final Button skipButton = findViewById(R.id.bypass_button);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSkipAlertDialog();
            }
        });
    }

    /**
     * Sets the on click listener for help on ethnologue codes, which sends them to
     * ethnologue.com to browse language names and their corresponding codes
     */
    private void addEthnologueQuestion() {
        final Button questionButton = findViewById(R.id.ethnologue_question_button);
        questionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://www.ethnologue.com/browse";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });
    }

    /**
     * Sets the listener for the back button pressed
     */
    @Override
    public void onBackPressed() {
        showExitAlertDialog();
    }

    /**
     * This function takes a scroll view as the root view of a xml layout and searches for
     * TextInputEditText and spinner_item fields to add to the List.
     *
     * @param rootScrollView The root scroll view where all the children will be visited to
     *                       check if there is an TextInputEditText field.
     * @return The list of input fields that will be parsed either a spinner_item or a
     * TextInputEditText.
     */
    private List<View> getInputFields(ScrollView rootScrollView) {

        List<View> inputFieldsList = new ArrayList<>();
        Stack<ViewGroup> viewStack = new Stack<>();
        String viewName, storedValue;
        int storedSpinnerIndex;
        EditText textFieldView;
        Spinner spinnerView;

        //error check
        if (rootScrollView == null) {
            return inputFieldsList;
        }

        viewStack.push(rootScrollView);

        while (viewStack.size() > 0) {
            ViewGroup currentView = viewStack.pop();
            if (currentView instanceof TextInputLayout) {
                textFieldView = ((TextInputLayout) currentView).getEditText();
                if (textFieldView != null) {
                    storedValue = getStoredValueForView(textFieldView);
                    if (!storedValue.isEmpty()) {
                        textFieldView.setText(storedValue);
                    }
                    inputFieldsList.add(textFieldView);
                }
            } else if (currentView instanceof Spinner) {
                spinnerView = (Spinner) currentView;
                storedValue = getStoredValueForView(spinnerView);
                if(!storedValue.isEmpty()) {
                    storedSpinnerIndex = getSpinnerIndexFromString(storedValue);
                    if (storedSpinnerIndex >= 0) {
                        spinnerView.setSelection(storedSpinnerIndex);
                    }
                }
                inputFieldsList.add(spinnerView);
            } else {
                //push children onto stack from right to left
                //pushing on in reverse order so that the traversal is in-order traversal
                for (int i = currentView.getChildCount() - 1; i >= 0; i--) {
                    View child = currentView.getChildAt(i);
                    if (child instanceof ViewGroup) {
                        viewStack.push((ViewGroup) child);
                    }
                }
            }
        }

        return inputFieldsList;
    }

    /**
     * Takes a field and searches the preference file for a value corresponding to it
     * @param view the view to be queried
     * @return the value if found or an empty string if no value found
     */
    private String getStoredValueForView(View view) {
        SharedPreferences preferences = this.getSharedPreferences(this.getString(R.string.registration_filename), MODE_PRIVATE);
        String viewName = getResources().getResourceName(view.getId());
        viewName = viewName.replace(ID_PREFIX, "");
        return preferences.getString(viewName, "");
    }

    /**
     * Finds the index of the spinner array given the string value
     * @param value the value to look for
     * @return index of the spinner array
     */
    private int getSpinnerIndexFromString(String value) {
        String[] search = getResources().getStringArray(R.array.orthography_list);

        for (int i = 0; i < search.length; i++) {
            if (value.equals(search[i])) {
                return i;
            }
        }

        search = getResources().getStringArray(R.array.communication_list);
        for (int i = 0; i < search.length; i++) {
            if (value.equals(search[i])) {
                return i;
            }
        }

        search = getResources().getStringArray(R.array.location_type_list);
        for (int i = 0; i < search.length; i++) {
            if (value.equals(search[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Parse the text fields when the submit button has been clicked.
     *
     * @return true if all fields are filled in, false if any field is blank
     */
    private boolean parseTextFields() {
        for (int i = 0; i < inputFields.size(); i++) {
            View field = inputFields.get(i);
            if (field instanceof TextInputEditText) {
                String inputString = ((TextInputEditText) field).getText().toString();
                if (inputString.trim().isEmpty()) {
                    // Set focus to first empty field and make section visible if hidden
                    field.requestFocus();
                    for (int j = 0; j < sectionViews.length; j++) {
                        if (sectionViews[j].findFocus() != null) {
                            sectionViews[j].setVisibility(View.VISIBLE);
                            headerViews[j].setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.primary, null));
                            toggleKeyboard(SHOW_KEYBOARD, field);
                            return false;
                        }
                    }
                    return false;
                }
            }

        }
        return true;
    }

    private void postRegistrationInfo(){
        final Context myContext = this.getApplicationContext();

        final SharedPreferences prefs = this.getSharedPreferences(this.getString(R.string.registration_filename), MODE_PRIVATE);

        js = new HashMap<>();
         String PhoneId = Secure.getString(myContext.getContentResolver(),
                Secure.ANDROID_ID);

            js.put("Key", getString(R.string.api_token));
            js.put("PhoneId", PhoneId);
            js.put("TranslatorEmail", prefs.getString("translator_email", " "));
            js.put("TranslatorPhone", prefs.getString("translator_phone", " "));
            js.put("TranslatorLanguage", prefs.getString("translator_languages", " "));
            js.put("ProjectEthnoCode",  prefs.getString("ethnologue", " "));
            js.put("ProjectLanguage",  prefs.getString("language", " "));
            js.put("ProjectCountry",  prefs.getString("country", " "));
            js.put("ProjectMajorityLanguage",  prefs.getString("lwc", " "));
            js.put("ConsultantEmail",  prefs.getString("consultant_email", " "));
            js.put("ConsultantPhone",  prefs.getString("consultant_phone", " "));
            js.put("TrainerEmail",  prefs.getString("trainer_email", " "));
            js.put("TrainerPhone",  prefs.getString("trainer_phone", " "));

        Log.i("LOG_VOLLEY", js.toString());
        StringRequest req = new StringRequest(Request.Method.POST, getString(R.string.url_register_phone), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("LOG_VOLEY", response);
                resp  = response;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("LOG_VOLLEY", error.toString());
                Log.e("LOG_VOLLEY", "HIT ERROR");
                testErr = error.toString();

            }

        }) {
            @Override
            protected Map<String, String> getParams()
            {
                return js;
            }
        };


        RequestQueue test = VolleySingleton.getInstance(myContext).getRequestQueue();
        test.add(req);


    }

    /**
     * This function stores the registration information to the saved preference file. The
     * preference file is located in getString(R.string.Registration_File_Name).
     */
    private void storeRegistrationInfo() {
        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.registration_filename), MODE_PRIVATE).edit();
        Calendar calendar;
        String date, androidVersion, manufacturer, model;
        String day, month, year, hour, min;

        for (int i = 0; i < inputFields.size(); i++) {
            View field = inputFields.get(i);
            if (field instanceof TextInputEditText) {
                TextInputEditText textField = (TextInputEditText) field;
                String textFieldName = getResources().getResourceEntryName(textField.getId());
                textFieldName = textFieldName.replace("input_", "");
                String textFieldText = textField.getText().toString();
                editor.putString(textFieldName, textFieldText);

                if (textFieldName.equals("country")) {
                    country = textFieldText;
                } else if (textFieldName.equals("ethnologue")) {
                    languageCode = textFieldText;
                }
            } else if (field instanceof Spinner) {
                Spinner spinner = (Spinner) field;
                String spinnerName = getResources().getResourceEntryName(spinner.getId());
                spinnerName = spinnerName.replace("input_", "");
                String spinnerText = spinner.getSelectedItem().toString();
                editor.putString(spinnerName, spinnerText);
                //if(spinnerText.equals("Remote")){
                    //isRemoteConsultant = true;
                //}
            }

        }
        // Create timestamp for when the data was submitted
        calendar = Calendar.getInstance();
        day = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
        month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        year = Integer.toString(calendar.get(Calendar.YEAR));
        hour = Integer.toString(calendar.get(Calendar.HOUR_OF_DAY));
        min = Integer.toString(calendar.get(Calendar.MINUTE));
        if (min.length() < 2) {
            min = "0" + min;
        }
        date = month + "/" + day + "/" + year + " " + hour + ":" + min;
        editor.putString("date", date);

        // Retrieve phone information
        manufacturer = Build.MANUFACTURER;
        model = Build.MODEL;
        androidVersion = Build.VERSION.RELEASE;
        editor.putString("manufacturer", manufacturer);
        editor.putString("model", model);
        editor.putString("android_version", androidVersion);

        editor.apply();
    }

    /**
     * This function sets the click listeners to implement the accordion functionality
     * for each section of the registration page
     *
     * @param headerView  a variable of type View denoting the field the user will click to open up
     *                    a section of the registration
     * @param sectionView a variable of type View denoting the section that will open up
     */
    private void setAccordionListener(final View headerView, final View sectionView) {
        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sectionView.getVisibility() == View.GONE) {
                    sectionView.setVisibility(View.VISIBLE);
                    headerView.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.primary, null));
                    toggleKeyboard(SHOW_KEYBOARD, sectionView);         
                } else {
                    sectionView.setVisibility(View.GONE);
                    headerView.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.black_semi_transparent, null));
                    toggleKeyboard(CLOSE_KEYBOARD, sectionView);
                }
            }
        });
    }

    /**
     * Creates an alert dialog asking if the user wants to exit registration (without saving)
     * If they respond yes, finish activity or send them back to MainActivity
     */
    private void showExitAlertDialog() {
        AlertDialog dialog = new AlertDialog.Builder(RegistrationActivity.this)
                .setTitle(getString(R.string.registration_exit_title))
                .setMessage(getString(R.string.registration_exit_message))
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(isFirstActivity()) {
                            Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                        else {
                            finish();
                        }
                    }
                }).create();

        dialog.show();
    }

    /**
     * Creates an alert dialog asking if the user wants to skip registration
     * If they respond yes, finish activity or send them back to MainActivity
     */
    private void showSkipAlertDialog() {
        AlertDialog dialog = new AlertDialog.Builder(RegistrationActivity.this)
                .setTitle(getString(R.string.registration_skip_title))
                .setMessage(getString(R.string.registration_skip_message))
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        storeRegistrationInfo();
                        if(isFirstActivity()) {
                            Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                        else {
                            finish();
                        }
                    }
                }).create();

        dialog.show();
    }

    /**
     * Checks the bundle variables to see if this activity was launched at the app's start.
     * @return true if this is opening registration, false if not
     */
    private boolean isFirstActivity() {
        // Check to see if registration is first activity
        Bundle extras = getIntent().getExtras();
        return extras != null && extras.getBoolean(RegistrationActivity.FIRST_ACTIVITY_KEY);
    }

    /**
     * Shows error dialog if user did not provide an email to send the information to
     *
     * @param emailTextField the text field to check if it is blank
     */
    private void createErrorDialog(final EditText emailTextField) {
        AlertDialog dialog = new AlertDialog.Builder(RegistrationActivity.this)
                .setTitle(getString(R.string.registration_error_title))
                .setMessage(getString(R.string.registration_error_message))
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // The index here comes from the index of the archive section and header
                        // If another section is added or the sections are rearranged, this index
                        // will need to be changed
                        sectionViews[4].setVisibility(View.VISIBLE);
                        headerViews[4].setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.primary, null));
                        emailTextField.requestFocus();
                    }
                }).create();
        dialog.show();
    }

    /**
     * Creates a dialog to confirm the user wants to submit
     *
     * @param completeFields true if all fields filled in, false if any fields are empty
     */
    private void createSubmitConfirmationDialog(boolean completeFields) {
        String message;
        if (completeFields) {
            message = getString(R.string.registration_submit_complete_message);
        } else {
            message = getString(R.string.registration_submit_incomplete_message);
        }
        AlertDialog dialog = new AlertDialog.Builder(RegistrationActivity.this)
                .setTitle(getString(R.string.registration_submit_title))
                .setMessage(message)
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        storeRegistrationInfo();
                        postRegistrationInfo();
                        Toast saveToast = Toast.makeText(RegistrationActivity.this, R.string.registration_saved_successfully, Toast.LENGTH_LONG);
                        saveToast.show();
                        sendEmail();
                    }
                }).create();

        dialog.show();
    }

    /**
     * This function toggles the soft input keyboard. Allowing the user to have the keyboard
     * to open or close seamlessly alongside the rest UI.
     * @param showKeyBoard The boolean to be passed in to determine if the keyboard show be shown.
     * @param aView The view associated with the soft input keyboard.
     */
    private void toggleKeyboard(boolean showKeyBoard, View aView) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (showKeyBoard) {
            imm.showSoftInput(aView, 0);
        } else {
            imm.hideSoftInputFromWindow(aView.getWindowToken(), 0);
        }
    }

    private void sendEmail() {

        SharedPreferences prefs = this.getSharedPreferences(this.getString(R.string.registration_filename), MODE_PRIVATE);
        SharedPreferences.Editor preferenceEditor = getSharedPreferences(getString(R.string.registration_filename), MODE_PRIVATE).edit();
        Map<String, ?> preferences = prefs.getAll();

        String message = formatEmailFromPreferences(prefs);

        String[] TO =  { (String)preferences.get("database_email_1"), (String)preferences.get("database_email_2"),
                (String)preferences.get("database_email_3"), (String)preferences.get("translator_email"),
                (String)preferences.get("consultant_email"), (String)preferences.get("trainer_email") };

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");

        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "StoryProducer Registration Info");
        emailIntent.putExtra(Intent.EXTRA_TEXT, message);

        try {
            this.startActivity(Intent.createChooser(emailIntent, "Send mail"));
            this.finish();
            preferenceEditor.putBoolean(EMAIL_SENT, true);
            preferenceEditor.apply();
            Log.i("Finished sending email", "");
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,
                    "There is no email client installed.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Takes the preferences file and returns a string of formatted fields in readable format
     *
     * @param prefs the SharedPreferences object for registration
     * @return a well formatted string of registration information
     */
    private static String formatEmailFromPreferences(SharedPreferences prefs) {
        // Gives the order for retrieval and printing
        // Empty strings ("") are used to separate sections in the printing phase
        String[] keyListOrder = {"date", "", "language", "ethnologue", "country", "location",
                "town", "lwc", "orthography", "", "translator_name", "translator_education", "translator_languages",
                "translator_phone", "translator_email", "translator_communication_preference",
                "translator_location", "", "consultant_name", "consultant_languages",
                "consultant_phone", "consultant_email", "consultant_communication_preference",
                "consultant_location", "consultant_location_type", "", "trainer_name", "trainer_languages",
                "trainer_phone", "trainer_email", "trainer_communication_preference",
                "trainer_location", "", "manufacturer", "model", "android_version"};

        StringBuilder message = new StringBuilder();
        String formattedKey;

        for (String aKeyListOrder : keyListOrder) {
            // Section separation appends newline
            if (aKeyListOrder.isEmpty()) {
                message.append("\n");
                // Find key and value and print in clean format
            } else {
                formattedKey = aKeyListOrder.replace("_", " ");
                formattedKey = formattedKey.toUpperCase();
                message.append(formattedKey);
                message.append(": ");
                message.append(prefs.getString(aKeyListOrder, "NA"));
                message.append("\n");
            }
        }
        return message.toString();
    }

    //public static boolean haveRemoteConsultant(){ return isRemoteConsultant;}

    public static String getCountry() {
        return country;
    }

    public static String getLanguageCode() {
        return languageCode;
    }
}
