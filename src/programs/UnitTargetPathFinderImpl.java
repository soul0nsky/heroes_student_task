package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.Edge;
import com.battle.heroes.army.programs.UnitTargetPathFinder;

import java.util.*;

/**
 * Реализация поиска кратчайшего пути между юнитами на игровом поле.
 *
 * Используем алгоритм A* (A-star) с поддержкой движения в 8 направлениях (включая диагонали).
 * Учитывает препятствия в виде других юнитов на поле.
 */
public class UnitTargetPathFinderImpl implements UnitTargetPathFinder {
    private static final int FIELD_WIDTH = 27;
    private static final int FIELD_HEIGHT = 21;


    // Направления движения: 8 направлений (4 кардинальных + 4 диагональных).
    private static final int[][] DIRECTIONS = {
            {0, 1},   // Вверх
            {1, 0},   // Вправо
            {0, -1},  // Вниз
            {-1, 0},  // Влево
            {1, 1},   // Вправо-вверх (диагональ)
            {1, -1},  // Вправо-вниз (диагональ)
            {-1, 1},  // Влево-вверх (диагональ)
            {-1, -1}  // Влево-вниз (диагональ)
    };

    private static final int STRAIGHT_MOVE_COST = 10;

    private static final int DIAGONAL_MOVE_COST = 14;

    // Коэффициент для октильной эвристики: (√2 - 1) * STRAIGHT_MOVE_COST ≈ 4.14
    private static final int OCTILE_DIAGONAL_FACTOR = 4;

    private static final int INFINITY = Integer.MAX_VALUE;

    /**
     * Находит кратчайший путь между атакующим и целевым юнитом.
     *
     * Алгоритм A* работает следующим образом:
     *   Инициализация: создаём карту занятости, очередь с приоритетом
     *   Добавляем стартовую позицию в очередь
     *   На каждой итерации берём клетку с минимальным f(n) = g(n) + h(n)
     *   Исследуем соседей, обновляем их стоимости если нашли путь короче
     *   Повторяем пока не достигнем цели или очередь не опустеет
     */
    @Override
    public List<Edge> getTargetPath(Unit attackUnit, Unit targetUnit, List<Unit> existingUnitList) {
        if (!isValidInput(attackUnit, targetUnit)) {
            return Collections.emptyList();
        }

        int startX = attackUnit.getxCoordinate();
        int startY = attackUnit.getyCoordinate();
        int targetX = targetUnit.getxCoordinate();
        int targetY = targetUnit.getyCoordinate();

        if (startX == targetX && startY == targetY) {
            return Collections.singletonList(new Edge(startX, startY));
        }

        boolean[][] occupiedCells = createOccupancyMap(existingUnitList, attackUnit, targetUnit);

        return executeAStarSearch(startX, startY, targetX, targetY, occupiedCells);
    }

    private boolean isValidInput(Unit attackUnit, Unit targetUnit) {
        if (attackUnit == null || targetUnit == null) {
            return false;
        }
        if (!targetUnit.isAlive()) {
            return false;
        }
        return true;
    }

    private boolean[][] createOccupancyMap(List<Unit> units, Unit attackUnit, Unit targetUnit) {
        boolean[][] occupied = new boolean[FIELD_WIDTH][FIELD_HEIGHT];

        for (Unit unit : units) {
            // Пропускаем мёртвых, атакующего и цель
            if (!unit.isAlive() || unit == attackUnit || unit == targetUnit) {
                continue;
            }

            int x = unit.getxCoordinate();
            int y = unit.getyCoordinate();

            if (isWithinBounds(x, y)) {
                occupied[x][y] = true;
            }
        }

        return occupied;
    }

