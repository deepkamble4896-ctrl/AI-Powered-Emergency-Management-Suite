AI-Powered Emergency Management Suite

Real-time emergency response system for large-scale hospitality environments. Built for the Google AI Solution Challenge 2026.

Problem Statement
Large hotels manage thousands of guests across multiple floors with no real-time communication layer during emergencies. Existing systems rely on radio calls, manual headcounts, and static evacuation maps — creating dangerous delays when seconds matter.

Solution
A dual-interface mobile system that connects guests directly to security command in real time, powered by Gemini AI and Firebase infrastructure.

Features

- Interactive floor plan with BFS evacuation pathfinding
- Press-and-hold SOS with GPS dispatch to command
- Gemini AI threat triage from natural language reports
- Real-time safety feed across all active incidents
- Staff command dashboard with global broadcast


Tech Stack:
Layer          &        Technology
------------------------------------------------
Frontend       -         Flutter;
AI             - Gemini AI (Google AI Studio);
Database       -       Firebase Firestore;
Auth           -    Firebase Authentication;
Pathfinding    -      BFS Algorithm;

Architecture
Guest App  ──→  Firebase Firestore  ──→  Command Dashboard
     ↓                                        ↑
Gemini AI Triage  ──────────────────────────→ Alert Queue

Project Status
✅ Guest SOS App — complete;
✅ Command Dashboard — complete;
✅ Gemini AI Triage — complete;
🔄 Floor Plan with BFS routing — in progress;
🔄 Multi-floor support — in progress;
🔄 Hotel admin panel — planned;

Built By
Deep Kamble
