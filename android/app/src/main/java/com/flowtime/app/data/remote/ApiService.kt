package com.flowtime.app.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit binding to the Flowtime web API (single-user, no auth). */
interface ApiService {

    // Tasks
    @GET("api/tasks")
    suspend fun getTasks(): List<TaskDto>

    @POST("api/tasks")
    suspend fun createTask(@Body body: TaskBody): TaskDto

    @PATCH("api/tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body body: TaskBody): TaskDto

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): OkResponse

    // Habits
    @GET("api/habits")
    suspend fun getHabits(): List<HabitDto>

    @POST("api/habits")
    suspend fun createHabit(@Body body: HabitBody): HabitDto

    @PATCH("api/habits/{id}")
    suspend fun updateHabit(@Path("id") id: String, @Body body: HabitBody): HabitDto

    @PATCH("api/habits/{id}")
    suspend fun setHabitActive(@Path("id") id: String, @Body body: ActiveBody): HabitDto

    @DELETE("api/habits/{id}")
    suspend fun deleteHabit(@Path("id") id: String): OkResponse

    // Events
    @GET("api/events")
    suspend fun getEvents(): List<EventDto>

    @GET("api/events")
    suspend fun getEventsInRange(
        @Query("from") from: String,
        @Query("to") to: String,
    ): List<EventDto>

    @POST("api/events")
    suspend fun createEvent(@Body body: EventBody): EventDto

    @PATCH("api/events/{id}")
    suspend fun moveEvent(@Path("id") id: String, @Body body: EventMoveBody): EventDto

    @PATCH("api/events/{id}")
    suspend fun moveSeriesEvent(@Path("id") id: String, @Body body: EventSeriesMoveBody): EventDto

    @PATCH("api/events/{id}")
    suspend fun moveSeriesFirst(@Path("id") id: String, @Body body: EventSeriesFirstMoveBody): EventDto

    @PATCH("api/events/{id}")
    suspend fun lockEvent(@Path("id") id: String, @Body body: EventLockBody): EventDto

    @PATCH("api/events/{id}")
    suspend fun setEventBuffer(@Path("id") id: String, @Body body: EventBufferBody): EventDto

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: String): OkResponse

    // Recurring commitments
    @GET("api/recurring")
    suspend fun getRecurring(): List<RecurringEventDto>

    @POST("api/recurring")
    suspend fun createRecurring(@Body body: RecurringBody): RecurringEventDto

    @PATCH("api/recurring/{id}")
    suspend fun updateRecurring(@Path("id") id: String, @Body body: RecurringBody): RecurringEventDto

    @PATCH("api/recurring/{id}")
    suspend fun setRecurringActive(@Path("id") id: String, @Body body: ActiveBody): RecurringEventDto

    @DELETE("api/recurring/{id}")
    suspend fun deleteRecurring(@Path("id") id: String): OkResponse

    // Time off
    @GET("api/timeoff")
    suspend fun getTimeOff(): List<TimeOffDto>

    @POST("api/timeoff")
    suspend fun createTimeOff(@Body body: TimeOffBody): TimeOffDto

    @DELETE("api/timeoff/{id}")
    suspend fun deleteTimeOff(@Path("id") id: String): OkResponse

    // Settings
    @GET("api/settings")
    suspend fun getSettings(): SettingsDto

    @PATCH("api/settings")
    suspend fun updateSettings(@Body body: SettingsBody): SettingsDto

    // Replan
    @POST("api/plan")
    suspend fun plan(): PlanResultDto

    // LLM (deterministic server-side fallbacks when no GROQ key)
    @POST("api/llm/prioritize")
    suspend fun prioritize(@Body body: PrioritizeBody): PrioritizeResult

    @POST("api/llm/parse")
    suspend fun parse(@Body body: ParseBody): ParseResult
}
