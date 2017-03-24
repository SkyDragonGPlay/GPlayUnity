#include "gplayhook.h"
#include <jni.h>
#include <dlfcn.h>
#include <stdio.h>
#include "utils.h"

char gplayResDefaultPath[100] = "/sdcard/Gplay/";
int searchPathCount = 0;
char resSearchPath[20][100];
const char* packageCodePath = "";
const char* libraryPath = NULL;

int (*UnitySendMessageHandle)(const char*, const char*, const char*) = NULL;

void hookApkOpen();
void hookIsFileCreated();


void SetDefaultResourceRootPath(const char* resRootPath)
{
    strcpy(gplayResDefaultPath, resRootPath);

    int strLen = strlen(resRootPath);
    if(resRootPath[strLen-1] != '/' && gplayResDefaultPath[strLen-1] != '\\')
        strcat(gplayResDefaultPath, "/");

    LOGD("SetDefaultResourceRootPath :   %s", gplayResDefaultPath);
}

void AddResourceSearchPath(const char* resPath)
{
    if(resPath == NULL)
        return;

    strcpy(resSearchPath[searchPathCount], resPath);

    int strLen = strlen(resPath);
    if(resPath[strLen - 1] != '/' && resPath[strLen - 1] != '\\')
        strcat(resSearchPath[searchPathCount], "/");

    searchPathCount++;
}


void GplayUnitySendMessage(const char* objName, const char* methodName, const char* message)
{
    LOGD("gplay UnitySendMessage: %s   %s   %s", objName, methodName, message);
    if(UnitySendMessageHandle != NULL)
        UnitySendMessageHandle(objName, methodName, message);
}

int GetResourceAbsolutePath(const char* resName, char* absolutePath)
{
    int result = 0;

    int i = 0;
    for(i=0; i<searchPathCount; ++i)
    {
        strcpy(absolutePath, resSearchPath[i]);
        strcat(absolutePath, resName);

        // 第二个参数F_OK:0 只判断文件是否存在    0:存在   -1:不存在
        int isAccess = access(absolutePath, 0);

        LOGI("is file access: %s   isAccess: %d", absolutePath, isAccess);
        if(isAccess == 0)
            return 1;
    }

    strcpy(absolutePath, gplayResDefaultPath);
    strcat(absolutePath, resName);

    int isAccess = access(absolutePath, 0);
    LOGI("is file access: %s   isAccess: %d", absolutePath, isAccess);
    if(isAccess == 0)
        result = 1;

    return result;
}

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

void hookfopen()
{
    LOGD("hook fopen");
    hook((uint32_t)fopen, (uint32_t)new_fopen, (uint32_t **) &old_fopen);
}
//========================== end ==========================


//========================== dlopen ==========================
void* jniOnLoad = NULL;


void *(*old_dlopen)(const char* libPath, int mode) = NULL;
void *new_dlopen(const char* libPath, int mode)
{
    void *libHandle = NULL;
	if (old_dlopen != NULL)
	{
        LOGI("dlopen libPath: %s", libPath);

        const char* tmp = strstr(libPath, "libunity.so");
        if(tmp != NULL)
        {
            void* libUnityHandle = old_dlopen(libPath, mode);
            if(libUnityHandle != NULL)
            {
                const char* error = dlerror();
                jniOnLoad = dlsym(libUnityHandle, "JNI_OnLoad");
                if((error = dlerror()) != NULL)
                    LOGE("dlsym error: %s", error);

                UnitySendMessageHandle = (int (*)(const char*, const char*, const char*) )dlsym(libUnityHandle, "UnitySendMessage");

                if((error = dlerror()) != NULL)
                    LOGE("dlsym error: %s", error);

                // uint32_t u5_3_5p3_dev = 0x1AAE8, u5_3_6p5_dev = 0x1AF14;
                // if(jniOnLoad != NULL)
                //     UnitySendMessageHandle = (int (*)(const char*, const char*, const char*) )((uint32_t)jniOnLoad - u5_3_6p5);
            }

            hookApkOpen();
            hookIsFileCreated();

            libHandle = libUnityHandle;
        }
        else
        {
    		libHandle = old_dlopen(libPath, mode);
        }
		return libHandle;
	}
	return NULL;
}

