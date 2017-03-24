#!/usr/bin/env python
#coding=utf-8

import os
import sys
import subprocess
import shutil
import hashlib
import json
import re
import stat
import codecs
import zipfile2 as zipfile
import Constants

TAG = "common.py"

####################################################
class Logging:
    # TODO maybe the right way to do this is to use something like colorama?
    RED = '\033[31m'
    GREEN = '\033[32m'
    YELLOW = '\033[33m'
    MAGENTA = '\033[35m'
    RESET = '\033[0m'

    @staticmethod
    def _print(s, color=None):
        if color and sys.stdout.isatty() and sys.platform != 'win32':
            print(color + "==> " + s + Logging.RESET)
        else:
            print("==> " + s)

    @staticmethod
    def debug(s):
        Logging._print(s, Logging.MAGENTA)

    @staticmethod
    def info(s):
        Logging._print(s, Logging.GREEN)

    @staticmethod
    def warning(s):
        Logging._print(s, Logging.YELLOW)

    @staticmethod
    def error(s):
        Logging._print(s, Logging.RED)


# Logging something with tag
def LOGI(tag, message):
    Logging.info("INFO <" + tag + ">: " + message)

def LOGD(tag, message):
    Logging.debug("DEBUG <" + tag + ">: " + message)

def LOGE(tag, message):
    Logging.error("ERROR <" + tag + ">: " + message)

def LOGW(tag, message):
    Logging.warning("WARN <" + tag + ">: " + message)

####################################################

class ErrorMessage(Exception):
    pass


def mac_os():
    if sys.platform == 'darwin':
        return True
    return False


def win32():
    if sys.platform == 'win32':
        return True
    return False


def checked_space_character(path):
    if " " in path:
        return "\"" + path + "\""

    return path


def run_command(cmd, success_message=None, error_message=None):
    LOGI(TAG, "run_command:" + cmd)
    ret = subprocess.call(cmd, stderr=subprocess.STDOUT, shell=True)
    if ret != 0:
        if error_message is not None:
            LOGE(TAG, error_message)
        raise ErrorMessage("Error running command %s" % ret)
    else:
        if success_message is not None:
            LOGD(TAG, success_message)


def assert_if_failed(cond, error_message):
    if not cond:
        raise ErrorMessage(error_message)   

def quote(s):
    return "\"" + s + "\""

def generate_class_files(src, des, extra_libs=None, encoded="UTF-8"):

    LOGI(TAG, "src=" + str(src) + ", des=" + des + ", extra_libs=" + str(extra_libs) + ", encoded=" + encoded)

    if src is None:
        LOGI(TAG, "generate_class_files 's src argument is None'")
        return

    if type(src) == str:
        assert_if_failed(is_dir(src), "The (%s) path must be exist!" % src)
        if len(find_file_in_dir(src, "\.java$")) == 0:
            LOGW(TAG, "The (%s) path doesn't contain any java source files" % src)
            return

    elif type(src) == list:
        found_java_source_count = 0
        src_dirs = ""
        for src_dir in src:
            assert_if_failed(is_dir(src_dir), "The (%s) path must be exist!" % src_dir)
            if len(find_file_in_dir(src_dir, "\.java$")) > 0:
                found_java_source_count += 1
            src_dirs = src_dirs + " " + quote(src_dir)
        if found_java_source_count == 0:
            LOGW(TAG, "The (%s) path doesn't contain any java source files" % str(src))
            return

        src = src_dirs
    else:
        raise ErrorMessage("The 'src' argument only supports string or string list")

    separator = ":" if not win32() else ";"

    if extra_libs:
        if type(extra_libs) == str:
            assert_if_failed(is_file(extra_libs), "1:The (%s) path must exist!" % extra_libs)
        elif type(extra_libs) == list:
            for lib in extra_libs:
                assert_if_failed(is_file(lib), "2:The (%s) path must exist!" % lib)
            extra_libs = separator.join(extra_libs)
        else:
            raise ErrorMessage("The 'extra_libs' argument only supports string or string list")

    class_path = ""

    if extra_libs:
        class_path = class_path + separator + extra_libs

    command = "java -jar " + \
              quote(Constants.JDT_CORE_JAR_FILE_PATH) + \
              " -g:lines,source" + \
              " -1.7 -nowarn -encoding %s " % quote(encoded) + \
              src + \
              " -d " + quote(des) + \
              " -classpath " + class_path
    LOGI(TAG, command)
    run_command(command, error_message="Generating *.class file failed.")


