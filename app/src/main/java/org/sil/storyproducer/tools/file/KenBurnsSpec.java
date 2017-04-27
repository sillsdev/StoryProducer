package org.sil.storyproducer.tools.file;

import android.graphics.Rect;

import org.sil.storyproducer.model.Template;
import org.sil.storyproducer.model.TemplateSlide;
import org.sil.storyproducer.tools.media.graphics.BitmapHelper;
import org.sil.storyproducer.tools.media.graphics.KenBurnsEffect;
import org.sil.storyproducer.tools.media.graphics.KenBurnsEffectHelper;

import java.io.File;

public class KenBurnsSpec {
    public static KenBurnsEffect getKenBurnsEffect(String story, int index) {
        TemplateSlide slide = Template.getSlide(story, index);
        if(slide != null) {
            return slide.getKenBurnsEffect();
        }
        else {
            File image = ImageFiles.getFile(story, index);
            String imagePath = image.getPath();
            Rect dimensions = BitmapHelper.getDimensions(imagePath);
            double widthToHeight = dimensions.width() / (double) dimensions.height();

            return KenBurnsEffectHelper.getRandom(imagePath, widthToHeight);
        }
    }
}