void hookDlopen()
{
    LOGD("hook dlopen");
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
    if(getRelativeApkPath(packageCodePath, filePath, relativePath) == 1)
    {
        char absolutePath[512];
        char tmp[512];

        strcpy(tmp, relativePath);
        strcat(tmp, ".obb");

        const char* fileName = getFileName(filePath);
        int isExist = GetResourceAbsolutePath(tmp, absolutePath);
        if(isExist == 0)
        {
            strcpy(tmp, fileName);
            strcat(tmp, ".obb");
            isExist = GetResourceAbsolutePath(tmp, absolutePath);
        }

        if(isExist)
        {
            LOGI("gplay apkOpen new filePath: %s", absolutePath);
            strcat(absolutePath, "/");
            strcat(absolutePath, fileName);
            result = old_apkOpen(absolutePath);
        }
        else
        {
            LOGI("new_apkOpen no found file call old_apkOpen: %s", filePath);
            result = old_apkOpen(filePath);
        }


        // LOGI("gplay apkOpen new dir: %s", absolutePath);

        // //F_OK:0 只判断文件是否存在    0:存在   -1:不存在
        // int isAccess = access(absolutePath, 0);
        // if(isAccess == 0)
        // {
        //     const char* fileName = getFileName(filePath);
        //     strcat(absolutePath, "/");
        //     strcat(absolutePath, fileName);

        //     LOGI("gplay apkOpen new filePath: %s", absolutePath);
        //     result = old_apkOpen(absolutePath);
        // }
        // else
        // {
        //     // ///////////////////////////////////////////////////////////
        //     // const char* tmp = strstr(filePath, ".apk");
        //     // if(tmp != NULL)
        //     // {
        //     //     strcpy(absolutePath, gplayResDefaultPath);
        //     //     strcat(absolutePath, "GPlayDemo.obb");
        //     //     strcat(absolutePath, "/");
        //     //     strcat(absolutePath, relativePath);

        //     //     LOGI("gplay apkOpen new filePath: %s", absolutePath);
        //     //     result = old_apkOpen(absolutePath);
        //     // }
        //     // else
        //     // {
        //     //     result = old_apkOpen(filePath);
        //     // }
        //     // ///////////////////////////////////////////////////////////

        //     // strcpy(absolutePath, gplayResDefaultPath);
        //     // strcat(absolutePath, relativePath);
        //     // strcat(absolutePath, ".split.obb");
        //     // isAccess = access(absolutePath, 0);
        //     // if(isAccess == 0)
        //     // {
        //     //     const char* fileName = getFileName(filePath);
        //     //     strcat(absolutePath, "/");
        //     //     strcat(absolutePath, fileName);
        //     //     strcat(absolutePath, ".split0");
        //     //     LOGI("gplay apkOpen new filePath: %s", absolutePath);
        //     //     result = old_apkOpen(absolutePath);
        //     // }
        //     // else
        //         result = old_apkOpen(filePath);
        // }
    }
    else
    {
        result = old_apkOpen(filePath);
    }
    LOGI("apkOpen result: %d", result);
    return result;
}

