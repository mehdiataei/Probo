package com.utoronto.ece1778.probo.Login;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;

public class ImageBitmap {
    private Bitmap image;

    ImageBitmap(Bitmap image) {
        this.image = image;
    }

    public Bitmap getBitmap() {
        return this.image;
    }

    public RoundedBitmapDrawable getCroppedRoundedBitmapDrawable(Resources res) {
        this.centerCrop();

        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(
                res,
                this.image
        );

        roundedBitmapDrawable.setCornerRadius(this.image.getWidth() / 2.0f);
        roundedBitmapDrawable.setAntiAlias(true);

        return roundedBitmapDrawable;
    }

    public void centerCrop() {
        if (this.image == null) {
            return;
        }

        int size = Math.min(this.image.getWidth(), this.image.getHeight());
        int x = (this.image.getWidth() - size) / 2;
        int y = (this.image.getHeight() - size) / 2;

        this.image = Bitmap.createBitmap(
                this.image,
                x,
                y,
                size,
                size
        );
    }

    public void resizeSquare(int size) {
        if (this.image == null) {
            return;
        }

        int correctedSize = Math.min(this.image.getWidth(), size);

        this.image = Bitmap.createScaledBitmap(
                this.image,
                correctedSize,
                correctedSize,
                false
        );
    }

    public void rotate(int degrees) {
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees);
        this.applyMatrix(matrix);
    }

    public void flipHorizontal() {
        Matrix matrix = new Matrix();

        matrix.setScale(-1, 1);

        this.applyMatrix(matrix);
    }

    public void flipVertical() {
        Matrix matrix = new Matrix();

        matrix.setRotate(180);
        matrix.postScale(-1, 1);

        this.applyMatrix(matrix);
    }

    public void transpose() {
        Matrix matrix = new Matrix();

        matrix.setRotate(90);
        matrix.postScale(-1, 1);

        this.applyMatrix(matrix);
    }

    public void traverse() {
        Matrix matrix = new Matrix();

        matrix.setRotate(270);
        matrix.postScale(-1, 1);

        this.applyMatrix(matrix);
    }

    private void applyMatrix(Matrix matrix) {
        if (this.image == null) {
            return;
        }

        this.image = Bitmap.createBitmap(
                this.image,
                0,
                0,
                image.getWidth(),
                image.getHeight(),
                matrix,
                true
        );
    }
}
