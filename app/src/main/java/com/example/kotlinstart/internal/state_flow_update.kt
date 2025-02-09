import kotlinx.coroutines.flow.StateFlow


// 为 StateFlow 添加扩展函数（适用于所有类似场景）
inline fun <T> StateFlow<List<T>>.update(
    condition: (T) -> Boolean,
    crossinline transform: (T) -> T
): List<T> {
    return value.map { if (condition(it)) transform(it) else it }
}
