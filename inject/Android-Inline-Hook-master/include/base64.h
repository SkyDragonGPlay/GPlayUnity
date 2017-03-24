#ifndef _BASE64_H
#define _BASE64_H

int base64_encode(const char *in, size_t size, char *result);
int base64_decode(const char *in, char *result);

#endif
