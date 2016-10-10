package org.sil.storyproducer;

import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class RegistrationActivity extends AppCompatActivity {

    View generalSection, translatorSection, consultantSection, trainerSection, databaseSection;

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

    }

    protected void onPostCreate(Bundle savedInstanceState){
        super.onPostCreate(savedInstanceState);
        this.traverseTextFields();
    }

    /*
    Adds the text field watchers and adds parsing
     */
    private void traverseTextFields(){
        View view = findViewById(R.id.scroll_view);
        LinearLayout linearLayout = null;

        //Find the top level linear layout
        if(view instanceof ScrollView){
            ScrollView scrollView = (ScrollView)view;
            View tempView = scrollView.getChildAt(0);
            if(tempView instanceof LinearLayout){
                linearLayout = (LinearLayout)tempView;
            }
        }

        //If the top level linear layout was found then LinearLayout should not equal null
        if(linearLayout != null){
            for(int i = 0; i < linearLayout.getChildCount(); i++){
                View tempLinearLayout = linearLayout.getChildAt(i);
                if(tempLinearLayout instanceof TextInputLayout){
                    TextInputLayout textInputLayout =  (TextInputLayout)tempLinearLayout;
                    final TextInputEditText inputtedTextField = (TextInputEditText)textInputLayout.getEditText();
                    inputtedTextField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus){
                            if(!hasFocus){
                                //This convoluted statement below gets the integer id ( inputtedTextField.getId() )
                                //and converts it to the actual string representation sourced from the activity_registration.xml...
                                String id = inputtedTextField.getResources().getResourceEntryName(inputtedTextField.getId());
                                String inputString = inputtedTextField.getText().toString();

                                System.out.printf("%s says: %s %n", id, inputString);
                                parseTextField(id, inputString);
                            }
                        }
                    });
                }
            }
        }
    }


    //Used to validate the text field... may use an Enum instead of multiple strings... I don't know yet
    //This may have to be a help class
    //String TextFieldType is the "type" of the second parameter field
    //String inputtedTextField is the textField to parse
    private void parseTextField(String TextFieldType, String inputtedTextField){
        switch(TextFieldType){
            case "Regular text field":
                //do something with inputtedTextField
                break;
            case "Phone number":
                // do something with inputtedTextField
                break;
            default:
                // do something special
                break;
        }
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
