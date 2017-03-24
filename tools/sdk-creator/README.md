## 运行脚本前的准备工作

#### Windows

* 使用Ant编译

    添加名称为`ANT_ROOT`的系统环境变量，值为ant安装目录bin所在目录，e.g. `C:\software\ant\bin`

* 使用Gradle编译

    下载[Gradle](https://downloads.gradle.org/distributions/gradle-2.4-bin.zip)
    解压下载的`gradle-2.4-bin.zip`到某个目录, e.g. `C:\software\`
    添加名称为`GRADLE_HOME`的系统环境变量，值为`C:\software\gradle-2.4\bin`

* 添加名称为`ANDROID_HOME`的系统环境变量，值为`android sdk`的根目录
* 安装[JDK](http://www.oracle.com/technetwork/cn/java/javase/downloads/jdk8-downloads-2133151-zhs.html)
* 添加名称为`JAVA_HOME`的系统环境变量，值为`JDK`的安装根目录
* 添加`JDK`安装目录下的`bin`目录(e.g. C:\Program Files\Java\jdk1.6.0_38\bin)到系统变量`Path`中，确保在`命令提示符`下能够使用`java -?`命令
* 添加`dx.bat`所在的目录(e.g. C:\Software\android-sdk\build-tools\22.0.1)到系统的`Path`环境变量中，确保在`命令提示符`下能够使用`dx`命令

#### Mac

* 使用Ant编译

    添加名称为`ANT_ROOT`的系统环境变量，值为ant安装目录bin所在目录，e.g. `export ANT_ROOT=/usr/local/bin`

* 使用Gradle编译
  
    使用brew安装gradle: `brew install gradle`
    修改`~/.bash_profile`, 添加`export GRADLE_HOME=/usr/local/bin`

* 添加名称为`ANDROID_HOME`的系统环境变量，值为`android sdk`的根目录, `export ANDROID_HOME=/xxx/xxx/android-sdk`
* 确认在终端下能够使用`java`，如果无法使用，需要安装`JDK`
* 确认在终端下能够使用`dx`，如果无法使用，需要下载`android sdk`, 并export`build-tools\xx.x.x\`目录到PATH变量中

## 编译打包SDK

```
$ python SDKCreator.py
```

输出目录:

* out-runtimesdk/: Runtime SDK的输出目录
* out-gplaysdk/: Gplay SDK的输出目录
* out-host/: Runtime SDK依赖的host库的输出目录
* out-client/: Client SDK的输出目录

可选参数：

```
[-t, --sdk-type=SDK_TYPE]: 指定生成的SDK用于`online`,`sandbox`,`develop`环境, e.g. `python SDKCreator.py -t sandbox`
[-b BUILD_SDK, --build-sdk=BUILD_SDK]: 指定要编译的SDK，值为`gplay`, `runtime`或者`client`, e.g. ：`python SDKCreator.py -b runtime`
[--dont-proguard]: 是否混淆代码
```

## 打包APK

```
$ python APKCreator.py
```

输出目录：

* out-apk/

可选参数：

```
[-p, --project=PROJECT]: 指定要编译的Sample工程，目前支持`GplaySDKDemo`, `GplayEmulator`, `GplayQuickDemo`,  可以指定只编译打包某个工程, e.g. `python APKCreator.py -p GplayDemo`
[--dont-build-sdk]: 指定是否需要编译SDK，在确认SDK已经正常编译的情况下，可以指定此参数，节省编译时间
[--use-gradle]: 是否用gradle进行打包
```

## 发布版本

```
$ python publish.py
```

输出目录：

* publish/: 此目录为最终发布版本的目录

可选参数：

```
[--use-gradle]: 是否用gradle进行打包
```

