package com.example.lyrics.romanization

object RomajiConverter {

    // Comprehensive compound words, common lyric Kanji, and phrases in Japanese songs
    private val kanjiCompoundDictionary = mapOf(
        // High frequency song concepts
        "愛" to "ai", "愛してる" to "aishiteru", "愛しい" to "itoshii", "愛情" to "aijou",
        "心" to "kokoro", "想い" to "omoi", "想う" to "omou", "感情" to "kanjou",
        "夢" to "yume", "希望" to "kibou", "未来" to "mirai", "過去" to "kako",
        "現在" to "genzai", "今" to "ima", "明日" to "ashita", "昨日" to "kinou", "今日" to "kyou",
        "光" to "hikari", "輝く" to "kagayaku", "眩しい" to "mabushii", "闇" to "yami",
        "夜" to "yoru", "朝" to "asa", "夕暮れ" to "yuugure", "昼" to "hiru", "真夜中" to "mayonaka",
        "空" to "sora", "青空" to "aozora", "星" to "hoshi", "月" to "tsuki", "太陽" to "taiyou",
        "涙" to "namida", "泣いて" to "naite", "泣かないで" to "nakanaide", "笑顔" to "egao", "笑って" to "waratte",
        "雨" to "ame", "風" to "kaze", "虹" to "niji", "雪" to "yuki", "雲" to "kumo",
        "花" to "hana", "桜" to "sakura", "花火" to "hanabi", "蕾" to "tsubomi",
        "歌" to "uta", "歌う" to "utau", "声" to "koe", "音" to "oto", "言葉" to "kotoba",
        "時間" to "jikan", "時" to "toki", "永遠" to "eien", "刹那" to "setsuna", "瞬間" to "shunkan",
        "世界" to "sekai", "地球" to "chikyuu", "宇宙" to "uchuu", "海" to "umi", "波" to "nami",
        "道" to "michi", "旅" to "tabi", "約束" to "yakusoku", "絆" to "kizuna", "奇跡" to "kiseki",
        "運命" to "unmei", "孤独" to "kodoku", "一人" to "hitori", "二人" to "futari", "皆" to "minna",
        "君" to "kimi", "あなた" to "anata", "僕" to "boku", "私" to "watashi", "俺" to "ore",
        "誰" to "dare", "何処" to "doko", "何時" to "itsu", "何故" to "naze",
        "手" to "te", "瞳" to "hitomi", "目" to "me", "胸" to "mune", "体" to "karada",
        "翼" to "tsubasa", "羽" to "hane", "飛ぶ" to "tobu", "羽ばたく" to "habataku",
        "信じて" to "shinjite", "信じる" to "shinjiru", "忘れない" to "wasurenai", "忘れて" to "wasurete",
        "記憶" to "kioku", "思い出" to "omoide", "温もり" to "nukumori", "優しさ" to "yasashisa",
        "強さ" to "tsuyosa", "弱さ" to "yowasa", "悲しみ" to "kanashimi", "喜び" to "yorokobi",
        "幸せ" to "shiawase", "秘密" to "himitsu", "真実" to "shinjitsu", "嘘" to "uso",
        "始まり" to "hajimari", "終わり" to "owari", "サヨナラ" to "sayonara", "さよなら" to "sayonara",
        "出会い" to "deai", "別れ" to "wakare", "繋ぐ" to "tsunagu", "離さない" to "hasanai",
        "歩き出す" to "arukidasu", "走る" to "hashiru", "見つめる" to "mitsumeru", "見上げて" to "miagete",
        "抱きしめて" to "dakishimete", "届く" to "todoku", "消えない" to "kienai", "揺れる" to "yureru",
        "祈り" to "inori", "願い" to "negai", "叶う" to "kanau", "守る" to "mamoru", "救う" to "sukuu",
        "自由" to "jiyuu", "勇気" to "yuuki", "誇り" to "hokori", "情熱" to "jounetsu",
        "白" to "shiro", "黒" to "kuro", "赤" to "aka", "青" to "ao", "黄色" to "kiiro", "金色" to "kin'iro",
        "春" to "haru", "夏" to "natsu", "秋" to "aki", "冬" to "fuyu", "季節" to "kisetsu",
        "色" to "iro", "景色" to "keshiki", "扉" to "tobira", "鍵" to "kagi", "影" to "kage",
        "鏡" to "kagami", "物語" to "monogatari", "神話" to "shinwa", "魔法" to "mahou",
        "命" to "inochi", "魂" to "tamashii", "呼吸" to "kokyuu", "鼓動" to "kodou",
        "火" to "hi", "炎" to "honoo", "水" to "mizu", "大地" to "daichi",
        "遠く" to "tooku", "近く" to "chikaku", "深海" to "shinkai", "深紅" to "shinku"
    )

