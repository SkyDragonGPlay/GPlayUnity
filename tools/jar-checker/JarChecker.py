#!/usr/bin/env python
# coding=utf-8
import sys
import os
import shutil
import re

class JarChecker:
    def __init__(self, opts):
        self.TAG = "JarChecker"
        common.LOGI(self.TAG, "opts: " + str(opts))
        self.workspace = opts['workspace']
        self.src = common.to_absolute_path(self.workspace, opts['src'])
        self.temp_dir = os.path.join(self.workspace, 'temp')
        self.out_jar_folder_list = None
        self.classes_dir_list = []

    def extract_jar_files(self):
        common.LOGI(self.TAG, "Extract jar files in src folder")
        common.extract_jar_files(self.src, self.temp_dir)
        self.out_jar_folder_list = common.get_folder_list_in_dir(self.temp_dir)
        common.assert_if_failed(len(self.out_jar_folder_list) > 0, "There aren't any valid jar files")

    def convert_to_class_files(self):

        for jar_folder_name in self.out_jar_folder_list:
            jar_folder_path = os.path.join(self.temp_dir, jar_folder_name)
            unzip_classes_folder_path = os.path.join(jar_folder_path, "classes")
            self.classes_dir_list.append(unzip_classes_folder_path)
            dex_file_path = os.path.join(jar_folder_path, "classes.dex")
            common.run_command("dex2jar " + dex_file_path)
            common.unzip_file(jar_folder_path, "classes_dex2jar.jar", unzip_classes_folder_path)


    def get_file_list_of_relative_path_list(self, dir_path):

        ret = []

        def callback(path, is_dir):
            if not is_dir:
                ret.append(path[len(dir_path) + 1:])
            return False

        common.deep_iterate_dir(dir_path, callback)

        return ret

    def compare(self):

        all_files = []

        for classes_dir in self.classes_dir_list:
            files = self.get_file_list_of_relative_path_list(classes_dir)
            all_files += files

        duplicated_class = set()

        remove_duplicated_list = list(set(all_files))

        for file_path in remove_duplicated_list:
            count = 0
            for e in all_files:
                if e == file_path:
                    count += 1
            if count > 1:
                duplicated_class.add(file_path)

        common.LOGI(self.TAG, "======================================================")
        if len(duplicated_class) > 0:
            common.LOGE(self.TAG, "Duplicated classes: " + str(duplicated_class))
        else:
            common.LOGI(self.TAG, "Congratulations, there aren't any duplicated classes!")
        common.LOGI(self.TAG, "======================================================")

    def clean(self):
        common.LOGI(self.TAG, "Removing temp files ...")
        common.safe_remove_file(self.temp_dir)

    def run(self):
        common.LOGI(self.TAG, "RUNNING ...")

        common.safe_remove_file(self.temp_dir)

        self.extract_jar_files()
        self.convert_to_class_files()
        self.compare()
        self.clean()
        common.LOGI(self.TAG, "DONE!")


def main():
    common.LOGI("JarChecker.py", "main entry ...")

    from optparse import OptionParser

    parser = OptionParser()

    parser.add_option("-s", "--src",
                      action="store", type="string", dest="src", default=None,
                      help="The original jar folder path")


    (opts, args) = parser.parse_args()

    common.assert_if_failed(opts.src, "The original jar file path isn't assigned!")

    opts = vars(opts)
    opts['workspace'] = os.path.abspath(os.getcwd())
    cutter = JarChecker(opts)
    cutter.run()

if __name__ == '__main__':
    sys.path.append(os.path.join(os.path.split(os.path.realpath(__file__))[0], '..', 'common'))
    import common
    try:
        main()
    except Exception as e:
        # raceback.print_exc()
        if e.__class__.__name__ == "ErrorMessage":
            common.LOGE("JarChecker.py", e.message)
            sys.exit(1)
        else:
            raise
