package com.devtalks.apigateway.domain

import com.devtalks.v1.Talk
import com.devtalks.v1.TalkStatus
import com.google.cloud.Timestamp
import com.google.cloud.firestore.DocumentSnapshot
import java.time.Instant
import java.time.format.DateTimeFormatter

// Maps between Firestore docs (free-form key/value with native Timestamp / string)
// and the proto Talk message (RFC3339 string for published_at, proto enum for status).
//
// Firestore schema (PLAN.md:227-241):
//   id, title, channel, description: string
//   durationSeconds: number
//   publishedAt, createdAt, updatedAt: timestamp
//   thumbnailUrl: string
//   status: "INGESTED" | "TRANSCRIBED" | "EMBEDDED" | "FAILED"

fun DocumentSnapshot.toTalk(): Talk =
    Talk.newBuilder()
        .setId(getString("id") ?: id)
        .setTitle(getString("title").orEmpty())
        .setChannel(getString("channel").orEmpty())
        .setDescription(getString("description").orEmpty())
        .setDurationSeconds((getLong("durationSeconds") ?: 0L).toInt())
        .setPublishedAt(get("publishedAt")?.let { timestampToRfc3339(it as Timestamp) }.orEmpty())
        .setThumbnailUrl(getString("thumbnailUrl").orEmpty())
        .setStatus(talkStatusFromFirestore(getString("status")))
        .build()

fun Talk.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "title" to title,
    "channel" to channel,
    "description" to description,
    "durationSeconds" to durationSeconds.toLong(),
    "publishedAt" to rfc3339ToTimestamp(publishedAt),
    "thumbnailUrl" to thumbnailUrl,
    "status" to status.toFirestoreString(),
)

fun TalkStatus.toFirestoreString(): String = name.removePrefix("TALK_STATUS_")

fun talkStatusFromFirestore(value: String?): TalkStatus = when (value) {
    null, "" -> TalkStatus.TALK_STATUS_UNSPECIFIED
    else -> runCatching { TalkStatus.valueOf("TALK_STATUS_$value") }
        .getOrDefault(TalkStatus.TALK_STATUS_UNSPECIFIED)
}

private fun timestampToRfc3339(ts: Timestamp): String =
    DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(ts.seconds, ts.nanos.toLong()))

private fun rfc3339ToTimestamp(rfc3339: String): Timestamp {
    val instant = Instant.parse(rfc3339)
    return Timestamp.ofTimeSecondsAndNanos(instant.epochSecond, instant.nano)
}
