package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.SuitableForAttackUnitsFinder;

import java.util.*;

public class SuitableForAttackUnitsFinderImpl implements SuitableForAttackUnitsFinder {

    @Override
    public List<Unit> getSuitableUnits(List<List<Unit>> unitsByRow, boolean isLeftArmyTarget) {
        List<Unit> suitableUnits = new ArrayList<>();

        for (List<Unit> row : unitsByRow) {
            if (row == null || row.isEmpty()) {
                continue;
            }

            Unit suitableUnit = findSuitableUnitInRow(row, isLeftArmyTarget);

            if (suitableUnit != null) {
                suitableUnits.add(suitableUnit);
            }
        }

        return suitableUnits;
    }

    private Unit findSuitableUnitInRow(List<Unit> row, boolean isLeftArmyTarget) {
        List<Unit> sortedRow = new ArrayList<>(row);
        sortedRow.sort(Comparator.comparingInt(Unit::getyCoordinate));

        if (isLeftArmyTarget) {
            return findFirstAliveFromEnd(sortedRow);
        } else {
            return findFirstAliveFromStart(sortedRow);
        }
    }

    private Unit findFirstAliveFromStart(List<Unit> sortedRow) {
        for (Unit unit : sortedRow) {
            if (unit.isAlive()) {
                return unit;
            }
        }
        return null;
    }

    private Unit findFirstAliveFromEnd(List<Unit> sortedRow) {
        for (int i = sortedRow.size() - 1; i >= 0; i--) {
            Unit unit = sortedRow.get(i);
            if (unit.isAlive()) {
                return unit;
            }
        }
        return null;
    }
}