android-studio-proteus-plugin
==================================

A plugin for Android studio / IntelliJ IDEA which helps you to convert XML layouts to JSON layouts required by proteus.
In addition to converting the format, it also flattens the xml by recursively traversing the @include resources and also uploads local drawables to the endpoint specified in Config file.

Please note that proteus keeps getting updated with support for new Views and new Parsers, but this plugin does not validate that the views used in your xml are registered inside Proteus beforehand.

Todo : Allow previewing of the Proteus layouts on your adb devices. 
