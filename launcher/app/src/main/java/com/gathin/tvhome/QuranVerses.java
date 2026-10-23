package com.gathin.tvhome;

import java.util.Random;

/**
 * A small bundled set of short verses, so this part of Muslim Screen needs no network and can't fail.
 * The Arabic text and the Sahih International translation below are reproduced from memory, not fetched
 * from a verified source: check them against a trusted Quran text before relying on them for more than
 * a screensaver.
 */
final class QuranVerses {
    static final class Verse {
        final String arabic;
        final String translation;
        final String reference;

        Verse(String arabic, String translation, String reference) {
            this.arabic = arabic;
            this.translation = translation;
            this.reference = reference;
        }
    }

    private static final Verse[] VERSES = {
            new Verse("مَا يُبَدَّلُ الْقَوْلُ لَدَيَّ وَمَا أَنَا بِظَلَّامٍ لِلْعَبِيدِ",
                    "The word will not be changed with Me, and never will I be unjust to the servants.",
                    "Qaaf 50:29"),
            new Verse("أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
                    "Unquestionably, by the remembrance of Allah hearts are assured.",
                    "Ar-Ra'd 13:28"),
            new Verse("فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
                    "So remember Me; I will remember you. And be grateful to Me and do not deny Me.",
                    "Al-Baqarah 2:152"),
            new Verse("فَإِنَّ مَعَ الْعُسْرِ يُسْرًا",
                    "For indeed, with hardship will be ease.",
                    "Ash-Sharh 94:5"),
            new Verse("وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ",
                    "And whoever relies upon Allah - then He is sufficient for him.",
                    "At-Talaq 65:3"),
            new Verse("ادْعُونِي أَسْتَجِبْ لَكُمْ",
                    "Call upon Me; I will respond to you.",
                    "Ghafir 40:60"),
    };

    private QuranVerses() {}

    static Verse random(Random random) {
        return VERSES[random.nextInt(VERSES.length)];
    }
}
