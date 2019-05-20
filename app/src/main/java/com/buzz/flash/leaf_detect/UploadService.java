package com.buzz.flash.leaf_detect;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okio.BufferedSink;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class UploadService {

    private Context context;
    private File uploadingFile;
    private String mCurrentPhotoPath;
    private int SELECT_FILE = 584;
    static final int REQUEST_TAKE_PHOTO = 1;
    private OnCallbackListener callbackListener;
    private Object object;

    public interface OnCallbackListener<T> {
        void success(String result);

        void failed();

        boolean uploadToServer(File file);

    }

    public UploadService(Context context) {
        this.context = context;
    }

    public void setOnCallback(OnCallbackListener onCallback) {
        this.callbackListener = onCallback;

    }

    public void openImageCaptureDialog() {
        final CharSequence[] items = {"Take Photo", "Choose from Library",
                "Cancel"};
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(context);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    dispatchTakePictureIntent();
                } else if (items[item].equals("Choose from Library")) {
                    getFromGallery();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_TAKE_PHOTO)
                onCaptureImageResult(data);
        }
    }

    private void onCaptureImageResult(Intent data) {
        try {
            Bitmap bmp = BitmapFactory.decodeFile(mCurrentPhotoPath);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 70, bos);
            writeFileInBackGround(bos);
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }

    }

    private void onSelectFromGalleryResult(Intent data) {
        Bitmap bm = null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(context.getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (bm != null) {
            android.util.Log.d("Got image", "from gallery");
            uploadBitmap(bm);
        }
    }

    private void uploadBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        writeFileInBackGround(bos);
    }


    private void writeFileInBackGround(ByteArrayOutputStream bytes) {
        new AsyncTask<ByteArrayOutputStream, Void, File>() {

            @Override
            protected File doInBackground(ByteArrayOutputStream... bytes) {
                try {
                    File destination = new File(Environment.getExternalStorageDirectory(),
                            System.currentTimeMillis() + ".jpg");
                    destination.createNewFile();
                    FileOutputStream fo = new FileOutputStream(destination);
                    fo.write(bytes[0].toByteArray());
                    fo.close();
                    return destination;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(File destination) {
                super.onPostExecute(destination);

                if (callbackListener == null || callbackListener.uploadToServer(destination)) {
                    requestServerUploading(destination);
                }

            }
        }.execute(bytes);
    }


    public void getFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        Activity activity = (Activity) context;
        activity.startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_FILE);
    }


    public void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(context, context.getPackageName(),
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                Activity activity = (Activity) context;
                activity.startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DCIM);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private void requestServerUploading(File destination) {
        this.uploadingFile = destination;
        if ((uploadingFile.length() / (1024 * 1024)) > 1) {
            destination = reduceImage(destination);
        }
        Log.d("FILEX", destination.toString());
//        MultipartBody.Part filePart = MultipartBody.Part.createFormData("diet", destination.getName(), RequestBody.create(MediaType.parse("image/*"), destination));
        progressBar();
        ProgressRequestBody fileBody = new ProgressRequestBody(destination, new ProgressRequestBody.UploadCallbacks() {
            @Override
            public void onProgressUpdate(int percentage) {
                Log.d("updating", "" + percentage);
                progressDialog.setProgress(percentage);
            }

            @Override
            public void onError() {
                progressDialog.dismiss();
                //retryPopUp();
            }

            @Override
            public void onFinish() {
                progressDialog.dismiss();
                Log.d("updateFlavourCall", "Finish");
            }
        });

    }


    private void
    retryPopUp() {
        final CharSequence[] items = {"Retry",
                "Cancel"};
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(context);
        builder.setTitle("Uploading Fail!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Retry")) {
                    retryUploading();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();


    }

    public void retryUploading() {
        requestServerUploading(uploadingFile);
    }

    ProgressDialog progressDialog;

    void progressBar() {
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Uploading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();
        progressDialog.dismiss();
    }


    public static class ProgressRequestBody extends RequestBody {
        private File mFile;
        private String mPath;
        private UploadCallbacks mListener;

        private static final int DEFAULT_BUFFER_SIZE = 2048;

        public interface UploadCallbacks {
            void onProgressUpdate(int percentage);

            void onError();

            void onFinish();
        }

        public ProgressRequestBody(final File file, final UploadCallbacks listener) {
            mFile = file;
            mListener = listener;
        }

        @Override
        public MediaType contentType() {
            return MediaType.parse("image/*");
        }

        @Override
        public long contentLength() throws IOException {
            return mFile.length();
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            long fileLength = mFile.length();
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            FileInputStream in = new FileInputStream(mFile);
            long uploaded = 0;

            try {
                int read;
                Handler handler = new Handler(Looper.getMainLooper());
                while ((read = in.read(buffer)) != -1) {

                    // update progress on UI thread
                    handler.post(new ProgressUpdater(uploaded, fileLength));

                    uploaded += read;
                    sink.write(buffer, 0, read);

                }
            } finally {
                in.close();
            }
        }

        private class ProgressUpdater implements Runnable {
            private long mUploaded;
            private long mTotal;

            public ProgressUpdater(long uploaded, long total) {
                mUploaded = uploaded;
                mTotal = total;
            }

            @Override
            public void run() {
                mListener.onProgressUpdate((int) (100 * mUploaded / mTotal));
            }
        }
    }
    public File reduceImage(File file){
        try {

            // BitmapFactory options to downsize the image
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            o.inSampleSize = 6;
            // factor of downsizing the image

            FileInputStream inputStream = new FileInputStream(file);
            //Bitmap selectedBitmap = null;
            BitmapFactory.decodeStream(inputStream, null, o);
            inputStream.close();

            // The new size we want to scale to
            final int REQUIRED_SIZE=75;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while(o.outWidth / scale / 2 >= REQUIRED_SIZE &&
                    o.outHeight / scale / 2 >= REQUIRED_SIZE) {
                scale *= 2;
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            inputStream = new FileInputStream(file);

            Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2);
            inputStream.close();

            // here i override the original image file
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file);

            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100 , outputStream);

            return file;
        } catch (Exception e) {
            return null;
        }
    }
}