package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaMuxer;
import android.util.Log;

import java.nio.ByteBuffer;

public class PipedMediaMuxer implements MediaByteBufferDest {
    private MediaMuxer mMuxer = null;

    private MediaByteBufferSource mSource = null;
    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    @Override
    public void addSource(MediaByteBufferSource src) throws SourceUnacceptableException {

    }

    public void crunch() {
        // Poll frames from the audio encoder and send them to the muxer.
        while (mUseAudio && !mAudioEncoderDone
                && (encoderOutputAudioFormat == null || mMuxerStarted)) {
            int encoderOutputBufferIndex = mAudioEncoder.dequeueOutputBuffer(
                    audioEncoderOutputBufferInfo, TIMEOUT_USEC);
            if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (VERBOSE) Log.d(TAG, "no audio encoder output buffer");
                break;
            }
            if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                if (VERBOSE) Log.d(TAG, "audio encoder: output buffers changed");
                audioEncoderOutputBuffers = mAudioEncoder.getOutputBuffers();
                break;
            }
            if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (VERBOSE) Log.d(TAG, "audio encoder: output format changed");
                if (mAudioTrackIndex >= 0) {
                    throw new RuntimeException("audio encoder changed its output format again?");
                }
                encoderOutputAudioFormat = mAudioEncoder.getOutputFormat();
                break;
            }

            if (VERBOSE) {
                Log.d(TAG, "audio encoder: returned output buffer: "
                        + encoderOutputBufferIndex);
                Log.d(TAG, "audio encoder: returned buffer of size "
                        + audioEncoderOutputBufferInfo.size);
            }
            ByteBuffer encoderOutputBuffer =
                    audioEncoderOutputBuffers[encoderOutputBufferIndex];
            if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                    != 0) {
                if (VERBOSE) Log.d(TAG, "audio encoder: codec config buffer");
                // Simply ignore codec config buffers.
                mAudioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                break;
            }
            if (VERBOSE) {
                Log.d(TAG, "audio encoder: returned buffer for time "
                        + audioEncoderOutputBufferInfo.presentationTimeUs);

//                long nextTimeExpected = audioEncoderOutputBufferInfo.presentationTimeUs
//                        + (audioEncoderOutputBufferInfo.size / 2 / encoderOutputAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)) * ();
//                Log.d(TAG, "audio encoder: expecting next time " + nextTimeExpected);
            }

            if (audioPresentationTimeUsLast > audioEncoderOutputBufferInfo.presentationTimeUs) {
                if(VERBOSE) Log.d(TAG, "audio encoder: correcting presentation time from "
                        + audioEncoderOutputBufferInfo.presentationTimeUs + " to " + (audioPresentationTimeUsLast + 1));
                audioEncoderOutputBufferInfo.presentationTimeUs = audioPresentationTimeUsLast + 1;
            }
            audioPresentationTimeUsLast = audioEncoderOutputBufferInfo.presentationTimeUs;

            ByteBuffer buffer = mSource.getBuffer(mInfo);
            mMuxer.writeSampleData(0, buffer, mInfo);
            mSource.releaseBuffer(buffer);

            // Write data
            if (audioEncoderOutputBufferInfo.size != 0) {
                mMuxer.writeSampleData(
                        mAudioTrackIndex, encoderOutputBuffer, audioEncoderOutputBufferInfo);
            }
            else {
                throw new RuntimeException("buffer of size 0?");
            }

            if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    != 0) {
                if (VERBOSE) Log.d(TAG, "audio encoder: EOS");
                mAudioEncoderDone = true;
            }
            mAudioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
//            audioEncodedFrameCount++;
            // We enqueued an encoded frame, let's try something else next.
//            break;
        }

    }
}
