package org.sil.storyproducer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * The purpose of this class is to create the Registration activity.
 *
 * Key classes used in this class:
 * @see ParseText used for input parsing.
 * @see android.widget.Spinner for input from a selection menu.
 * @see android.support.design.widget.TextInputEditText for inputting text for registration fields.
 * @see android.content.SharedPreferences for Saving registration information.
 *
 * Flow of RegistrationActivty:
 * 1. onCreate() is called and calls the following:
 *  a.  setAccordionListener() is called which adds click listeners to the header sections of the
 *      accordion.
 * 2. onPostCreate() is called and calls the following:
 *  a.  setupInputFields() is called which takes a root ScrollView.
 *          I. getInputFields() is called and takes the root ScrollView and does an in-order
 *          traversal of the nodes in the registration xml to find the TextInputEditText
 *          and Spinner inputs. Each TextInputEditText and Spinner inputs are added to the
 *          mySelectionViews[] for parsing and saving.
 *  b.  addSubmitButtonSave() is called which only parses the TextInpuEditText(not the Spinner input)
 *      to check for valid inputs.
 *          I. textFieldParsed() is called. Parsing is done with the ParseText class. If a TextInputEditText is not valid input
 *          then the saving is halted and the user is prompted to redo the input.
 *          II. If all text fields are valid input then storeRegistrationInfo() is called.
 *  c. createAlertDialog() is called and greets users with a message from the string.xml.
 *
 */
public class RegistrationActivity extends AppCompatActivity {

    private final int [] viewIntId = {R.id.general_section, R.id.translator_section,R.id.consultant_section,R.id.trainer_section,R.id.database_section};
    private final int [] headIntId = {R.id.general_header, R.id.translator_header, R.id.consultant_header, R.id.trainer_header, R.id.database_header};
    private View[] mySelectionViews = new View[viewIntId.length];
    private View[] myHeaderViews = new View[headIntId.length];
    private Resources classResources;
    private List<View> listOfInputFields;

    private final int SHOW = 0;
    private final int HIDE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        //Initialize mySelectionViews[] with the integer id's of the various LinearLayouts
        //Add the listeners to the LinearLayouts's header section.
        for(int i = 0; i < viewIntId.length; i++){
            mySelectionViews[i] = findViewById(viewIntId[i]);
            setAccordionListener(findViewById(headIntId[i]), mySelectionViews[i]);
            myHeaderViews[i] = findViewById(headIntId[i]);
        }

