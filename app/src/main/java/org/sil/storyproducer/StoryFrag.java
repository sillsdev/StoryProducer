package org.sil.storyproducer;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import org.sil.storyproducer.media.MyEncodeAndMuxTest;

/**
 * Created by hannahbrown on 9/25/15.
 */
public class StoryFrag extends Fragment{

    ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.activity_list_view, container, false);

        // Get ListView object from xml
        listView = (ListView)getActivity().findViewById(R.id.story_list_view);

        // Defined Array values to show in ListView
        final String[] values = FileSystem.getStoryNames();
        final ListFiles[] listFiles = new ListFiles[values.length];

        for(int i = 0; i < listFiles.length; i++) {
            FileSystem.loadSlideContent(values[i], 1);
            listFiles[i] = new ListFiles(FileSystem.getImage(values[i], 1), FileSystem.getTitle(), FileSystem.getSubTitle());
        }

        CustomAdapter adapter = new CustomAdapter(getContext(), R.layout.story_list_item, listFiles);

        listView = (ListView)view.findViewById(R.id.story_list_view);
//        ImageView playVideo = (ImageView)listView.findViewById(R.id.story_list_image);
//        playVideo.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });
        // Assign adapter to ListView
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Thread encodeThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MyEncodeAndMuxTest test = new MyEncodeAndMuxTest();
                        test.runTest();
                    }
                });
                encodeThread.start();
//                int slideNum = FileSystem.getImageAmount(values[position]);
//                ((MainActivity)getActivity()).startFragment(1, slideNum, values[position]);
            }
        });

        return view;
    }

}
