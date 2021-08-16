package com.boris.expert.csvmagic.model

import java.io.Serializable

data class Sheet (
          val id:String,
          val name:String
        ):Serializable{
    override fun toString(): String {
        return name
    }
}