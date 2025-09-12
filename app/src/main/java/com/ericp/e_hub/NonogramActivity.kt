package com.ericp.e_hub

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import com.ericp.e_hub.nonogram.CelebrationToast
import com.ericp.e_hub.nonogram.GameTimer
import com.ericp.e_hub.nonogram.NonogramGame
import com.ericp.e_hub.nonogram.NonogramStyles
import com.ericp.e_hub.utils.api.NonogramApi
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date

class NonogramActivity : Activity() {

    private lateinit var gridLayout: GridLayout
    private lateinit var newGameButton: Button
    private lateinit var resetButton: Button
    private lateinit var backButton: Button
    private lateinit var settingsButton: Button
    private lateinit var timerText: TextView
    private lateinit var currentTimeText: TextView

    private val game = NonogramGame()
    private val timer = GameTimer()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var celebrationToast: CelebrationToast
    private lateinit var vibrator: Vibrator
    private lateinit var sharedPreferences: SharedPreferences

    private var cellButtons = Array(game.gridSize) { Array<Button?>(game.gridSize) { null } }
    private var rowClueViews = Array<TextView?>(game.gridSize) { null }
    private lateinit var nonogramApi: NonogramApi
    private var colClueViews = Array<TextView?>(game.gridSize) { null }

    // Track game session dates - moved from companion object to instance variable
    private var gameStartDate: Date? = null

    companion object {
        private const val PREFS_NAME = "nonogram_preferences"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_TIMER_VISIBLE = "timer_visible"

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nonogram)