    // Выполняем поиск пути алгоритмом A*.
    private List<Edge> executeAStarSearch(
            int startX, int startY,
            int targetX, int targetY,
            boolean[][] occupied) {

        PriorityQueue<PathNode> openSet = new PriorityQueue<>(
                Comparator.comparingInt(node -> node.fCost)
        );

        int[][] gCost = new int[FIELD_WIDTH][FIELD_HEIGHT];  // Стоимость пути от старта
        PathNode[][] cameFrom = new PathNode[FIELD_WIDTH][FIELD_HEIGHT];  // Для восстановления пути
        boolean[][] inClosedSet = new boolean[FIELD_WIDTH][FIELD_HEIGHT];  // Уже обработанные
        boolean[][] inOpenSet = new boolean[FIELD_WIDTH][FIELD_HEIGHT];  // В очереди

        // Инициализация gCost бесконечностью
        for (int x = 0; x < FIELD_WIDTH; x++) {
            Arrays.fill(gCost[x], INFINITY);
        }

        gCost[startX][startY] = 0;
        int startH = calculateOctileHeuristic(startX, startY, targetX, targetY);
        PathNode startNode = new PathNode(startX, startY, 0, startH);
        openSet.add(startNode);
        inOpenSet[startX][startY] = true;

        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();
            int currentX = current.x;
            int currentY = current.y;

            if (currentX == targetX && currentY == targetY) {
                return reconstructPath(cameFrom, targetX, targetY, startX, startY);
            }

            inOpenSet[currentX][currentY] = false;
            inClosedSet[currentX][currentY] = true;

            exploreNeighbors(
                    current, targetX, targetY,
                    occupied, gCost, cameFrom,
                    openSet, inOpenSet, inClosedSet
            );
        }

        return Collections.emptyList();
    }

    private void exploreNeighbors(
            PathNode current,
            int targetX, int targetY,
            boolean[][] occupied,
            int[][] gCost,
            PathNode[][] cameFrom,
            PriorityQueue<PathNode> openSet,
            boolean[][] inOpenSet,
            boolean[][] inClosedSet) {

        int currentX = current.x;
        int currentY = current.y;

        for (int[] direction : DIRECTIONS) {
            int neighborX = currentX + direction[0];
            int neighborY = currentY + direction[1];

            if (!isWithinBounds(neighborX, neighborY)) {
                continue;
            }
            if (occupied[neighborX][neighborY]) {
                continue;
            }
            if (inClosedSet[neighborX][neighborY]) {
                continue;
            }

            int moveCost = calculateMoveCost(direction);
            int tentativeGCost = gCost[currentX][currentY] + moveCost;

            if (tentativeGCost < gCost[neighborX][neighborY]) {
                cameFrom[neighborX][neighborY] = current;
                gCost[neighborX][neighborY] = tentativeGCost;

                int hCost = calculateOctileHeuristic(neighborX, neighborY, targetX, targetY);
                PathNode neighborNode = new PathNode(neighborX, neighborY, tentativeGCost, hCost);

                if (!inOpenSet[neighborX][neighborY]) {
                    openSet.add(neighborNode);
                    inOpenSet[neighborX][neighborY] = true;
                }
            }
        }
    }

    /**
     * Рассчитываем октильную эвристику для A*.
     *
     * Формула: h = STRAIGHT_COST * (dx + dy) + (DIAGONAL_COST - 2*STRAIGHT_COST) * min(dx, dy)
     * Упрощённо: h = 10 * max(dx, dy) + 4 * min(dx, dy)
     */
    private int calculateOctileHeuristic(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x1 - x2);
        int dy = Math.abs(y1 - y2);

        int minD = Math.min(dx, dy);
        int maxD = Math.max(dx, dy);

        return STRAIGHT_MOVE_COST * maxD + OCTILE_DIAGONAL_FACTOR * minD;
    }

    private int calculateMoveCost(int[] direction) {
        boolean isDiagonal = Math.abs(direction[0]) + Math.abs(direction[1]) == 2;
        return isDiagonal ? DIAGONAL_MOVE_COST : STRAIGHT_MOVE_COST;
    }

    private List<Edge> reconstructPath(
            PathNode[][] cameFrom,
            int endX, int endY,
            int startX, int startY) {

        List<Edge> path = new ArrayList<>();
        int currentX = endX;
        int currentY = endY;

        while (currentX != startX || currentY != startY) {
            path.add(new Edge(currentX, currentY));

            PathNode predecessor = cameFrom[currentX][currentY];
            if (predecessor == null) {
                return Collections.emptyList();
            }

            currentX = predecessor.x;
            currentY = predecessor.y;
        }

        path.add(new Edge(startX, startY));

        Collections.reverse(path);

        return path;
    }


    private boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < FIELD_WIDTH && y >= 0 && y < FIELD_HEIGHT;
    }

    private static class PathNode {
        final int x;
        final int y;
        final int gCost;  // Стоимость от старта до этого узла
        final int hCost;  // Эвристическая оценка до цели
        final int fCost;  // Полная стоимость: g + h

        PathNode(int x, int y, int gCost, int hCost) {
            this.x = x;
            this.y = y;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PathNode other = (PathNode) obj;
            return x == other.x && y == other.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }
}