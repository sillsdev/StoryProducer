package org.sil.storyproducer.controller;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import org.sil.storyproducer.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Stack;

/**
 * The purpose of this class is to create the Registration activity.
 *
 * <p>Flow of RegistrationActivity:</p>
 * <ol>
 *     <li>onCreate() is called and calls the following:</li>
 *     <ul>
 *         <li>setAccordionListener() is called which adds click listeners to the header sections of the accordion.</li>
 *     </ul>
 *     <li>onPostCreate() is called and calls the following:</li>
 *     <ol>
 *         <li>setupInputFields() is called which takes a root ScrollView.</li>
 *         <ul>
 *             <li>getInputFields() is called and takes the root ScrollView and does an in-order
 *          traversal of the nodes in the registration xml to find the TextInputEditText
 *          and Spinner inputs. Each TextInputEditText and Spinner inputs are added to the
 *          sectionViews[] for parsing and saving.</li>
 *         </ul>
 *         <li>addSubmitButtonSave() is called which only parses the TextInpuEditText(not the Spinner input) to check for valid inputs.</li>
 *         <ul>
 *             <li>textFieldParsed() is called. This checks to see if all fields were entered</li>
 *             <li>A confirmation dialog is launched to ask if the user wants to submit the info</li>
 *         </ul>
 *         <li>addRegistrationSkip() is called to set the on click listener for skipping the registration phase temporarily</li>
 *     </ol>
 * </ol>
 *  <p>Key classes used in this class:</p>
 *  <ul>
 *      <li>{@link android.widget.Spinner} for input from a selection menu.</li>
 *      <li>{@link android.support.design.widget.TextInputEditText} for inputting text for registration fields.</li>
 *      <li>{@link android.content.SharedPreferences} for saving registration information.</li>
 *  </ul>
 *
 */
public class RegistrationActivity extends AppCompatActivity {

    public static final String SKIP_KEY = "skip";

    private final int [] sectionIds = {R.id.general_section, R.id.translator_section,R.id.consultant_section,R.id.trainer_section,R.id.database_section};
    private final int [] headerIds = {R.id.general_header, R.id.translator_header, R.id.consultant_header, R.id.trainer_header, R.id.database_header};
    private View[] sectionViews = new View[sectionIds.length];

    private List<View> inputFields;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        //Initialize sectionViews[] with the integer id's of the various LinearLayouts
        //Add the listeners to the LinearLayouts's header section.
        for(int i = 0; i < sectionIds.length; i++){
            sectionViews[i] = findViewById(sectionIds[i]);
            setAccordionListener(findViewById(headerIds[i]), sectionViews[i]);
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState){
        super.onPostCreate(savedInstanceState);
        setupInputFields();
        addSubmitButtonSave();
        addRegistrationSkip();
    }

    /**
     * Initializes the inputFields to the inputs of this activity.
     */
    private void setupInputFields(){
        View view = findViewById(R.id.scroll_view);

        //Find the top level linear layout
        if(view instanceof ScrollView) {
            ScrollView scrollView = (ScrollView) view;
            inputFields = getInputFields(scrollView);
        }
    }

