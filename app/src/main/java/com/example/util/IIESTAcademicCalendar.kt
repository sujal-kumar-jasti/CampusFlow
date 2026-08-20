package com.example.util

import java.text.SimpleDateFormat
import java.util.*

object IIESTAcademicCalendar {

    data class CalendarEvent(
        val name: String, 
        val date: String, 
        val type: EventType,
        val isImportant: Boolean = false // Only true for Registration, Fees, Exams, Electives
    )
    
    enum class EventType { HOLIDAY, ACADEMIC, FEST, EXAM, BREAK, REGISTRATION, ADMIN }

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val events = listOf(
        // HOLIDAYS (Standard 2026-27)
        CalendarEvent("Republic Day", "2026-01-26", EventType.HOLIDAY),
        CalendarEvent("Holi", "2026-03-03", EventType.HOLIDAY),
        CalendarEvent("Good Friday", "2026-04-03", EventType.HOLIDAY),
        CalendarEvent("Independence Day", "2026-08-15", EventType.HOLIDAY),
        CalendarEvent("Mahatma Gandhi's Birthday", "2026-10-02", EventType.HOLIDAY),
        CalendarEvent("Dussehra", "2026-10-24", EventType.HOLIDAY),
        CalendarEvent("Diwali", "2026-11-12", EventType.HOLIDAY),
        CalendarEvent("Christmas Day", "2026-12-25", EventType.HOLIDAY),
        
        // ODD SEMESTER 2026-27 (UG 7th SEM RELEVANT)
        CalendarEvent("Elective Finalization Deadline", "2026-07-10", EventType.ADMIN, true),
        CalendarEvent("Sem Pre-registration (W1)", "2026-07-13", EventType.REGISTRATION, true),
        CalendarEvent("Sem Pre-registration Deadline (W1)", "2026-07-19", EventType.REGISTRATION, true),
        CalendarEvent("Registration with Late Fine (W2)", "2026-07-20", EventType.REGISTRATION, true),
        CalendarEvent("Registration High Late Fine (W3)", "2026-07-27", EventType.REGISTRATION, true),
        CalendarEvent("Registration Ends", "2026-07-31", EventType.REGISTRATION, true),
        
        CalendarEvent("Beginning of Classes", "2026-07-20", EventType.ACADEMIC),
        CalendarEvent("Project Guide Allotment", "2026-07-24", EventType.ADMIN),
        
        CalendarEvent("INSTRUO (Technical Fest)", "2026-09-24", EventType.FEST),
        CalendarEvent("INSTRUO (Technical Fest)", "2026-09-25", EventType.FEST),
        CalendarEvent("INSTRUO (Technical Fest)", "2026-09-26", EventType.FEST),
        CalendarEvent("INSTRUO (Technical Fest)", "2026-09-27", EventType.FEST),
        
        CalendarEvent("Mid-Semester Exams", "2026-10-05", EventType.EXAM, true),
        CalendarEvent("Mid-Semester Exams End", "2026-10-13", EventType.EXAM, true),
        CalendarEvent("Mid-Sem Make-up Application", "2026-10-19", EventType.EXAM),
        
        CalendarEvent("Semester Break", "2026-10-19", EventType.BREAK),
        CalendarEvent("Semester Break End", "2026-10-25", EventType.BREAK),
        
        CalendarEvent("Mid-Sem Make-up Exam", "2026-10-26", EventType.EXAM),
        CalendarEvent("Showing Mid-Sem Answer Scripts", "2026-10-29", EventType.ADMIN),
        
        CalendarEvent("Last Day of Classes", "2026-11-13", EventType.ACADEMIC),
        CalendarEvent("End-Semester Exams Begin", "2026-11-16", EventType.EXAM, true),
        CalendarEvent("Institute Foundation Day", "2026-11-24", EventType.FEST),
        CalendarEvent("End-Semester Exams End", "2026-11-28", EventType.EXAM, true),
        
        CalendarEvent("Winter Break", "2026-12-01", EventType.BREAK),
        CalendarEvent("Winter Break End", "2026-12-31", EventType.BREAK),

        // EVEN SEMESTER 2026-27 (UG 8th SEM RELEVANT)
        CalendarEvent("Even Sem Registration (W1)", "2026-12-07", EventType.REGISTRATION, true),
        CalendarEvent("Even Sem Registration Deadline", "2026-12-14", EventType.REGISTRATION, true),
        CalendarEvent("Even Sem Late Registration", "2026-12-15", EventType.REGISTRATION, true),
        
        CalendarEvent("Classes Begin (Even Sem)", "2027-01-04", EventType.ACADEMIC),
        CalendarEvent("REBECA (Cultural Fest)", "2027-01-21", EventType.FEST),
        CalendarEvent("REBECA (Cultural Fest)", "2027-01-22", EventType.FEST),
        CalendarEvent("REBECA (Cultural Fest)", "2027-01-24", EventType.FEST),
        CalendarEvent("Annual Athletic Meet", "2027-01-29", EventType.FEST),
        
        CalendarEvent("Supplementary Exams", "2027-02-01", EventType.EXAM),
        CalendarEvent("Mid-Semester Exams (UG)", "2027-02-22", EventType.EXAM, true),
        CalendarEvent("Mid-Semester Exams End", "2027-02-27", EventType.EXAM, true),
        
        CalendarEvent("Showing Mid-Sem Scripts", "2027-04-02", EventType.ADMIN),
        CalendarEvent("Last Day of Classes", "2027-04-23", EventType.ACADEMIC),
        CalendarEvent("End-Semester Exams Begin", "2027-04-26", EventType.EXAM, true),
        CalendarEvent("End-Semester Exams End", "2027-05-10", EventType.EXAM, true),
        
        CalendarEvent("Summer Vacation", "2027-05-17", EventType.BREAK),
        CalendarEvent("Summer Vacation End", "2027-07-16", EventType.BREAK)
    )
    
    fun getEventsForDate(dateStr: String): List<CalendarEvent> {
        return events.filter { it.date == dateStr }
    }

    fun isNoClassDay(dateStr: String): Boolean {
        val dateEvents = getEventsForDate(dateStr)
        if (dateEvents.isEmpty()) return false
        return dateEvents.any { 
            it.type == EventType.HOLIDAY || 
            it.type == EventType.BREAK || 
            (it.type == EventType.FEST && (dateStr == "2026-09-24" || dateStr == "2026-09-25" || dateStr == "2027-01-22" || dateStr == "2027-01-29"))
        }
    }
    
    fun getUpcomingEvents(limit: Int = 10): List<CalendarEvent> {
        val now = sdf.format(Date())
        return events.filter { it.date >= now }
            .sortedBy { it.date }
            .take(limit)
    }
}
