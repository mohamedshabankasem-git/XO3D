package com.example.xo.engine
enum class Cell { EMPTY, X, O; fun opponent() = when (this) { X -> O; O -> X; EMPTY -> EMPTY } }
