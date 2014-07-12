LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LDLIBS		:= -llog
LOCAL_MODULE		:= facebook
LOCAL_SRC_FILES		:= facebook.cpp

include $(BUILD_SHARED_LIBRARY)
