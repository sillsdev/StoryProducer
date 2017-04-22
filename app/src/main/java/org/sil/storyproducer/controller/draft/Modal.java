package org.sil.storyproducer.controller.draft;


/**
 * The purpose of this class is to have a parent class of the DraftListRecordingsModal and DramaListRecordingsModal classes.
 * The parent class allows the recording toolbar to have a modal object passed in from the draft or dramatization fragments.
 * <br/><br/>
 * See the children classes here:
 * {@link org.sil.storyproducer.controller.dramatization.DramaListRecordingsModal} {@link DraftListRecordingsModal}
 */
public abstract class Modal {
    public Modal() {
    }

    public void show(){

    }
}
