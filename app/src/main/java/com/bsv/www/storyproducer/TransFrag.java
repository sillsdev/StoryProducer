package com.bsv.www.storyproducer;

import android.app.ActionBar;
import android.app.Dialog;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import com.getbase.floatingactionbutton.FloatingActionButton;

import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class TransFrag extends Fragment {
    private MediaRecorder audioRecorder;
    private static final String SLIDE_NUM = "slidenum";
    private static final String NUM_OF_SLIDES = "numofslide";
    private static final String STORY_NAME = "storyname";
    private String outputFile=null;
    private File output = null;
    private String fileName = "recording";
    private int record_count = 2;
    private int failure;
    private boolean isSpeakButtonLongPressed;

    public static TransFrag newInstance(int position, int numOfSlides, String storyName){
        TransFrag frag = new TransFrag();
        Bundle args = new Bundle();
        args.putInt(SLIDE_NUM, position);
        args.putInt(NUM_OF_SLIDES, numOfSlides);
        args.putString(STORY_NAME, storyName);
        frag.setArguments(args);
        return frag;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
//        getActivity().getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_bg_trans));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_trans, container, false);
        //Load content
        loadSlideContent(view);
        //stuff for saving and playing the audio
        //TODO test to see where exacly getPath is in our files and if we even need the directory path

//        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/BSVP/";
        final File output = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +"/BSVP/" +
                getArguments().getString(STORY_NAME));

        if (output.exists()) {
        }
        else {
            output.mkdirs();
        }
        final FloatingActionButton floatingActionButton1 = (FloatingActionButton) view.findViewById(R.id.trans_record);
        final FloatingActionButton floatingActionButton2 = (FloatingActionButton) view.findViewById(R.id.trans_play);
        floatingActionButton2.setVisibility(View.INVISIBLE);

        floatingActionButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        floatingActionButton1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                v.setPressed(true);
                outputFile = fileName + getArguments().getInt(SLIDE_NUM) + ".mp3";
                audioRecorder = createAudioRecorder(output.getAbsolutePath() + "/" + outputFile);
                startAudioRecorder(audioRecorder);
                Toast.makeText(getContext(), "Recording Started", Toast.LENGTH_SHORT).show();
                isSpeakButtonLongPressed = true;
                return true;
            }
        });

        //TODO handle an event when you simply click -> it crashes when you do this
            //hopefully the click function above this does that.
        floatingActionButton1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (isSpeakButtonLongPressed) {
                        Toast.makeText(getContext(), "Recording Stopped", Toast.LENGTH_SHORT).show();
                        failure = 1;
                        stopAudioRecorder(audioRecorder);
                        //keep track of the number of records
                        if (record_count == 2 & failure == 1) {
                            record_count--;
                            floatingActionButton1.setColorNormalResId(R.color.yellow);
                            floatingActionButton2.setVisibility(View.VISIBLE);
                        } else if (record_count == 1 & failure == 1) {
                            record_count--;
                            floatingActionButton1.setColorNormalResId(R.color.green);
                        } else if (record_count == 0 & failure == 0) {
                            record_count++;
                            floatingActionButton1.setColorNormalResId(R.color.yellow);
                        }
                        v.setPressed(false);
                        isSpeakButtonLongPressed = false;
                    }
                }

                /*switch (event.getAction() & MotionEvent.ACTION_MASK) {

                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        outputFile = fileName + getArguments().getInt(SLIDE_NUM) + ".mp3";
                        audioRecorder = createAudioRecorder(output.getAbsolutePath() + "/" + outputFile);
                        startAudioRecorder(audioRecorder);
                        Toast.makeText(getContext(), "Recording Started", Toast.LENGTH_SHORT).show();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_OUTSIDE:
                    case MotionEvent.ACTION_CANCEL:
                        if (isSpeakButtonLongPressed == false) {
                            Toast.makeText(getContext(), "Recording Stopped", Toast.LENGTH_SHORT).show();
                            failure = 1;
                            stopAudioRecorder(audioRecorder);
                            //keep track of the number of records
                            if (record_count == 2 & failure == 1) {
                                record_count--;
                                floatingActionButton1.setColorNormalResId(R.color.yellow);
                                floatingActionButton2.setVisibility(View.VISIBLE);
                            } else if (record_count == 1 & failure == 1) {
                                record_count--;
                                floatingActionButton1.setColorNormalResId(R.color.green);
                            } else if (record_count == 0 & failure == 0) {
                                record_count++;
                                floatingActionButton1.setColorNormalResId(R.color.yellow);
                            }

                            v.setPressed(false);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_POINTER_DOWN:
                    case MotionEvent.ACTION_POINTER_UP:
                        break;

                }*/
                return true;
            }
        });

        floatingActionButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaPlayer m = new MediaPlayer();

                try {
                    m.setDataSource(output.getAbsolutePath() + "/" + outputFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    m.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                m.start();
                Toast.makeText(getContext(), "Playing Audio", Toast.LENGTH_LONG).show();
            }
        });


        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_play){
            FileSystem fileSystem = new FileSystem();
            Snackbar.make(getView(), "Playing Narration Audio...", Snackbar.LENGTH_SHORT).show();
            MediaPlayer m = new MediaPlayer();
            try {
                m.setDataSource(fileSystem.getPath("English") + "/" + getArguments().getString(STORY_NAME) + "/narration" + getArguments().getInt(SLIDE_NUM) + ".wav");
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                m.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            m.start();
        }
        return super.onOptionsItemSelected(item);
    }

    private void startAudioRecorder(MediaRecorder recorder){
        try {
            recorder.prepare();
            recorder.start();
        } catch (IllegalStateException | IOException e){
            e.printStackTrace();
        }
    }
    private void stopAudioRecorder(MediaRecorder recorder){
        try{
            recorder.stop();
        }catch(RuntimeException stopException){
            Toast.makeText(getContext(), "Please record again", Toast.LENGTH_SHORT).show();
            failure = 0;
        }
        recorder.reset();
        recorder.release();
    }
    private MediaRecorder createAudioRecorder(String fileName){
        MediaRecorder mediaRecorder = new MediaRecorder();

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(fileName);

        return  mediaRecorder;
    }

    private void loadSlideContent(View view){
        int currentSlide = getArguments().getInt(SLIDE_NUM);
        String storyName = getArguments().getString(STORY_NAME);

        FileSystem fileSystem = new FileSystem();
        fileSystem.loadSlideContent(storyName, currentSlide);

        ImageView slideImage = (ImageView)view.findViewById(R.id.trans_image_slide);
        slideImage.setImageBitmap(fileSystem.getImage(storyName, currentSlide));

        TextView slideTitle = (TextView)view.findViewById(R.id.trans_slide_title_primary);
        slideTitle.setText(fileSystem.getTitle());

        TextView slideSubTitle = (TextView)view.findViewById(R.id.trans_slide_title_secondary);
        slideSubTitle.setText(fileSystem.getSlideVerse());

        TextView slideVerse = (TextView)view.findViewById(R.id.trans_scripture_title);
        slideVerse.setText(fileSystem.getSlideVerse());

        TextView slideContent = (TextView)view.findViewById(R.id.trans_scripture_body);
        slideContent.setText(fileSystem.getSlideContent());

        TextView slideNum = (TextView)view.findViewById(R.id.trans_slide_indicator);
        slideNum.setText("#" + (getArguments().getInt(SLIDE_NUM) + 1));
        slideNum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchSlideSelectDialog();
            }
        });
    }

    private void launchSlideSelectDialog() {
        final Dialog dialog = new Dialog(getContext());
        int[] slides = new int[getArguments().getInt(NUM_OF_SLIDES)];
        for (int i = 0; i < getArguments().getInt(NUM_OF_SLIDES); i++) {
            slides[i] = (i + 1);
        }
        final DialogListAdapter dialogListAdapter = new DialogListAdapter(getActivity(), slides, getArguments().getInt(SLIDE_NUM));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_slide_indicator);
        dialog.setCanceledOnTouchOutside(true);
        final ListView listView = (ListView)dialog.findViewById(R.id.dialog_listview);
        listView.setAdapter(dialogListAdapter);
        Button okText = (Button)dialog.findViewById(R.id.dialog_ok);
        Button cancelText = (Button)dialog.findViewById(R.id.dialog_cancel);
        cancelText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        okText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                ((MainActivity)getActivity()).changeSlide(dialogListAdapter.getSelectedSlide());
            }
        });
        dialog.show();
    }
}