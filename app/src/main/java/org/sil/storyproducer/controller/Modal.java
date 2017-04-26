package org.sil.storyproducer.controller;


/**
 * The purpose of this interface is specify the behavior of implementations of the Modal interface.
 * <br/><br/>
 * See the current implementations here:
 * {@link org.sil.storyproducer.controller.dramatization.DramaListRecordingsModal} {@link org.sil.storyproducer.controller.draft.DraftListRecordingsModal}
 */
public interface Modal {
    void show();
}
