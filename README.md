# PrEPare - Helping HIV patients overcome daily challenges.

This application tracks pill intake by interfacing with a smart pill bottle equipped with a [Metawear C](https://mbientlab.com/metawearc/), which has a 6-axis inertial measurement unit (IMU): accelerometer and gyroscope. The user may optionally be equipped with a smartwatch that supports Android Wear in order to additionally track the wrist trajectory and the watch-to-bottle RSSI (received signal strength indicator), which provides useful proximity estimates. The primary intent is to help HIV patients follow their daily pill intake regiment; however, this system may be advantageous to anyone who might need help remembering to take their daily pills.

# Background

Pre-exposure Prophylaxis, known as PrEP, is a medication that can prevent HIV infection and is generally provided to those who are at substantial risk e.g. due to their genetic predisposition or sexual preferences. Although PrEP significantly reduces the risk of infection, by up to 92%, it is essential that it be taken consistently See [http://www.cdc.gov/hiv/risk/prep/](http://www.cdc.gov/hiv/risk/prep/).

On the other hand, those who have already contracted HIV may be subject to take several pills daily for effective treatment. The FDA has approved more than 25 drugs available for treatment. A comprehensive list can be found [here](http://www.healthline.com/health/hiv-aids/medications-list#4).

# Components

This application has several components. These include 

⋅⋅* The data collection application is responsible for collecting labeled streams from all relevant available sensors. This is an Android application.
..* The data analysis application is a suite of Python scripts provide useful statistical information pertaining to the data collected and provide evaluations for several customized Machine Learning approaches to this detection problem.
..* The PrEPare pill intake detection application identifies instances of pill intake and sets daily reminders for the patient. Day-to-day intake data is available to the user through the main UI and can also be shared with a personal healthcare provider.

## Data Collection

Before a classifier can be learned to identify pill intake instances, we require a substantial dataset, preferably with ground-truth labels. The data collection application is responsible for this process. It simultaneously streams synchronized data of the following modalities:

..* Accelerometer/Gyroscope from the Metawear C
..* Accelerometer/Gyroscope from the Android Wearable
..* RSSI (Received Signal Strength Indicator) between the Metawear C and Android Wearable 
..* Video/Audio from the mobile device
..* Self reports in the absence of video

Any of the sensors can be disabled through the main application preferences accessible from the user interface on the mobile device. Additionally, all enabled sensors can run in the background, allowing the user to continue with ordinary phone usage during data collection, only at the expense of satisfactory video recording.

## Data Analysis

TODO

## PrEPare - Detection in the real world

TODO
