#!/usr/bin/env python
# coding=utf-8
import sys
import os
import shutil
import re

class JarCutter:
    def __init__(self, opts):
        self.TAG = "JarCutter"
        self.workspace = opts['workspace']
        common.LOGI(self.TAG, "workspace: " + self.workspace)
        self.src = common.to_absolute_path(self.workspace, opts['src'])
        self.out = common.to_absolute_path(self.workspace, opts['out'])
        self.temp_dir = os.path.join(self.out, 'temp')
        self.config = common.read_object_from_json_file(common.to_absolute_path(self.workspace, opts['config']))
        self.out_jar_folder_list = None

    def extract_jar_files(self):
        common.LOGI(self.TAG, "Extract jar files in src folder")
        common.extract_jar_files(self.src, self.temp_dir)
        self.out_jar_folder_list = common.get_folder_list_in_dir(self.temp_dir)
        common.assert_if_failed(len(self.out_jar_folder_list) > 0, "There aren't any valid jar files")

    def remove_ignore_class_files(self):
        common.LOGI(self.TAG, "Remove ignored class files")
        ignore_file_list = self.config['ignore_files']

        to_remove_file_list = []

        for folder_name in self.out_jar_folder_list:
            for ignore_file in ignore_file_list:
                if ignore_file.startswith("#"):
                    is_re_search = True
                    match_word = ignore_file[1:]
                    common.LOGI(self.TAG, "find rex: " + match_word)
                else:
                    is_re_search = False
                    match_word = ignore_file

                def callback(path, is_dir):
                    if is_dir:
                        return False
                    if is_re_search:
                        if re.search(match_word, common.to_unix_path(path)):
                            common.LOGD(self.TAG, "RE (%s) matched: (%s)" % (match_word, path))
                            to_remove_file_list.append(path)
                    else:
                        if common.to_unix_path(path).find(match_word) != -1:
                            common.LOGD(self.TAG, "String (%s) matched: (%s)" % (match_word, path))
                            to_remove_file_list.append(path)

                    return False

                common.deep_iterate_dir(os.path.join(self.temp_dir, folder_name), callback)

        for remove_file in to_remove_file_list:
            common.safe_remove_file(remove_file)

    def repack_jar_files(self):
        common.LOGI(self.TAG, "Re-pack the jar files")
        for folder_name in self.out_jar_folder_list:
            common.generate_jar_file(os.path.join(self.temp_dir, folder_name), os.path.join(self.out, folder_name + '.jar'))

    def clean(self):
        common.LOGI(self.TAG, "Removes temp files")
        common.safe_remove_file(self.temp_dir)

    def run(self):
        common.safe_remove_file(self.out)

        self.extract_jar_files()
        self.remove_ignore_class_files()
        self.repack_jar_files()
        self.clean()
        common.LOGI(self.TAG, "JarCutter RUNNING DONE!")


def main():
    common.LOGI("JarCutter.py", "main entry ...")

    from optparse import OptionParser

    parser = OptionParser()

    parser.add_option("-s", "--src",
                      action="store", type="string", dest="src", default=None,
                      help="The original jar folder path")

    parser.add_option("-o", "--out",
                  action="store", type="string", dest="out", default=None,
                  help="The generated jar folder path")

    parser.add_option("-c", "--config",
              action="store", type="string", dest="config", default=None,
              help="The JSON config file")

    (opts, args) = parser.parse_args()

    common.assert_if_failed(opts.src, "The original jar file path isn't assigned!")
    common.assert_if_failed(opts.config, "The config file path isn't assigned!")

    if opts.out is None:
        opts.out = 'out'

    opts = vars(opts)
    opts['workspace'] = os.path.abspath(os.getcwd())
    cutter = JarCutter(opts)
    cutter.run()

if __name__ == '__main__':
    sys.path.append(os.path.join(os.path.split(os.path.realpath(__file__))[0], '..', 'common'))
    import common
    try:
        main()
    except Exception as e:
        # raceback.print_exc()
        if e.__class__.__name__ == "ErrorMessage":
            common.LOGE("JarCutter.py", e.message)
            sys.exit(1)
        else:
            raise
