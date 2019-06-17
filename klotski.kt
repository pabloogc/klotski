import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashSet

val input = """
XXXXXX
XabbcX
XabbcX
XdeefX
XdghfX
Xi  jX
XXZZXX
""".trimIndent()


fun parseInput(input: String): Board {
    val matrix = input.lines()
        .map { it.toCharArray().toTypedArray() }
        .toTypedArray()

    val boardWidth = matrix[0].size
    val boardHeight = matrix.size

    val pieces = ArrayList<Piece>()
    lateinit var end: Piece
    var indexOfMain = 0

    //Find pieces
    matrix.flatten()
        .forEachIndexed { i, _ ->
            val x = i % boardWidth
            val y = i / boardWidth
            val name = matrix[y][x]
            if (name != ' ' && name != '&') {
                var pieceWidth = 1
                var pieceHeight = 1

                while (x + pieceWidth < boardWidth && matrix[y][x + pieceWidth] == name) {
                    pieceWidth++
                }
                while (y + pieceHeight < boardHeight) {
                    val rowBelow = matrix[y + pieceHeight].drop(x).take(pieceWidth)
                    if (!rowBelow.all { it == name }) {
                        break
                    }
                    pieceHeight++
                }

                val type = when {
                    pieceWidth == 1 && pieceHeight == 1 -> 'a'
                    pieceWidth == 1 && pieceHeight == 2 -> 'b'
                    pieceWidth == 2 && pieceHeight == 1 -> 'c'
                    pieceWidth == 2 && pieceHeight == 2 -> '#'
                    else -> '*'
                }

                val piece = Piece(x, y, pieceWidth, pieceHeight, type, name, pieces.size)
                //Fill it with '&' for processed
                for (xx in piece.left..piece.right) {
                    for (yy in piece.top..piece.bottom) {
                        matrix[yy][xx] = '&'
                    }
                }
                if (name == 'Z') {
                    end = piece
                } else {
                    if (piece.type == '#') {
                        indexOfMain = piece.index
                    }
                    pieces.add(piece)
                }
            }
        }

    return Board(
        width = boardWidth, height = boardHeight, pieces = pieces,
        end = end, indexOfMain = indexOfMain
    )
}

data class Piece(
    val x: Int, val y: Int, val w: Int, val h: Int,
    val type: Char, val name: Char, val index: Int
) {

    val canMove get() = name != 'Z' && name != 'X'

    inline val left get() = x
    inline val top get() = y
    inline val right get() = x + w - 1
    inline val bottom get() = y + h - 1

    fun contains(x: Int, y: Int): Boolean {
        return x >= this.x && x < this.x + this.w
                && y >= this.y && y < this.y + this.h
    }

    fun contains(piece: Piece): Boolean {
        return contains(piece.left, piece.top)
                && contains(piece.right, piece.top)
                && contains(piece.left, piece.bottom)
                && contains(piece.right, piece.bottom)
    }

    fun intersects(piece: Piece): Boolean {
        return contains(piece.left, piece.top)
                || contains(piece.right, piece.top)
                || contains(piece.left, piece.bottom)
                || contains(piece.right, piece.bottom)
    }

    fun offset(direction: Side, times: Int = 1): Piece {
        return this.copy(
            x = this.x + direction.dx * times,
            y = this.y + direction.dy * times
        )
    }

    override fun toString(): String {
        return "$name($x,$y)"
    }

}

enum class Side(val dx: Int, val dy: Int) {
    LEFT(-1, 0), TOP(0, -1), RIGHT(1, 0), BOTTOM(0, 1)
}

data class Movement(val piece: Piece, val side: Side, val steps: Int = 1) {
    override fun toString(): String {
        return "$piece -> ${side}x$steps"
    }
}

