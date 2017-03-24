#ifndef _UTILS_H
#define _UTILS_H

#include <string.h>
#include "log.h"

int getRelativeApkPath(const char* rootPath, const char* absolutePath, char* relativePath);
const char* getFileName(const char* path);
void getPathFirstComponent(const char* path, char* result);

#endif

