package org.sil.storyproducer.controller;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.app.AlertDialog;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.adapter.CustomAdapter;
import org.sil.storyproducer.model.ListFiles;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.SlideText;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.FileSystem;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.file.TextFiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class StoryListFrag extends Fragment {

    private ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        View view;

        // Define array storyNames to show in ListView
        final String[] storyNames = FileSystem.getStoryNames();


        if (storyNames.length == 0) {
            view = inflater.inflate(R.layout.fragment_no_stories, container, false);
            return view;
        }

        view = inflater.inflate(R.layout.activity_list_view, container, false);

        // Get ListView object from xml
        listView = getActivity().findViewById(R.id.story_list_view);


        final ListFiles[] listFiles = new ListFiles[storyNames.length];

        for(int i = 0; i < listFiles.length; i++) {
            SlideText slideText = TextFiles.getSlideText(storyNames[i], 1);
            listFiles[i] = new ListFiles(ImageFiles.getBitmap(storyNames[i], 1, 25), storyNames[i], slideText.getSubtitle());
        }

        CustomAdapter adapter = new CustomAdapter(getContext(), R.layout.story_list_item, listFiles);

        listView = view.findViewById(R.id.story_list_view);
        // Assign adapter to ListView
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ((MainActivity)getActivity()).switchToStory(storyNames[position]);
            }
        });

        return view;
    }

}
