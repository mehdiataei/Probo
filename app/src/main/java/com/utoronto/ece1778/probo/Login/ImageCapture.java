package com.utoronto.ece1778.probo.Login;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.IOException;

public class ImageCapture {
    private Context context;
    private File imageFile;

    public ImageCapture(Context context) {
        this.context = context;
    }

    public Intent getCaptureIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(this.context.getPackageManager()) != null) {
            try {
                this.imageFile = this.createImageFile();
            } catch (IOException e) {
                return null;
            }

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Uri imageUri = FileProvider.getUriForFile(
                    this.context,
                    this.context.getApplicationContext().getPackageName() + ".provider",
                    this.imageFile.getAbsoluteFile()
            );

            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

            return intent;
        }

        return null;
    }

    public ImageBitmap getCapturedImage() {
        if (this.imageFile == null) {
            return new ImageBitmap(null);
        }

        try {
            Uri imageUri = FileProvider.getUriForFile(
                    this.context,
                    this.context.getApplicationContext().getPackageName() + ".provider",
                    this.imageFile.getAbsoluteFile()
            );

            Bitmap image = MediaStore.Images.Media.getBitmap(this.context.getContentResolver(), imageUri);
            return this.getCorrectedImage(image);
        } catch (IOException e) {
            return null;
        }
    }

    public File createImageFile() throws IOException {
        Long currentTime = System.currentTimeMillis() / 1000;
        String timestamp = currentTime.toString();
        String fileName = "PNG_" + timestamp + "_";

        return File.createTempFile(
                fileName,
                ".png",
                this.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        );
    }

    public void deleteImageFile() {
        if (this.imageFile != null) {
            this.imageFile.delete();
        }
    }

    private ImageBitmap getCorrectedImage(Bitmap image) {
        ImageBitmap imageBitmap = new ImageBitmap(image);

        try {
            ExifInterface exif = new ExifInterface(this.imageFile.getAbsolutePath());

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
            );

            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    imageBitmap.flipHorizontal();
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    imageBitmap.rotate(180);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    imageBitmap.flipVertical();
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    imageBitmap.transpose();
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    imageBitmap.rotate(90);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    imageBitmap.traverse();
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    imageBitmap.rotate(270);
                    break;
                default:
                    break;
            }

            return imageBitmap;
        } catch (IOException e) {
            return imageBitmap;
        }
    }
}
