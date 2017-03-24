#!/usr/bin/env python
# coding=utf-8

import sys
import os
import shutil

sys.path.append(os.path.join(os.path.dirname(os.path.realpath(__file__)), '..', 'common'))
import common

_creator_list = []

class UnityRuntimeCreator(object):
    def __init__(self, opts):
        self.TAG = "UnityRuntimeCreator"
        common.LOGI(self.TAG, "opts: " + str(opts))
        self.build_sdk = opts["build_sdk"]
        self.workspace = opts["workspace"]
        self.script_path = opts["script_path"]
        self.target = opts["target"]
        if not self.target:
            self.target = "online"

        self.build_replacer_list = []

        self.proj_unityruntime_dir = os.path.join(self.script_path, "../../framework/UnityRuntime")
        self.proj_gplayenginebridge_dir = os.path.join(self.script_path, "../../framework/GplayEngineBridge")

        self.proj_unityruntime_dir_src_dir = os.path.join(self.proj_unityruntime_dir, "src", "main", "java")
        self.proj_gplayenginebridge_src_dir = os.path.join(self.proj_gplayenginebridge_dir, "src", "main", "java")

        self.proj_unityruntime_build_dir = os.path.join(self.proj_unityruntime_dir, "build")
        self.proj_gplayenginebridge_build_dir = os.path.join(self.proj_gplayenginebridge_dir, "build")

        self.version = self._get_version()
        self.versionName = self._get_version_name().replace('"', "")
        common.LOGI(self.TAG, "Runtime SDK versionCode: " + self.version)

        if not opts["out"]:
            self.out_dir = os.path.join(self.workspace, 'out-unityruntime-sdk')
            self.out_dir = os.path.join(self.out_dir, self._get_version_name().replace('"', ""))
            common.recreate_folder(self.out_dir)
            self.debug_directory = os.path.join(self.workspace, 'debug')
        else:
            self.out_dir = os.path.join(opts["out"], self._get_version_name().replace('"', ""))
            self.debug_directory = os.path.join(self.out_dir, 'debug')
        self.out_dir_temp = os.path.join(self.workspace, "out-unityruntime-sdk-temp")
        common.recreate_folder(self.out_dir_temp)

        self.sdk_file_for_runtimesdk_no_dex = os.path.join(self.out_dir, "libunityruntime-not-dex-" + self.versionName + ".jar")
        self.sdk_file_for_runtimesdk = os.path.join(self.out_dir, "libunityruntime-" + self.versionName + ".jar")

        self.sdk_jar_no_dex_obfuscated_path = os.path.join(self.out_dir_temp, "libunityruntime-sdk-no-dex-obfuscated.jar")
        self.sdk_jar_dex_path = os.path.join(self.out_dir_temp, "libunityruntime-sdk-dex.jar")
        self.output_class_path = os.path.join(self.out_dir_temp, "classes")
        self.proguard = not opts["dont_proguard"]

    def _get_version(self):
        # Get the version of current SDK
        return common.find_string_from_file(
            os.path.join(self.proj_unityruntime_dir_src_dir, "com", "skydragon", "gplay", "runtime", "bridge", "CocosRuntimeBridge.java"),
            r"private\s+static\s+final\s+int\s+VERSION_CODE\s*=\s*(.*)\s*;"
        )

    def _get_version_name(self):
        versionName =  common.find_string_from_file(
            os.path.join(self.proj_unityruntime_dir_src_dir, "com", "skydragon", "gplay", "runtime", "bridge", "CocosRuntimeBridge.java"),
            r"private\s+static\s+final\s+String\s+VERSION\s*=\s*(.*);"
        )
        return versionName

    def _clean(self):
        common.LOGI(self.TAG, "Cleaning ...")

        file_to_remove_list = [
            self.out_dir_temp,
            self.proj_unityruntime_build_dir,
            self.proj_gplayenginebridge_build_dir
        ]

        common.safe_remove_files(file_to_remove_list)


    def _generate_cut_jar(self, jar_path, config, out_dir):
        '''Invoke JarCutter tool to customize jar file'''
        # Ensure the our dir is empty
        common.recreate_folder(out_dir)

        # Run the JarCutter
        common.run_command(
            "python " + os.path.join(self.script_path, "../jar-cutter/JarCutter.py") +
            " -c " + config +
            " -s " + jar_path +
            " -o " + out_dir,
            error_message="cutting jar file (%s) failed!" % jar_path
        )

    def _generate_unityruntime_jar_file(self, force_dont_proguard=False):
        common.LOGI(self.TAG, "Generating runtime sdk jar file ..., force_dont_proguard:" + str(force_dont_proguard))

        common.ensure_folder_exists(self.debug_directory)

        proguard_file_path = "None"
        mapping_file_path = "None"
        if not force_dont_proguard and self.proguard:
            proguard_file_path = os.path.join(self.script_path, "proguard-unityruntime.txt")
            mapping_file_path = os.path.join(self.debug_directory, "mapping-libunityruntime-" + self.version + ".txt")

        common.run_command(
            "python " + os.path.join(self.script_path, "../jar-maker/JarMaker.py") +
            " -s " + self.proj_unityruntime_dir_src_dir +
            " -s " + self.proj_gplayenginebridge_src_dir +
            " -o " + self.out_dir_temp +
            " -f " + os.path.split(self.sdk_jar_no_dex_obfuscated_path)[-1] +
            " --ref-lib " + os.path.join(self.script_path, "..", "common", "lib", "android.jar") +
            " -p " + proguard_file_path +
            " -m " + mapping_file_path
        )

    def _generate_cut_jar_for_unityruntime_sdk_dex(self):
        common.LOGI(self.TAG, "Generating jar file for unityruntime sdk ...")
        out_jar_file_name = common.get_file_name(self.sdk_jar_no_dex_obfuscated_path)
        jar_cutter_out_dir = os.path.join(self.out_dir_temp, "jar-cutter-out-for-unityruntime-dex")
        jar_cutter_config_path = os.path.join(self.script_path, "jar-cutter-for-unityruntime-dex.json")
        self._generate_cut_jar(self.sdk_jar_no_dex_obfuscated_path, jar_cutter_config_path, jar_cutter_out_dir)
        cut_jar_file_path = os.path.join(jar_cutter_out_dir, out_jar_file_name)
        common.generate_dex_jar(cut_jar_file_path, self.sdk_jar_dex_path)
        # Copy dex sdk file to the output directory
        shutil.copy(self.sdk_jar_dex_path, self.sdk_file_for_runtimesdk)
        shutil.copy(self.sdk_jar_no_dex_obfuscated_path, self.sdk_file_for_runtimesdk_no_dex)
        # Remove the dex file since it may be generated with the same name
        # in next step (I means generating cut jar for tencent)
        common.safe_remove_file(self.sdk_jar_dex_path)

    def revert(self):
        for replacer in self.build_replacer_list:
            replacer.revert()
        return

    def run(self):
        common.LOGI(self.TAG, "RUNNING ...")
        # Clean temp files before generate classes files
        self._clean()

        if self.build_sdk == "all" or self.build_sdk == "runtime":
            # Generate runtime for normal channel
            self._generate_unityruntime_jar_file()
            self._generate_cut_jar_for_unityruntime_sdk_dex()
            self.revert()
            
        # clean after build
        self._clean()

        common.LOGI(self.TAG, "DONE!")


