package org.sil.storyproducer.model;

/**
 * This class encapsulates the contents of the text files for each slide
 */

public class SlideText {
    String title;
    String subtitle;
    String reference;
    String verse;

    public SlideText() {
        title = "Story Title";
        subtitle = "Story Subtitle";
        reference = "Reference 1:1";
        verse = "Story Verse";
    }

    public SlideText(String title, String subtitle, String reference, String verse) {
        this.title = title;
        this.subtitle = subtitle;
        this.reference = reference;
        this.verse = verse;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getReference() {
        return reference;
    }

    public String getVerse() {
        return verse;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public void setVerse(String verse) {
        this.verse = verse;
    }
}