    // Japanese Digraphs (Hiragana & Katakana combinations with small y / w)
    private val kanaDigraphs = mapOf(
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
        "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo",
        "シャ" to "sha", "シュ" to "shu", "ショ" to "sho",
        "チャ" to "cha", "チュ" to "chu", "チョ" to "cho",
        "ニャ" to "nya", "ニュ" to "nyu", "ニョ" to "nyo",
        "ヒャ" to "hya", "ヒュ" to "hyu", "ヒョ" to "hyo",
        "ミャ" to "mya", "ミュ" to "myu", "ミョ" to "myo",
        "リャ" to "rya", "リュ" to "ryu", "リョ" to "ryo",
        "ギャ" to "gya", "ギュ" to "gyu", "ギョ" to "gyo",
        "ジャ" to "ja", "ジュ" to "ju", "ジョ" to "jo",
        "ビャ" to "bya", "ビュ" to "byu", "ビョ" to "byo",
        "ピャ" to "pya", "ピュ" to "pyu", "ピョ" to "pyo",
        "ティ" to "ti", "ディ" to "di", "トゥ" to "tu", "ドゥ" to "du",
        "ファ" to "fa", "フィ" to "fi", "フェ" to "fe", "フォ" to "fo",
        "ウィ" to "wi", "ウェ" to "we", "ウォ" to "wo", "ヴァ" to "va",
        "ヴィ" to "vi", "ヴ" to "vu", "ヴェ" to "ve", "ヴォ" to "vo",
        "チェ" to "che", "ジェ" to "je", "シェ" to "she"
    )

    // Single Kana mapping (Hiragana & Katakana)
    private val singleKana = mapOf(
        'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o",
        'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
        'さ' to "sa", 'し' to "shi", 'す' to "su", 'せ' to "se", 'そ' to "so",
        'た' to "ta", 'ち' to "chi", 'つ' to "tsu", 'て' to "te", 'と' to "to",
        'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
        'は' to "ha", 'ひ' to "hi", 'ふ' to "fu", 'へ' to "he", 'ほ' to "ho",
        'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
        'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
        'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
        'わ' to "wa", 'を' to "o", 'ん' to "n",
        'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
        'ざ' to "za", 'じ' to "ji", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
        'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de", 'ど' to "do",
        'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
        'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
        'ぁ' to "a", 'ぃ' to "i", 'ぅ' to "u", 'ぇ' to "e", 'ぉ' to "o",

        'ア' to "a", 'イ' to "i", 'ウ' to "u", 'エ' to "e", 'オ' to "o",
        'カ' to "ka", 'キ' to "ki", 'ク' to "ku", 'ケ' to "ke", 'コ' to "ko",
        'サ' to "sa", 'シ' to "shi", 'ス' to "su", 'セ' to "se", 'ソ' to "so",
        'タ' to "ta", 'チ' to "chi", 'ツ' to "tsu", 'テ' to "te", 'ト' to "to",
        'ナ' to "na", 'ニ' to "ni", 'ヌ' to "nu", 'ネ' to "ne", 'ノ' to "no",
        'ハ' to "ha", 'ヒ' to "hi", 'フ' to "fu", 'ヘ' to "he", 'ホ' to "ho",
        'マ' to "ma", 'ミ' to "mi", 'ム' to "mu", 'メ' to "me", 'モ' to "mo",
        'ヤ' to "ya", 'ユ' to "yu", 'ヨ' to "yo",
        'ラ' to "ra", 'リ' to "ri", 'ル' to "ru", 'レ' to "re", 'ロ' to "ro",
        'ワ' to "wa", 'ヲ' to "o", 'ン' to "n",
        'ガ' to "ga", 'ギ' to "gi", 'グ' to "gu", 'ゲ' to "ge", 'ゴ' to "go",
        'ザ' to "za", 'ジ' to "ji", 'ズ' to "zu", 'ゼ' to "ze", 'ゾ' to "zo",
        'ダ' to "da", 'ヂ' to "ji", 'ヅ' to "zu", 'デ' to "de", 'ド' to "do",
        'バ' to "ba", 'ビ' to "bi", 'ブ' to "bu", 'ベ' to "be", 'ボ' to "bo",
        'パ' to "pa", 'ピ' to "pi", 'プ' to "pu", 'ペ' to "pe", 'ポ' to "po",
        'ァ' to "a", 'ィ' to "i", 'ゥ' to "u", 'ェ' to "e", 'ォ' to "o",
        'ー' to "-"
    )

