package com.artf.shoppinglistcompose.ui.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.artf.data.database.model.Product
import com.artf.data.database.model.ShoppingList
import com.artf.shoppinglistcompose.ui.data.model.ProductUi
import com.artf.data.repository.ShoppingListRepository
import com.artf.data.status.ResultStatus
import com.artf.shoppinglistcompose.ui.data.mapper.asDomainModel
import com.artf.shoppinglistcompose.ui.data.mapper.asUiModel
import com.artf.shoppinglistcompose.ui.data.model.MutableScreenState
import com.artf.shoppinglistcompose.ui.data.model.ScreenState
import com.artf.shoppinglistcompose.ui.data.model.ShoppingListUi
import com.artf.shoppinglistcompose.ui.data.status.ScreenStatus
import com.artf.shoppinglistcompose.ui.data.status.ShoppingListStatus
import com.artf.shoppinglistcompose.util.ext.addSourceInvoke
import kotlinx.coroutines.launch

class SharedViewModel constructor(
    private val backStack: ScreenBackStackImpl,
    private val shoppingListRepository: ShoppingListRepository
) : ViewModel(), ScreenBackStack by backStack {

    private val _currentScreen = backStack.getCurrentScreen()
    private val currentScreenStatus: LiveData<ScreenStatus> = _currentScreen

    private val _updateShoppingListLoading = MutableLiveData<Boolean>()
    private val updateShoppingListLoading: LiveData<Boolean> = _updateShoppingListLoading

    private val _deleteProductLoading = MutableLiveData<Boolean>()
    private val deleteProductLoading: LiveData<Boolean> = _deleteProductLoading

    private val _createProductLoading = MutableLiveData<Boolean>()
    private val createProductLoading: LiveData<Boolean> = _createProductLoading

    private val _createShoppingListLoading = MutableLiveData<Boolean>()
    private val createShoppingListLoading: LiveData<Boolean> = _createShoppingListLoading

    private val shoppingListStatus: LiveData<ShoppingListStatus?> = currentScreenStatus.map {
        when (it) {
            is ScreenStatus.CurrentShoppingList -> ShoppingListStatus.CURRENT
            is ScreenStatus.ArchivedShoppingList -> ShoppingListStatus.ARCHIVED
            else -> null
        }
    }

    private val shoppingLists: LiveData<ResultStatus<List<ShoppingList>>> = shoppingListStatus.switchMap {
        when (it) {
            ShoppingListStatus.CURRENT -> shoppingListRepository.getCurrentShoppingList()
            ShoppingListStatus.ARCHIVED -> shoppingListRepository.getArchivedShoppingList()
            else -> MutableLiveData()
        }
    }

    private val shoppingListsUi = shoppingLists.map {
        when (it) {
            is ResultStatus.Loading -> ResultStatus.Loading
            is ResultStatus.Success -> ResultStatus.Success(it.data.asUiModel())
            is ResultStatus.Error -> it
        }
    }

    private val selectedShoppingList = currentScreenStatus.map {
        when (it) {
            is ScreenStatus.CurrentProductList -> it.shoppingList
            is ScreenStatus.ArchivedProductList -> it.shoppingList
            else -> null
        }
    }

    private val productList = selectedShoppingList.switchMap {
        if (it == null) {
            MutableLiveData<ResultStatus<List<Product>>>().apply { value = null }
        } else {
            shoppingListRepository.getProductList(it.id)
        }
    }

    private val productListUi: LiveData<ResultStatus<List<ProductUi>>?> = productList.map {
        when (it) {
            is ResultStatus.Loading -> ResultStatus.Loading
            is ResultStatus.Success -> ResultStatus.Success(it.data.asUiModel())
            is ResultStatus.Error -> it
            else -> null
        }
    }

    fun updateShoppingList(shoppingList: ShoppingListUi, isArchived: Boolean) {
        _updateShoppingListLoading.value = true
        shoppingList.isArchived = isArchived
        viewModelScope.launch {
            shoppingListRepository.updateShoppingList(shoppingList.asDomainModel())
            _updateShoppingListLoading.value = false
        }
    }

    fun createShoppingList(name: String) {
        _createShoppingListLoading.value = true
        val shoppingList = ShoppingList(shoppingListName = name)

        viewModelScope.launch {
            shoppingListRepository.insertShoppingList(shoppingList)
            _createShoppingListLoading.value = false
        }
    }

    fun createProduct(name: String, quantity: Long, shoppingListId: Long) {
        _createProductLoading.value = true

        val product = Product(
            productName = name,
            productQuantity = quantity,
            shoppingListId = shoppingListId
        )
        viewModelScope.launch {
            shoppingListRepository.insertProduct(product)
            _createProductLoading.value = false
        }
    }

    fun deleteProduct(product: ProductUi) {
        _deleteProductLoading.value = true
        viewModelScope.launch {
            shoppingListRepository.deleteProduct(product.asDomainModel())
            _deleteProductLoading.value = false
        }
    }

    private val _screenState = MediatorLiveData<MutableScreenState>().apply {
        value = MutableScreenState(ScreenStatus.CurrentShoppingList)
        addSourceInvoke(currentScreenStatus) { value?.currentScreenStatus = it!! }
        addSourceInvoke(selectedShoppingList) { value?.selectedShoppingList = it }
        addSourceInvoke(createShoppingListLoading) { value?.createShoppingListLoading = it }
        addSourceInvoke(updateShoppingListLoading) { value?.updateShoppingListLoading = it }
        addSourceInvoke(createProductLoading) { value?.createProductLoading = it }
        addSourceInvoke(deleteProductLoading) { value?.deleteProductLoading = it }
        addSourceInvoke(shoppingListsUi) { value?.shoppingListsUi = it }
        addSourceInvoke(productListUi) { value?.productListUi = it }
    }
    val screenState: LiveData<ScreenState> = Transformations.map(_screenState) { it.copy() }
}
