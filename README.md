<p align="center">
  <a href="https://play.google.com/store/apps/dev?id=7086930298279250852" target="_blank">
    <img alt="" src="https://github-production-user-asset-6210df.s3.amazonaws.com/125717930/246971879-8ce757c3-90dc-438d-807f-3f3d29ddc064.png" width=500/>
  </a>  
</p>

### Our facial recognition algorithm is globally top-ranked by NIST in the FRVT 1:1 leaderboards.<span> <img src="https://github.com/kby-ai/.github/assets/125717930/bcf351c5-8b7a-496e-a8f9-c236eb8ad59e" style="margin: 4px; width: 36px; height: 20px"> <span/> </br> ([Latest NIST frvt evaluation report 2024-12-20](https://pages.nist.gov/frvt/html/frvt11.html)) </br>
![frvt-sheet](https://github.com/user-attachments/assets/16b4cee2-3a91-453f-94e0-9e81262393d7) 

#### 🆔 ID Document Liveness Detection - Linux - [Here](https://web.kby-ai.com) <span> <img src="https://github.com/kby-ai/.github/assets/125717930/bcf351c5-8b7a-496e-a8f9-c236eb8ad59e" style="margin: 4px; width: 36px; height: 20px"> <span/>
#### 🤗 Hugging Face - [Here](https://huggingface.co/kby-ai)
#### 📚 Product & Resources - [Here](https://github.com/kby-ai/Product)
#### 🛟 Help Center - [Here](https://docs.kby-ai.com)
#### 💼 KYC Verification Demo - [Here](https://github.com/kby-ai/KYC-Verification-Demo-Android)
#### 🙋‍♀️ Docker Hub - [Here](https://hub.docker.com/u/kbyai)

# FaceActiveLivenessDetection-Android

## Overview
This repository showcases real-time `Face Active Liveness Detection` technology on `Android` device.
Active face liveness detection is a security measure used in biometric systems to confirm that a live person, rather than a fraudulent representation like a photo or mask, is present during authentication. This method requires users to perform specific actions in real-time, such as blinking, smiling, or turning their head, to demonstrate liveness. By analyzing these prompted movements, the system can effectively distinguish between genuine users and potential spoofing attempts.

> In this repository, we implemented face active liveness detection by integrating KBY-AI's Face SDK premium package into Android project.

### ◾FaceSDK(Mobile) Product List
  | No.      | Repository | SDK Details |
  |------------------|------------------|------------------|
  | 1        | [Face Liveness Detection - Android](https://github.com/kby-ai/FaceLivenessDetection-Android)    | Basic SDK |
  | 2        | [Face Liveness Detection - iOS](https://github.com/kby-ai/FaceLivenessDetection-iOS)    | Basic SDK |
  | 3        | [Face Recognition - Android](https://github.com/kby-ai/FaceRecognition-Android)    | Standard SDK |
  | 4        | [Face Recognition - iOS](https://github.com/kby-ai/FaceRecognition-iOS)    | Standard SDK |
  | 5        | [Face Recognition - Flutter](https://github.com/kby-ai/FaceRecognition-Flutter)        | Standard SDK |
  | 6        | [Face Recognition - Ionic-Cordova](https://github.com/kby-ai/FaceRececogniion-Ionic-Cordova)        | Standard SDK |
  | 7        | [Face Recognition - React-Native](https://github.com/kby-ai/FaceRecognition-React-Native)        | Standard SDK |
  | 8        | [Face Attribute - Android](https://github.com/kby-ai/FaceAttribute-Android)        | Premium SDK |
  | 9        | [Face Attribute - iOS](https://github.com/kby-ai/FaceAttribute-iOS)        | Premium SDK |
  | 10        | [Face Attribute - Flutter](https://github.com/kby-ai/FaceAttribute-Flutter)        | Premium SDK |
  | ➡️        | <b>[Face Active Liveness Detection - Android](https://github.com/kby-ai/FaceActiveLivenessDetection-Android)</b>        | <b>Premium SDK</b> |

> To get Face SDK(server), please visit products [here](https://github.com/kby-ai/Product).<br/>
## Try the APK

### Google Play

<a href="https://play.google.com/store/apps/details?id=com.kbyai.facelivedemo" target="_blank">
  <img alt="" src="https://user-images.githubusercontent.com/125717930/230804673-17c99e7d-6a21-4a64-8b9e-a465142da148.png" height=80/>
</a>

## Performance Video

You can visit our YouTube video [here](https://www.youtube.com/watch?v=F7c5ZqtbIsA) to see how well our demo app works.</br></br>
[![Face Recognition Android](https://img.youtube.com/vi/F7c5ZqtbIsA/0.jpg)](https://www.youtube.com/watch?v=F7c5ZqtbIsA)

## SDK License

This project uses `KBY-AI`'s liveness detection SDK. The SDK requires a license per `application ID`.

- The code below shows how to use the license: https://github.com/kby-ai/FaceLivenessDetection-Android/blob/f81f001b0a2f65330d2adaabc9b001003af9a112/app/src/main/java/com/kbyai/facelivedemo/CameraActivity.java#L69-L77

- To request a license, please contact us:</br>
🧙`Email:` contact@kby-ai.com</br>
🧙`Telegram:` [@kbyai](https://t.me/kbyai)</br>
🧙`WhatsApp:` [+19092802609](https://wa.me/+19092802609)</br>
🧙`Skype:` [live:.cid.66e2522354b1049b](https://join.skype.com/invite/OffY2r1NUFev)</br>
🧙`Facebook:` https://www.facebook.com/KBYAI</br>

## About SDK

### Set up
1. Copy the SDK (`libfacesdk` folder) to the `root` folder in your project.

2. Add SDK to the project in `settings.gradle`.
```kotlin
include ':libfacesdk'
```

3. Add dependency to your `build.gradle`.
```kotlin
implementation project(path: ':libfacesdk')
```

### Initializing an SDK

- Step One

To begin, you need to activate the SDK using the license that you have received.
```kotlin
FaceSDK.setActivation("...")
```

If activation is successful, the return value will be `SDK_SUCCESS`. Otherwise, an error value will be returned.

- Step Two

After activation, call the SDK's initialization function.
```kotlin
FaceSDK.init(getAssets());
```
If initialization is successful, the return value will be `SDK_SUCCESS`. Otherwise, an error value will be returned.

### Face Detection and Liveness Detection

The `FaceSDK` offers a single function for detecting face and liveness detection, which can be used as follows:
```kotlin
FaceSDK.faceDetection(bitmap)
```

This function takes a single parameter, which is a `bitmap` object. The return value of the function is a list of `FaceBox` objects. Each FaceBox object contains the detected face rectangle, liveness score, and facial angles such as `yaw`, `roll`, and `pitch`.

### Yuv to Bitmap
The SDK provides a function called `yuv2Bitmap`, which converts a `yuv` frame to a `bitmap`. Since camera frames are typically in `yuv` format, this function is necessary to convert them to `bitmap`. The usage of this function is as follows:
```kotlin
Bitmap bitmap = FaceSDK.yuv2Bitmap(nv21, image.getWidth(), image.getHeight(), 7);
```
The first parameter is an `nv21` byte array containing the `yuv` data. 

The second parameter is the width of the `yuv` frame, and the third parameter is its height. 

The fourth parameter is the `conversion mode`, which is determined by the camera orientation.

To determine the appropriate `conversion mode`, the following method can be used:
```kotlin
 1        2       3      4         5            6           7          8

 888888  888888      88  88      8888888888  88                  88  8888888888
 88          88      88  88      88  88      88  88          88  88      88  88
 8888      8888    8888  8888    88          8888888888  8888888888          88
 88          88      88  88
 88          88  888888  888888
```

