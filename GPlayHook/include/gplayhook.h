#ifndef _GPLAY_HOOK_H
#define _GPLAY_HOOK_H

void hookDlopen();
void SetDefaultResourceRootPath(const char* resRootPath);
void AddResourceSearchPath(const char* resPath);
void GplayUnitySendMessage(const char* objName, const char* method, const char* message);
#endif