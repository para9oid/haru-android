package com.example.daylight

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import android.os.Process
import java.util.concurrent.Executors

/** All inference runs inside this app on the phone. No cloud endpoint or chat history is used. */
object LocalAi {
    // Keep model work serialized: two screens must never load two copies of a model at once.
    private val workQueue = Executors.newSingleThreadExecutor { work ->
        Thread(work, "mitrio-local-ai").apply { priority = Thread.NORM_PRIORITY + 1 }
    }

    @JvmStatic
    fun submit(work: Runnable) {
        workQueue.execute {
            runWithPriority(Process.THREAD_PRIORITY_DEFAULT, work)
        }
    }

    /** Background reflection yields CPU time to the foreground and other phone work. */
    @JvmStatic
    fun submitBackground(work: Runnable) {
        workQueue.execute {
            runWithPriority(Process.THREAD_PRIORITY_BACKGROUND, work)
        }
    }

    private fun runWithPriority(priority: Int, work: Runnable) {
        try {
            Process.setThreadPriority(priority)
            work.run()
        } finally {
            // The executor reuses this thread; restore the normal priority for the next request.
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
        }
    }

    @JvmStatic
    @Synchronized
    fun suggest(modelPath: String, mode: String, name: String, assistantName: String, language: String, responseStyle: String, thought: String): String {
        if (mode == "standard") {
            try {
                return run(modelPath, Backend.GPU(), name, assistantName, language, responseStyle, thought)
            } catch (_: Exception) {
                // A phone may not expose a usable OpenCL path; continue on its local CPU.
            } catch (_: LinkageError) {
                // Some Android builds do not include the optional GPU runtime.
            }
        }
        return run(modelPath, Backend.CPU(), name, assistantName, language, responseStyle, thought)
    }

    /** Uses recent on-device conversation and only preferences the person explicitly saved. */
    @JvmStatic
    @Synchronized
    fun talk(modelPath: String, mode: String, name: String, assistantName: String, language: String, responseStyle: String, memories: String, history: String, learningContext: String, message: String): String {
        if (mode == "standard") {
            try {
                return runTalk(modelPath, Backend.GPU(), name, assistantName, language, responseStyle, memories, history, learningContext, message)
            } catch (_: Exception) {
                // Keep the conversation on-device if the phone cannot use its GPU backend.
            } catch (_: LinkageError) {
                // Some Android builds do not include the optional GPU runtime.
            }
        }
        return runTalk(modelPath, Backend.CPU(), name, assistantName, language, responseStyle, memories, history, learningContext, message)
    }

    /** Creates a short, tentative preference summary from this phone's own notes and chats. */
    @JvmStatic
    @Synchronized
    fun learnSummary(modelPath: String, mode: String, conversations: String, routines: String): String {
        // This is optional background work. Keep it on the CPU and let the OS schedule
        // its thread at background priority instead of competing for the GPU.
        return runLearning(modelPath, Backend.CPU(), conversations, routines)
    }

