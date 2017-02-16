package org.sil.storyproducer.model;

/**
 * This class encapsulates the contents of the text files for each slide
 */

public class SlideText {
    String title;
    String subtitle;
    String reference;
    String content;

    public SlideText() {
        title = "Story Title";
        subtitle = "Story Subtitle";
        reference = "Reference 1:1";
        content = "Story Verse";
    }

    public SlideText(String title, String subtitle, String reference, String content) {
        this.title = title;
        this.subtitle = subtitle;
        this.reference = reference;
        this.content = content;
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

    public String getContent() {
        return content;
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

    public void setContent(String content) {
        this.content = content;
    }
}
