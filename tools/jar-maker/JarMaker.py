#!/usr/bin/env python
# coding=utf-8
import sys
import os
import shutil

class JarMaker(object):
    '''JarMaker is for generating a jar file from Java source code and lib files'''
    def __init__(self, opts):
        self.TAG = "JarMaker"
        common.LOGI(self.TAG, "opts:" + str(opts))
        self.workspace = opts["workspace"]
        self.src_list = []
        for src in opts["src_list"]:
            common.LOGI(self.TAG, "src:" + src)
            self.src_list.append(common.to_absolute_path(self.workspace, src))

        self.temp = os.path.join(self.workspace, "jar-maker-temp")
        self.output_class_path = os.path.join(self.temp, "classes")
        self.out = common.to_absolute_path(self.workspace, opts["out"])
        common.ensure_folder_exists(self.out)

        self.out_jar_file_no_obfuscated_temp = os.path.join(self.temp, "libxxx-no-obfuscated.jar")
        self.out_jar_file_obfuscated_temp = os.path.join(self.temp, "libxxx-obfuscated.jar")
        self.out_jar_file_final = common.to_absolute_path(self.out, opts["out_file_name"])
        common.safe_remove_file(self.out_jar_file_final)

        self.out_jar_file_name_without_suffix = os.path.splitext(opts["out_file_name"])[0]
        self.assets_dir = common.to_absolute_path(self.workspace, opts["assets_dir"])
        self.extract_lib_list = common.to_absolute_path_for_list(self.workspace, opts["extract_lib_list"])
        self.extract_lib_list_join_proguard = common.to_absolute_path_for_list(self.workspace, opts["extract_lib_list_join_proguard"])
        self.ref_lib_list = self.extract_lib_list + self.extract_lib_list_join_proguard + common.to_absolute_path_for_list(self.workspace, opts["ref_lib_list"])
        self.out_mapping_file_path = common.to_absolute_path(self.workspace, opts["out_mapping_file"])
        self.proguard_file_path = common.to_absolute_path(self.workspace, opts["proguard_file"]) if opts["proguard_file"] else None

    def _do_proguard(self):
        '''Proguard jar file according proguard file'''
        common.LOGI(self.TAG, "Obfuscating Java code ...")

        common.proguard_jar_file(
            self.proguard_file_path,
            injars=os.path.join(self.workspace, "jar-maker-temp/libxxx-no-obfuscated.jar"),
            outjars=os.path.join(self.workspace, "jar-maker-temp/libxxx-obfuscated.jar"),
            libraryjars=self.ref_lib_list,
            printmapping=os.path.join(self.workspace, "jar-maker-temp/mapping.txt")
        )

        proguard_mapping_path = os.path.join(self.temp, "mapping.txt")
        if common.is_file(proguard_mapping_path):
            debug_directory = os.path.join(self.workspace, "debug")
            common.ensure_folder_exists(debug_directory)
            shutil.move(proguard_mapping_path, self.out_mapping_file_path)
        else:
            common.LOGW(self.TAG, "mapping file (%s) doesn't exist, please check!" % proguard_mapping_path)

    def _copy_assets(self):
        if self.assets_dir:
            IGNORE_PATTERNS = ('*.pyc', 'CVS', '.git', '.idea', '.svn', '.DS_Store')
            ignore_pattern = shutil.ignore_patterns(*IGNORE_PATTERNS)
            common.LOGI(self.TAG, "Copying assets ...")
            common.copytree(self.assets_dir, os.path.join(self.output_class_path, "assets"), ignore=ignore_pattern)

    def _generate_class_files(self):
        '''Generate .class files from Java source code or jar file '''
        common.LOGI(self.TAG, "Generating *.class files ...")

        # generate sdk class files
        common.generate_class_files(
            self.src_list,
            self.output_class_path,
            self.ref_lib_list
        )

    def _generate_jar_file(self):
        common.LOGI(self.TAG, "Packing all .class files to a jar file")
        self._copy_assets()
        common.generate_jar_file(self.output_class_path, self.out_jar_file_no_obfuscated_temp)

    def _do_proguard_or_not(self):
        # Whether to obfuscate java code
        if self.proguard_file_path:
            self._do_proguard()
        else:
            # if dont proguard, rename the non-obfuscated jar file to obfuscated file name
            # to keep the same logic after this step
            shutil.move(self.out_jar_file_no_obfuscated_temp, self.out_jar_file_obfuscated_temp)

    def _extract_3rd_jar_to_classes_dir(self, lib_list):
        common.LOGI(self.TAG, "Extract extra jar files to classes folder")
        if len(lib_list) > 0:
            for lib_path in lib_list:
                common.extract_jar_files(lib_path, self.output_class_path, False)
        else:
            common.LOGD(self.TAG, "No extra 3rd libraries for being packed into jar!")

    def _clean(self):
        '''Clean temporary files'''
        common.LOGI(self.TAG, "Cleaning ...")
        common.safe_remove_file(self.temp)

    def run(self):
        '''Run JarMaker'''
        common.LOGI(self.TAG, "RUNNING ...")
        self._clean()

        self._generate_class_files()

        self._extract_3rd_jar_to_classes_dir(self.extract_lib_list_join_proguard)
        self._generate_jar_file() # out: self.out_jar_file_no_obfuscated_temp
        self._do_proguard_or_not() # in: self.out_jar_file_no_obfuscated_temp, out: self.out_jar_file_obfuscated_temp

        if self.extract_lib_list and len(self.extract_lib_list) > 0:
            common.LOGI(self.TAG, "Generating sdk jar file which packed with 3rd libraries ...")
            # re-create `classes` folder since we have already generate the jar file
            # and the jar file will be extracted in the next step, so we need to clean `classes` folder before that
            common.recreate_folder(self.output_class_path)
            self._extract_3rd_jar_to_classes_dir(self.extract_lib_list)
            # Extract sdk jar file
            common.extract_jar_files(self.out_jar_file_obfuscated_temp, self.output_class_path, False)
            # Generate new sdk jar file that includes third party jar libraries
            common.generate_jar_file(self.output_class_path, self.out_jar_file_final)
        else:
            shutil.move(self.out_jar_file_obfuscated_temp, self.out_jar_file_final)

        self._clean()
        common.LOGI(self.TAG, "DONE!")