        initializeComponents()
        setupGame()
    }

    private fun initializeComponents() {
        initializeViews()
        initializeTimer()
        initializeVibrator()
        initializePreferences()
        nonogramApi = NonogramApi(this)
        celebrationToast = CelebrationToast(this)
    }

    private fun initializeViews() {
        gridLayout = findViewById(R.id.nonogramGrid)
        newGameButton = findViewById(R.id.newGameButton)
        resetButton = findViewById(R.id.resetButton)
        backButton = findViewById(R.id.backButton)
        settingsButton = findViewById(R.id.nonogramSettingsButton)
        timerText = findViewById(R.id.timerText)
        currentTimeText = findViewById(R.id.currentTimeText)

        timerText.text = getString(R.string.initial_timer_display)

        newGameButton.setOnClickListener { startNewGame() }
        resetButton.setOnClickListener { resetBoard() }
        backButton.setOnClickListener { finish() }
        settingsButton.setOnClickListener { openSettings() }
    }

    private fun openSettings() {
        val intent = Intent(this, NonogramSettingsActivity::class.java)
        startActivity(intent)
    }

    private fun initializeTimer() {
        timer.setOnTimerUpdateListener { gameTime, currentTime ->
            timerText.text = gameTime
            currentTimeText.text = currentTime
        }
    }

    private fun initializeVibrator() {
        val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibrator = vm.defaultVibrator
    }

    private fun initializePreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    private fun isVibrationEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_VIBRATION_ENABLED, true)
    }

    private fun isTimerVisible(): Boolean {
        return sharedPreferences.getBoolean(KEY_TIMER_VISIBLE, true)
    }

    private fun updateTimerVisibility() {
        val visibility = if (isTimerVisible()) android.view.View.VISIBLE else android.view.View.GONE
        timerText.visibility = visibility
        currentTimeText.visibility = visibility
    }

    private fun performHapticFeedback() {
        if (!isVibrationEnabled()) return

        vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun setupGame() {
        gameStartDate = Date()
        game.generateNewPuzzle()
        setupGrid()
        timer.start()
        updateTimerVisibility()
    }

    private fun startNewGame() {
        gameStartDate = Date()
        game.generateNewPuzzle()
        setupGrid()
        timer.restart()
    }

    private fun setupGrid() {
        gridLayout.removeAllViews()
        gridLayout.columnCount = game.gridSize + 1
        gridLayout.rowCount = game.gridSize + 1

        setupCornerCell()
        setupColumnClues()
        setupRowsWithCells()
        validateAllLines()
    }

    private fun setupCornerCell() {
        val cornerView = TextView(this).apply {
            layoutParams = createCellLayoutParams()
            background = NonogramStyles.createCornerDrawable()
        }
        gridLayout.addView(cornerView)
    }

    private fun setupColumnClues() {
        for (j in 0 until game.gridSize) {
            val clueView = createColumnClueView(j)
            colClueViews[j] = clueView
            gridLayout.addView(clueView)
        }
    }

    private fun createColumnClueView(columnIndex: Int): TextView {
        return TextView(this).apply {
            val maxClues = (game.gridSize + 1) / 2
            val clues = game.colClues[columnIndex]

            text = if (clues.contentEquals(arrayOf(0))) {
                "\n\n0"
            } else {
                val paddingNewlines = "\n".repeat(maxClues - clues.size)
                paddingNewlines + clues.joinToString("\n")
            }

            textSize = NonogramStyles.TextSizes.CLUE_TEXT_SIZE
            setTextColor(NonogramStyles.Colors.TEXT_GRAY)
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            layoutParams = createCellLayoutParams()
            background = NonogramStyles.createClueDrawable(false)
            elevation = NonogramStyles.Dimensions.ELEVATION_LOW
        }
    }

    private fun setupRowsWithCells() {
        for (i in 0 until game.gridSize) {
            setupRowClue(i)
            setupRowCells(i)
        }
    }

    private fun setupRowClue(rowIndex: Int) {
        val rowClueView = TextView(this).apply {
            val clueText = if (game.rowClues[rowIndex].contentEquals(arrayOf(0))) {
                "0"
            } else {
                game.rowClues[rowIndex].joinToString(" ")
            }

            text = clueText
            textSize = NonogramStyles.TextSizes.CLUE_TEXT_SIZE
            setTextColor(NonogramStyles.Colors.TEXT_GRAY)
            textAlignment = TextView.TEXT_ALIGNMENT_VIEW_END
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = createCellLayoutParams()
            setPadding(
                NonogramStyles.Dimensions.CELL_PADDING,
                NonogramStyles.Dimensions.CELL_PADDING / 2,
                NonogramStyles.Dimensions.CELL_PADDING,
                NonogramStyles.Dimensions.CELL_PADDING
            )
            background = NonogramStyles.createClueDrawable(false)
            elevation = NonogramStyles.Dimensions.ELEVATION_LOW
        }

        rowClueViews[rowIndex] = rowClueView
        gridLayout.addView(rowClueView)
    }

    private fun setupRowCells(rowIndex: Int) {
        for (j in 0 until game.gridSize) {
            val cellButton = Button(this).apply {
                layoutParams = createCellLayoutParams()
                setOnClickListener { handleCellClick(rowIndex, j) }
            }

            cellButtons[rowIndex][j] = cellButton
            updateCellAppearance(rowIndex, j)
            gridLayout.addView(cellButton)
        }
    }

    private fun createCellLayoutParams(): GridLayout.LayoutParams {
        return GridLayout.LayoutParams().apply {
            width = NonogramStyles.Dimensions.CELL_SIZE
            height = NonogramStyles.Dimensions.CELL_SIZE
            setMargins(
                NonogramStyles.Dimensions.CELL_MARGIN,
                NonogramStyles.Dimensions.CELL_MARGIN,
                NonogramStyles.Dimensions.CELL_MARGIN,
                NonogramStyles.Dimensions.CELL_MARGIN
            )
        }
    }

    private fun handleCellClick(row: Int, col: Int) {
        performHapticFeedback()
        game.toggleCell(row, col)
        updateCellAppearance(row, col)
        validateAllLines()

        if (game.isPuzzleSolved()) {
            handlePuzzleSolved()
        }
    }

    private fun updateCellAppearance(row: Int, col: Int) {
        val button = cellButtons[row][col] ?: return
        val isFilled = game.playerGrid[row][col]

        button.apply {
            background = NonogramStyles.createCellDrawable(isFilled)
            text = ""
            elevation = if (isFilled) {
                NonogramStyles.Dimensions.ELEVATION_HIGH
            } else {
                NonogramStyles.Dimensions.ELEVATION_MEDIUM
            }
        }
    }

    private fun validateAllLines() {
        validateRows()
        validateColumns()
    }

    private fun validateRows() {
        for (i in 0 until game.gridSize) {
            val isValid = game.isLineValid(game.playerGrid[i], game.rowClues[i])
            updateClueAppearance(rowClueViews[i], isValid)
        }
    }

    private fun validateColumns() {
        for (j in 0 until game.gridSize) {
            val column = game.getColumnForValidation(j)
            val isValid = game.isLineValid(column, game.colClues[j])
            updateClueAppearance(colClueViews[j], isValid)
        }
    }

    private fun updateClueAppearance(clueView: TextView?, isValid: Boolean) {
        clueView?.let { view ->
            view.background = NonogramStyles.createClueDrawable(isValid)
            view.setTextColor(
                if (isValid) NonogramStyles.Colors.GREEN_TEXT
                else NonogramStyles.Colors.TEXT_GRAY
            )
            view.elevation = NonogramStyles.Dimensions.ELEVATION_LOW
        }
    }

    private fun handlePuzzleSolved() {
        timer.stop()

        // Submit game data to API
        submitGameData()

        showCelebrationEffects()

        handler.postDelayed({
            startNewGame()
        }, 1000)
    }

    private fun submitGameData() {
        val startDate = gameStartDate
        if (startDate != null) {
            val endDate = Date()
            val startDateStr = dateFormat.format(startDate)
            val endDateStr = dateFormat.format(endDate)

            val gameData = mapOf(
                "started" to startDateStr,
                "ended" to endDateStr
            )

            nonogramApi.submitNonogramAsync(
                data = gameData,
                onSuccess = { _ ->
                    // Game data submitted successfully
                },
                onError = { _ ->
                    // Silently to not disrupt game flow
                }
            )
        }
    }

    private fun showCelebrationEffects() {
        highlightSolutionCells()
        showCelebrationToast()
    }

    private fun highlightSolutionCells() {
        for (i in 0 until game.gridSize) {
            for (j in 0 until game.gridSize) {
                val button = cellButtons[i][j] ?: continue
                if (game.playerGrid[i][j]) {
                    button.background = NonogramStyles.createCellDrawable(isFilled = true, isCelebration = true)
                    button.elevation = NonogramStyles.Dimensions.ELEVATION_CELEBRATION
                }
            }
        }

        handler.postDelayed({
            for (i in 0 until game.gridSize) {
                for (j in 0 until game.gridSize) {
                    updateCellAppearance(i, j)
                }
            }
        }, 800)
    }

    private fun showCelebrationToast() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        celebrationToast.show(rootView)
    }

    override fun onResume() {
        super.onResume()
        timer.resume()
        updateTimerVisibility()
    }

    override fun onPause() {
        super.onPause()
        timer.pause()
    }

    override fun finish() {
        timer.stop()
        super.finish()
    }

    private fun resetBoard() {
        gameStartDate = Date() // Reset start time when board is reset
        game.resetBoard()
        for (i in 0 until game.gridSize) {
            for (j in 0 until game.gridSize) {
                updateCellAppearance(i, j)
            }
        }
        validateAllLines()
        timer.restart()
    }
}