def generate_jar_file(classes_dir, out_file_path):
    LOGI(TAG, "Packing all .class in (%s) files to a jar file (%s)" % (classes_dir, out_file_path))
    assert_if_failed(is_dir(classes_dir), "The (%s) classes folder doesn't exist!" % classes_dir)

    run_command(
        "jar cf " + out_file_path + " -C " + classes_dir + " .",
        error_message="Packing .class files failed!"
    )

def generate_dex_jar(in_jar, out_dex_jar):
    LOGI(TAG, "Generating dex jar file (%s -> %s)!" % (in_jar, out_dex_jar))
    assert_if_failed(is_file(in_jar), "The input jar (%s) file  doesn't exist!" % in_jar)

    run_command(
        "dx --dex --output=%s %s" % (out_dex_jar, in_jar),
        error_message="Generating dex jar (%s) file failed!" % out_dex_jar
    )

def extract_jar_files(jar_folder_or_file_path, output_class_path, with_jar_name_folder=True):
    LOGI(TAG, "Extracting jar files (" + str(jar_folder_or_file_path) + ") to " + output_class_path)
    old_path = os.getcwd()
    LOGD(TAG, "EXTRA_JAR: Old cwd: " + old_path)
    output_class_path = to_absolute_path(jar_folder_or_file_path, output_class_path)
    assert_if_failed(os.path.exists(jar_folder_or_file_path), "The jar source (%s) doesn't exist!" % jar_folder_or_file_path) 

    def unpack_jar_file(jar_path):
        if with_jar_name_folder:
            output_path = os.path.join(output_class_path, get_file_name_without_extension(jar_path))
        else:
            output_path = output_class_path

        ensure_folder_exists(output_path)
        os.chdir(output_path)
        LOGD(TAG, "Extracting %s to %s" % (jar_path, output_path))
        run_command(
            "jar xf " + jar_path,
            error_message="extract %s file failed" % jar_path
        )
        safe_remove_file(os.path.join(output_path, "META-INF"))

    if os.path.isdir(jar_folder_or_file_path):
        def callback(path, is_dir):
            if is_dir or not path.endswith('.jar'):
                return False

            unpack_jar_file(path)
            return False

        deep_iterate_dir(jar_folder_or_file_path, callback)

    elif os.path.isfile(jar_folder_or_file_path) and jar_folder_or_file_path.endswith('.jar'):
        unpack_jar_file(jar_folder_or_file_path)
    else:
        LOGE(TAG, "extract_jar_files: Unsupported jar source!")

    os.chdir(old_path)
    LOGD(TAG, "EXTRA_JAR: after revert cwd: " + os.getcwd())


def list_to_string_with_prefix(prefix, l):
    ret = ""
    if type(l) == list:
        for e in l:
            ret = ret + prefix + e
    elif type(l) == str:
        ret = prefix + l
    return ret

def proguard_jar_file(proguard_file_path, injars, outjars, libraryjars, printmapping):
    assert_if_failed(is_file(proguard_file_path), "Can not find proguard config!")

    run_command(
        "java -jar " + Constants.PROGUARD_JAR_FILE_PATH +
        list_to_string_with_prefix(" -injars ", injars) +
        list_to_string_with_prefix(" -outjars ", outjars) +
        list_to_string_with_prefix(" -libraryjars ", libraryjars) +
        " -printmapping " + printmapping +
        " @" + proguard_file_path,
        success_message="Generating proguard successfully!",
        error_message="Failed to generating proguard!"
    )


def to_unix_path(path):
    ret = path.replace('\\', '/')
    while ret.find("//") != -1:
        ret = ret.replace("//", "/")
    return ret

