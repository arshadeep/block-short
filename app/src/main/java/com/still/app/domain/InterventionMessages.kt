package com.still.app.domain

data class InterventionMessage(
    val eyebrow: String,
    val title: String,
    val body: String,
    val sourceLabel: String,
)

object InterventionMessages {
    val hard = listOf(
        InterventionMessage(
            eyebrow = "THUMB, CLOCK OUT",
            title = "Breaking news: your thumb has been promoted to unpaid night shift.",
            body = "Across a systematic review of people aged 16–25, nighttime digital media use was associated with shorter sleep, poorer sleep quality and more daytime tiredness. The feed gets tomorrow-you. Rude.",
            sourceLabel = "Digital media & sleep review, 2023",
        ),
        InterventionMessage(
            eyebrow = "INFINITE MEANS INFINITE",
            title = "Congrats. You found the part of the internet that never says ‘we should wrap up.’",
            body = "In a small trial of 60 students with heavy short-video use and poor sleep, personalized feeds produced worse sleep scores than community-based feeds. Your willpower is fighting custom software. Bit of a rigged boss battle.",
            sourceLabel = "Feng et al., 2026 · randomized trial",
        ),
        InterventionMessage(
            eyebrow = "PLOT TWIST",
            title = "Consuming every crisis did not personally fix Earth. Weird, right?",
            body = "Across three studies, heavier doomscrolling was associated with more psychological distress and lower life satisfaction, mental wellbeing and harmony. This is correlation—not a diagnosis—but your nervous system may still enjoy the exit.",
            sourceLabel = "Satici et al., 2023 · three-study validation",
        ),
        InterventionMessage(
            eyebrow = "FOMO HAS ENTERED THE CHAT",
            title = "Your brain asked for closure. The feed sent another cliffhanger.",
            body = "In 553 college students, FOMO statistically helped explain the link between short-video addiction and poorer self-reported sleep. Loss of control was tied to difficulty falling asleep. Observational evidence, painfully relatable plot.",
            sourceLabel = "Wu & Yao, 2026 · cross-sectional study",
        ),
        InterventionMessage(
            eyebrow = "FOURTH WALL: BROKEN",
            title = "This reel will not self-destruct. That is, unfortunately, the entire business model.",
            body = "A study of 400 adults linked doomscrolling with lower present-moment awareness and more secondary traumatic stress, which statistically explained lower mental wellbeing. The next swipe is optional. Extremely optional.",
            sourceLabel = "Taskin et al., 2024 · mediation study",
        ),
    )

    val approaching = listOf(
        "Three left. Choose wisely, tiny screen gladiator.",
        "Two left. Your thumb has unionized and demands a break.",
        "Final swipe. Make it cinema, not emotional wallpaper.",
    )

    val limit = InterventionMessage(
        eyebrow = "SEASON FINALE",
        title = "You reached the limit. The surprise villain was ‘just one more’ all along.",
        body = "Leaving protects the time you meant to spend somewhere else. Continuing is still your choice—but first we add enough friction for your frontal lobe to rejoin the group chat.",
        sourceLabel = "Design principle: intentional friction",
    )

    val extension = InterventionMessage(
        eyebrow = "DIRECTOR’S CUT?",
        title = "You requested more plot. The plot is advertisements wearing a trench coat.",
        body = "Most evidence shows associations, not proof that this session causes harm. Still, nighttime media can displace sleep, while FOMO and loss of control are linked with poorer sleep. Read eight seconds; then make the choice on purpose.",
        sourceLabel = "Sleep review, 2023; Wu & Yao, 2026",
    )
}
