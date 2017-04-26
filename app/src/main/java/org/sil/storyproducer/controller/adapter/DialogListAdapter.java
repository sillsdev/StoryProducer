package org.sil.storyproducer.controller.adapter;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import org.sil.storyproducer.R;

public class DialogListAdapter extends BaseAdapter  {
    private int[] slides;
    private int currentSlide;
    private int checkSlide;
    private Boolean firstRun = true;
    private static LayoutInflater inflater;
    private Context context;
    public DialogListAdapter(FragmentActivity mainActivity, int[] slides, int currentSlide) {
        this.slides = slides;
        this.currentSlide = currentSlide;
        this.context = mainActivity;
        this.inflater = (LayoutInflater)mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return slides.length;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    public int getSelectedSlide(){
        return checkSlide;
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    RadioButton currentlyCheckedRadio;

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = inflater.inflate(R.layout.dialog_slide_list_item, null);
        TextView textView = (TextView)view.findViewById(R.id.dialog_slide_text);
        final RadioButton radioButton = (RadioButton)view.findViewById(R.id.dialog_radio);
        if(position == currentSlide && firstRun){
            currentlyCheckedRadio = radioButton;
            currentlyCheckedRadio.setChecked(true);
            checkSlide = position;
            firstRun = false;
        }
        if(checkSlide == position & !firstRun){
            currentlyCheckedRadio = radioButton;
            currentlyCheckedRadio.setChecked(true);
        }
        textView.setText("Slide " + slides[position]);
        radioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioButton radioButton = (RadioButton)v.findViewById(R.id.dialog_radio);
                if (currentlyCheckedRadio == radioButton) {
                    return;
                }
                if (currentlyCheckedRadio == null) {
                    currentlyCheckedRadio = radioButton;
                    currentlyCheckedRadio.setChecked(true);
                }
                currentlyCheckedRadio.setChecked(false);
                radioButton.setChecked(true);
                checkSlide = position;
                currentlyCheckedRadio = radioButton;
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioButton radioButton = (RadioButton)v.findViewById(R.id.dialog_radio);
                if (currentlyCheckedRadio == radioButton) {
                    return;
                }
                if (currentlyCheckedRadio == null) {
                    currentlyCheckedRadio = radioButton;
                    currentlyCheckedRadio.setChecked(true);
                }
                currentlyCheckedRadio.setChecked(false);
                radioButton.setChecked(true);
                checkSlide = position;
                currentlyCheckedRadio = radioButton;

            }
        });
        return view;
    }

}

