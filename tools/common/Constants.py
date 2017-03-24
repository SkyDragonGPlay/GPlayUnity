import os
import sys

COMMON_PACKAGE_PATH = os.path.split(os.path.realpath(__file__))[0]
JDT_CORE_JAR_FILE_PATH = os.path.join(COMMON_PACKAGE_PATH, "lib", "org.eclipse.jdt.core_3.8.3.v20130121-145325.jar")
PROGUARD_JAR_FILE_PATH = os.path.join(COMMON_PACKAGE_PATH, "lib", "proguard.jar")
BSDIFF_PATH = os.path.join(COMMON_PACKAGE_PATH, "lib", "bsdiff", "bsdiff" if sys.platform != 'win32' else "bsdiff.exe")
