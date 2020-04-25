package com.preprint.server.data

data class Author(val name : String,
                  val affiliation : String? = null,
                  val firstName : String? = null,
                  val secondName : String? = null)