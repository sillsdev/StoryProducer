package org.sil.storyproducer.tools.file;

import java.io.File;

public class AudioFiles {
    private static final String SOUNDTRACK_PREFIX = "SoundTrack";
    private static final String LWC_AUDIO_PREFIX = "narration";
    private static final String TRANSLATION_AUDIO_PREFIX = "translation";

    public static File getLWC(String story, int i){
        return new File(FileSystem.getTemplatePath(story), LWC_AUDIO_PREFIX +i+".wav");
    }

    public static File getTranslation(String story, int i){
        return new File(FileSystem.getTemplatePath(story), TRANSLATION_AUDIO_PREFIX +i+".mp3");
    }

    public static File getSoundtrack(String story){
        return getSoundtrack(story, 0);
    }
    //TODO: Some stories have multiple soundtrack files. Is that desired and used?
    public static File getSoundtrack(String story, int i){
        return new File(FileSystem.getTemplatePath(story), SOUNDTRACK_PREFIX+i+".mp3");
    }

    //TODO: Re-implement or discard this method.
//    public static String getAudioPath(String story, int number) {
//        String path = FileSystem.getTemplatePath(story);
//        File f = new File(path);
//        File file[] = f.listFiles();
//        String audioName = "narration" + number;
//
//        for (int i = 0; i < file.length; i++) {
//            String[] audioExtensions = {".wav", ".mp3", ".wma"};
//            for (String extension : audioExtensions) {
//                if (file[i].getName().equals(audioName + extension)) {
//                    return file[i].getAbsolutePath();
//                }
//            }
//        }
//        return null;
//    }
}
