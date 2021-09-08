package com.boris.expert.csvmagic.retrofit

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.boris.expert.csvmagic.model.FeedbackResponse
import com.boris.expert.csvmagic.model.SNPayload
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ApiRepository {

    var apiInterface: ApiServices = RetrofitClientApi.createService(ApiServices::class.java)

    companion object {
        lateinit var context: Context

        private var apiRepository: ApiRepository? = null
        fun getInstance(mContext: Context): ApiRepository {
            context = mContext
            if (apiRepository == null) {
                apiRepository = ApiRepository()
            }
            return apiRepository!!
        }
    }

    // THIS FUNCTION WILL SEND THE POST REQUEST TO SERVER FOR CREATING DYNAMIC QR
     fun createDynamicQrCode(body: HashMap<String, String>): MutableLiveData<JsonObject> {
        val res = MutableLiveData<JsonObject>()
        val bodyJson = Gson().toJsonTree(body).asJsonObject
        Log.d("TEST199", bodyJson.toString())
        apiInterface.createDynamicQrCode(bodyJson).enqueue(object:Callback<JsonObject>{
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                res.postValue(response.body())
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                res.postValue(null)
            }
        })

        return res
     }

    // THIS FUNCTION WILL SEND THE POST REQUEST TO SERVER FOR CREATING COUPON QR
    fun createCouponQrCode(body: HashMap<String, String>): MutableLiveData<JsonObject> {
        val res = MutableLiveData<JsonObject>()
        val bodyJson = Gson().toJsonTree(body).asJsonObject
        Log.d("TEST199", bodyJson.toString())
        apiInterface.createCouponQrCode(bodyJson).enqueue(object:Callback<JsonObject>{
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                res.postValue(response.body())
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                res.postValue(null)
            }
        })

        return res
    }

    // THIS FUNCTION WILL SEND THE POST REQUEST TO SERVER FOR CREATING COUPON QR
    fun createFeedbackQrCode(body: HashMap<String, String>): MutableLiveData<JsonObject> {
        val res = MutableLiveData<JsonObject>()
        val bodyJson = Gson().toJsonTree(body).asJsonObject
        Log.d("TEST199", bodyJson.toString())
        apiInterface.createFeedbackQrCode(bodyJson).enqueue(object:Callback<JsonObject>{
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                res.postValue(response.body())
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                res.postValue(null)
            }
        })

        return res
    }


    fun signUp(body: HashMap<String, String>): MutableLiveData<JsonObject> {
        val res = MutableLiveData<JsonObject>()
        val bodyJson = Gson().toJsonTree(body).asJsonObject
        Log.d("TEST199", bodyJson.toString())
        apiInterface.signUp(bodyJson).enqueue(object:Callback<JsonObject>{
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                res.postValue(response.body())
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                res.postValue(null)
            }
        })

        return res
    }

    fun signIn(email:String): MutableLiveData<JsonObject> {
        val res = MutableLiveData<JsonObject>()
        apiInterface.signIn(email).enqueue(object:Callback<JsonObject>{
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                res.postValue(response.body())
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                res.postValue(null)
            }
        })

        return res
    }

    // THIS FUNCTION WILL SEND THE POST REQUEST TO SERVER FOR CREATING SOCIAL NETWORK QR
    fun createSnTemplate(body: SNPayload): MutableLiveData<JsonObject> {
        val res = MutableLiveData<JsonObject>()
        val bodyJson = Gson().toJsonTree(body).asJsonObject
        apiInterface.createSnTemplate(bodyJson).enqueue(object:Callback<JsonObject>{
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                res.postValue(response.body())
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                res.postValue(null)
            }
        })

        return res
    }

    fun getAllFeedbacks(id:String): MutableLiveData<FeedbackResponse> {
        val res = MutableLiveData<FeedbackResponse>()
        apiInterface.getAllFeedbacks(id).enqueue(object:Callback<FeedbackResponse>{
            override fun onResponse(call: Call<FeedbackResponse>, response: Response<FeedbackResponse>) {
                res.postValue(response.body())
            }

            override fun onFailure(call: Call<FeedbackResponse>, t: Throwable) {
                res.postValue(null)
            }
        })

        return res
    }

    fun purchase(packageName:String,productId:String,token:String): MutableLiveData<JsonObject> {
        val res = MutableLiveData<JsonObject>()
        apiInterface.purchase(packageName,productId,token).enqueue(object:Callback<JsonObject>{
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                res.postValue(response.body())
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                res.postValue(null)
            }
        })

        return res
    }
}