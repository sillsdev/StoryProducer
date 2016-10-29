package org.sil.storyproducer;

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
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Stack;

/**
 * The purpose of this class is to create the Registration activity.
 *
 * Key classes used in this class:
 * @see android.widget.Spinner for input from a selection menu.
 * @see android.support.design.widget.TextInputEditText for inputting text for registration fields.
 * @see android.content.SharedPreferences for Saving registration information.
 *
 * Flow of RegistrationActivity:
 * 1. onCreate() is called and calls the following:
 *  a.  setAccordionListener() is called which adds click listeners to the header sections of the
 *      accordion.
 * 2. onPostCreate() is called and calls the following:
 *  a.  setupInputFields() is called which takes a root ScrollView.
 *          I. getInputFields() is called and takes the root ScrollView and does an in-order
 *          traversal of the nodes in the registration xml to find the TextInputEditText
 *          and Spinner inputs. Each TextInputEditText and Spinner inputs are added to the
 *          sectionViews[] for parsing and saving.
 *  b.  addSubmitButtonSave() is called which only parses the TextInpuEditText(not the Spinner input)
 *      to check for valid inputs.
 *          I. textFieldParsed() is called. This checks to see if all fields were entered
 *          II. A confirmation dialog is launched to ask if the user wants to submit the info
 *  c. addRegistrationSkip() is called to set the on click listener for skipping the registration phase temporarily
 *
 */
public class RegistrationActivity extends AppCompatActivity {

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

    protected void onPostCreate(Bundle savedInstanceState){
        super.onPostCreate(savedInstanceState);
        this.setupInputFields();
        this.addSubmitButtonSave();
        this.addRegistrationSkip();
    }

    /***
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
     * This function adds the on click listener for the submit button.
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
        //error check
        if(rootScrollView == null){
            return null;
        }

        List<View> listOfEditText = new ArrayList<>();
        Stack<ViewGroup> myStack = new Stack<>();
        myStack.push(rootScrollView);

        while(myStack.size() > 0){
            ViewGroup currentView = myStack.pop();
            if(currentView instanceof TextInputLayout){
                listOfEditText.add(((TextInputLayout) currentView).getEditText());
            }
            else if(currentView instanceof Spinner){
                listOfEditText.add(currentView);
            }
            else{
                if(currentView.getChildCount() > 0){
                    //push children onto stack from right to left
                    //pushing on in reverse order so that the traversal is in-order traversal
                    for(int i = currentView.getChildCount() - 1; i >= 0; i--){
                        View child = currentView.getChildAt(i);
                        if(child instanceof ViewGroup){
                            myStack.push((ViewGroup)child);
                        }
                    }
                }
            }
        }

        return listOfEditText;
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
                    for(int j = 0; j < this.sectionViews.length; j++){
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

    /***
     * Create a toast with the default location with a message.
     * @param context The current app context.
     * @param message The message that the toast will display.
     */
    private void createToast(Context context, String message){
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    /***
     * This function stores the registration information to the saved preference file. The
     * preference file is located in getString(R.string.Registration_File_Name).
     */
    private void storeRegistrationInfo(){
        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.Registration_Filename), MODE_PRIVATE).edit();
        Calendar calendar;
        String date, androidVersion, manufacturer, model;
        String day, month, year, hour, min;

        for(int i = 0; i < inputFields.size(); i++){
            View field = inputFields.get(i);
            if(field instanceof TextInputEditText){
                final TextInputEditText textField = (TextInputEditText)field;
                String textFieldName = getResources().getResourceEntryName(textField.getId());
                textFieldName = textFieldName.replace("input_", "");
                String textFieldText = textField.getText().toString();
                editor.putString(textFieldName, textFieldText);
            } else if (field instanceof Spinner) {
                final Spinner spinner = (Spinner)field;
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
                .setTitle(getString(R.string.skip_title))
                .setMessage(getString(R.string.skip_message))
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                        intent.putExtra("skip", true);
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
            message = getString(R.string.submit_complete_message);
        } else {
            message = getString(R.string.submit_incomplete_message);
        }
        AlertDialog dialog = new AlertDialog.Builder(RegistrationActivity.this)
                .setTitle(getString(R.string.submit_title))
                .setMessage(message)
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        storeRegistrationInfo();
                        createToast(getApplicationContext(), getString(R.string.saved_successfully));
                        Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                }).create();

        dialog.show();
    }
}