data class Board(
    val width: Int,
    val height: Int,
    val pieces: List<Piece>,
    val movements: List<Movement> = emptyList(),
    val end: Piece,
    val indexOfMain: Int,
    val parent: Board? = null
) {
    fun solve(findAll: Boolean): List<Board> {

        val solvedBoards = ArrayList<Board>()
        val evaluatedBoards = LinkedHashSet<Any>()
        //Sort by least amount of moves
        val queue = PriorityQueue<Board>(Comparator { a, b ->
            a.movements.size.compareTo(b.movements.size)
        })

        queue.add(this)
        evaluatedBoards.add(this.encode())
        var currentSize = -1

        while (true) {
            if (queue.isEmpty()) return solvedBoards
            val current = queue.remove()
            if (current.movements.size > currentSize) {
                currentSize = current.movements.size
                println("Evaluating boards solved in $currentSize movements. ${queue.size}")
            }

            for (piece in current.pieces) {
                if (!piece.canMove) continue
                Side.values().forEach { side ->
                    if (current.canMove(piece, side)) {
                        val movement = Movement(piece = piece, side = side, steps = 1)
                        val newBoard = current.movePiece(movement)

                        if (newBoard.isSolved()) {
                            solvedBoards.add(newBoard)
                            if(!findAll) {
                                return solvedBoards
                            }
                        }
                        if (evaluatedBoards.add(newBoard.encode())) {
                            queue.add(newBoard)
                        }
                    }
                }
            }
        }
    }

    private fun canMove(piece: Piece, direction: Side): Boolean {
        if (!piece.canMove) return false
        val moved = piece.offset(direction)
        if (moved.left < 0
            || moved.right >= width
            || moved.top < 0
            || moved.bottom >= height
        ) {
            return false
        }

        val collisionWithAny = pieces.any {
            if (it.index == piece.index) false //It's same piece
            else it.intersects(moved)
        }

        val collisionWithEnd = moved.intersects(end) && moved.type != '#'

        return !(collisionWithAny || collisionWithEnd)
    }

    private fun isSolved(): Boolean {
        return pieces[indexOfMain].contains(end)
    }

    private fun movePiece(movement: Movement): Board {
        val newPieces = pieces.toMutableList()
        val oldPiece = pieces[movement.piece.index]
        val newPiece = oldPiece.offset(movement.side, movement.steps)
        newPieces[movement.piece.index] = newPiece
        val newMovements = movements.toMutableList()
        val lastMovement = newMovements.lastOrNull()

        if (lastMovement != null
            && lastMovement.side == movement.side
            && lastMovement.piece.index == movement.piece.index
        ) {
            newMovements[newMovements.lastIndex] = lastMovement.copy(steps = lastMovement.steps + 1)
        } else {
            newMovements.add(movement)
        }

        return this.copy(pieces = newPieces, movements = newMovements, parent = this)
    }

    override fun toString(): String {
        val out = Array(height) {
            CharArray(width) { '_' }
        }

        pieces.forEach { piece ->
            for (x in piece.left..piece.right) {
                for (y in piece.top..piece.bottom) {
                    out[y][x] = piece.name
                }
            }
        }

        return out.joinToString("\n") { array ->
            array.joinToString("") { "$it" }
        }
    }

    /**
     * Encode board state into easy hashable string.
     */
    private fun encode(): String {
        val out = CharArray(width * height)
        pieces.forEach { piece ->
            for (x in piece.left..piece.right) {
                for (y in piece.top..piece.bottom) {
                    out[y * width + x] = piece.type
                }
            }
        }
        return String(out)
    }
}


fun main() {
    val board = parseInput(input)
    val start = System.currentTimeMillis()
    val solved = board.solve(false).minBy { it.movements.size }!!
    val elapsed = System.currentTimeMillis() - start
    println(solved)

    val steps = ArrayList<Board>()
    var current: Board? = solved
    while (current != null) {
        steps.add(current)
        current = current.parent
    }
    steps.reverse()

    steps.forEach {
        println("---------------------")
        println(it.movements.lastOrNull())
        println(it.toString())
    }
    println("Solved in ${solved.movements.size} moves in $elapsed ms")
}

main()
