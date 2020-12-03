package com.shalatan.entertainmentapp.ui.moviedetail

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import com.shalatan.entertainmentapp.database.MovieDAO
import com.shalatan.entertainmentapp.database.SavedMovie
import com.shalatan.entertainmentapp.model.CompleteMovieDetail
import com.shalatan.entertainmentapp.model.Movie
import com.shalatan.entertainmentapp.network.LmdbApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Month

class DetailViewModel(val database: MovieDAO, movie: Movie, app: Application) :
    AndroidViewModel(app) {

    //fetch all movies to check if current movie already exist in table
    private val databaseMovies = database.getAllMovies()

    // The internal MutableLiveData String that stores the most recent response status
    private val _status = MutableLiveData<String>()

    // The external immutable LiveData for the status String
    val status: LiveData<String>
        get() = _status

    private val _selectedMovieDetail = MutableLiveData<Movie>()
    val selectedMovieDetail: LiveData<Movie>
        get() = _selectedMovieDetail

    private val _completeMovieDetail = MutableLiveData<CompleteMovieDetail>()
    val completeMovieDetail: LiveData<CompleteMovieDetail>
        get() = _completeMovieDetail

    /**
     * Request a toast by setting this value to true.
     */
    private var _showAddedToWatchedSnackbarEvent = MutableLiveData<Boolean>()
    private var _showAddedToWatchLaterSnackbarEvent = MutableLiveData<Boolean>()

    /**
     * If this is true, immediately `show()` a toast and call `doneShowingSnackbar()`.
     */
    val showAddedToWatchedSnackbarEvent: LiveData<Boolean>
        get() = _showAddedToWatchedSnackbarEvent
    val showAddedToWatchLaterSnackbarEvent: LiveData<Boolean>
        get() = _showAddedToWatchLaterSnackbarEvent

    /**
     * Call this immediately after calling `show()` on a toast.
     *
     * It will clear the toast request, so if the user rotates their phone it won't show a duplicate
     * toast.
     */
    fun doneShowingSnackbar() {
        _showAddedToWatchedSnackbarEvent.value = false
        _showAddedToWatchLaterSnackbarEvent.value = false
    }


    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)


    init {
        _selectedMovieDetail.value = movie
        fetchCurrentMovieDetails()
        Log.e("EMPTY",databaseMovies.value?.isEmpty().toString())
    }

    /**
     * fetch complete movie details with retrofit
     */
    private fun fetchCurrentMovieDetails() {
        coroutineScope.launch {
            val getCompleteMovieDetail =
                LmdbApi.retrofitService.getCompleteMovieDetail(_selectedMovieDetail.value!!.id)
            try {
                val completeMovie = getCompleteMovieDetail.await()
                _completeMovieDetail.value = completeMovie
            } catch (t: Throwable) {
                Log.e("Error fetching complete detail : ", t.message.toString())
                _status.value = t.message
            }
        }
    }

    /**
     * add or replace existing data of movie to database with isWatched value true
     */
    fun addMovieToWatched() {
        viewModelScope.launch {
            Log.e("CLICKED", "ADDED TO WATCHED")
            val id = _selectedMovieDetail.value!!.id
            val name = _selectedMovieDetail.value!!.original_title
            val savedMovie =
                name?.let { SavedMovie(id, it, isWatched = true, isWatchLater = false) }
            if (savedMovie != null) {
                insert(savedMovie)
            }
        }
        _showAddedToWatchedSnackbarEvent.value = true
    }

    /**
     * add or replace existing data of movie to database with isWatchlater value true
     */
    fun addMovieToWatchLater() {
        viewModelScope.launch {
            Log.e("CLICKED", "ADDED TO WATCH LATER")
            val id = _selectedMovieDetail.value!!.id
            val name = _selectedMovieDetail.value!!.original_title
            val savedMovie =
                name?.let { SavedMovie(id, it, isWatched = false, isWatchLater = true) }
            if (savedMovie != null) {
                insert(savedMovie)
            }
        }
        _showAddedToWatchLaterSnackbarEvent.value = true
    }

    private suspend fun insert(savedMovie: SavedMovie) {
        database.insert(savedMovie)
    }
}