    // Korean Hangul Jamo tables
    private val hangulInitials = arrayOf(
        "g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h"
    )
    private val hangulMedials = arrayOf(
        "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i"
    )
    private val hangulFinals = arrayOf(
        "", "k", "k", "ks", "n", "nj", "nh", "d", "l", "lg", "lm", "lb", "ls", "lt", "lp", "lh", "m", "b", "bs", "s", "ss", "ng", "j", "ch", "k", "t", "p", "h"
    )

    // Cyrillic to Latin mapping
    private val cyrillicMap = mapOf(
        'А' to "A", 'а' to "a", 'Б' to "B", 'б' to "b", 'В' to "V", 'в' to "v",
        'Г' to "G", 'г' to "g", 'Д' to "D", 'д' to "d", 'Е' to "E", 'е' to "e",
        'Ё' to "Yo", 'ё' to "yo", 'Ж' to "Zh", 'ж' to "zh", 'З' to "Z", 'з' to "z",
        'И' to "I", 'и' to "i", 'Й' to "Y", 'й' to "y", 'К' to "K", 'к' to "k",
        'Л' to "L", 'л' to "l", 'М' to "M", 'м' to "m", 'Н' to "N", 'н' to "n",
        'О' to "O", 'о' to "o", 'П' to "P", 'п' to "p", 'Р' to "R", 'р' to "r",
        'С' to "S", 'с' to "s", 'Т' to "T", 'т' to "t", 'У' to "U", 'у' to "u",
        'Ф' to "F", 'ф' to "f", 'Х' to "Kh", 'х' to "kh", 'Ц' to "Ts", 'ц' to "ts",
        'Ч' to "Ch", 'ч' to "ch", 'Ш' to "Sh", 'ш' to "sh", 'Щ' to "Shch", 'щ' to "shch",
        'Ъ' to "", 'ъ' to "", 'Ы' to "Y", 'ы' to "y", 'Ь' to "'", 'ь' to "'",
        'Э' to "E", 'э' to "e", 'Ю' to "Yu", 'ю' to "yu", 'Я' to "Ya", 'я' to "ya"
    )

    // Popular Mandarin characters to Pinyin
    private val pinyinCommon = mapOf(
        '你' to "ni", '好' to "hao", '我' to "wo", '的' to "de", '爱' to "ai", '心' to "xin",
        '在' to "zai", '是' to "shi", '不' to "bu", '了' to "le", '人' to "ren", '有' to "you",
        '天' to "tian", '地' to "di", '风' to "feng", '月' to "yue", '夜' to "ye", '雨' to "yu",
        '花' to "hua", '梦' to "meng", '海' to "hai", '光' to "guang", '一' to "yi", '生' to "sheng",
        '想' to "xiang", '念' to "nian", '忘' to "wang", '记' to "ji", '走' to "zou", '听' to "ting",
        '说' to "shuo", '歌' to "ge", '声' to "sheng", '情' to "qing", '泪' to "lei", '笑' to "xiao"
    )

