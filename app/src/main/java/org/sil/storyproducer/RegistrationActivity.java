package org.sil.storyproducer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class RegistrationActivity extends AppCompatActivity {

    View generalSection, translatorSection, consultantSection, trainerSection, databaseSection;
    private Resources classResources;

    //The listOfTextFieldsParsed and listOfTextFields are used together and is
    //used for parsing purposes.
    private boolean [] listOfTextFieldsParsed;
    private List<TextInputEditText> listOfTextFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);


        generalSection = findViewById(R.id.general_section);
        translatorSection = findViewById(R.id.translator_section);
        consultantSection = findViewById(R.id.consultant_section);
        trainerSection = findViewById(R.id.trainer_section);
        databaseSection = findViewById(R.id.database_section);

        setAccordionListener(findViewById(R.id.general_header), generalSection);
        setAccordionListener(findViewById(R.id.translator_header), translatorSection);
        setAccordionListener(findViewById(R.id.consultant_header), consultantSection);
        setAccordionListener(findViewById(R.id.trainer_header), trainerSection);
        setAccordionListener(findViewById(R.id.database_header), databaseSection);

        //Used later in a context that needs the resources
        classResources = this.getResources();


    }

    protected void onPostCreate(Bundle savedInstanceState){
        super.onPostCreate(savedInstanceState);
        this.addParserToTextFields();
        this.addSubmitButtonSave();
    }

    /**
     * This function adds parsing capabilities per TextInputEditText fields.
     */
    private void addParserToTextFields(){
        View view = findViewById(R.id.scroll_view);

        //Find the top level linear layout
        if(view instanceof ScrollView){
            boolean hasError = true;
            ScrollView scrollView = (ScrollView)view;
            listOfTextFields = getTextFields(scrollView);
            listOfTextFieldsParsed = new boolean[listOfTextFields.size()];

            for(int i = 0; i < listOfTextFields.size(); i++){
                final TextInputEditText inputtedTextField = listOfTextFields.get(i);
                listOfTextFieldsParsed[i] =  hasError;
                inputtedTextField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus){
                        if(!hasFocus){
                            int type = inputtedTextField.getInputType();
                            String inputString = inputtedTextField.getText().toString();

                            ParseText.parseText(type, inputString, classResources);
                            listOfTextFieldsParsed[listOfTextFields.indexOf(inputtedTextField)] = ParseText.hasError();
                        }
                    }
                });
            }
        }
    }

    /**
     * This function adds the on click listner for the submit button.
     */
    private void addSubmitButtonSave(){
        Button submitButton = (Button)findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                for(int i = 0; i < listOfTextFieldsParsed.length; i++){
                    if(listOfTextFieldsParsed[i]){
                        final TextInputEditText errorTextField = listOfTextFields.get(i);
                        createErrorDialog(errorTextField);
                        errorTextField.requestFocus();
                        return;
                    }
                }

                storeRegistrationInfo();

                retrieveRegistrationInfo();
            }
        });


    }

    /**
     * This function takes a scroll view as the root view of a xml layout and searches for
     * TextInputEditText fields to add to the List.
     * Don't mind the superfluous casts. The multiple casts are in place so that all nodes are
     * visited, regardless of what class the node is.
     * @param rootScrollView The root scroll view where all the children will be visited to
     *                       check if there is an TextInputEditText field.
     * @return               The list of TextInputEditText fields that will be parsed.
     */
    private List<TextInputEditText> getTextFields(ScrollView rootScrollView){
        //error check
        if(rootScrollView == null){
            return null;
        }

        List<TextInputEditText> listOfEditText = new ArrayList<>();
        Stack<ViewGroup> myStack = new Stack<>();
        myStack.push(rootScrollView);

        while(myStack.size() > 0){
            ViewGroup currentView = myStack.pop();
            if(currentView instanceof TextInputLayout){
                listOfEditText.add((TextInputEditText)((TextInputLayout) currentView).getEditText());
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
     * Custom dialog box creation for a parsing error.
     * @param myText The TextInputEditText field that might need to regain focus depending
     *               on user input.
     * @return The dialog box that the user must encounter.
     */
    private Dialog createErrorDialog(final TextInputEditText myText){
        AlertDialog dialog = new AlertDialog.Builder(RegistrationActivity.this)
        .setTitle(" ")
        .setMessage(" ")
        .setIcon(android.R.drawable.ic_dialog_info)
        .setPositiveButton(" ", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                myText.requestFocus();
            }
        })
        .setNegativeButton(" ", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                myText.clearComposingText();
            }
        }).create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positiveButton = ((AlertDialog) dialog)
                        .getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setBackgroundResource(android.R.drawable.ic_menu_revert);

                Button negativeButton = ((AlertDialog) dialog)
                        .getButton(AlertDialog.BUTTON_NEGATIVE);
                negativeButton.setBackgroundResource(android.R.drawable.ic_delete);
//                //((AlertDialog) dialog).setIcon(android.R.drawable.ic_dialog_info);
//                ((AlertDialog)dialog).requestWindowFeature(Window.FEATURE_LEFT_ICON);
//                ((AlertDialog)dialog).setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_info);
                //((AlertDialog)dialog).setContentView(R.layout.custom_dialog);
            }
        });
        dialog.show();
        return dialog;
    }

    /***
     * This function retrieves the registration information from the saved preference file. The
     * preference file is located in getString(R.string.Registration_File_Name).
     */
    private void retrieveRegistrationInfo(){

        SharedPreferences prefs = getSharedPreferences(getString(R.string.Registration_File_Name), MODE_PRIVATE);
        HashMap<String, String> myMap = (HashMap<String, String>)prefs.getAll();

        System.out.println("Ouputting values!");
        System.out.println("Ouputting values!");
        System.out.println("Ouputting values!");
        System.out.println("Ouputting values!");

        for (Map.Entry<String, String> entry : myMap.entrySet())
        {
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }
    }

    /***
     * This function stores the registration information to the saved preference file. The
     * preference file is located in getString(R.string.Registration_File_Name).
     */
    private void storeRegistrationInfo(){
        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.Registration_File_Name), MODE_PRIVATE).edit();
        for(int i = 0; i < listOfTextFields.size(); i++){
            final TextInputEditText textField = listOfTextFields.get(i);
            String textFieldName = getResources().getResourceEntryName(textField.getId());
            System.out.println(textFieldName);
            editor.putString(textFieldName, textField.getText().toString());
        }
        editor.commit();
    }




    /**
     * This function sets the click listeners to implement the accordion functionality
     * for each section of the registration page
     * @param headerView a variable of type View denoting the field the user will click to open up
     *                   a section of the registration
     * @param sectionView a variable of type View denoting the section that will open up
     */
    private void setAccordionListener(View headerView, final View sectionView) {
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

}
