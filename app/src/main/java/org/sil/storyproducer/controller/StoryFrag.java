package org.sil.storyproducer.controller;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.adapter.CustomAdapter;
import org.sil.storyproducer.model.ListFiles;
import org.sil.storyproducer.model.SlideText;
import org.sil.storyproducer.tools.file.FileSystem;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.file.TextFiles;

public class StoryFrag extends Fragment {

    private ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.activity_list_view, container, false);

        // Get ListView object from xml
        listView = (ListView)getActivity().findViewById(R.id.story_list_view);

        // Defined Array storyNames to show in ListView
        final String[] storyNames = FileSystem.getStoryNames();
        final ListFiles[] listFiles = new ListFiles[storyNames.length];

        for(int i = 0; i < listFiles.length; i++) {
            SlideText slideText = TextFiles.getSlideText(storyNames[i], 1);
            listFiles[i] = new ListFiles(ImageFiles.getBitmap(storyNames[i], 1, 25), slideText.getTitle(), slideText.getSubtitle());
        }

        CustomAdapter adapter = new CustomAdapter(getContext(), R.layout.story_list_item, listFiles);

        listView = (ListView)view.findViewById(R.id.story_list_view);
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