void hookApkOpen()
{
    LOGD("hook ApkOpen");
    if(jniOnLoad != NULL)
    {
        uint32_t u5_3_5p3_dev = 0xFB84, u5_3_6p5_dev = 0xF808, u5_3_6p5_release = 0xE288;
        uint32_t apkOpen = ((uint32_t)jniOnLoad) - u5_3_6p5_release;
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
    if(getRelativeApkPath(packageCodePath, *filePath, relativePath) == 1)
    {
        char tmp[256];
        char absolutePath[256];

        strcpy(tmp, relativePath);
        strcat(tmp, ".obb");

        const char* fileName = getFileName(*filePath);
        result = GetResourceAbsolutePath(tmp, absolutePath);
        if(result == 0)
        {
            strcpy(tmp, fileName);
            strcat(tmp, ".obb");
            result = GetResourceAbsolutePath(tmp, absolutePath);
        }

        if(result == 1)
            LOGI("new_isFileCreated file exist absolutePath: %s", absolutePath);

        if(result == 0)
        {
            result = couldFileSplit(fileName);
            if(result)
                LOGI("%s   could split", *filePath);
        }

        if(result == 0)
        {
            LOGI("file not exist, call old_isFileCreated");
            result = old_isFileCreated(filePath);
        }


        // // strcpy(absolutePath, gplayResDefaultPath);

        // // strcat(absolutePath, relativePath);
        // // strcat(absolutePath, ".obb");
        // //F_OK:0 只判断文件是否存在    0:存在   -1:不存在
        // int isAccess = access(absolutePath, 0);

        // // ///////////////////////////////////////////////////////////
        // // int isAccess = -1;
        // // const char* tmp = strstr(*filePath, ".apk");
        // // if(tmp != NULL)
        // //     isAccess = 0;
        // // else
        // // {
        // //     strcat(absolutePath, relativePath);
        // //     strcat(absolutePath, ".obb");
        // //     //F_OK:0 只判断文件是否存在    0:存在   -1:不存在
        // //     isAccess = access(absolutePath, 0);
        // // }
        // // ///////////////////////////////////////////////////////////

        // if(isAccess == 0)
        //     result = 1;
        // else
        // {
        //     strcpy(absolutePath, gplayResDefaultPath);
        //     strcat(absolutePath, relativePath);

        //     strcat(absolutePath, ".split0.obb");
        //     isAccess = access(absolutePath, 0);
        //     if(isAccess == 0)
        //         result = 1;
        //     else
        //         result = old_isFileCreated(filePath);
        // }
    }
    else
    {
        result = old_isFileCreated(filePath);
    }
    LOGI("IsFileCreated result: %d", result);
    return result;
}

void hookIsFileCreated()
{
    LOGD("hook IsFIleCreated");
    int result = 0;
    if(jniOnLoad != NULL)
    {
    	// apkPath = (*env)->GetStringUTFChars(env, japkpath, 0);
        
        // LOGI("Java_com_skydragon_gplay_GPlayHook_HookIsFileCreated  apkPath: %s", apkPath);
        uint32_t u5_3_5p3_dev = 0x93268, u5_3_6p5_dev = 0x936DC, u5_3_6p5_release = 0x89438;
        uint32_t isFileCreated = ((uint32_t)jniOnLoad) - u5_3_6p5_release;
        result = hook(isFileCreated, (uint32_t)new_isFileCreated, (uint32_t **) &old_isFileCreated);
    }
    if(result != 0)
        LOGI("IsFileCreated fail");
}
//========================== end ==========================



// JNIEXPORT void JNICALL Java_com_unity3d_player_UnityPlayer_nativeHookDlopen(JNIEnv *env, jobject obj)
// {
//     hookDlopen();
// }

// JNIEXPORT void JNICALL Java_com_unity3d_player_UnityPlayer_nativeUnitySendMessage(JNIEnv *env, jobject obj, jstring jstrObjName, jstring jstrMethod, jstring jstrMessage)
// {
//     const char* objName = (*env)->GetStringUTFChars(env, jstrObjName, 0);
//     const char* method = (*env)->GetStringUTFChars(env, jstrMethod, 0);
//     const char* message = (*env)->GetStringUTFChars(env, jstrMessage, 0);
//     HookUnitySendMessage(objName, method, message);
// }

// JNIEXPORT jint JNI_OnLoad(JavaVM* jvm, void* reserved)
// {
//     JNIEnv* env = NULL;
//     jint result = -1; 

//     if ((*jvm)->GetEnv(jvm, (void**) &env, JNI_VERSION_1_6) != JNI_OK) {
//         return result;
//     }

//     jclass clazz = (*env)->FindClass(env, "com/unity3d/player/UnityPlayer");
//     if(clazz != NULL)
//     {
//         jfieldID fid = (*env)->GetStaticFieldID(env, clazz, "packageCodePath", "Ljava/lang/String;");
//         jstring jstrApkPath = (jstring)(*env)->GetStaticObjectField(env, clazz, fid);
//         apkPath = (*env)->GetStringUTFChars(env, jstrApkPath, 0);

//         LOGI("JNI_OnLoad    %s", apkPath);

//         // jfieldID fid = (*env)->GetStaticFieldID(env, clazz, "sInstance", "Lcom/skydragon/gplay/runtime/bridge/UnityRuntimeBridge;");
//         // jobject objRuntimeBridge = (jobject)(*env)->GetStaticIntField(env, clazz, fid);
//         // if(objRuntimeBridge != NULL)
//         // {
//         //     jmethodID mid = (*env)->GetMethodID(env, clazz, "getDefaultResourceRootPath", "()Ljava/lang/String;");
//         //     jstring jstrApkPath = (jstring)(*env)->CallObjectMethod(env, objRuntimeBridge, mid);
//         //     apkPath = (*env)->GetStringUTFChars(env, jstrApkPath, 0);
//         // }
//     }
    
//     hookDlopen();


//     // 返回jni的版本
//     return JNI_VERSION_1_6;
// }



