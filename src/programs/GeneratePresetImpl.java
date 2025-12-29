package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.GeneratePreset;

import java.util.*;

public class GeneratePresetImpl implements GeneratePreset {

    private static final int COMPUTER_ARMY_START_X = 24;

    private static final int COMPUTER_ARMY_START_Y = 10;

    private static final int ARMY_COLUMNS = 3;

    private static final int MIN_Y_COORDINATE = 0;

    private static final int MAX_Y_COORDINATE = 20;

    private static final int MAX_UNITS_PER_TYPE = 11;

    private static final double ATTACK_WEIGHT = 0.7;

    private static final double HEALTH_WEIGHT = 0.3;

    private static final double ATTACK_BONUS_MULTIPLIER = 1.2;


    @Override
    public Army generate(List<Unit> unitList, int maxPoints) {
        if (unitList == null || unitList.isEmpty() || maxPoints <= 0) {
            return createEmptyArmy();
        }

        List<UnitEfficiency> efficiencies = calculateEfficiencies(unitList);
        efficiencies.sort((a, b) -> Double.compare(b.efficiency, a.efficiency));

        List<Unit> armyUnits = new ArrayList<>();
        int remainingPoints = maxPoints;
        int totalCost = 0;

        for (UnitEfficiency ue : efficiencies) {
            Unit template = ue.unit;
            int unitCost = template.getCost();

            int maxAffordable = remainingPoints / unitCost;
            int actualCount = Math.min(MAX_UNITS_PER_TYPE, maxAffordable);

            for (int i = 0; i < actualCount; i++) {
                Unit newUnit = createUnitWithPosition(template, armyUnits.size() + 1);
                armyUnits.add(newUnit);
                remainingPoints -= unitCost;
                totalCost += unitCost;
            }

            if (remainingPoints <= 0) {
                break;
            }
        }

        Army army = new Army(armyUnits);
        army.setPoints(totalCost);
        return army;
    }

    private List<UnitEfficiency> calculateEfficiencies(List<Unit> unitList) {
        List<UnitEfficiency> result = new ArrayList<>();

        for (Unit unit : unitList) {
            double efficiency = calculateUnitEfficiency(unit);
            result.add(new UnitEfficiency(unit, efficiency));
        }

        return result;
    }

    private double calculateUnitEfficiency(Unit unit) {
        int cost = unit.getCost();
        if (cost == 0) {
            return 0.0;
        }

        double attackRatio = (double) unit.getBaseAttack() / cost;
        double healthRatio = (double) unit.getHealth() / cost;

        double baseEfficiency = ATTACK_WEIGHT * attackRatio + HEALTH_WEIGHT * healthRatio;
        double bonusMultiplier = hasAttackBonuses(unit) ? ATTACK_BONUS_MULTIPLIER : 1.0;

        return baseEfficiency * bonusMultiplier;
    }

    private boolean hasAttackBonuses(Unit unit) {
        Map<String, Double> bonuses = unit.getAttackBonuses();
        return bonuses != null && !bonuses.isEmpty();
    }


    private Unit createUnitWithPosition(Unit template, int index) {
        int gridX = COMPUTER_ARMY_START_X + ((index - 1) % ARMY_COLUMNS);
        int gridY = COMPUTER_ARMY_START_Y + ((index - 1) / ARMY_COLUMNS);

        gridY = Math.max(MIN_Y_COORDINATE, Math.min(gridY, MAX_Y_COORDINATE));

        Map<String, Double> attackBonuses = copyMap(template.getAttackBonuses());
        Map<String, Double> defenceBonuses = copyMap(template.getDefenceBonuses());

        String unitName = template.getUnitType() + " " + index;

        return new Unit(
                unitName,
                template.getUnitType(),
                template.getHealth(),
                template.getBaseAttack(),
                template.getCost(),
                template.getAttackType(),
                attackBonuses,
                defenceBonuses,
                gridX,
                gridY
        );
    }

    private Map<String, Double> copyMap(Map<String, Double> original) {
        return original != null ? new HashMap<>(original) : new HashMap<>();
    }

    private Army createEmptyArmy() {
        Army army = new Army(new ArrayList<>());
        army.setPoints(0);
        return army;
    }

    private static class UnitEfficiency {
        final Unit unit;
        final double efficiency;

        UnitEfficiency(Unit unit, double efficiency) {
            this.unit = unit;
            this.efficiency = efficiency;
        }
    }
}