
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FabState(
    var currentValue:Int = 0
)

object MainViewModel : ViewModel() {

    private val _mainFabState = MutableStateFlow(FabState())
    val mainFabState = _mainFabState.asStateFlow()


    init{
        Log.d("MainViewModel",mainFabState.value.toString()) // 打印以确认变量是否被初始化
        initModel()
    }

    fun initModel(){
        _mainFabState.value.currentValue = 3
    }

    fun increaseCount(){
        //更新需要赋值整个Object才能检测得到更新 不知道以后有没有改良手段
        _mainFabState.value = _mainFabState.value.copy(
            currentValue = mainFabState.value.currentValue+1
        )

    }

}




