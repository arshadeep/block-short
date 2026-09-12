package com.still.app.domain

data class InterventionMessage(
    val eyebrow: String,
    val title: String,
    val body: String,
)

object InterventionMessages {
    val hard = InterventionMessage(
        eyebrow = "REELS BLOCKING IS ON",
        title = "Reels can wait.",
        body = "Choose an exit to count today toward your streak.",
    )
}