def main():
    '''JarMaker entry function'''
    common.LOGI("JarMaker.py:main", "main entry ...")

    from optparse import OptionParser

    parser = OptionParser()

    parser.add_option("-s", "--src",
                      action="append", type="string", dest="src_list", default=None,
                      help="Java source folder")

    parser.add_option("-o", "--out",
                      action="store", type="string", dest="out", default="out",
                      help="The output folder")

    parser.add_option("-f", "--out-file-name",
                      action="store", type="string", dest="out_file_name", default=None,
                      help="The output file name")

    parser.add_option("", "--ref-lib",
                      action="append", type="string", dest="ref_lib_list", default=None,
                      help="The reference librariy list")

    parser.add_option("", "--extract-lib",
                      action="append", type="string", dest="extract_lib_list", default=None,
                      help="The librariy path for extracting to classes directory")

    parser.add_option("", "--extract-lib-join-proguard",
                      action="append", type="string", dest="extract_lib_list_join_proguard", default=None,
                      help="The librariy path for extracting to classes directory, it'll join proguard")

    parser.add_option("-a", "--assets-dir",
                      action="store", type="string", dest="assets_dir", default=None,
                      help="The `assets` directory to be packed into jar file` ")

    parser.add_option("-p", "--proguard-file",
                      action="store", dest="proguard_file", default=None,
                      help="proguard file path")

    parser.add_option("-m", "--out-mapping-file",
                      action="store", dest="out_mapping_file", default=None,
                      help="The mapping file after proguard")

    (opts, args) = parser.parse_args()

    common.assert_if_failed(opts.out, "-o argument isn't assigned!")
    common.assert_if_failed(opts.out_file_name, "-f argument isn't assigned!")

    if opts.proguard_file == "None":
        opts.proguard_file = None

    if opts.out_mapping_file == "None":
        opts.out_mapping_file = None

    if opts.proguard_file:
        common.assert_if_failed(opts.out_mapping_file, "-m argument isn't assigned!")

    opts = vars(opts)
    opts['workspace'] = os.path.abspath(os.getcwd())

    maker = JarMaker(opts)
    maker.run()


if __name__ == '__main__':
    sys.path.append(os.path.join(os.path.split(os.path.realpath(__file__))[0], '..', 'common'))
    import common
    try:
        main()
    except Exception as e:
        # raceback.print_exc()
        if e.__class__.__name__ == "ErrorMessage":
            common.LOGE("JarMaker.py", e.message)
            sys.exit(1)
        else:
            raise
