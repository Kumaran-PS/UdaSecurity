package com.udacity.catpoint.image.service;

import java.awt.image.BufferedImage;

public interface ImageServiceInterface {
    boolean imageContainsCat(BufferedImage currentCameraImage, float v);
}
