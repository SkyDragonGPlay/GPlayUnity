#include <string.h>
#include "utils.h"
#include "inlineHook.h"

int hook(uint32_t ori_func, uint32_t new_func, uint32_t **old_func_addr)
{
    if (registerInlineHook(ori_func, new_func, old_func_addr) != ELE7EN_OK) {
        return -1;
    }
    if (inlineHook(ori_func) != ELE7EN_OK) {
        LOGI("hook fail");
        return -1;
    }
    
    LOGI("hook ELE7EN_OK");

    return 0;
}

int unHook(uint32_t ori_func)
{
    LOGI("unHook");
    if (inlineUnHook(ori_func) != ELE7EN_OK) {
        LOGI("unHook return -1");
        return -1;
    }

    LOGI("unHook return 0");
    return 0;
}

int getRelativeApkPath(const char* rootPath, const char* absolutePath, char* relativePath)
{
    int result = 0;
    // const char* tmp = strstr(absolutePath, rootPath);

    const char* tmp = strstr(absolutePath, ".apk");
    if(tmp != NULL)
    {
        // int rootLen = strlen(rootPath);
        // strcpy(relativePath, absolutePath + rootLen + 1);
        strcpy(relativePath, tmp + 5);
        result = 1;
    }
    else if((tmp = strstr(absolutePath, "assets")) == absolutePath)
    {
        strcpy(relativePath, absolutePath);
        result = 1;
    }
    else if((tmp = strstr(absolutePath, "Managed")) == absolutePath ||
        (tmp = strstr(absolutePath, "Resources")) == absolutePath)
    {
        strcpy(relativePath, "assets/bin/Data/");
        strcat(relativePath, absolutePath);
        result = 1;
    }
    return result;
}

const char* getFileName(const char* path)
{
    char* tmp = (char*)path + strlen(path);
    
    do
    {
        if(*tmp == '/' || *tmp == '\\')
            return tmp + 1;
        tmp--;
    }while(tmp != path);

    return tmp;
}

void getPathFirstComponent(const char* path, char* result)
{
    if(*path == '/' || *path=='\\')
    {
        result[0] = '\0';
        return;
    }

    int pathLen = strlen(path);
    int i;
    for(i = 0; i<pathLen; ++i)
    {
        result[i] = path[i];
        if(path[i] == '\0')
        {
            return;
        }
        else if(path[i] == '/' || path[i] == '\\')
        {
            result[i] = '\0';
            return;
        }
    }
}

int couldFileSplit(const char* filePath)
{
    int result = 1;
    const char* fileName = getFileName(filePath);
    int len = strlen(filePath);
    if(strncmp(filePath, "level", 5))
    {
        if(len < 7)
            return 0;
        const char* tmp = filePath + len;
        if(strcmp(tmp - 7, ".assets"))
        {
            if(len < 9)
                return 0;
            if(strcmp(tmp - 9, ".resource"))
                result = 0;
        }
    }
    return result;
}


