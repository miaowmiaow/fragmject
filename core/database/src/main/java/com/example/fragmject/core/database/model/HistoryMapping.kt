package com.example.fragmject.core.database.model

import com.example.fragmject.core.model.History

/** [HistoryEntity]（Room Entity）→ 领域模型 [History]。 */
fun HistoryEntity.toDomain() = History(
    id = id,
    key = key,
    value = value,
    url = url,
)

/** 领域模型 [History] → [HistoryEntity]（Room Entity）。 */
fun History.toEntity() = HistoryEntity(
    id = id,
    key = key,
    value = value,
    url = url,
)
