package com.example.kotlinDownloader

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

// 委托的类
class DelegateHurt {

    private var health = 0;

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        println("属性被获取: ${property.name}")
        return health
    }

    operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: Int
    ): Int {
        var result:Int = value;
        result-=5;
        //一大堆的计算公式
        health = result;
        return result;
    }
}

fun main(args: Array<String>) {
    var p: Int by DelegateHurt();
    println(p)     // 访问该属性，调用 getValue() 函数

    p = 50   // 调用 setValue() 函数
    println(p)  //45

    p += 5   // 调用 setValue() 函数
    println(p)  //45





var address: String by Delegates.vetoable(
    initialValue = "NanJing",
    onChange = {
        property, oldValue, newValue ->
            println("property: ${property.name}  oldValue: $oldValue  newValue: $newValue")
            return@vetoable newValue == "BeiJing"
    })

}


