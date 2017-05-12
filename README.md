# ROS Android Bag
The ROS Bag package of Robot Operating System provides a convenient way to record data
from topics. More precisely, the record application from the ROS Bag package does this
job. The goal of this project is to bring this functionality to Android devices.

The rationale for this ability is the need to capture high volumes of data that you
otherwise unable to capture due to huge bandwidth. For example, the raw video stream. It
can not be transmitted over most wireless networks, but sometimes it's essential to
capture the stream for quality demanding applications. There may become more and more
use cases as ROS goes to Android. Sometimes it's easier to prototype some applications
on a mobile phone that prepare more complicated hardware.

## How it works
This application is basically a wrapper around the native record app from the
<http://wiki.ros.org/rosbag> package. It was compiled for ARM using the
<https://github.com/ekumenlabs/roscpp_android> repository. The binary is distributed in
precompiled form, but you can compile it manually using the mentioned repository. So you
use native high performance and well tested components from ROS wrapped with this tiny app.

## Install
The installation process is standard for Android applications, you can also open the
project in the Android Studio. Alternatively you can always download the app from
Android Market.

## Usage
Provide the Master URL, specify topics you want to capture, specify additional arguments
and start recording. Then you can get the bag using `adb pull` from the phone. Please,
read help in the app before using it.

## Compatibility
It should be compatible with all ARM devices, as the binary is now available only for ARM.

## Contributing
Contributions are welcome: open issues or even better open issues and pull requests :)