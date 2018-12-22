package com.programmer74.katolk.client.feign

import feign.Feign
import feign.auth.BasicAuthRequestInterceptor
import feign.gson.GsonDecoder
import feign.gson.GsonEncoder
import feign.okhttp.OkHttpClient

class FeignRepository(val url: String, val username: String, val password: String) {

  fun getUserClient() = Feign.builder()
      .client(OkHttpClient())
      .encoder(GsonEncoder())
      .decoder(GsonDecoder())
      .requestInterceptor(BasicAuthRequestInterceptor(username, password))
      .target(UserClient::class.java, "$url/api/user")

  fun getDialogueClient() = Feign.builder()
      .client(OkHttpClient())
      .encoder(GsonEncoder())
      .decoder(GsonDecoder())
//      .requestInterceptor(BasicAuthRequestInterceptor(username, password))
      .requestInterceptor {
        val basic = BasicAuthRequestInterceptor(username, password)
        basic.apply(it)
        it.header("Content-Type", "application/json")
      }
      .target(DialogueClient::class.java, "$url/api/dialog")
}