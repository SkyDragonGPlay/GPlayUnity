#include <jni.h>
#include <dlfcn.h>
#include <stdio.h>
#include "utils.h"
#include "base64.h"

const char* apkPath = NULL;
const char* GPLAY_PATH = "/sdcard/Gplay/";

//========================== fopen ==========================
FILE *(*old_fopen)(const char *, const char *) = NULL;
FILE *new_fopen(const char *path, const char *mode)
{
    LOGI("new_fopen  %s", path);

    FILE *file = NULL;
    if(old_fopen != NULL)
        file = old_fopen(path, mode);

    return file;
}

// HookFopen()
JNIEXPORT void JNICALL Java_com_skydragon_gplay_GPlayHook_HookFopen(JNIEnv *env, jobject obj)
{
    hook((uint32_t)fopen, (uint32_t)new_fopen, (uint32_t **) &old_fopen);
}

// UnHookFopen()
JNIEXPORT void JNICALL Java_com_skydragon_gplay_GPlayHook_UnHookFopen(JNIEnv *env, jobject obj)
{
    unHook((uint32_t) fopen);
}
//========================== end ==========================


//========================== dlopen ==========================
int libCount = 0;
const char* libPaths[128];
void* libs[128];
void* jniOnLoad = NULL;

void *getLibHandle(const char* libName)
{
	void *handle = NULL;
    int i = 0;
	for (i = 0; i < libCount; ++i)
	{
		const char* tmp = strstr(libPaths[i], libName);
		if (tmp != NULL)
		{
            LOGI("strstr result: %s", libPaths[i]);
			handle = libs[i];
			break;
		}
	}
	return handle;
}

void *(*old_dlopen)(const char* libPath, int mode) = NULL;
void *new_dlopen(const char* libPath, int mode)
{
	if (old_dlopen != NULL)
	{
        LOGI("dlopen ID: %d    libPath: %s", libCount + 1, libPath);

        const char* tmp = strstr(libPath, "libunity.so");
        if(tmp != NULL)
        {
            void* libUnityHandle = old_dlopen(libPath, mode);
            if(libUnityHandle != NULL)
            {
                const char* error = dlerror();
                jniOnLoad = dlsym(libUnityHandle, "JNI_OnLoad");
                if((error = dlerror()) != NULL)
                    LOGI("dlsym error: %s", error);
            }

            libs[libCount] = libUnityHandle;
        }
        else
        {
    		libs[libCount] = old_dlopen(libPath, mode);
        }
		libPaths[libCount] = libPath;
		return libs[libCount++];
	}
	return NULL;
}

JNIEXPORT void JNICALL Java_com_skydragon_gplay_GPlayHook_UnHookDlOpen(JNIEnv *env, jobject obj)
{
    LOGI("Java_com_skydragon_gplay_GPlayHook_UnHookDlOpen");
    unHook((uint32_t) dlopen);
}

JNIEXPORT void JNICALL Java_com_skydragon_gplay_GPlayHook_HookDlOpen(JNIEnv *env, jobject obj)
{
    LOGI("Java_com_skydragon_gplay_GPlayHook_HookDlOpen");
    hook((uint32_t)dlopen, (uint32_t)new_dlopen, (uint32_t **) &old_dlopen);
}
//========================== end ==========================


