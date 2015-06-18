#
# Jitsi, the OpenSource Java VoIP and Instant Messaging client.
#
# Copyright @ 2015 Atlassian Pty Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# After build outputs should be moved from /libs/armeabi to /lib/native/armeabi

ROOT := $(call my-dir)
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_LDLIBS    := -lEGL -lGLESv1_CM -llog
LOCAL_MODULE    := jnawtrenderer
LOCAL_SRC_FILES := JAWTRenderer_Android.c org_jitsi_impl_neomedia_jmfext_media_renderer_video_JAWTRenderer.c
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := jnffmpeg
LOCAL_SRC_FILES := libjnffmpeg.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := jng722
LOCAL_SRC_FILES := libjng722.so
include $(PREBUILT_SHARED_LIBRARY)

# include $(CLEAR_VARS)
# LOCAL_CFLAGS    := -I../android/platform/frameworks/base/include/media/stagefright/openmax
# LOCAL_LDLIBS    := -llog
# LOCAL_MODULE    := jnopenmax
# LOCAL_SRC_FILES := org_jitsi_impl_neomedia_codec_video_h264_OMXDecoder.c
# include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := jnspeex
LOCAL_SRC_FILES := libjnspeex.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_LDLIBS    := -lOpenSLES -llog
LOCAL_MODULE    := jnopensles
LOCAL_SRC_FILES := org_jitsi_impl_neomedia_device_OpenSLESSystem.c org_jitsi_impl_neomedia_jmfext_media_protocol_opensles_DataSource.c org_jitsi_impl_neomedia_jmfext_media_renderer_audio_OpenSLESRenderer.c
include $(BUILD_SHARED_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_CFLAGS    := -I${ffmpeg} -D_XOPEN_SOURCE=600
#LOCAL_LDLIBS    := -L${ffmpeg}/libavcodec -L${ffmpeg}/libavfilter -L${ffmpeg}/libavformat -L${ffmpeg}/libavutil -L${ffmpeg}/libswscale -L${x264} -lavformat -lavcodec -lavfilter -lavutil -lswscale -lx264
#LOCAL_MODULE    := jnffmpeg
#LOCAL_SRC_FILES := org_jitsi_impl_neomedia_codec_FFmpeg.c
#include $(BUILD_SHARED_LIBRARY)


# Opus sources should be put into jni/opus in order to build
LOCAL_PATH := $(ROOT)/opus
include $(CLEAR_VARS)
LOCAL_MODULE    := jnopus
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include $(LOCAL_PATH)/celt $(LOCAL_PATH)/silk $(LOCAL_PATH)/silk/fixed
LOCAL_SRC_FILES		:= celt/bands.c								celt/celt.c \
					   celt/cwrs.c								celt/entcode.c \
					   celt/entdec.c							celt/entenc.c \
					   celt/celt_decoder.c                      celt/celt_encoder.c \
					   celt/kiss_fft.c							celt/laplace.c \
					   celt/mathops.c							celt/mdct.c \
					   celt/modes.c								celt/pitch.c \
					   celt/celt_lpc.c							celt/quant_bands.c \
					   celt/rate.c								celt/vq.c \
					   silk/CNG.c								silk/code_signs.c \
					   silk/init_decoder.c						silk/decode_core.c \
					   silk/decode_frame.c						silk/decode_parameters.c \
					   silk/decode_indices.c					silk/decode_pulses.c \
					   silk/decoder_set_fs.c					silk/dec_API.c \
					   silk/enc_API.c							silk/encode_indices.c \
					   silk/encode_pulses.c						silk/gain_quant.c \
					   silk/interpolate.c						silk/LP_variable_cutoff.c \
					   silk/NLSF_decode.c						silk/NSQ.c \
					   silk/NSQ_del_dec.c						silk/PLC.c \
					   silk/shell_coder.c						silk/tables_gain.c \
					   silk/tables_LTP.c						silk/tables_NLSF_CB_NB_MB.c \
					   silk/tables_NLSF_CB_WB.c					silk/tables_other.c \
					   silk/tables_pitch_lag.c					silk/tables_pulses_per_block.c \
					   silk/VAD.c								silk/control_audio_bandwidth.c \
					   silk/quant_LTP_gains.c					silk/VQ_WMat_EC.c \
					   silk/HP_variable_cutoff.c				silk/NLSF_encode.c \
					   silk/NLSF_VQ.c							silk/NLSF_unpack.c \
					   silk/NLSF_del_dec_quant.c				silk/process_NLSFs.c \
					   silk/stereo_LR_to_MS.c					silk/stereo_MS_to_LR.c \
					   silk/check_control_input.c				silk/control_SNR.c \
					   silk/init_encoder.c						silk/control_codec.c \
					   silk/A2NLSF.c							silk/ana_filt_bank_1.c \
					   silk/biquad_alt.c						silk/bwexpander_32.c \
					   silk/bwexpander.c						silk/debug.c \
					   silk/decode_pitch.c						silk/inner_prod_aligned.c \
					   silk/lin2log.c							silk/log2lin.c \
					   silk/LPC_analysis_filter.c				silk/LPC_inv_pred_gain.c \
					   silk/table_LSF_cos.c						silk/NLSF2A.c \
					   silk/NLSF_stabilize.c					silk/NLSF_VQ_weights_laroia.c \
					   silk/pitch_est_tables.c					silk/resampler.c \
					   silk/resampler_down2_3.c					silk/resampler_down2.c \
					   silk/resampler_private_AR2.c				silk/resampler_private_down_FIR.c \
					   silk/resampler_private_IIR_FIR.c			silk/resampler_private_up2_HQ.c \
					   silk/resampler_rom.c						silk/sigm_Q15.c \
					   silk/sort.c								silk/sum_sqr_shift.c \
					   silk/stereo_decode_pred.c				silk/stereo_encode_pred.c \
					   silk/stereo_find_predictor.c				silk/stereo_quant_pred.c \
					   silk/fixed/LTP_analysis_filter_FIX.c		silk/fixed/LTP_scale_ctrl_FIX.c \
					   silk/fixed/corrMatrix_FIX.c				silk/fixed/encode_frame_FIX.c \
					   silk/fixed/find_LPC_FIX.c				silk/fixed/find_LTP_FIX.c \
					   silk/fixed/find_pitch_lags_FIX.c			silk/fixed/find_pred_coefs_FIX.c \
					   silk/fixed/noise_shape_analysis_FIX.c	silk/fixed/prefilter_FIX.c \
					   silk/fixed/process_gains_FIX.c			silk/fixed/regularize_correlations_FIX.c \
					   silk/fixed/residual_energy16_FIX.c		silk/fixed/residual_energy_FIX.c \
					   silk/fixed/solve_LS_FIX.c				silk/fixed/warped_autocorrelation_FIX.c \
					   silk/fixed/apply_sine_window_FIX.c		silk/fixed/autocorr_FIX.c \
					   silk/fixed/burg_modified_FIX.c			silk/fixed/k2a_FIX.c \
					   silk/fixed/k2a_Q16_FIX.c					silk/fixed/pitch_analysis_core_FIX.c \
					   silk/fixed/vector_ops_FIX.c				silk/fixed/schur64_FIX.c \
					   silk/fixed/schur_FIX.c					src/opus.c \
					   src/opus_decoder.c						src/opus_encoder.c \
					   src/opus_multistream.c					src/repacketizer.c \
                       ../org_jitsi_impl_neomedia_codec_audio_opus_Opus.c
LOCAL_CFLAGS	:= -Drestrict='' -D__EMX__ -DOPUS_BUILD -DFIXED_POINT -DDISABLE_FLOAT_API -DUSE_ALLOCA -DHAVE_LRINT -DHAVE_LRINTF -O3 -fno-math-errno
include $(BUILD_SHARED_LIBRARY)

