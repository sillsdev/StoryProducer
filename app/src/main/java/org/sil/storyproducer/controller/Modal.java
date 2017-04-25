package org.sil.storyproducer.controller;


/**
 * The purpose of this class is to specify the Modal implementations.
 * <br/><br/>
 * See the current implementations classes here:
 * {@link org.sil.storyproducer.controller.dramatization.DramaListRecordingsModal} {@link org.sil.storyproducer.controller.draft.DraftListRecordingsModal}
 */
public interface Modal {
    public void show();
}
