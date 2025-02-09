import kotlinx.coroutines.*
import kotlin.random.Random


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
//class Mob(
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
    val runStart :Boolean = true
    println("begin of main")

//    println(4.moreAdd())

//    val player:Mob = Mob(60){
//        this*2
//    }.apply {
//        update()
//
//        currentChange = { this-10 }
//
//        update()
//        update()
//    }




    if(runStart){
        runBlocking {

            TestMultiThreadDownloadManager.addTask(
                TestDownloadTask(
                    taskName = "",
                    downloadUrl = "http://cn.ejie.me/uploads/setup_Clover@3.5.6.exe",
                    storagePath = "./downloads/setup_Clover@3.5.6.exe",
                    taskID = "Clover ${Random.nextInt()}"
                ),
            )

            TestMultiThreadDownloadManager.waitForAll()


            println("All downloads completed.")


        }
    }

    println("end of main")
}





fun logCurrentThreadName() = println(Thread.currentThread().name)


