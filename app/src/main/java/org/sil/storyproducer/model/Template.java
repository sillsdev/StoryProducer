package org.sil.storyproducer.model;

import android.graphics.Rect;
import android.support.v4.util.LruCache;
import android.util.Log;

import org.sil.storyproducer.tools.file.FileSystem;
import org.sil.storyproducer.tools.file.ProjectXML;
import org.sil.storyproducer.tools.media.graphics.KenBurnsEffect;
import org.sil.storyproducer.tools.media.graphics.RectHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the metadata of a story template.
 */
public class Template {
    private static final String TAG = "Template";
    private static final int CACHE_SIZE = 2;

    /**
     * Get the slide from the story template at the specified index.
     * @param story
     * @param index
     * @return requested slide or null if not found.
     */
    public static TemplateSlide getSlide(String story, int index) {
        List<TemplateSlide> slides = TEMPLATE_CACHE.get(story);
        if(slides != null && slides.size() > index) {
            return slides.get(index);
        }
        return null;
    }

    private static final LruCache<String, List<TemplateSlide>> TEMPLATE_CACHE
            = new LruCache<String, List<TemplateSlide>>(CACHE_SIZE) {
        @Override
        protected List<TemplateSlide> create(String key) {
            return createSlidesFromProjectXML(key);
        }
    };

    /**
     * Convert a story's ProjectXML into a list of TemplateSlide.
     * @param story
     * @return list of story's slides or null if unable to read/parse project.xml.
     */
    private static List<TemplateSlide> createSlidesFromProjectXML(String story) {
        String templatePath = FileSystem.getTemplatePath(story);

        ProjectXML xml;
        try {
            xml = new ProjectXML(story);
        } catch (Exception e) {
            Log.e(TAG, "Error reading or parsing project.xml file!", e);
            return null;
        }
        List<TemplateSlide> slides = new ArrayList<>();

        for(int i = 0; i < xml.units.size(); i++) {
            ProjectXML.VisualUnit unit = xml.units.get(i);

            String narrationPath = unit.narrationFilename;
            File narration = narrationPath == null ? null : new File(templatePath, narrationPath);

            String imagePath = unit.imageInfo.filename;

            int width = unit.imageInfo.width;
            int height = unit.imageInfo.height;
            Rect imageDimensions = new Rect(0, 0, width, height);

            Rect start = unit.imageInfo.motion.start;
            //Ensure the rectangle fits within the image.
            RectHelper.clip(start, imageDimensions);

            Rect end = unit.imageInfo.motion.end;
            //Ensure the rectangle fits within the image.
            RectHelper.clip(end, imageDimensions);

            //TODO: Should we use crop here? (Are start and end relative to crop or absolute?)
            Rect crop = null;
            if(unit.imageInfo.edit != null) {
                crop = unit.imageInfo.edit.crop;
            }
            KenBurnsEffect kbfx = new KenBurnsEffect(start, end, crop);

            File soundtrack = null;
            int soundtrackVolume = 0;
            if(i > 0) {
                TemplateSlide previous = slides.get(i - 1);
                soundtrack = previous.getSoundtrack();
                soundtrackVolume = previous.getSoundtrackVolume();
            }
            if(unit.imageInfo.musicTrack != null) {
                String soundtrackPath = unit.imageInfo.musicTrack.filename;
                soundtrack = new File(templatePath, soundtrackPath);
                soundtrackVolume = unit.imageInfo.musicTrack.volume;
            }

            TemplateSlide currentSlide = new TemplateSlide(narration,
                    new File(imagePath), imageDimensions, kbfx, soundtrack, soundtrackVolume);
            slides.add(currentSlide);
        }

        return slides;
    }
}
