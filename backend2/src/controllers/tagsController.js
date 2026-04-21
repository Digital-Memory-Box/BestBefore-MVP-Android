export const TAGS_DIRECTORY = {
    general: [
        "sport", "travel", "family", "college", "friends", "work", "hobby", "music", "art"
    ],
    travel: [
        "vacation", "roadtrip", "beach", "mountains", "europe", "asia", "camping", "hiking"
    ],
    events: [
        "wedding", "birthday", "party", "graduation", "anniversary", "festival", "concert"
    ],
    dailyLife: [
        "pets", "food", "cooking", "fitness", "gaming", "reading", "photography", "weekend"
    ],
    vibes: [
        "nostalgia", "happy", "memories", "throwback", "chill", "adventure", "love", "fun"
    ],
    seasons: [
        "summer", "winter", "fall", "spring", "holiday", "christmas", "newyear"
    ],
    projects: [
        "diy", "renovation", "coding", "startup", "study", "learning", "growth"
    ]
}

export const getAllTags = (req, res) => {
    res.json(TAGS_DIRECTORY)
}

export const getFlatTags = (req, res) => {
    const flat = Object.values(TAGS_DIRECTORY).flat()
    res.json(flat)
}