def main():
    common.LOGI("SDKCreator.py:main", "main entry ...")

    from optparse import OptionParser

    parser = OptionParser()

    build_sdk = ("all", "gplay", "runtime", "cocosv2", "cocosv3")

    build_target = ("online", "sandbox", "develop")

    parser.add_option("-t", "--target",
                      action="store", type="choice", choices=build_target, dest="target", default="online",
                      help="SDK run in target to be built, could be " + "'" + "', '".join(build_target) + "'")

    parser.add_option("-o", "--out",
                      action="store", type="string", dest="out", default=None,
                      help="The output folder")

    parser.add_option("-b", "--build-sdk",
                      action="store", type="choice", choices=build_sdk,
                      dest="build_sdk", default="all",
                      help="SDK kind to be built, could be " + "'" + "', '".join(build_sdk) + "'")

    parser.add_option("", "--dont-proguard",
                      action="store_true", dest="dont_proguard", default=False,
                      help="Whether to obfuscate java codes")

    (opts, args) = parser.parse_args()

    common.assert_if_failed(len(args) == 0, "Don't add parameters without `-t`,`-b` and `--dont-proguard`")

    opts = vars(opts)
    opts['workspace'] = os.path.abspath(os.getcwd())
    opts['script_path'] = os.path.dirname(os.path.realpath(__file__))

    if opts["build_sdk"] == "all" or opts["build_sdk"] == "unityv5":
        opts["engineVersion"] = "V5"
        unity_creator = UnityRuntimeCreator(opts)
        _creator_list.append(unity_creator)
        unity_creator.run()


if __name__ == '__main__':
    try:
        main()
    except Exception as e:
        # raceback.print_exc()
        #for creator in _creator_list:
        #    creator.revert()

        if e.__class__.__name__ == "ErrorMessage":
            common.LOGE("SDKCreator.py", e.message)
            sys.exit(1)
        else:
            raise
