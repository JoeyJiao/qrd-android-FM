ifeq ($(BOARD_HAVE_QCOM_FM),true)
ifeq (, $(filter msm7627a_sku1 msm7627a_sku3, $(QCOM_TARGET_PRODUCT)))

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src/com/quicinc/fmradio) \
	src/com/quicinc/fmradio/IFMRadioServiceCallbacks.aidl \
	src/com/quicinc/fmradio/IFMRadioService.aidl \
	src/com/quicinc/fmradio/IFMTransmitterServiceCallbacks.aidl \
	src/com/quicinc/fmradio/IFMTransmitterService.aidl \


ifeq ($(call is-android-codename,HONEYCOMB),true)
LOCAL_SRC_FILES +=  $(call all-java-files-under, src/com/quicinc/hc_utils)
else
LOCAL_SRC_FILES +=  $(call all-java-files-under, src/com/quicinc/utils)
endif
LOCAL_PACKAGE_NAME := FM
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

endif
endif
