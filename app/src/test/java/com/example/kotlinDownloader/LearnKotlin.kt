import com.example.kotlinDownloader.internal.TaskInformation
import kotlinx.coroutines.*


suspend fun delayDuration(ms:Long): Long {
    delay(ms)
    println("$ms done")
    return ms
}
//
//open class Father(val a: Int){
//    open fun f() : Int = 5
//    fun result(): Int = a
//}
//
//class Son : Father(3){
//    override fun f():Int = 30
//}
//
//interface Settings<T>{
//    fun update()
//    fun read()
//}
//
//class DataSetting<T>: Settings<T>{
//    override fun update() { }
//    override fun read() { }
//
//}
//
//data class UISettings(
//    val darkMode : Boolean = false,
//    val fontSize : Int = 14,
//){
//    companion object{
//        val Default = this
//    }
//}
//
//interface RespProp{
//    val uiSettings: Settings<UISettings>
//}
//
//class RespImp: RespProp{
//    override val uiSettings: Settings<UISettings> = DataSetting()
//}
//
//interface HealthPoint<T,R>{
//    var point: T
//    var currentChange: T.() -> T
//
//    fun get(value: T) = point
//    fun update(): R
//
//}
//
//    override var point: Int,
//    override var currentChange : Int.() -> Int,
//) :HealthPoint<Int,Unit>{
//    override fun update(){
//        point = currentChange(point)
//        println(point)
//    }
//}
//
//fun Int.moreAdd() = this+2



fun main(){
    val runStart :Boolean = false
    println("begin of main")

    val scope = CoroutineScope(Dispatchers.IO)




   runBlocking {
       launch {
           delayDuration(2000).run{ println("first") }
           val waitTaskA = async { delayDuration(3000) }.await()  //等待
           println("A-middle")
           val waitTaskB = async { delayDuration(1000) }  //不等
           println("B-middle")
       }

       launch {
           val waitTaskC = async { delayDuration(1000) }  //不触发 await 直接 执行 C
           println("C-middle")

       }

       // result
       // C => first => A => B
   }


    println("end of main")


}





fun logCurrentThreadName() = println(Thread.currentThread().name)


