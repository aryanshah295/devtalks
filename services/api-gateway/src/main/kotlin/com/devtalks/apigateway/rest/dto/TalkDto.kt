package com.devtalks.apigateway.rest.dto

import com.devtalks.v1.Talk

// JSON shape returned by /api/v1/talks endpoints. Proto messages don't serialize
// cleanly via Jackson (binary fields, descriptor noise), so we go through small
// Kotlin data classes.

data class TalkDto(
    val id: String,
    val title: String,
    val channel: String,
    val description: String,
    val durationSeconds: Int,
    val publishedAt: String,
    val thumbnailUrl: String,
    val status: String,
)

data class ListTalksDto(
    val talks: List<TalkDto>,
    val nextPageToken: String,
)

data class GetTalkDto(
    val talk: TalkDto,
)

fun Talk.toDto(): TalkDto = TalkDto(
    id = id,
    title = title,
    channel = channel,
    description = description,
    durationSeconds = durationSeconds,
    publishedAt = publishedAt,
    thumbnailUrl = thumbnailUrl,
    status = status.name.removePrefix("TALK_STATUS_"),
)