def unix_path_split(path):
    path_section_list = path.split("/")
    dir_path = "/".join(path_section_list[:-1])
    file_name = path_section_list[-1]
    return dir_path, file_name

def to_absolute_path(base_path, relative_path):
    if relative_path and not os.path.isabs(relative_path):
        relative_path = os.path.join(base_path, relative_path)
    return relative_path

def to_absolute_path_for_list(base_path, relative_path_list):
    ret = []
    if relative_path_list:
        for relative_path in relative_path_list:
            ret.append(to_absolute_path(base_path, relative_path))
    return ret

def get_file_size(file_path):
    return os.stat(file_path).st_size


def get_files_size(file_path_list):
    total_size = 0
    for path in file_path_list:
        total_size += get_file_size(path)
    return total_size


def get_file_name(file_path):
    return os.path.split(file_path)[-1]


def get_file_name_without_extension(file_path):
    return os.path.splitext(get_file_name(file_path))[0]


def zip_file_with_path_info_list(path_info_list, dst_file_path, debug=False, fixed_modified_time=False):
    zf = zipfile.ZipFile(dst_file_path, "w", zipfile.ZIP_DEFLATED)
    for path_info in path_info_list:
        full_path = path_info['full_path']
        path_in_zip = path_info['path_in_zip']
        file_name = os.path.split(full_path)[-1]
        if file_name == '.DS_Store':
            continue

        assert_if_failed(os.path.exists(full_path), "The path (%s) doesn't exist!" % full_path)
        zf.write(full_path, path_in_zip, None, fixed_modified_time)
    zf.close()

    if debug:
        for info in zf.infolist():
            LOGD(TAG, "===========================")
            LOGD(TAG, "filename:" + info.filename)
            LOGD(TAG, "date_time:" + str(info.date_time))
            LOGD(TAG, "compress_type:" + str(info.compress_type))
            LOGD(TAG, "comment:" + info.comment)
            LOGD(TAG, "extra:" + info.extra)
            LOGD(TAG, "create_system:" + str(info.create_system))
            LOGD(TAG, "create_version:" + str(info.create_version))
            LOGD(TAG, "extract_version:" + str(info.extract_version))
            LOGD(TAG, "reserved:" + str(info.reserved))
            LOGD(TAG, "flag_bits:" + str(info.flag_bits))
            LOGD(TAG, "volume:" + str(info.volume))
            LOGD(TAG, "internal_attr:" + str(info.internal_attr))
            LOGD(TAG, "external_attr:" + str(info.external_attr))
            LOGD(TAG, "header_offset:" + str(info.header_offset))
            LOGD(TAG, "CRC:" + str(info.CRC))
            LOGD(TAG, "compress_size: " + str(info.compress_size))
            LOGD(TAG, "file_size:" + str(info.file_size))

    LOGD(TAG, 'zip_files: zipping %s  finished!' % dst_file_path)


def zip_files(dir_path, full_file_path_list, dst_file_path, fixed_modified_time=False):
    new_file_list = []
    for full_path in full_file_path_list:
        new_file_list.append({'full_path': full_path, 'path_in_zip': full_path[len(dir_path)+1:]})

    zip_file_with_path_info_list(new_file_list, dst_file_path, False, fixed_modified_time)


def unzip_file(dir_name, zip_file, dst_file_path):
    file_path = os.path.join(dir_name, zip_file)
    if not os.path.exists(file_path):
        raise ErrorMessage("Cannot find %s" % file_path)

    if not os.path.exists(dst_file_path):
        os.mkdir(dst_file_path)

    zf = zipfile.ZipFile(file_path, "r")
    zf.extractall(dst_file_path)

    LOGD(TAG, 'unzip_files: unzipping %s  finished!' % file_path)


def unzip_files(zip_file_list, dst_file_path):
    assert_if_failed(unzip_file, "unzip_file cannot be None")

    for zip_file_path in zip_file_list:
        path, file_name = os.path.split(zip_file_path)
        unzip_file(path, file_name, os.path.join(dst_file_path, os.path.splitext(file_name)[0]))