        //Used later in a context that needs the resources of this activity.
        classResources = this.getResources();
    }

    protected void onPostCreate(Bundle savedInstanceState){
        super.onPostCreate(savedInstanceState);
        this.setupInputFields();
        this.addSubmitButtonSave();
        this.addRegistrationSkip();
        createAlertDialog(getString(R.string.bypass_message));
    }

    /***
     * Initializes the listOfInputFields to the inputs of this activity.
     */
    private void setupInputFields(){
        View view = findViewById(R.id.scroll_view);

        //Find the top level linear layout
        if(view instanceof ScrollView) {
            ScrollView scrollView = (ScrollView) view;
            listOfInputFields = getInputFields(scrollView);
        }
    }

    /**
     * This function adds the on click listener for the submit button.
     */
    private void addSubmitButtonSave(){
        final Button submitButton = (Button)findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                submitButton.requestFocus();
                if(textFieldsParsed()){
                    storeRegistrationInfo();
                    createToast(getApplicationContext(), getString(R.string.saved_successfully));
                    retrieveRegistrationInfo();
                    hideKeyboard();
                    Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                    startActivity(intent);
                }
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
     * @return Returns true if all the text fields are inputted correctly, else,
     * returns false if text fields are not inputted correctly. Only need to check the
     * TextInputEditText fields and not the spinner fields.
     */
    private boolean textFieldsParsed(){
        for(int i = 0; i < listOfInputFields.size(); i++){
            View aView = listOfInputFields.get(i);
            if(aView instanceof TextInputEditText){
                TextInputEditText textField = (TextInputEditText)aView;
                int type = textField.getInputType();
                String inputString = textField.getText().toString();
                String errorMessage = "";
                errorMessage = ParseText.parseText(type, inputString, classResources);

                if(ParseText.hasError()){
                    createAlertDialog(textField, errorMessage);
                    textField.requestFocus();
                    for(int j = 0; j < this.mySelectionViews.length; j++){
                        if(mySelectionViews[j].findFocus() != null){
                            mySelectionViews[j].setVisibility(View.VISIBLE);
                            replaceHeaderText(myHeaderViews[j], SHOW);
                        }
                    }
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Creates and shows a custom dialog box for parsing the fields.
     * @param myText The TextInputEditText field that might need to regain focus depending
     *               on user input.
     * @return The dialog box that the user must encounter. The dialog box does not need to be
     * assigned to anything when the createAlertDialog is called. The return value is an optional
     * value to initialize an object with.
     */
    private Dialog createAlertDialog(final TextInputEditText myText, String message){
        AlertDialog dialog = new AlertDialog.Builder(RegistrationActivity.this)
        .setTitle(" ")
        .setMessage(message)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setPositiveButton(" ", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                myText.requestFocus();
            }
        }).create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positiveButton = ((AlertDialog) dialog)
                        .getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setBackgroundResource(android.R.drawable.ic_menu_revert);
            }
        });
        dialog.show();
        return dialog;
    }

    /**
     * Creates and shows the dialog box for a particular message.
     * @param message The message that the alert box will show for the RegistrationActivity context.
     * @return The dialog box that the user must encounter. The dialog box does not need to be
     * assigned to anything when the createAlertDialog is called. The return value is an optional
     * value to initialize an object with.
     */
    private Dialog createAlertDialog(String message){
        final AlertDialog instructionDialog = new AlertDialog.Builder(RegistrationActivity.this)
                .setTitle("Welcome!")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //
                    }
                }).create();
        instructionDialog.show();

        return instructionDialog;
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
     * This function retrieves the registration information from the saved preference file. The
     * preference file is located in getString(R.string.Registration_File_Name).
     */
    private void retrieveRegistrationInfo(){

        SharedPreferences prefs = getSharedPreferences(getString(R.string.Registration_File_Name), MODE_PRIVATE);
        HashMap<String, String> myMap = (HashMap<String, String>)prefs.getAll();

        System.out.println("                ");
        System.out.println("                ");
        System.out.println("                ");
        System.out.println("                ");

        for (Map.Entry<String, String> entry : myMap.entrySet())
        {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

    /***
     * This function stores the registration information to the saved preference file. The
     * preference file is located in getString(R.string.Registration_File_Name).
     */
    private void storeRegistrationInfo(){
        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.Registration_File_Name), MODE_PRIVATE).edit();
        final String BYPASS_STRING = getString(R.string.bypass_field_parse);
        Calendar calendar;
        String date, androidVersion, manufacturer, model;
        String day, month, year, hour, min;
        for(int i = 0; i < listOfInputFields.size(); i++){
            View aView = listOfInputFields.get(i);
            if(aView instanceof TextInputEditText){
                final TextInputEditText textField = (TextInputEditText)aView;
                String textFieldName = getResources().getResourceEntryName(textField.getId());
                String textFieldText = textField.getText().toString().equals(BYPASS_STRING) ? "N/A"
                        : textField.getText().toString();
                System.out.println(textFieldName);
                editor.putString(textFieldName, textFieldText);
            }else if(aView instanceof Spinner){
                final Spinner spinner = (Spinner)aView;
                String spinnerName = getResources().getResourceEntryName(spinner.getId());
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
     * This function hides the keyboard if current view has enabled the keyboard.
     */
    private void hideKeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
                    replaceHeaderText(headerView, SHOW);
                } else {
                    sectionView.setVisibility(View.GONE);
                    replaceHeaderText(headerView, HIDE);
                }
            }
        });
    }

    /**
     * This function changes the header text when the section is pulled up or down
     * Standard format is +  <headerText> for hidden sections
     * Standard format is ^  <headerText> for shown sections
     * @param headerView indicates the view to be changed
     * @param action, indicates the action taken on the section, either showing or hiding
     */
    private void replaceHeaderText(View headerView, int action) {
        String oldText;
        String newText = "";
        TextView headerTextView = (TextView) headerView;
        oldText = headerTextView.getText().toString();
        if (action == SHOW) {
            newText = oldText.replace("+", "^");
        } else if (action == HIDE) {
            newText = oldText.replace("^", "+");
        } else {
            return;
        }
        headerTextView.setText(newText);
    }

    /**
     * Creates an alert dialog asking if the user wants to exit registration
     * If they respond yes, sends them back to MainActivity
     */
    private void showExitAlertDialog() {
        AlertDialog dialog = new AlertDialog.Builder(RegistrationActivity.this)
                .setTitle("Skip Registration?")
                .setMessage("Are you sure you want to skip registration? Your information " +
                        "will not be saved.")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                        intent.putExtra("skip", true);
                        startActivity(intent);
                    }
                }).create();

        dialog.show();
    }
}
