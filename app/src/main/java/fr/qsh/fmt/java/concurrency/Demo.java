package fr.qsh.fmt.java.concurrency;

import java.time.Duration;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class Demo {

    public enum State {
        LIVE,
        DEAD,
    }

    public record Point(int x, int y) {
    }

    public static class Board {
        private final int width;
        private final int height;

        /**
         * Compressed storage of cells. Each long contains 64 cells ('long' is used as a bitfield).
         */
        private final long[] cells;

        private final Object[] locks;

        private final AtomicBoolean frozen = new AtomicBoolean(false);

        public Board(int width, int height) {
            if (width * height % 64 != 0)
                throw new IllegalArgumentException("Board size should be a multiple of 64: " + width + " and " + height + " are " + (width * height));

            this.width = width;
            this.height = height;
            cells = new long[(int) (((long) width) * height / 64L)];
            locks = new Object[cells.length / 64];

            for (int i = 0; i < locks.length; i++) {
                locks[i] = new Object();
            }
        }

        public Board(int width, int height, Random random) {
            this(width, height);

            for (int i = 0; i < cells.length; i++) {
                cells[i] = random.nextLong();
            }

            for (int i = 0; i < cells.length * 16; i++) {
                Point p = new Point(random.nextInt(width), random.nextInt(height));
                setCellAt(p, State.DEAD);
            }

            freeze();
        }

        public State cellAt(Point point) {
            try {
                final long linearCoordinate = (long) point.x * height + point.y;
                final long cellIndex = linearCoordinate / 64;
                final long bitIndex = linearCoordinate % 64;
                return (cells[(int) cellIndex] & (1L << bitIndex)) != 0 ? State.LIVE : State.DEAD;
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot find cell at " + point + " in a board with dimensions " + width + "×" + height + " (" + cells.length + " compressed cells)", e);
            }
        }

        public void setCellAt(Point point, State state) {
            if (frozen.get()) {
                throw new IllegalStateException("Cannot modify a frozen board");
            }

            try {
                final long linearCoordinate = (long) point.x * height + point.y;
                final long cellIndex = linearCoordinate / 64;
                final long bitIndex = linearCoordinate % 64;
                synchronized (locks[(int) (cellIndex * locks.length / cells.length)]) {
                    if (state == State.LIVE) {
                        cells[(int) cellIndex] = cells[(int) cellIndex] | (1L << bitIndex);
                    } else {
                        cells[(int) cellIndex] = cells[(int) cellIndex] & ~(1L << bitIndex);
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot set cell at " + point + " in a board with dimensions " + width + "×" + height + " (" + cells.length + " compressed cells)", e);
            }
        }

        public int countNeighborsOf(Point point, State expected) {
            int count = 0;
            for (int x = point.x - 1; x <= point.x + 1; x++) {
                for (int y = point.y - 1; y <= point.y + 1; y++) {
                    if (x == point.x && y == point.y)
                        continue; // Skip the cell itself

                    int realX = x < 0 ? width + x : x >= width ? x - width : x;
                    int realY = y < 0 ? height + y : y >= height ? y - height : y;

                    if (cellAt(new Point(realX, realY)) == expected) {
                        count++;
                    }
                }
            }
            return count;
        }

        public State nextStateOf(Point point) {
            if (cellAt(point) == State.LIVE) {
                final int liveNeighbors = countNeighborsOf(point, State.LIVE);
                if (liveNeighbors < 2 || liveNeighbors > 3) {
                    // Underpopulation, overpopulation
                    return State.DEAD;
                } else {
                    return State.LIVE;
                }
            } else {
                // Reproduction
                if (countNeighborsOf(point, State.LIVE) == 3) {
                    return State.LIVE;
                } else {
                    return State.DEAD;
                }
            }
        }

        public long count(State expected) {
            long count = 0;
            for (long cell : cells) {
                int setBitCount = Long.bitCount(cell);
                count += expected == State.LIVE ? setBitCount : (64 - setBitCount);
            }
            return count;
        }

        @Override
        public String toString() {
            long liveCells = count(State.LIVE);
            return "Board(" + width + "×" + height + "; " + cells.length + " compressed cells, " + (100L * liveCells / (cells.length * 64L)) + "% alive)";
        }

        public void freeze() {
            frozen.set(true);
        }
    }

    private static Board simulateStep(Board board) {
        final Board newBoard = new Board(board.width, board.height);

        for (int x = 0; x < board.width; x++) {
            for (int y = 0; y < board.height; y++) {
                final Point p = new Point(x, y);
                newBoard.setCellAt(p, board.nextStateOf(p));
            }
        }

        newBoard.freeze();
        return newBoard;
    }

    public static void simulate(int iterationCount) {
        Board board = new Board(10000, 10000, new Random(42));

        for (int iteration = 0; iteration < iterationCount; iteration++) {
            System.out.println("Step " + iteration + "/" + iterationCount + " of " + board);
            board = simulateStep(board);
        }

        System.out.println("Final board:\n" + board);
    }

    private record SeedResult(int seed, int iterations) {
    }

    private static void seedHunter() {
        System.out.println("Hunting for the best seed…");
        final var result = IntStream.range(0, 32)
                .parallel()
                .mapToObj(seed -> {
                    Board board = new Board(10000, 10000, new Random(seed));

                    int iteration = 0;
                    while (board.count(State.LIVE) > 1000 && iteration < 30) {
                        board = simulateStep(board);
                        iteration++;
                    }

                    System.out.println("Simulated seed " + seed + " done in " + iteration + " iterations");
                    return new SeedResult(seed, iteration);
                })
                .max(Comparator.comparingInt(SeedResult::iterations))
                .get();
        System.out.println("Best seed: " + result.seed + " with " + result.iterations + " iterations");
    }

    public static void main(String[] args) {
        final long startTime = System.nanoTime();

        simulate(10);

        System.out.println("Total execution time: " + Duration.ofNanos(System.nanoTime() - startTime));
    }
}