def zip_folder(src, dst_file_path, with_root_folder, re_skip_list=None, fixed_modified_time=False):
    zf = zipfile.ZipFile(dst_file_path, "w", zipfile.ZIP_DEFLATED)
    abs_src = os.path.abspath(src)
    for dirname, subdirs, files in os.walk(abs_src):
        for filename in files:
            absname = os.path.abspath(os.path.join(dirname, filename))
            if not with_root_folder:
                arcname = absname[len(abs_src) + 1:]
            else:
                arcname = absname[len(os.path.dirname(dirname)) + 1:]
                # LOGD(TAG, 'zipping %s as %s' % (os.path.join(dirname, filename), arcname)

            if re_skip_list:
                to_skip = False
                for re_skip in re_skip_list:
                    if re.search(re_skip, arcname):
                        to_skip = True
                        break

                if to_skip:
                    continue

            file_name = os.path.split(arcname)[-1]
            if file_name == '.DS_Store':
                continue
            zf.write(absname, arcname, None, fixed_modified_time)
    zf.close()
    LOGI(TAG, 'zip_folder: zipping %s  finished!' % dst_file_path)


# Returns True of callback indicates to stop iteration
def deep_iterate_dir(root_dir, callback, to_iter=True):
    if not is_dir(root_dir):
        LOGW(TAG, root_dir + " doesn't exist")
        return

    for lists in os.listdir(root_dir):
        path = root_dir + '/' + lists
        if os.path.isdir(path):
            if not to_iter:
                LOGD(TAG, "*** Skip sub directory: " + path)
                continue
            if callback(path, True):
                return True
            else:
                if deep_iterate_dir(path, callback, to_iter):
                    return True
        elif os.path.isfile(path):
            if callback(path, False):
                return True
    return False


def is_folder_in_dir(file_name, dir_path):
    file_path = os.path.join(dir_path, file_name)
    if os.path.exists(file_path):
        if os.path.isdir(file_path):
            return True

    return False


def is_file_in_dir(file_name, dir_path):
    file_path = os.path.join(dir_path, file_name)
    if os.path.exists(file_path):
        if os.path.isfile(file_path):
            return True

    return False

def is_file(file_path):
    if file_path and os.path.exists(file_path) and os.path.isfile(file_path):
        return True
    return False

def is_dir(dir_path):
    if dir_path and os.path.exists(dir_path) and os.path.isdir(dir_path):
        return True
    return False

def get_folder_list_in_dir(dir_path):
    ret = []
    for file_name in os.listdir(dir_path):
        file_path = os.path.join(dir_path, file_name)
        if os.path.exists(file_path):
            if os.path.isdir(file_path):
                ret.append(file_name)

    return ret


def get_file_list_in_dir(dir_path, ext):
    ret = []
    for file_name in os.listdir(dir_path):
        n, e = os.path.splitext(file_name)
        is_zip_file = False
        if type(ext) == str:
            if e == ext:
                is_zip_file = True
        elif type(ext) == list:
            if e in ext:
                is_zip_file = True
        if is_zip_file:
            ret.append(file_name)
    return ret


def find_file_in_dir(dir_path, re_file_name):
    if not is_dir(dir_path):
        return False

    found_file_list = []
    def callback(path, is_dir):
        if is_dir:
            return False

        if re.search(re_file_name, path):
            found_file_list.append(path)

        return False

    deep_iterate_dir(dir_path, callback)

    return found_file_list


def get_zip_file_in_dir(dir_path):
    return get_file_list_in_dir(dir_path, ['.zip', '.cpk'])


def md5sum(file_path, block_size=65536):
    hash_value = hashlib.md5()
    with open(file_path, "r+b") as f:
        for block in iter(lambda: f.read(block_size), ""):
            hash_value.update(block)
    return hash_value.hexdigest()


