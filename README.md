# PrEPare
###Helping HIV patients overcome daily challenges.

## Introduction

This application tracks pill intake by interfacing with a smart pill bottle equipped with a [Metawear C](https://mbientlab.com/metawearc/), which has a 6-axis inertial measurement unit (IMU): accelerometer and gyroscope. The user may optionally be equipped with a smartwatch that supports Android Wear in order to additionally track the wrist trajectory and the watch-to-bottle RSSI (received signal strength indicator), which provides useful proximity estimates. The primary intent is to help HIV patients follow their daily pill intake regiment; however, this system may be advantageous to anyone who might need help remembering to take their daily pills.

## Background

Pre-exposure Prophylaxis, known as PrEP, is a medication that can prevent HIV infection and is generally provided to those who are at substantial risk e.g. due to their genetic predisposition or sexual preferences. Although PrEP significantly reduces the risk of infection, by up to 92%, it is essential that it be taken consistently. See [http://www.cdc.gov/hiv/risk/prep/](http://www.cdc.gov/hiv/risk/prep/).

On the other hand, those who have already contracted HIV may be subject to take several pills daily for effective treatment. The FDA has approved more than 25 drugs available for treatment. A comprehensive list can be found [here](http://www.healthline.com/health/hiv-aids/medications-list#4).

## Components

This application has several components. These include 

  1. The data collection application is responsible for collecting labeled streams from all relevant available sensors. This is an Android application.
  2. The data analysis application is a suite of Python scripts provide useful statistical information pertaining to the data collected and provide evaluations for several customized Machine Learning approaches to this detection problem.
  3. The PrEPare pill intake detection application identifies instances of pill intake and sets daily reminders for the patient. Day-to-day intake data is available to the user through the main UI and can also be shared with a personal healthcare provider.

### Data Collection

Before a classifier can be learned to identify pill intake instances, we require a substantial dataset, preferably with ground-truth labels. The data collection application is responsible for this process. It simultaneously streams synchronized data of the following modalities:

  * Accelerometer/Gyroscope from the Metawear C
  * Accelerometer/Gyroscope from the Android Wearable
  * RSSI (Received Signal Strength Indicator) between the Metawear C and Android Wearable 
  * Video/Audio from the mobile device
  * Self reports in the absence of video

Any of the sensors can be disabled through the main application preferences accessible from the user interface on the mobile device. Additionally, all enabled sensors can run in the background, allowing the user to continue with ordinary phone usage during data collection, only at the expense of satisfactory video recording.

#### Low-Power Data Collection

Using several sensing modalities simultaneously poses the issue of high power consumption. However, notice that the data collection is distributed among three dedicated devices.

  1. The phone handles the user interactions, the video recording, the Bluetooth connection with the smartwatch and the ongoing background service.
  2. The watch handles basic user interactions and notifications, accelerometer and gyroscope streaming, the Bluetooth connection with the Metawear device and the sensor data transmission to the phone.
  3. The Metawear device handles only its own accelerometer and gyroscope data, sending them periodically to the smartwatch.

The mobile and wearable devices are both rechargeable and we recommend recharging these devices each night. However, the Metawear device runs on a non-rechargeable replaceable CR-2032 coin cell battery. For convenience, the battery level is displayed in the application during data collection and updated periodically. The user is notified both on the mobile device and on the watch when the Metawear battery reaches low battery charge.

Because the Metawear device sits in Beacon mode until the watch is in range, it consumes very little power when data collection is not in process. Even when the user is in range of the Metawear device, it consumes minimal power until the user is sufficiently close to the device. The proximity is estimated by the RSSI between the wearable and the Metawear device.

The Metawear device is estimated to last six weeks before battery replacement is required. However, more comprehensive testing is required for accurate estimates.

To reduce the daily power consumption of the pill bottle and the watch, it is recommended that the gyroscope is disabled. The gyroscope consumes two orders of magnitude greater power than the accelerometer and offers little boost in performance.

#### Application Permissions

The only essential application permission is ```WRITE_EXTERNAL_STORAGE```, which is used to write data to disk. Without it, no data collection can really take place.

In addition, the application needs the following permissions:

  * [```ACCESS_COARSE_LOCATION```](https://developer.android.com/reference/android/Manifest.permission.html#ACCESS_COARSE_LOCATION) is since Android version 6.0 required to scan for Bluetooth Low Energy devices. The user may alternatively select to enter the MAC ID of the Metawear device manually.
  * ```CAMERA``` is required if video recording is enabled for use in ground-truth labeling.
  * ```RECORD_AUDIO``` is required if in addition to video recording, audio recording is also enabled.
  * ```SYSTEM_ALERT_WINDOW``` is a special permission that allows the application to draw over all other applications. It is required for background recording video when the application is not visible or closed entirely. If you do not wish to grant this permission, then video will be disabled.

### Data Analysis

TODO

### PrEPare - Detection in the real world

TODO
