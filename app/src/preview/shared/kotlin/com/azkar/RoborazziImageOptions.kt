package com.azkar

import com.github.takahirom.roborazzi.LosslessWebPImageIoFormat
import com.github.takahirom.roborazzi.RoborazziOptions

@OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)
internal fun RoborazziOptions.withLosslessWebp() =
    copy(recordOptions = recordOptions.copy(imageIoFormat = LosslessWebPImageIoFormat()))