def dump_object_to_json_file(obj, json_file_path, to_sort_keys=True, ascii=False):
    # Uses codecs module to fix error when dumping chinese characters.
    # It will trigger the following error if codecs wasn't used!
    # UnicodeEncodeError: 'ascii' codec can't encode characters in position 1-13: ordinal not in range(128)
    # We used the patch from http://crazyof.me/blog/archives/1533.html
    with codecs.open(json_file_path, "wb", "utf-8") as f:
        json.dump(obj, f, sort_keys=to_sort_keys, indent=4, ensure_ascii=ascii)


def read_object_from_json_file(json_file_path):
    ret = None
    with open(json_file_path, 'rb') as json_file:
        ret = json.load(json_file)
    return ret


def recreate_folder(folder_path):
    if os.path.exists(folder_path) and os.path.isdir(folder_path):
        shutil.rmtree(folder_path)

    os.makedirs(folder_path)


def recreate_folders(folder_path_list):
    for folder_path in folder_path_list:
        recreate_folder(folder_path)


def ensure_folder_exists(folder_path):
    if not os.path.exists(folder_path):
        os.makedirs(folder_path)


def ensure_key_exists_in_dict(dict, key):
    if key not in dict:
        raise Exception(key + " doesn't exist in " + str(dict))

def safe_remove_file(path):
    assert_if_failed(path, "safe_remove_file with null argument!")

    if os.path.isdir(path):
        LOGD(TAG, "removing dir: " + path)
        shutil.rmtree(path)
    elif os.path.isfile(path):
        LOGD(TAG, "removing file: " + path)
        os.remove(path)
    else:
        LOGD(TAG, "safe_remove_file( %s ) doesn't exist!" % path)

def safe_remove_files(file_path_list):
    for file_path in file_path_list:
        safe_remove_file(file_path)

def utf8_to_unicode(utf8_str):
    return utf8_str.decode('utf-8')


def unicode_to_utf8(unicode_str):
    return unicode_str.encode('utf-8')


def get_duplicate_items_in_list(l):
    d = {}
    for elem in l:
        if elem in d:
            d[elem] += 1
        else:
            d[elem] = 1

    return [x for x, y in d.items() if y > 1]


def copytree(src, dst, symlinks=False, ignore=None):
    if not os.path.exists(dst):
        os.makedirs(dst)
        shutil.copystat(src, dst)
    lst = os.listdir(src)
    if ignore:
        excl = ignore(src, lst)
        lst = [x for x in lst if x not in excl]
    for item in lst:
        s = os.path.join(src, item)
        d = os.path.join(dst, item)
        if symlinks and os.path.islink(s):
            if os.path.lexists(d):
                os.remove(d)
            os.symlink(os.readlink(s), d)
            try:
                st = os.lstat(s)
                mode = stat.S_IMODE(st.st_mode)
                os.lchmod(d, mode)
            except:
                pass  # lchmod not available
        elif os.path.isdir(s):
            copytree(s, d, symlinks, ignore)
        else:
            shutil.copy2(s, d)


def replace_string_in_file(file_path, cb_get_new_line):
    assert_if_failed(cb_get_new_line is not None, "cb argument for replace_string_in_file is None!")
    infile = open(file_path)
    outfile_path = file_path + '.tmp'
    outfile = open(outfile_path, 'w')
    for line in infile:
        new_line = cb_get_new_line(line)
        assert_if_failed(new_line is not None, "Callback function return None!")
        if len(new_line) > 0:
            outfile.write(new_line)
    infile.close()
    outfile.close()
    os.remove(file_path)
    shutil.move(outfile_path, file_path)


def remove_last_slash(path):
    while path.endswith('/') or path.endswith('\\'):
        path = path[:len(path)-1]
    return path


def to_unix_path_and_remove_last_slash(path):
    return remove_last_slash(to_unix_path(path))
    

def bsdiff(old_verison, new_version, patch_file):
    assert_if_failed(old_verison, "Cannot find the %s" % old_verison)
    assert_if_failed(new_version, "Cannot find the %s" % new_version)

    LOGI(TAG, "%s %s %s %s" % (Constants.BSDIFF_PATH, old_verison, new_version, patch_file))

    run_command(
        "%s %s %s %s" % (Constants.BSDIFF_PATH, old_verison, new_version, patch_file),
        error_message="Create runtime patch error",
        success_message="Create runtime patch success"
    )

