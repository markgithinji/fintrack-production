package com.fintrack.shared.feature.user.data.model

import com.fintrack.shared.feature.user.domain.model.User

fun UserDto.toDomain(): User = User(
    name = name,
    email = email,
    trackedCategoryIds = trackedCategoryIds
)
