package com.ericp.e_hub.nonogram

import kotlin.random.Random

data class NonogramGame(
    val gridSize: Int = 5
) {
    var solution = Array(gridSize) { BooleanArray(gridSize) }
        private set

    var playerGrid = Array(gridSize) { BooleanArray(gridSize) }
        private set

    var rowClues = Array(gridSize) { emptyArray<Int>() }
        private set

    var colClues = Array(gridSize) { emptyArray<Int>() }
        private set

    fun generateNewPuzzle() {
        var attempts = 0
        do {
            generateRandomSolution()
            calculateClues()
            attempts++
        } while (!isPuzzleValid() && attempts < 100)

        if (attempts >= 100) {
            createSimplePuzzle()
            calculateClues()
        }

        resetPlayerGrid()
    }

    private fun generateRandomSolution() {
        val fillProbability = 0.4 + Random.nextDouble() * 0.2
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                solution[i][j] = Random.nextDouble() < fillProbability
            }
        }
    }

    private fun createSimplePuzzle() {
        solution = Array(gridSize) { BooleanArray(gridSize) }
        val patterns = listOf(
            arrayOf(
                booleanArrayOf(false, false, true, false, false),
                booleanArrayOf(false, false, true, false, false),
                booleanArrayOf(true, true, true, true, true),
                booleanArrayOf(false, false, true, false, false),
                booleanArrayOf(false, false, true, false, false)
            ),
            arrayOf(
                booleanArrayOf(false, false, true, false, false),
                booleanArrayOf(false, true, false, true, false),
                booleanArrayOf(true, false, false, false, true),
                booleanArrayOf(false, true, false, true, false),
                booleanArrayOf(false, false, true, false, false)
            ),
            arrayOf(
                booleanArrayOf(true, true, true, true, true),
                booleanArrayOf(true, false, false, false, true),
                booleanArrayOf(true, false, false, false, true),
                booleanArrayOf(true, false, false, false, true),
                booleanArrayOf(true, true, true, true, true)
            )
        )

        val selectedPattern = patterns[Random.nextInt(patterns.size)]
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                solution[i][j] = selectedPattern[i][j]
            }
        }
    }

    private fun calculateClues() {
        for (i in 0 until gridSize) {
            rowClues[i] = calculateLineClues(solution[i])
        }

        for (j in 0 until gridSize) {
            val column = BooleanArray(gridSize) { i -> solution[i][j] }
            colClues[j] = calculateLineClues(column)
        }
    }

    private fun calculateLineClues(line: BooleanArray): Array<Int> {
        val clues = mutableListOf<Int>()
        var currentGroup = 0

        for (cell in line) {
            if (cell) {
                currentGroup++
            } else {
                if (currentGroup > 0) {
                    clues.add(currentGroup)
                    currentGroup = 0
                }
            }
        }

        if (currentGroup > 0) {
            clues.add(currentGroup)
        }

        return if (clues.isEmpty()) arrayOf(0) else clues.toTypedArray()
    }

    private fun isPuzzleValid(): Boolean {
        for (i in 0 until gridSize) {
            if (rowClues[i].sum() > gridSize) return false
            if (colClues[i].sum() > gridSize) return false
        }

        val totalClues = rowClues.sumOf { it.size } + colClues.sumOf { it.size }
        return totalClues in 6..20
    }

    private fun resetPlayerGrid() {
        playerGrid = Array(gridSize) { BooleanArray(gridSize) }
    }

    fun resetBoard() {
        playerGrid = Array(gridSize) { BooleanArray(gridSize) }
    }

    fun toggleCell(row: Int, col: Int) {
        if (row in 0 until gridSize && col in 0 until gridSize) {
            playerGrid[row][col] = !playerGrid[row][col]
        }
    }

    fun isLineValid(line: BooleanArray, clues: Array<Int>): Boolean {
        val actualClues = calculateLineClues(line)
        return if (clues.contentEquals(arrayOf(0)) && actualClues.contentEquals(arrayOf(0))) {
            true
        } else {
            actualClues.contentEquals(clues)
        }
    }

    fun isPuzzleSolved(): Boolean {
        for (i in 0 until gridSize) {
            if (!isLineValid(playerGrid[i], rowClues[i])) {
                return false
            }
        }

        for (j in 0 until gridSize) {
            val column = BooleanArray(gridSize) { i -> playerGrid[i][j] }
            if (!isLineValid(column, colClues[j])) {
                return false
            }
        }

        return true
    }

    fun getColumnForValidation(col: Int): BooleanArray {
        return BooleanArray(gridSize) { row -> playerGrid[row][col] }
    }
}
