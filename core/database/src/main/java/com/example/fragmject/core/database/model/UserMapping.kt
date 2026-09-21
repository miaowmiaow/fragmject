package com.example.fragmject.core.database.model

import com.example.fragmject.core.model.User

/** [UserEntity]（Room Entity）→ 领域模型 [User]。 */
fun UserEntity.toDomain() = User(
    id = id,
    username = username,
    nickname = nickname,
    token = token,
    password = password,
    admin = admin,
    email = email,
    icon = icon,
    type = type,
    publicName = publicName,
    coinCount = coinCount,
    collectIds = collectIds,
)

/** 领域模型 [User] → [UserEntity]（Room Entity）。 */
fun User.toEntity() = UserEntity(
    id = id,
    username = username,
    nickname = nickname,
    token = token,
    password = password,
    admin = admin,
    email = email,
    icon = icon,
    type = type,
    publicName = publicName,
    coinCount = coinCount,
    collectIds = collectIds,
)
