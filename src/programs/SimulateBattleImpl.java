package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.PrintBattleLog;
import com.battle.heroes.army.programs.SimulateBattle;

import java.util.*;

public class SimulateBattleImpl implements SimulateBattle {

    private static final int DELAY_BETWEEN_TURNS_MS = 10;

    private static final int DELAY_BETWEEN_ROUNDS_MS = 100;

    private static final String BATTLE_END_SEPARATOR = "\n=== БОЙ ЗАВЕРШЕН ===";

    private static final String PLAYER_WINS_MESSAGE = "ПОБЕДИЛ ИГРОК!";

    private static final String COMPUTER_WINS_MESSAGE = "ПОБЕДИЛ КОМПЬЮТЕР!";

    private static final String DRAW_MESSAGE = "НИЧЬЯ!";

    private PrintBattleLog printBattleLog;

    public void setPrintBattleLog(PrintBattleLog printBattleLog) {
        this.printBattleLog = printBattleLog;
    }

    @Override
    public void simulate(Army playerArmy, Army computerArmy) throws InterruptedException {
        List<Unit> playerUnits = new ArrayList<>(playerArmy.getUnits());
        List<Unit> computerUnits = new ArrayList<>(computerArmy.getUnits());

        int roundNumber = 1;

        while (hasAliveUnits(playerUnits) && hasAliveUnits(computerUnits)) {
            executeRound(playerUnits, computerUnits);
            roundNumber++;
            Thread.sleep(DELAY_BETWEEN_ROUNDS_MS);
        }

        announceWinner(playerUnits, computerUnits);
    }


    private void executeRound(List<Unit> playerUnits, List<Unit> computerUnits)
            throws InterruptedException {

        List<Unit> allAliveUnits = collectAliveUnits(playerUnits, computerUnits);

        sortByTurnPriority(allAliveUnits);

        for (Unit unit : allAliveUnits) {
            // Пропускаем мёртвых (могли погибнуть в этом раунде)
            if (!unit.isAlive()) {
                continue;
            }

            Unit target = unit.getProgram().attack();

            if (target != null && printBattleLog != null) {
                printBattleLog.printBattleLog(unit, target);
            }

            if (!hasAliveUnits(playerUnits) || !hasAliveUnits(computerUnits)) {
                break;
            }

            Thread.sleep(DELAY_BETWEEN_TURNS_MS);
        }
    }

    private List<Unit> collectAliveUnits(List<Unit> playerUnits, List<Unit> computerUnits) {
        List<Unit> result = new ArrayList<>();

        for (Unit unit : playerUnits) {
            if (unit.isAlive()) {
                result.add(unit);
            }
        }

        for (Unit unit : computerUnits) {
            if (unit.isAlive()) {
                result.add(unit);
            }
        }

        return result;
    }

    private void sortByTurnPriority(List<Unit> units) {
        units.sort((u1, u2) -> {
            int attackComparison = Integer.compare(u2.getBaseAttack(), u1.getBaseAttack());
            if (attackComparison != 0) {
                return attackComparison;
            }

            return Integer.compare(u2.getHealth(), u1.getHealth());
        });
    }

    private boolean hasAliveUnits(List<Unit> units) {
        for (Unit unit : units) {
            if (unit.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private void announceWinner(List<Unit> playerUnits, List<Unit> computerUnits) {
        boolean playerSurvived = hasAliveUnits(playerUnits);
        boolean computerSurvived = hasAliveUnits(computerUnits);

        System.out.println(BATTLE_END_SEPARATOR);

        if (playerSurvived && !computerSurvived) {
            System.out.println(PLAYER_WINS_MESSAGE);
        } else if (!playerSurvived && computerSurvived) {
            System.out.println(COMPUTER_WINS_MESSAGE);
        } else {
            System.out.println(DRAW_MESSAGE);
        }
    }
}