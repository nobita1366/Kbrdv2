package com.flexboard.ime

import android.content.Context
import com.flexboard.data.db.AppDatabase
import com.flexboard.data.db.DictionaryWord
import com.flexboard.data.db.SavedSentence

object SuggestionEngine {

    private val BUILTIN_EN = listOf(
        "the","be","to","of","and","a","in","that","have","I","it","for","not","on","with","he","as","you","do","at",
        "this","but","his","by","from","they","we","say","her","she","or","an","will","my","one","all","would","there",
        "their","what","so","up","out","if","about","who","get","which","go","me","when","make","can","like","time","no",
        "just","him","know","take","people","into","year","your","good","some","could","them","see","other","than","then",
        "now","look","only","come","its","over","think","also","back","after","use","two","how","our","work","first",
        "well","way","even","new","want","because","any","these","give","day","most","us"
    )

    suspend fun ensureSeeded(ctx: Context) {
        val dao = AppDatabase.get(ctx).dictionaryDao()
        // cheap probe
        val any = dao.suggest("a", 1)
        if (any.isEmpty()) {
            BUILTIN_EN.forEach { dao.insert(DictionaryWord(word = it, frequency = 5)) }
        }
    }

    suspend fun suggest(ctx: Context, prefix: String, prevWord: String?): List<String> {
        val dao = AppDatabase.get(ctx).dictionaryDao()
        if (prefix.isBlank() && prevWord != null) {
            return dao.nextWordFor(prevWord, 3).map { it.word }
        }
        val list = dao.suggest(prefix.lowercase(), 6).map { it.word }
        return list.take(3)
    }

    suspend fun learnWord(ctx: Context, word: String) {
        if (word.isBlank() || word.length > 30) return
        val dao = AppDatabase.get(ctx).dictionaryDao()
        val w = word.lowercase()
        dao.insert(DictionaryWord(word = w))
        dao.bump(w)
    }

    suspend fun learnSentence(ctx: Context, sentence: String) {
        val s = sentence.trim()
        val minLen = com.flexboard.utils.SettingsStore.prefs(ctx)
            .getInt(com.flexboard.utils.SettingsStore.KEY_AUTO_SAVE_MIN_LEN, 12)
        if (s.length < minLen) return
        val enabled = com.flexboard.utils.SettingsStore.prefs(ctx)
            .getBoolean(com.flexboard.utils.SettingsStore.KEY_AUTO_SAVE_SENTENCE, true)
        if (!enabled) return
        val dao = AppDatabase.get(ctx).savedSentenceDao()
        val existing = dao.find(s)
        if (existing == null) dao.insert(SavedSentence(sentence = s))
        else dao.bump(existing.id)
    }
}