def read_file_content(file_path, encoding="UTF-8"):
    assert_if_failed(is_file(file_path), "read_file_content: Cannot find %s" % file_path)
    return codecs.open(file_path, "rb", encoding=encoding).read()

def write_file_content(file_path, content, encoding="UTF-8"):
    codecs.open(file_path, "wb", encoding=encoding).write(content)

def find_string_from_file(file_path, regexp, encoding="UTF-8"):
    content = read_file_content(file_path, encoding)
    match = re.search(regexp, content)
    if match:
        return match.group(1)
    return None

def generate_apk_by_gradle(proj_dir):
    LOGI(TAG, "Generating APK from " + os.path.normpath(proj_dir))
    assert_if_failed(is_dir(proj_dir), "(%s) doesn't exist")
    LOGI(TAG, "Checking gradle path...")
    assert_if_failed("GRADLE_HOME" in os.environ, "Cannot find GRADLE_HOME path in the system environment")
    # setting gradle path
    gradle_home = os.environ["GRADLE_HOME"]
    gradle = os.path.join(gradle_home, "gradle.bat" if win32() else "gradle")
    run_command("%s clean assemblerelease " % gradle +
                       "-p %s" % proj_dir)

def generate_apk_by_ant(proj_dir, key_config):
    LOGI(TAG, "Generating CocosRuntime Demo APK file...")
    LOGI(TAG, "Checking android sdk and ant path...")
    assert_if_failed("ANDROID_HOME" in os.environ, "Cannot find ANDROID_HOME path in the system environment")
    assert_if_failed("ANT_ROOT" in os.environ, "Cannot find ANT_ROOT path in the system environment")

    # setting the android sdk and ant path
    android_sdk_root = os.environ["ANDROID_HOME"]
    ant_root = os.environ["ANT_ROOT"]

    build_xml = os.path.join(proj_dir, "build.xml")
    if not os.path.exists(build_xml):
        android = os.path.join(android_sdk_root, "tools", "android.bat" if win32() else "android")
        run_command(
            "%s update project -p %s" % (android, proj_dir),
            error_message="Update project failed",
            success_message="Update project successful"
        )

    ant = os.path.join(ant_root, "ant.bat" if win32() else "ant")

    run_command(
        "%s clean release " % ant +
        "-f %s " % proj_dir +
        "-Dkey.store=%s " % key_config["key_store"] +
        "-Dkey.alias=%s " % key_config["key_alias"] +
        "-Dkey.store.password=%s " % key_config["key_password"] +
        "-Dkey.alias.password=%s " % key_config["key_alias_password"]
    )


class FileStringReplacer(object):
    def __init__(self, file_path, encoding="UTF-8"):
        self.encoding = encoding
        self.file_path = file_path
        assert_if_failed(is_file(self.file_path), "Cannot find %s" % self.file_path)
        # LOGI(TAG, "Construct FileStringReplacer from (%s)" % os.path.normpath(self.file_path))
        self.origin_str = codecs.open(self.file_path, "rb", encoding=self.encoding).read()
        self.temp_str = self.origin_str

    def replace_string(self, old_str, new_str):
        ret = self.temp_str.replace(old_str, new_str)
        if ret == self.temp_str:
            LOGW(TAG, "FileStringReplacer:replace_string take no effect!")
        self.temp_str = ret

    def replace_regexp(self, regexp, replace_str):
        ret = re.sub(regexp, replace_str, self.temp_str)
        if ret == self.temp_str:
            LOGW(TAG, "FileStringReplacer:replace_regexp take no effect!")
        self.temp_str = ret

    def _flush_string(self, content):
        codecs.open(self.file_path, "wb", encoding=self.encoding).write(content)

    def flush(self):
        # LOGI(TAG, "Flushing changes to " + os.path.normpath(self.file_path))
        self._flush_string(self.temp_str)

    def revert(self):
        LOGD(TAG, "Reverting " + os.path.normpath(self.file_path))
        self._flush_string(self.origin_str)
