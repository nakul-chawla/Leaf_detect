package com.buzz.flash.leaf_detect;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "MainActivity";
    Button button;
    Uri fileUri;
    private File destination;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                        fileUri = MainActivity.this.getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                } catch (OutOfMemoryError error) {
                    Toast.makeText(MainActivity.this, "Unable to Capture image because" + error.getCause(), Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    public Uri getOutputMediaFileUri(int type) {
        File file = getOutputMediaFile(type);
        Log.i(TAG,file.toString());
        return FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", file);
    }

    private static File getOutputMediaFile(int type) {

        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Config.IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("fail", "Oops! Failed create "
                        + Config.IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        } else {
            Log.i(TAG,"null ");
            return null;
        }

        return mediaFile;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        System.out.println("onActivityResult Called");
        Bitmap imageBitmap = null;
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            System.out.println("request image capture");
            if (fileUri != null) {
                try {

                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    BitmapFactory.Options options = new BitmapFactory.Options();
//                        int nh = (int) ( bitmap.getHeight() * (512.0 / bitmap.getWidth()) );
//                        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, nh, true);
                    //String uri = compressImage(fileUri.toString());
                    //fileUri = Uri.parse(uri);
                    //Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath(), options);
                    //bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    byte[] byteArray = os.toByteArray();
                    System.out.println("byte array : " + byteArray);
                    for (int i = 0; i < byteArray.length; i++) {
                        System.out.print(byteArray[i]);
                    }
                    this.uploadImage(byteArray);
                } catch (OutOfMemoryError error) {
                    Log.d("out of memory", "" + error.getCause());
                    Toast.makeText(MainActivity.this, "unable to upload image:" + error.getCause(), Toast.LENGTH_SHORT).show();

                }

            }

        }
    }

    private void uploadImage(final byte[] byteArray) {
        new AsyncTask<byte[], Void, File>() {
            @Override
            protected File doInBackground(byte[]... bytes) {
                try {
                    destination = new File(Environment.getExternalStorageDirectory(),
                            System.currentTimeMillis() + ".jpg");
                    destination.createNewFile();
                    FileOutputStream fo = new FileOutputStream(destination);
                    fo.write(byteArray);
                    fo.close();
                    return destination;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(File file) {
                super.onPostExecute(file);
                uploadToServer(destination);

            }
        }.execute(byteArray);
    }

    public String compressImage(String imageUri) {

        String filePath = getRealPathFromURI(imageUri);
        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

//      by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
//      you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

//      max Height and width values of the compressed image is taken as 816x612

        float maxHeight = 816.0f;
        float maxWidth = 612.0f;

        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

//      width and height values are set maintaining the aspect ratio of the image

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }

//      setting inSampleSize value allows to load a scaled down version of the original image

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

//      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

//      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];

        try {
//          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();

        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

//      check the rotation of the image and display it properly
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap=Bitmap.createBitmap(scaledBitmap,0,0,scaledBitmap.getWidth(),scaledBitmap.getHeight());
//
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputStream out = null;
        String filename = getFilename();
        try {
            out = new FileOutputStream(filename);

//          write the compressed bitmap at the destination specified by filename.
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return filename;

    }
    public String getFilename() {
        File file = new File(Environment.getExternalStorageDirectory().getPath(), "MyFolder/Images");
        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
        return uriSting;

    }

    private String getRealPathFromURI(String contentURI) {
        Uri contentUri = Uri.parse(contentURI);
        Cursor cursor = MainActivity.this.getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(index);
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    private void uploadToServer(File destination) {
        UploadService.ProgressRequestBody fileBody = new UploadService.ProgressRequestBody(destination, new UploadService.ProgressRequestBody.UploadCallbacks() {
            @Override
            public void onProgressUpdate(int percentage) {
                Log.d("updating", "" + percentage);
//                progressBar.setIndeterminate(false);
//                textViewUpload.setText("Uploading... " + percentage + "% complete");
//                progressBar.setProgress(percentage);
            }

            @Override
            public void onError() {

                //retryPopUp();
            }

            @Override
            public void onFinish() {

                Log.d("updateFlavourCall", "Finish");
            }
        });

        MultipartBody.Part filePart = MultipartBody.Part.createFormData("image", destination.getName(), fileBody);
        RequestBody name = RequestBody.create(MediaType.parse("multipart/form-data"), destination.getName());
        UploadClient membershipClient = ApiClient.getClient().create(UploadClient.class);
        membershipClient.uploadImage(name,filePart).enqueue(new Callback<ResponseModel>() {
            @Override
            public void onResponse(Call<ResponseModel> call, Response<ResponseModel> response) {
                Log.d("Tag", "code" + response.code() + "");
                if (response.code()==200) {
                    String uri = response.body().getUrl();
                    if (uri != null) {
                        Log.i(TAG,uri);
                    } else {
                        System.out.println("failed to get the download uri");
                        Log.e(TAG,"failed to upload image");
                    }
                }

            }

            @Override
            public void onFailure(Call<ResponseModel> call, Throwable t) {
                t.printStackTrace();
                System.out.println("failed to upload the image");
            }
        });
    }


}
