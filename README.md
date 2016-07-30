# Unmaintained!
The development of Jitsi for Android has been stopped in early 2015. Issues and Pull Requests will not be addressed.

******

# Jitsi for Android

Jitsi for Android is an Android port of the [Jitsi] project: The most feature-rich communicator with support for encrypted audio/video, chat and presence over SIP and XMPP.

## Usage with ANT

After cloning the project, run the following command:

    ~/Library/Android/sdk/tools/android update project -p ~/AndroidStudioProjects/jitsi-android --subprojects

After updating library bundles, when building for the first time or after clean:

    ant setup-libs
To make the project:

    ant make

To rebuild (clean and make):

    ant rebuild

To run the project (will install the apk and will run it on default test device):

    ant run

To make and run the project after modification:

    ant make run

## Usage with IntelliJ

1. It is recommended that the initial setup be done via Ant.
1. Make sure that you have [Java] and [Android SDK] installed on your system and [IntelliJ] version is up to date(13.0.2).
1. (Optional) Assuming that [Jitsi for desktop] project is in the same parent directory you can call "copy-jitsi-bundles" ant target. This will sync Jitsi bundles. Ant targets can be found in "Ant Build" tools window(View->Tool Windows->Ant Build).
1. Before building for the first time call "setup-libs" ant target. This will process jitsi bundles and place all required libraries in /libs folder.
1. Now you can use IntelliJ IDE to run/debug/test Jitsi for Android like any other application.

## Sources

To obtain sources for .jar files located in lib folder checkout jitsi_android
 branch of jitsi and libjitsi projects.
 
 https://github.com/jitsi/jitsi/tree/jitsi_android
 
 https://github.com/jitsi/libjitsi/tree/jitsi_android

## Contribution

Before making any pull requests please see: https://jitsi.org/Documentation/FAQ#patch 

[Jitsi]: https://jitsi.org/
[Java]: http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html
[Android SDK]: http://developer.android.com/sdk/index.html
[IntelliJ]: http://www.jetbrains.com/idea/download/
[Jitsi for desktop]: https://github.com/jitsi/jitsi
