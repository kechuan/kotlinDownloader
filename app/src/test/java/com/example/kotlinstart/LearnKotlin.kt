import com.example.kotlinstart.internal.TaskStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

data class DownloadTask(
    val taskStatus: TaskStatus = TaskStatus.Pending,
)

class TaskController {
    private val _isActive = MutableStateFlow(true)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    suspend fun pause() {
        _isActive.emit(false)
    }

    suspend fun resume() {
        _isActive.emit(true)
    }
}

fun main(){
    val runStart :Boolean = true
    println("begin of main")

    val scope = CoroutineScope(Dispatchers.IO)

    runBlocking{
        val controller = TaskController()
        val job = launch {
            var step = 0
            while (true) {
                // 如果处于暂停状态，挂起直到恢复
                if (!controller.isActive.value) {
                    println("任务暂停，等待恢复...")
//                    controller.isActive.first { it } // 挂起直到 isActive 变为 true

                    controller.isActive.first { it } // 挂起直到 isActive 变为 true
                    println("任务恢复")
                }

                // 执行任务步骤
                println("执行步骤 ${step++}")
                delay(1000) // 模拟耗时操作
            }
        }

        // 模拟外部触发暂停和恢复
        delay(3000)
        println("触发暂停")
        controller.pause()

        delay(2000)
        println("触发恢复")
        controller.resume()

        delay(3000)
        job.cancel() // 取消任务
    }

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






    println("end of main")
}





fun logCurrentThreadName() = println(Thread.currentThread().name)


