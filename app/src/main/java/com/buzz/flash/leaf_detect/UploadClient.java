package com.buzz.flash.leaf_detect;

import com.buzz.flash.leaf_detect.ResponseModel;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UploadClient {

    @Multipart
    @POST("/upload_yes/")
    Call<ResponseModel> uploadImage(@Part("name") RequestBody name, @Part MultipartBody.Part filepart);
}