    /**
     * Sets the on click listener for the submit button.
     */
    private void addSubmitButtonSave(){
        final Button submitButton = (Button)findViewById(R.id.submit_button);

        submitButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                boolean completeFields;
                submitButton.requestFocus();
                completeFields = parseTextFields();
                createSubmitConfirmationDialog(completeFields);
            }
        });
    }

    /**
     * Sets the on click listener for the registration bypass button
     */
    private void addRegistrationSkip() {
        final Button skipButton = (Button)findViewById(R.id.bypass_button);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExitAlertDialog();
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
     * TextInputEditText and spinner fields to add to the List.
     * @param rootScrollView The root scroll view where all the children will be visited to
     *                       check if there is an TextInputEditText field.
     * @return               The list of input fields that will be parsed either a spinner or a
     *                       TextInputEditText.
     */
    private List<View> getInputFields(ScrollView rootScrollView){

        List<View> inputFieldsList = new ArrayList<>();
        Stack<ViewGroup> viewStack = new Stack<>();

        //error check
        if(rootScrollView == null){
            return inputFieldsList;
        }

        viewStack.push(rootScrollView);

        while(viewStack.size() > 0){
            ViewGroup currentView = viewStack.pop();
            if(currentView instanceof TextInputLayout){
                inputFieldsList.add(((TextInputLayout) currentView).getEditText());
            }
            else if(currentView instanceof Spinner){
                inputFieldsList.add(currentView);
            }
            else{
                //push children onto stack from right to left
                //pushing on in reverse order so that the traversal is in-order traversal
                for(int i = currentView.getChildCount() - 1; i >= 0; i--){
                    View child = currentView.getChildAt(i);
                    if(child instanceof ViewGroup){
                        viewStack.push((ViewGroup)child);
                    }
                }
            }
        }

        return inputFieldsList;
    }

    /**
     * Parse the text fields when the submit button has been clicked.
     * @return true if all fields are filled in, false if any field is blank
     */
    private boolean parseTextFields(){
        for(int i = 0; i < inputFields.size(); i++){
            View field = inputFields.get(i);
            if(field instanceof TextInputEditText) {
                String inputString = ((TextInputEditText)field).getText().toString();
                if (inputString.trim().isEmpty()) {
                    // Set focus to first empty field and make section visible if hidden
                    field.requestFocus();
                    for(int j = 0; j < sectionViews.length; j++){
                        if(sectionViews[j].findFocus() != null){
                            sectionViews[j].setVisibility(View.VISIBLE);
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This function stores the registration information to the saved preference file. The
     * preference file is located in getString(R.string.Registration_File_Name).
     */
    private void storeRegistrationInfo(){
        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.registration_filename), MODE_PRIVATE).edit();
        Calendar calendar;
        String date, androidVersion, manufacturer, model;
        String day, month, year, hour, min;

        for(int i = 0; i < inputFields.size(); i++){
            View field = inputFields.get(i);
            if(field instanceof TextInputEditText){
                TextInputEditText textField = (TextInputEditText)field;
                String textFieldName = getResources().getResourceEntryName(textField.getId());
                textFieldName = textFieldName.replace("input_", "");
                String textFieldText = textField.getText().toString();
                editor.putString(textFieldName, textFieldText);
            } else if (field instanceof Spinner) {
                Spinner spinner = (Spinner)field;
                String spinnerName = getResources().getResourceEntryName(spinner.getId());
                spinnerName = spinnerName.replace("input_", "");
                String spinnerText = spinner.getSelectedItem().toString();
                editor.putString(spinnerName, spinnerText);
            }

        }
        // Create timestamp for when the data was submitted
        calendar = Calendar.getInstance();
        day = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
        month = Integer.toString(calendar.get(Calendar.MONTH)+1);
        year = Integer.toString(calendar.get(Calendar.YEAR));
        hour = Integer.toString(calendar.get(Calendar.HOUR_OF_DAY));
        min = Integer.toString(calendar.get(Calendar.MINUTE));
        date = month + "/" + day + "/" + year + " " + hour + ":" + min;
        editor.putString("date", date);

        // Retrieve phone information
        manufacturer = Build.MANUFACTURER;
        model = Build.MODEL;
        androidVersion = Build.VERSION.RELEASE;
        editor.putString("manufacturer", manufacturer);
        editor.putString("model", model);
        editor.putString("android_version", androidVersion);

        editor.commit();
    }

    /**
     * This function sets the click listeners to implement the accordion functionality
     * for each section of the registration page
     * @param headerView a variable of type View denoting the field the user will click to open up
     *                   a section of the registration
     * @param sectionView a variable of type View denoting the section that will open up
     */
    private void setAccordionListener(final View headerView, final View sectionView) {
        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sectionView.getVisibility() == View.GONE) {
                    sectionView.setVisibility(View.VISIBLE);
                } else {
                    sectionView.setVisibility(View.GONE);
                    // Hide keyboard on accordion closing
                    hideKeyboard();
                }
            }
        });
    }

    /**
     * Creates an alert dialog asking if the user wants to exit registration
     * If they respond yes, sends them back to MainActivity
     */
    private void showExitAlertDialog() {
        AlertDialog dialog = new AlertDialog.Builder(RegistrationActivity.this)
                .setTitle(getString(R.string.registration_skip_title))
                .setMessage(getString(R.string.registration_skip_message))
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                        intent.putExtra(SKIP_KEY, true);
                        startActivity(intent);
                    }
                }).create();

        dialog.show();
    }

    /**
     * Creates a dialog to confirm the user wants to submit
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
                        Toast saveToast = Toast.makeText(RegistrationActivity.this, R.string.registration_saved_successfully, Toast.LENGTH_LONG);
                        saveToast.show();
                        Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                }).create();

        dialog.show();
    }

    /**
     * Hides keyboard from the view
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }
}
