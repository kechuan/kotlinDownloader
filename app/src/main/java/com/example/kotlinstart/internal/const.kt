import kotlinx.serialization.Serializable

enum class BinaryType(
    val binaryType: String,
    val size: Long
){
    B("B",0),
    KB("KB",1*1024),
    MB("MB",1*1024*1024),
    GB("GB",1*1024*1024*1024)
}

@Serializable
enum class TaskStatus{
    Pending,
    Activating,
    Finished,
    Paused,
    Stopped
}