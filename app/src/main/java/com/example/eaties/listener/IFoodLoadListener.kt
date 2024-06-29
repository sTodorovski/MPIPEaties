package com.example.eaties.listener

import com.example.eaties.model.FoodModel

interface IFoodLoadListener {
    fun onFoodLoadSuccess(foodModelList:List<FoodModel>?)
    fun onFoodLoadFailed(message:String?)
}