//========================== ApkOpen ==========================
int (*old_apkOpen)(const char* filePath) = NULL;
int new_apkOpen(const char* filePath)
{
    LOGI("new_apkOpen   %s", filePath);

    int result = 0;

    char relativePath[256];
    if(apkPath != NULL && getRelativeApkPath(apkPath, filePath, relativePath) == 1)
    {
        char tmp[256];
        char absolutePath[256];
        strcpy(absolutePath, GPLAY_PATH);
        getPathFirstComponent(relativePath, tmp);
        strcat(absolutePath, tmp);
        strcat(absolutePath, "/");

        LOGI("apkOpen relativePath: %s", relativePath);
        if(base64_encode(relativePath, 0, tmp) == 0)
            return result;

        strcat(absolutePath, tmp);
        strcat(absolutePath, ".obb");

        LOGI("gplay apkOpen new dir: %s", absolutePath);

        //F_OK:0 只判断文件是否存在    0:存在   -1:不存在
        int isAccess = access(absolutePath, 0);
        if(isAccess == 0)
        {
            // const char* fileName = getFileName(filePath);
            // strcat(absolutePath, "/");
            // strcat(absolutePath, fileName);

            strcat(absolutePath, "/");
            strcat(absolutePath, tmp);
            LOGI("gplay apkOpen new filePath: %s", absolutePath);
            result = old_apkOpen(absolutePath);
        }
        else
        {
            result = old_apkOpen(filePath);
        }
    }
    else
    {
        result = old_apkOpen(filePath);
    }

    // const char* tmp = strstr(fileName, "6ef021160ac49cd409c67338b14f8941");
    // // const char* tmp = strstr(fileName, "Assembly-CSharp.dll");
    // if(tmp != NULL)
    // {
    //     LOGI("apkOpen /sdcard/EditorTest.obb/0439452e00b803a4b9c397d75819b16b");
    //     res = old_apkOpen("/sdcard/EditorTest.obb/0439452e00b803a4b9c397d75819b16b");

    //     // tmp = strstr(fileName, ".mdb");
    //     // if(tmp != NULL)
    //     // {
    //     //     res = old_apkOpen(fileName);
    //     // }
    //     // else
    //     // {
    //     //     LOGI("apkOpen Assembly-CSharp.dll");
    //     //     res = old_apkOpen("/sdcard/Assembly-CSharp.obb/Assembly-CSharp.dll");
    //     // }
    // }
    // else
    // {
    //     res = old_apkOpen(fileName);
    // }
    
    LOGI("apkOpen result: %d", result);
    return result;
}
JNIEXPORT void JNICALL Java_com_skydragon_gplay_GPlayHook_HookApkOpen(JNIEnv *env, jobject obj)
{
    if(jniOnLoad != NULL)
    {
        uint32_t apkOpen = ((uint32_t)jniOnLoad) - 0xFB84;
        hook(apkOpen, (uint32_t)new_apkOpen, (uint32_t **) &old_apkOpen);
    }
}
//========================== end ==========================


//========================== IsFileCreated ==========================
char isStrEndWith(const char* str, const char* suffix)
{
    const char* tmp = strstr(str, suffix);
    if(tmp != NULL)
    {
        int strLen = strlen(str);
        int suffixLen = strlen(suffix);
        if(tmp == (str + strLen - suffixLen))
            return 1;
    }
    return 0;
}

int (*old_isFileCreated)(const char**) = NULL;
int new_isFileCreated(const char** filePath)
{
    LOGI("new_isFileCreated: %s", *filePath);

    int result = 0;
    char relativePath[256];
    if(apkPath != NULL && getRelativeApkPath(apkPath, *filePath, relativePath) == 1)
    {
        char tmp[256];
        char absolutePath[256];
        strcpy(absolutePath, GPLAY_PATH);
        getPathFirstComponent(relativePath, tmp);
        strcat(absolutePath, tmp);
        strcat(absolutePath, "/");

        if(base64_encode(relativePath, 0, tmp) == 0)
            return result;

        LOGI("IsFileCreated relativePath : %s", relativePath);
        
        strcat(absolutePath, tmp);
        strcat(absolutePath, ".obb");

        //F_OK:0 只判断文件是否存在    0:存在   -1:不存在
        int isAccess = access(absolutePath, 0);

        if(isAccess == 0)
            result = 1;
        else
            result = old_isFileCreated(filePath);
        LOGI("absolutePath: %s  access: %d", absolutePath, isAccess);
    }
    else
    {
        result = old_isFileCreated(filePath);
    }
    LOGI("IsFileCreated result: %d", result);
    return result;
}

JNIEXPORT void JNICALL Java_com_skydragon_gplay_GPlayHook_HookIsFileCreated(JNIEnv *env, jobject obj, jstring japkpath)
{
    if(jniOnLoad != NULL)
    {
    	apkPath = (*env)->GetStringUTFChars(env, japkpath, 0);
        
        LOGI("Java_com_skydragon_gplay_GPlayHook_HookIsFileCreated  apkPath: %s", apkPath);
        uint32_t isFileCreated = ((uint32_t)jniOnLoad) - 0x93268;
        hook(isFileCreated, (uint32_t)new_isFileCreated, (uint32_t **) &old_isFileCreated);
    }
}
//========================== end ==========================