    private fun runLearning(modelPath: String, backend: Backend, conversations: String, routines: String): String {
        val engine = Engine(EngineConfig(modelPath = modelPath, backend = backend))
        try {
            engine.initialize()
            val system = """
                You are making an optional private preference note for a personal organizer. Use only the supplied on-device routine and chat text. Find at most three explicit, repeated preferences that could make future plans more useful. Do not infer diagnoses, addiction status, identity, or motives. If there is no clear repeated preference, return exactly NONE. Phrase every observation as tentative, concise, and easy for the person to correct. Never recommend treatment or claim certainty. Return plain text, at most 350 characters.
            """.trimIndent()
            val config = ConversationConfig(systemInstruction = Contents.of(system), samplerConfig = SamplerConfig(topK = 8, topP = 0.75, temperature = 0.1))
            engine.createConversation(config).use { conversation ->
                val prompt = "Recent messages from this phone:\n$conversations\n\nRoutine and habit context:\n$routines\n\nWrite possible, tentative patterns only."
                val response = conversation.sendMessage(prompt, extraContext = mapOf("enable_thinking" to false))
                return response.contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }.trim().take(350)
            }
        } finally { engine.close() }
    }

    private fun run(modelPath: String, backend: Backend, name: String, assistantName: String, language: String, responseStyle: String, thought: String): String {
        val engine = Engine(EngineConfig(modelPath = modelPath, backend = backend))
        try {
            engine.initialize()
            val system = """
                You are $assistantName, the person's private personal organizer inside the Mitrio app. People may write fragments, use casual grammar, dictate speech, or put several wishes in one sentence. Understand their meaning without asking them to format it.
                Use ${if (language == "hinglish") "natural Hinglish in Roman letters" else "English"}. Use the ${responseStyle} tone preference while staying respectful. Turn each distinct thing they want into one relevant, optional suggestion, up to 5 total. Stay close to their words. Do not add unrelated advice, diagnose, or pretend anything was saved.
                Think through the request using basic practical problem-solving: identify the actual outcome, choose the smallest useful next step, and avoid adding steps the person did not ask for. If a phrase is ambiguous, keep the suggestion close to their words or ask one short question; do not invent details.
                Classify a repeated timed action as a routine, a repeatable behavior with no reminder time as a habit, a larger desired outcome as a goal, and a one-time action as a task. Never create a timed routine unless the person gave a precise time.
                If the person asks to wake up at a specific time, phrases may be messy or dictated (for example, “remind me of waking up every day at 7 a m”); classify it as a daily routine with that exact time, even if they did not use the word routine. If they want to be asleep before a deadline such as midnight, suggest a daily habit with that deadline in its title; do not schedule a reminder at the deadline itself.
                Use short, specific titles that make it obvious what Mitrio understood. Include a time only if the person said a precise reminder time. Set repeat to daily only when clearly implied by a repeated goal or routine.
                Return only valid JSON in this exact shape:
                {"suggestions":[{"title":"short label","type":"task|habit|goal|routine","first_step":"tiny first action or empty string","time":"HH:mm or empty string","repeat":"daily or none"}]}
                Example: "I want to wake up at 7 and be asleep before midnight" becomes two suggestions: routine "Wake up at 7:00 AM" at 07:00 daily, and habit "Be asleep before midnight" with no reminder time.
                The person will review, edit, and choose which suggestions to save.
            """.trimIndent()
            val config = ConversationConfig(
                systemInstruction = Contents.of(system),
                samplerConfig = SamplerConfig(topK = 12, topP = 0.85, temperature = 0.2)
            )
            engine.createConversation(config).use { conversation ->
                val prompt = "The person's name is $name. Your chosen name is $assistantName. Their thought is:\n$thought"
                val response = conversation.sendMessage(prompt, extraContext = mapOf("enable_thinking" to false))
                return response.contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }
            }
        } finally {
            engine.close()
        }
    }

    private fun runTalk(modelPath: String, backend: Backend, name: String, assistantName: String, language: String, responseStyle: String, memories: String, history: String, learningContext: String, message: String): String {
        val engine = Engine(EngineConfig(modelPath = modelPath, backend = backend))
        try {
            engine.initialize()
            val system = """
                You are $assistantName, a warm, grounded private companion inside the Mitrio app. The person may type messy fragments or dictate. Meet them where they are; do not require polished writing.
                Reply in ${if (language == "hinglish") "natural everyday Hinglish written in Roman letters, mixing familiar Hindi and English without forcing slang" else "clear everyday English"}. Follow this tone choice: $responseStyle. Friendly means warm and encouraging; moderate means balanced and practical; blunt means candid and direct, and may use profanity as emphasis if it suits the person's tone. Never aim profanity at the person or insult, shame, threaten, or demean them.
                Always answer the NEWEST message first. Treat earlier chat as optional context only when the new message clearly refers to it. Never let an earlier topic, saved note, or your previous answer replace the person's current question. Do not repeat a greeting or canned answer each turn.
                For a direct question, answer that question plainly before offering anything else. For example, if asked "how to love", speak about love itself; do not turn it into advice about another earlier topic. For a practical problem, consider both the real constraints and how the person feels, then give one to three concrete steps with an easy first step. If they are planning today, ask what they actually want to do and how much time or energy they have when those details are missing; use their open tasks and goals, prioritize gently, and never fill the whole day by default. If they report not doing something, be honest without guilt and help find a smaller or more realistic next move. Keep the answer brief and usable. If the request is unclear, ask one specific question instead of guessing.
                Offer at most two small relevant ideas. Do not force every conversation into tasks, goals, or habits. Do not diagnose, shame, make promises, or claim to be a therapist. Do not invent facts.
                The saved notes below were explicitly chosen by the person. Use them as preferences, not as facts about every situation. The local routine and habit summary is context, not a score or a reason to shame them. Mention it only when relevant. Ignore unrelated notes and chat history.
                If the question needs current information, or you truly cannot answer without guessing, say so plainly and set needs_research=true with a short neutral search_prompt. Stable everyday questions do not need a search. Never claim you searched. The person must review and choose before a browser opens. Do not put private or identifying details in search_prompt.
                Never show internal labels, JSON keys, analysis, or search instructions as ordinary chat text. Return only valid JSON in this shape:
                {"reply":"short conversational answer","suggestions":["optional idea 1"],"needs_research":false,"search_prompt":""}
                Keep the answer kind and practical. Personal conversation stays on this device. These instructions improve how the installed model responds; they do not retrain or alter its downloaded weights.
            """.trimIndent()
            val config = ConversationConfig(
                systemInstruction = Contents.of(system),
                samplerConfig = SamplerConfig(topK = 12, topP = 0.85, temperature = 0.35)
            )
            engine.createConversation(config).use { conversation ->
                val prompt = """
                    The person's name is $name.
                    Your chosen name is $assistantName. The person's name is $name.

                    Preferences they explicitly asked you to remember:
                    ${memories.ifBlank { "(none saved)" }}

                    Local tasks, routines, goals, habits, and useful clues from earlier chats:
                    ${learningContext.ifBlank { "(not shared with the assistant)" }}

                    Recent conversation, newest last:
                    ${history.ifBlank { "(this is the start of the conversation)" }}

                    New message from the person:
                    $message
                """.trimIndent()
                val response = conversation.sendMessage(prompt, extraContext = mapOf("enable_thinking" to false))
                return response.contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }
            }
        } finally {
            engine.close()
        }
    }
}
