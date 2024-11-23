package winspace;

import robocode.*;
import java.awt.*;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class ÍRIS extends AdvancedRobot {

    private double enemyEnergy = 100;
    private double moveDirection = 1;
    private ArrayList<Wave> enemyWaves = new ArrayList<>();
    private static final double MAX_BULLET_POWER = 3.0;

    @Override
    public void run() {
        // Configurações do robô
        setColors(Color.BLACK, Color.RED, Color.ORANGE);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true) {
            // Gira o radar
            setTurnRadarRight(360);
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        // Rastrear inimigo e calcular posição futura
        double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
        double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
        double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);

        // Detectar tiro e criar uma onda inimiga
        double changeInEnergy = enemyEnergy - e.getEnergy();
        if (changeInEnergy > 0 && changeInEnergy <= MAX_BULLET_POWER) {
            Wave wave = new Wave(this, enemyX, enemyY, changeInEnergy);
            enemyWaves.add(wave);
        }
        enemyEnergy = e.getEnergy();

        // Wave Surfing
        surfWaves();

        // Mira preditiva otimizada
        double bulletPower = Math.min(MAX_BULLET_POWER, Math.min(3, getEnergy() / 6));
        double bulletSpeed = 20 - 3 * bulletPower;
        double timeToTarget = e.getDistance() / bulletSpeed;

        double predictedX = enemyX + e.getVelocity() * timeToTarget * Math.sin(e.getHeadingRadians());
        double predictedY = enemyY + e.getVelocity() * timeToTarget * Math.cos(e.getHeadingRadians());

        double angleToTarget = Math.atan2(predictedX - getX(), predictedY - getY());

        // Girar a arma e atirar apenas se o alvo for certeiro
        setTurnGunRightRadians(Utils.normalRelativeAngle(angleToTarget - getGunHeadingRadians()));
        if (getGunHeat() == 0 && getEnergy() > bulletPower) {
            if (Math.abs(getGunTurnRemainingRadians()) < Math.toRadians(10)) { // Atire apenas se a mira estiver perto
                setFire(bulletPower);
            }
        }

        // Continua rastreando as ondas dos inimigos
        setTurnRadarRightRadians(Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians()) * 2);
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        // Movimenta rapido para desviar depois de ser atingido
        setTurnRight(90 - e.getBearing());
        setAhead(100 * moveDirection);
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        // Fica longe de paredes
        moveDirection *= -1;
        setAhead(100 * moveDirection);
    }

    private void surfWaves() {
        if (enemyWaves.isEmpty()) return;

        Wave closestWave = enemyWaves.get(0);

        // Calcular o ponto de menor risco, baseado na onda mais próxima
        double safestDistance = Double.MAX_VALUE;
        double bestAngle = 0;

        for (int angle = -90; angle <= 90; angle += 10) { // Testa alguns ângulos em volta do meu robô
            double testX = getX() + Math.sin(Math.toRadians(angle)) * 100;
            double testY = getY() + Math.cos(Math.toRadians(angle)) * 100;

            if (isOutOfBounds(testX, testY)) continue;

            double distance = closestWave.getDistance(testX, testY);
            if (distance < safestDistance) {
                safestDistance = distance;
                bestAngle = angle;
            }
        }

        // Movimentar meu robot para o lugar mais seguro
        setTurnRight(bestAngle);
        setAhead(100 * moveDirection);
    }

    private boolean isOutOfBounds(double x, double y) {
        return x < 18 || y < 18 || x > getBattleFieldWidth() - 18 || y > getBattleFieldHeight() - 18;
    }

    // Classe Wave que eu criei para representar as balas inimigas
    private static class Wave {
        private final double startX, startY, bulletSpeed;
        private final long creationTime;

        public Wave(Robot robot, double x, double y, double bulletPower) {
            this.startX = x;
            this.startY = y;
            this.bulletSpeed = 20 - 3 * bulletPower;
            this.creationTime = robot.getTime();
        }

        public double getDistance(double x, double y) {
            double timeElapsed = System.currentTimeMillis() - creationTime;
            double waveRadius = bulletSpeed * timeElapsed;

            return Math.abs(Point2D.distance(startX, startY, x, y) - waveRadius);
        }
    }
}