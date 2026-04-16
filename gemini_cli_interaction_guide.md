# Maximizing Collaboration with the Gemini CLI Agent

This document outlines best practices for interacting with the Gemini CLI agent to maximize efficiency, provide clear context, facilitate brainstorming, and give effective feedback.

## 1. Efficiency: LLMs vs. Humans

The greatest efficiency comes from a **human-LLM partnership**, leveraging the complementary strengths of both.

**Where the Agent Excels (More Efficient):**
*   **Speed and Scale:** Generating boilerplate code, unit tests, or refactoring multiple files rapidly.
*   **Boilerplate and Repetitive Tasks:** Handling tedious, consistent tasks without fatigue (e.g., data class creation, style conversions).
*   **Knowledge Recall:** Instant recall of syntax, library functions, and common patterns from vast training data.
*   **Prototyping:** Quickly generating multiple versions of components or functions for exploration.

**Where Humans Excel (More Efficient):**
*   **Understanding Intent and Context:** Comprehending the 'why' behind features, business goals, user needs, and long-term vision.
*   **High-Level Architecture and Design:** Creative design, foresight, and understanding complex trade-offs for scalable systems.
*   **Debugging Complex Issues:** Intuition and holistic understanding for novel or system-level problems.
*   **Navigating Ambiguity:** Making intelligent assumptions from vague requirements.

**Conclusion:** The agent acts as an incredibly fast, knowledgeable pair programmer and a force multiplier. You provide architectural vision, critical thinking, and understanding of the end goal; the agent provides rapid implementation and tireless execution.

## 2. Providing Context for Efficiency

The more context provided, the better and faster the agent's output.

*   **State the Goal First:** Always begin with the high-level objective.
    *   **Good:** "I need to add retry logic to our API calls in `ApiService.kt`. If a network request fails with a 503 error, it should retry up to 3 times with exponential backoff."
*   **Provide a "Code Anchor":** Show the relevant code, especially for targeted changes.
    *   Use `read_file` for specific files.
    *   For bugs, provide error messages and stack traces from `run_shell_command`.
*   **Specify Constraints and Conventions:** Explicitly state project standards.
    *   "Use the existing `Result` class for error handling."
    *   "This needs to be done without adding any new third-party libraries."
*   **Work Iteratively:** Break complex tasks into smaller sub-tasks.
    *   Give one sub-task at a time.
    *   Review changes (e.g., using `git diff`) and provide feedback for the next step.

## 3. Brainstorming: How to Avoid Deep Dives

To keep the agent in an exploratory, "what-if" mode instead of immediately implementing, frame prompts to request exploration, comparison, or planning.

*   **Frame as a Comparison:** "I'm considering three options for real-time updates: WebSockets, Server-Sent Events (SSE), and long-polling. Can you create a markdown table that compares their pros, cons, and ideal use cases for our app?"
*   **Ask for a High-Level Plan:** "Let's explore the WebSocket idea. Before we write any code, can you outline the main architectural components we would need? For example, what would we need on the server, what on the client, and how would they interact?"
*   **Request Multiple Variations:** "Can you show me two different ways to structure the UI for the user profile page? One using a single-column layout and one using a two-column layout."
*   **Use Explicit Keywords:** "Let's **brainstorm** some ideas for...", "I'm in the **exploration phase** for...", "Before we commit, let's **evaluate** a few alternatives."

## 4. Good vs. Bad Feedback

Direct, specific, and corrective feedback is most effective.

*   **Bad Feedback (Vague):** "That's not right. Try again."
*   **Good Feedback (Specific & Corrective):**
    *   "The code you wrote threw a `NullPointerException` when the user isn't logged in. You need to add a null check for the `user` object at the beginning of the `updateProfile` function."
    *   "You used a `for` loop. The convention in this project is to use functional methods. Please rewrite that logic using `list.map` and `list.filter`."
    *   "This function works, but it has too much nested logic. Can you refactor it using the 'Guard Clause' pattern to return early and reduce the indentation?"
    *   "You created the function as `createUser(name, email, age)`. I prefer a single settings object. Please change the signature to `createUser(options)` where `options` is an object containing `name`, `email`, and `age`."

**Pattern for Good Feedback:** "Here's what was wrong, here's why it was wrong (the rule or convention), and here's how to fix it."

## 5. Clarifying Assumptions and Recognizing Need for Clarification

**To Get the Agent to Clarify Assumptions:**

*   **Ask for a Plan First:** This is the most effective. My assumptions will be embedded in the proposed steps, allowing you to correct them before implementation.
    *   **Agent's (Potentially Wrong) Assumption:** "The file permissions on the server are wrong."
    *   **User's Clarification:** "Actually, I'm sure the server is correct. The problem is likely that the client is not sending the right authentication token. Please focus your investigation on `FileUploadService.kt`."
*   **Ask Direct Questions:** Explicitly ask, "What are you assuming is the primary cause of this problem?"

**To Know When the Agent Needs You to Clarify:**

*   **Direct Questions:** The agent will ask for specific details if the request is too ambiguous.
*   **Overly Generic Plan:** If the agent provides a very high-level or vague plan, it's a signal that more specific instructions are needed.
*   **Agent Does the Wrong Thing:** If the agent's output is incorrect, view it as an indirect request for clarification on your intent or constraints. Provide corrective, specific feedback.

**Golden Rule:** Treat the agent as a very intelligent but extremely literal junior developer with zero business context. You are the senior developer or architect. Be explicit in your instructions to prevent wasted cycles.

## 6. When to Start a New Session

Start a new session to maintain clear context and prevent contamination from unrelated or previous, incorrect paths.

*   **Switching to a New, Unrelated Task:** If the new task is completely distinct from the previous one (e.g., moving from authentication bug fix to UI styling).
*   **Agent is Stuck or Confused:** If the agent repeatedly references old, incorrect assumptions or seems to have lost track of key constraints.
*   **Previous Task is Fully Complete and Committed:** A session can be considered a unit of work; once done, start fresh.
*   **Exploring a Completely Different Approach:** If a previous solution path was unsuccessful and a new direction is being taken.
