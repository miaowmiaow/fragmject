package com.example.fragmject.core.database.model

import com.example.fragmject.core.model.Coin

fun Coin.toEntity(cacheKey: String, sortOrder: Int) = CoinRankEntity(
    userId = userId,
    cacheKey = cacheKey,
    sortOrder = sortOrder,
    username = username,
    nickname = nickname,
    coinCount = coinCount,
    level = level,
    rank = rank,
    timestamp = System.currentTimeMillis(),
)

fun CoinRankEntity.toDomain() = Coin(
    userId = userId,
    username = username,
    nickname = nickname,
    coinCount = coinCount,
    level = level,
    rank = rank,
)
