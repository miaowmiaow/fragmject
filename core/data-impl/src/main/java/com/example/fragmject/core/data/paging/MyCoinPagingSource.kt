package com.example.fragmject.core.data.impl.paging

import com.example.fragmject.core.model.MyCoin
import com.example.fragmject.core.data.contract.remote.MyRemoteDataSource

/**
 * 我的积分明细分页源（page 从 1 开始）。
 */
class MyCoinPagingSource(
    private val remote: MyRemoteDataSource,
) : BasePagingSource<MyCoin>(startPage = 1) {

    override suspend fun fetchPage(page: Int): PageData<MyCoin> {
        val data = remote.getMyCoinList(page).data
        return PageData(
            items = data?.datas.orEmpty(),
            over = data?.over == true,
        )
    }
}
