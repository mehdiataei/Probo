package com.utoronto.ece1778.probo.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageLoader {
    private String path;
    private Context context;
    private FirebaseStorage storage;

    private Bitmap image;

    public ImageLoader(String path, Context context) {
        this.path = path;
        this.context = context;

        this.storage = FirebaseStorage.getInstance();
    }

    public void load(final ImageLoaderCallback cb) {
        StorageReference storageReference = this.storage.getReference().child(this.path);
        final long MAX_SIZE = 1024 * 1024;

        Bitmap cachedImage = this.getFromCache();

        if (cachedImage != null) {
            image = cachedImage;

            cb.onSuccess(image);
            cb.onComplete();

            return;
        }

        storageReference.getBytes(MAX_SIZE)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                        saveToCache();

                        cb.onSuccess(image);
                        cb.onComplete();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onFailure(e);
                        cb.onComplete();
                    }
                });
    }

    private void saveToCache() {
        try {
            File imageFile = this.createCacheFile();
            FileOutputStream fileOutputStream = new FileOutputStream(imageFile.getAbsolutePath());
            this.image.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
        } catch (IOException e) {
        }
    }

    private Bitmap getFromCache() {
        try {
            String filePath = this.getCachedFilePath();
            File file = new File(filePath);

            Uri imageUri = FileProvider.getUriForFile(
                    this.context,
                    this.context.getApplicationContext().getPackageName() + ".provider",
                    file
            );

            return MediaStore.Images.Media.getBitmap(this.context.getContentResolver(), imageUri);
        } catch (IOException e) {
            return null;
        }
    }

    private File createCacheFile() {
        String filePath = this.getCachedFilePath();
        return new File(filePath);
    }

    private String getCachedFilePath() {
        return this.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" +
                this.path.replace("/", "_").replace(".jpg", "") +
                ".png";
    }

    public interface ImageLoaderCallback {
        void onSuccess(Bitmap image);
        void onFailure(Exception e);
        void onComplete();
    }
}
