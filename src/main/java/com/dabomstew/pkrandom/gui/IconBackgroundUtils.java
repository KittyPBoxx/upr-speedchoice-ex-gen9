package com.dabomstew.pkrandom.gui;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

public class IconBackgroundUtils {

    private static final int[][] BAYER_MATRIX = {
            {0, 8, 2, 10},
            {12, 4, 14, 6},
            {3, 11, 1, 9},
            {15, 7, 13, 5}
    };

    private static final int MATRIX_SIZE = 4;
    private static final int MATRIX_DIVISOR = MATRIX_SIZE * MATRIX_SIZE;


    @SuppressWarnings("SameParameterValue")
    public static BufferedImage createGradientCircle(int width, int height, Random random) {

        Color[] colours = generateRandomBackgroundGradientColors(random);
        return createGradientCircle(width, height, colours[0], colours[1]);

    }

    private static BufferedImage createGradientCircle(int width, int height, Color color1, Color color2) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = width / 2;
        int centerY = height / 2;
        float radius = (float) Math.min(width, height) / 2;

        RadialGradientPaint radialGradient = new RadialGradientPaint(new Point2D.Double(centerX, centerY),
                radius,
                new float[]{0f, 1f},
                new Color[]{color1, color2});
        g2d.setPaint(radialGradient);
        g2d.fillOval(0, 0, width, height);
        g2d.dispose();

        applyBayerDithering(image);

        return image;
    }

    private static Color[] generateRandomBackgroundGradientColors(Random random) {
        float hue1 = random.nextFloat();
        float hue2 = (hue1 + 0.4f + random.nextFloat() * 0.2f) % 1.0f;

        // Saturation between 0.3 and 0.7 for muted colors
        float saturation1 = 0.3f + random.nextFloat() * 0.4f;
        float saturation2 = 0.3f + random.nextFloat() * 0.4f;

        // Brightness between 0.4 and 0.7 for darker, background-friendly colors
        float brightness1 = 0.4f + random.nextFloat() * 0.3f;
        float brightness2 = 0.4f + random.nextFloat() * 0.3f;

        int alpha1 = 50;
        int alpha2 = 50;

        int rgba1 = Color.HSBtoRGB(hue1, saturation1, brightness1);
        int rgba2 = Color.HSBtoRGB(hue2, saturation2, brightness2);

        rgba1 = (rgba1 & 0xffffff) | (alpha1 << 24);
        rgba2 = (rgba2 & 0xffffff) | (alpha2 << 24);

        return new Color[]{new Color(rgba1, true), new Color(rgba2, true)};
    }

    public static void applyBayerDithering(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int oldPixel = image.getRGB(x, y);
                Color oldColor = new Color(oldPixel, true);

                // Apply dithering to each color channel (R, G, B)
                int red = applyBayerThreshold(oldColor.getRed(), x, y);
                int green = applyBayerThreshold(oldColor.getGreen(), x, y);
                int blue = applyBayerThreshold(oldColor.getBlue(), x, y);

                // Preserve the alpha channel
                int alpha = oldColor.getAlpha();

                // Set the new color
                Color newColor = new Color(red, green, blue, alpha);
                image.setRGB(x, y, newColor.getRGB());
            }
        }
    }

    private static int applyBayerThreshold(int colorValue, int x, int y) {
        // Get the Bayer matrix threshold for the pixel at (x, y)
        int threshold = BAYER_MATRIX[x % MATRIX_SIZE][y % MATRIX_SIZE] * 255 / MATRIX_DIVISOR;

        // Apply the threshold to determine whether to round up or down
        return (colorValue > threshold) ? 255 : 0;
    }

}
