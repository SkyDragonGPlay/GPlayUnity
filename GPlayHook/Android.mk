
LOCAL_PATH := $(call my-dir)

#===============================================================================
include $(CLEAR_VARS)

LOCAL_MODULE := hook_static
LOCAL_SRC_FILES := gplayhook.c      \
                   utils.c         \
                   inlineHook.c \
                   relocate.c

LOCAL_C_INCLUDES := $(LOCAL_PATH) \
                    ./include
#LOCAL_LDLIBS:=-llog

include $(BUILD_STATIC_LIBRARY)