    fun convertLine(input: String): String {
        if (input.isBlank()) return ""

        // Check if string contains any Asian or Cyrillic characters
        if (!hasTransliterableCharacters(input)) {
            return input
        }

        val sb = StringBuilder()
        var i = 0
        val len = input.length

        while (i < len) {
            val c = input[i]

            // 1. Check Hangul syllables (0xAC00 to 0xD7A3)
            if (c in '\uAC00'..'\uD7A3') {
                val syllableIndex = c.code - 0xAC00
                val initialIndex = syllableIndex / (21 * 28)
                val medialIndex = (syllableIndex % (21 * 28)) / 28
                val finalIndex = syllableIndex % 28

                val romanized = hangulInitials[initialIndex] +
                        hangulMedials[medialIndex] +
                        hangulFinals[finalIndex]

                sb.append(romanized)
                i++
                continue
            }

            // 2. Check Cyrillic
            if (cyrillicMap.containsKey(c)) {
                sb.append(cyrillicMap[c])
                i++
                continue
            }

            // 3. Check Kanji compounds (lookahead up to 6 characters)
            var matchedCompound = false
            for (lookAhead in 6 downTo 2) {
                if (i + lookAhead <= len) {
                    val sub = input.substring(i, i + lookAhead)
                    if (kanjiCompoundDictionary.containsKey(sub)) {
                        if (sb.isNotEmpty() && sb.last().isLetterOrDigit()) sb.append(" ")
                        sb.append(kanjiCompoundDictionary[sub])
                        i += lookAhead
                        matchedCompound = true
                        break
                    }
                }
            }
            if (matchedCompound) continue

            // 4. Single Kanji check
            val singleKanjiStr = c.toString()
            if (kanjiCompoundDictionary.containsKey(singleKanjiStr)) {
                if (sb.isNotEmpty() && sb.last().isLetterOrDigit()) sb.append(" ")
                sb.append(kanjiCompoundDictionary[singleKanjiStr])
                i++
                continue
            }

            // 5. Small tsu (sokuon) doubling next consonant
            if (c == 'っ' || c == 'ッ') {
                if (i + 1 < len) {
                    val next = input[i + 1]
                    // Peek next kana romanization
                    val nextRomaji = kanaDigraphs[input.substring(i + 1, minOf(len, i + 3))]
                        ?: singleKana[next]
                    if (nextRomaji != null && nextRomaji.isNotEmpty()) {
                        val firstChar = nextRomaji[0]
                        if (firstChar !in "aeiou-") {
                            sb.append(if (nextRomaji.startsWith("ch")) 't' else firstChar)
                            i++
                            continue
                        }
                    }
                }
                i++
                continue
            }

            // 6. Check Kana digraphs (2 characters)
            if (i + 1 < len) {
                val dig = input.substring(i, i + 2)
                if (kanaDigraphs.containsKey(dig)) {
                    sb.append(kanaDigraphs[dig])
                    i += 2
                    continue
                }
            }

            // 7. Check single Kana
            if (singleKana.containsKey(c)) {
                val rom = singleKana[c]!!
                // Check if particle は (wa) or へ (e)
                if (c == 'は' && (i == len - 1 || input[i + 1].isWhitespace())) {
                    sb.append("wa")
                } else if (c == 'へ' && (i == len - 1 || input[i + 1].isWhitespace())) {
                    sb.append("e")
                } else {
                    sb.append(rom)
                }
                i++
                continue
            }

            // 8. Mandarin Pinyin fallback
            if (pinyinCommon.containsKey(c)) {
                if (sb.isNotEmpty() && sb.last().isLetterOrDigit()) sb.append(" ")
                sb.append(pinyinCommon[c])
                i++
                continue
            }

            // Standard latin, punctuation, or spaces
            sb.append(c)
            i++
        }

        return sb.toString().replace(Regex("\\s{2,}"), " ").trim()
    }

    private fun hasTransliterableCharacters(text: String): Boolean {
        for (c in text) {
            if (c in '\u3040'..'\u309F' || // Hiragana
                c in '\u30A0'..'\u30FF' || // Katakana
                c in '\u4E00'..'\u9FAF' || // CJK Kanji
                c in '\uAC00'..'\uD7A3' || // Hangul
                c in '\u0400'..'\u04FF'    // Cyrillic
            ) {
                return true
            }
        }
        return false
    }
}
