#!/usr/bin/env python
# coding=utf-8
import sys
import os
import shutil
import time

# .docx should not be copied
IGNORE_PATTERNS = ('bin', 'build', 'gen', '*.pyc', 'CVS', '.git', '.idea', '.svn', '.DS_Store',
                   '*.docx', '*.iml', '*.keystore', ".settings", "lib", "ant.properties")
ignore_pattern = shutil.ignore_patterns(*IGNORE_PATTERNS)
_success = False

class Publisher(object):

    def __init__(self, opts):
        self.TAG = "Publisher"
        common.LOGI(self.TAG, "opts:" + str(opts))
        self.workspace = opts["workspace"]
        self.use_gradle = opts["use_gradle"]
        self.git_root_dir = os.path.join(self.workspace, "../..")
        current_time = time.localtime(time.time())
        self.current_time_day = time.strftime('%y%m%d', current_time)
        self.current_time_second = time.strftime('%y%m%d%H%M%S', current_time)
        self.out_dir = os.path.join(self.workspace, "publish", "SDK_" + self._get_runtime_sdk_version_name() + "_" + self.current_time_second,)
        common.ensure_folder_exists(self.out_dir)
        self.publish_info_list = None
        self._init_publish_info_list()

    def _get_runtime_sdk_version_name(self):
        return common.find_string_from_file(
            os.path.join(self.workspace, "../../frameworks/GplayRuntimeSDK", "src", "main", "java", "com", "skydragon", "gplay", "runtime", "RuntimeStub.java"),
            r"private\s+static\s+final\s+String\s+VERSION\s*=\s*\"(.*)\"\s*;"
        )

    def _get_runtime_sdk_version_code(self):
        return common.find_string_from_file(
            os.path.join(self.workspace, "../../frameworks/GplayRuntimeSDK", "src", "main", "java", "com", "skydragon", "gplay", "runtime", "RuntimeStub.java"),
            r"private\s+static\s+final\s+int\s+VERSION_CODE\s*=\s*(.*)\s*;"
        )

    def _get_gplay_sdk_version_name(self):
        return common.find_string_from_file(
            os.path.join(self.workspace, "../../../GplaySDK/frameworks/GplaySDK", "src", "main", "java", "com", "skydragon", "gplay", "Gplay.java"),
            r"private\s+static\s+final\s+String\s+VERSION\s*=\s*\"(.*)\";"
        )

    def _init_publish_info_list(self):
        self.publish_info_list = [
            {
                "name": "gplaysdk",
                "demo_project_name": "GplayDemo",
                "out_dir": os.path.join(
                    self.out_dir,
                    common.utf8_to_unicode("Gplay_SDK_") + self._get_gplay_sdk_version_name()
                ),

                "copy": [
                    {"from": "CHANGELOG", "to": ""},
                    {"from": "samples/GplayDemo", "to": "GplayDemo"},
                    {"from": "tools/sdk-creator/out-gplaysdk/libgplaysdk.jar", "to": "GplayDemo/libs"},
                    {"from": "tools/sdk-creator/out-gplaysdk/libplaysdk.jar", "to": ""}
                ],
                "rename": [
                    # {"from": "CHANGELOG_NEW", "to": "CHANGELOG"}
                ],
                "replace": [
                    {"file": "GplayDemo/build.gradle", "src": r"compile project\(':frameworks:GplaySDK.*SDK'\)", "dst": ""},
                    {
                        "file": "GplayDemo/build.gradle",
                        "src": r"(apply plugin: 'com\.android\.application')", 
                        "dst": r"""buildscript {
                                    repositories {
                                        mavenCentral()
                                    }
                                    dependencies {
                                        classpath 'com.android.tools.build:gradle:2.1.0'
                                    }
                                }

                                allprojects {
                                    repositories {
                                        mavenCentral()
                                    }
                                }

                                \g<1>"""
                    },
                    {
                        "file": "GplayDemo/build.gradle",
                        "src": r"""(    signingConfigs {
                                        release {)
                                .*
                                .*
                                .*
                                .*
                                (        }
                                    })""",
                                                        "dst":"",
                                                    },

                                                    {
                                                        "file": "GplayDemo/build.gradle",
                                                        "src":
                                r"""    buildTypes {
                                        release {
                                .*
                                .*
                                .*
                                .*
                                .*
                                        }
                                    }""",
                        "dst": ""
                    }

                ]
            }
        ]

    def generate_apk(self, project_name):
        common.LOGI(self.TAG, "Generating apk for (%s)" % project_name)
        use_gradle_arg = " --use-gradle" if self.use_gradle else ""
        common.run_command(
            "python " + "APKCreator.py" +
            " -p " + project_name + use_gradle_arg +
            " --dont-build-sdk", # Don't build SDK since we build it at self.before_run
            success_message="generate_apk (%s) succeed!" % project_name,
            error_message="generate_apk (%s) failed!" % project_name
        )

    def before_run(self):
        common.LOGI(self.TAG, "publish before_run ...")
        common.run_command("python %s" % os.path.join(self.workspace, "SDKCreator.py -b all"))

    def run(self):
        self.before_run()

        generated_apk_dir = os.path.join(self.workspace, "out-apk")

        for publish_info in self.publish_info_list:
            common.LOGI(self.TAG, "Publishing (%s) ..." % publish_info["name"])
            release_out_dir = publish_info["out_dir"]
            common.ensure_folder_exists(release_out_dir)
            demo_project_name = publish_info["demo_project_name"]
            self.generate_apk(demo_project_name)
            # Copy apk
            shutil.copy2(os.path.join(generated_apk_dir, demo_project_name + ".apk"), release_out_dir)

            for file_to_copy in publish_info["copy"]:
                copy_from = os.path.join(self.git_root_dir, common.utf8_to_unicode(file_to_copy["from"]))
                copy_to = os.path.join(release_out_dir, common.utf8_to_unicode(file_to_copy["to"]))
                if common.is_dir(copy_from):
                    common.copytree(copy_from, copy_to, ignore=ignore_pattern)
                else:
                    # common.LOGD(self.TAG, "copyfrom:" + copy_from + ", copy_to:" + copy_to)
                    shutil.copy2(copy_from, copy_to)

            for file_to_rename in publish_info["rename"]:
                rename_from = os.path.join(release_out_dir, common.utf8_to_unicode(file_to_rename["from"]))
                rename_to = os.path.join(release_out_dir, common.utf8_to_unicode(file_to_rename["to"]))
                shutil.move(rename_from, rename_to)

            for file_to_replace in publish_info["replace"]:
                replacer = common.FileStringReplacer(os.path.join(release_out_dir, file_to_replace["file"]))
                replacer.replace_regexp(file_to_replace["src"], file_to_replace["dst"])
                replacer.flush()
            common.LOGI(self.TAG, "Publishing (%s) DONE" % publish_info["name"])

        # zip SDK folder
        zip_file_path = os.path.join(
            self.out_dir,
            common.utf8_to_unicode("Gplay_SDK_V") + self._get_gplay_sdk_version_name() + "_" + self.current_time_day + ".zip"
        )

        common.zip_folder(release_out_dir, zip_file_path, False)
        common.safe_remove_file(release_out_dir)

        # zip package for test
        test_out_dir = os.path.join(self.out_dir, "test_out_dir")
        common.ensure_folder_exists(test_out_dir)
        shutil.copy2(os.path.join(generated_apk_dir, demo_project_name + ".apk"), test_out_dir)
        zip_file_path = os.path.join(
            self.out_dir,
            common.utf8_to_unicode("Gplay_SDK_V") + self._get_gplay_sdk_version_name() + "_For_Test" + ".zip"
        )
        common.zip_folder(test_out_dir, zip_file_path, False)
        common.safe_remove_file(test_out_dir)

        # Copy runtimesdk dex file to out directory
        shutil.copy2(os.path.join(self.workspace, "out-runtimesdk", "libruntime_" + self._get_runtime_sdk_version_code() + ".jar"), self.out_dir)

        global _success
        _success = True


def main():
    from optparse import OptionParser
    parser = OptionParser()

    parser.add_option("", "--use-gradle",
                      action="store_true", dest="use_gradle", default=False,
                      help="Whether to pack .apk by using gradle, if not assigned, use `ant` by default")

    (opts, args) = parser.parse_args()
    opts = vars(opts)
    opts['workspace'] = os.path.dirname(os.path.realpath(__file__))

    publisher = Publisher(opts)
    publisher.run()

if __name__ == '__main__':
    sys.path.append(os.path.join(os.path.split(os.path.realpath(__file__))[0], '..', 'common'))
    import common
    try:
        main()
    except Exception as e:
        if e.__class__.__name__ == "ErrorMessage":
            common.LOGE("publish.py", e.message)
            sys.exit(1)
        else:
            raise
    finally:
        common.LOGI("publish.py", "***************************************************")
        if _success:
            common.LOGI("publish.py", "***** Congratuations, publishing SDK succeed! *****")
        else:
            common.LOGI("publish.py", "***** ERROR, publishing SDK failed! *****")
        common.LOGI("publish.py", "***************************************************")
